package com.neotreks.accuterra.mobile.demo.trip.online

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.demo.DemoApplication
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.ActivityOnlineTripBinding
import com.neotreks.accuterra.mobile.demo.extensions.getLikeIconResource
import com.neotreks.accuterra.mobile.demo.extensions.getLocationLabelString
import com.neotreks.accuterra.mobile.demo.extensions.toLocalDateTimeString
import com.neotreks.accuterra.mobile.demo.longToast
import com.neotreks.accuterra.mobile.demo.toast
import com.neotreks.accuterra.mobile.demo.trip.recorded.TripSaveActivity
import com.neotreks.accuterra.mobile.demo.ui.OnListItemLongClickListener
import com.neotreks.accuterra.mobile.demo.ui.ProgressDialogHolder
import com.neotreks.accuterra.mobile.demo.user.DemoIdentityManager
import com.neotreks.accuterra.mobile.demo.util.CrashSupport
import com.neotreks.accuterra.mobile.demo.util.DialogUtil
import com.neotreks.accuterra.mobile.demo.util.visibility
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.ugc.model.PostTripCommentRequest
import com.neotreks.accuterra.mobile.sdk.ugc.model.TripComment
import kotlinx.coroutines.runBlocking

class OnlineTripActivity : AppCompatActivity(), ViewModelStoreOwner {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "OnlineTripActivity"
        private const val KEY_TRIP_UUID = "TRIP_UUID"
        private const val KEY_TAB_INDEX = "TAB_INDEX"

        const val RESULT_DELETED = 100
        const val RESULT_COMMENTS_COUNT_UPDATED = 101
        const val RESULT_EDIT_START = 110

        fun createNavigateToIntent(context: Context, uuid: String, tabIndex: OnlineTripTabDefinition? = null): Intent {
            return Intent(context, OnlineTripActivity::class.java)
                .apply {
                    putExtra(KEY_TRIP_UUID, uuid)
                    tabIndex?.position?.let { position ->
                        putExtra(KEY_TAB_INDEX, position)
                    }
                }
        }
    }

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var viewModel: OnlineTripViewModel

    private lateinit var binding: ActivityOnlineTripBinding

    private lateinit var commentAdapter: OnlineTripCommentAdapter

    private var dialogHolder = ProgressDialogHolder()

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnlineTripBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViewModel()
        setupToolbar()
        setupFragments()
        setupLikes()
        setupComments()
        setupObservers()

        parseIntent()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        if (menu != null) {
            menuInflater.inflate(R.menu.menu_trip_online, menu)
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        if (menu != null) {
            val isCurrentUserTrip = isCurrentUserTrip()
            menu.findItem(R.id.trip_online_menu_item_trip_edit).isEnabled  = isCurrentUserTrip
            menu.findItem(R.id.trip_online_menu_item_trip_promote).isEnabled  = isCurrentUserTrip
            menu.findItem(R.id.trip_online_menu_item_trip_delete).isEnabled  = isCurrentUserTrip
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.trip_online_menu_item_trip_edit -> {
                onEditTrip()
                return true
            }
            R.id.trip_online_menu_item_trip_delete -> {
                onDeleteTrip()
                return true
            }
            R.id.trip_online_menu_item_trip_promote -> {
                onPromoteTrip()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as DemoApplication).onlineTripVMStore.clear()
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun initViewModel() {
        // Please note it is taken from ViewModelStore because it is shared between more activities
        val viewModelStore = (application as DemoApplication).onlineTripVMStore
        viewModel = ViewModelProvider(viewModelStore, defaultViewModelProviderFactory).get(OnlineTripViewModel::class.java)
        viewModel.reset()
    }

    private fun parseIntent() {
        // Parse + load trip ID
        require(intent.hasExtra(KEY_TRIP_UUID)) { "No intent key $KEY_TRIP_UUID provided." }

        val tripUuid = intent.getStringExtra(KEY_TRIP_UUID)
        checkNotNull(tripUuid) { "Missing $KEY_TRIP_UUID value." }
        check(tripUuid.isNotBlank()) { "Invalid $KEY_TRIP_UUID value." }

        val tabPosition = intent.getIntExtra(KEY_TAB_INDEX, -1)

        lifecycleScope.launchWhenCreated {
            viewModel.tripUuid = tripUuid
            viewModel.loadTrip(this@OnlineTripActivity, tripUuid)
            // Try to switch to the default tab index
            if (tabPosition > 0) {
                binding.activityOnlineTripViewPager.setCurrentItem(tabPosition, true)
            }
            // Load also comments
            viewModel.loadTripComments(this@OnlineTripActivity, tripUuid)
        }
    }

    private fun setupObservers() {
        // Trip Observer
        viewModel.trip.observe(this, Observer { trip ->
            if (trip == null) {
                return@Observer
            }
            // Header
            binding.activityOnlineTripUserName.text = trip.userInfo.driverId
            binding.activityOnlineTripDate.text = trip.info.tripStart.toLocalDateTimeString()
            // Name + Location
            binding.activityOnlineTripTripName.text = trip.info.name
            trip.location.let { location ->
                val locationString = location.getLocationLabelString()
                binding.activityOnlineTripTripLocation.text = locationString
            }
            // Related trail name
            val trailId = trip.info.trailId
            if (trailId != null) {
                lifecycleScope.launchWhenCreated {
                    val service = ServiceFactory.getTrailService(this@OnlineTripActivity)
                    val trailBasicInfo = service.getTrailBasicInfoById(trailId)
                    trailBasicInfo?.name?.let { trailName ->
                        val trailNameText = getString(R.string.activity_online_trip_trail_with_name, trailName)
                        binding.activityOnlineTripRelatedTrail.visibility = true.visibility
                        binding.activityOnlineTripRelatedTrail.text = trailNameText
                    }
                }
            } else {
                binding.activityOnlineTripRelatedTrail.visibility = false.visibility
            }
            // Description
            binding.activityOnlineTripDescription.text = trip.info.description
            // Comments
            binding.activityOnlineTripComments.text = this.getString(R.string.general_comments_number, trip.commentsCount)
        })
        // Comments Observer
        viewModel.comments.observe(this, { comments ->
            commentAdapter.setItems(comments ?: listOf())
        })
        // User likes
        viewModel.userData.observe(this, Observer { userData ->
            if (userData == null) {
                return@Observer
            }
            binding.activityOnlineTripLikes.text = this.getString(R.string.general_likes_number, userData.likes)
            binding.activityOnlineTripLikesIcon.setImageResource(getLikeIconResource(userData.userLike))
        })
    }

    private fun setupFragments() {
        binding.activityOnlineTripViewPager.adapter = OnlineTripPageAdapter(
            supportFragmentManager,
            this
        )
    }

    private fun setupComments() {
        // Comments Adapter
        commentAdapter = OnlineTripCommentAdapter(this, mutableListOf())
        commentAdapter.setOnListItemLongClickListener(object : OnListItemLongClickListener<TripComment> {
            override fun onListItemLongClick(item: TripComment, view: View) {
                showCommentItemMenu(item, view)
            }
        })
        binding.activityOnlineTripCommentsList.adapter = commentAdapter
        // Comments sending
        binding.activityOnlineTripCommentsAddIcon.setOnClickListener {
            onAddCommentIconClicked()
        }
    }

    private fun showCommentItemMenu(comment: TripComment, view: View) {
        val menu = PopupMenu(this, view)
        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_edit_list_item_edit -> {
                    onEditTripComment(comment)
                }
                R.id.menu_edit_list_item_delete -> {
                    onDeleteTripComment(comment)
                }
            }
            true
        }
        menu.inflate(R.menu.menu_edit_list_item)
        menu.show()
    }

    private fun onAddCommentIconClicked() {
        if (!viewModel.isTripLoaded()) {
            return
        }
        // Build and display `Add Comment` dialog
        DialogUtil.buildInputDialog(this,
            title = getString(R.string.general_comment_add),
            positiveCode = { commentText ->
                val tripUuid = viewModel.tripUuid
                val comment = PostTripCommentRequest.build(tripUuid, commentText, null)
                postComment(comment)
            },
            hint = getString(R.string.general_comment_hint_comment_text),
            positiveCodeLabel = getString(R.string.general_comment_send),
            negativeCodeLabel = getString(R.string.cancel)
        ).show()
    }

    /**
     * Post a comment for given [PostTripCommentRequest]
     */
    private fun postComment(commentRequest: PostTripCommentRequest) {
        dialogHolder.displayProgressDialog(this, getString(R.string.trips_updating_trip))

        lifecycleScope.launchWhenCreated {
            try {
                val service = ServiceFactory.getTripService(this@OnlineTripActivity)
                val tripUuid = viewModel.tripUuid
                val result = service.postTripComment(commentRequest)
                if (result.isSuccess) {
                    // Let's reload comments
                    viewModel.loadTripComments(this@OnlineTripActivity, tripUuid)
                    setResult(RESULT_COMMENTS_COUNT_UPDATED)
                    viewModel.updateTripCommentsCount(result.value?.commentsCount)
                } else {
                    longToast("Cannot add a comment because of: ${result.errorMessage}")
                    CrashSupport.reportError(result, commentRequest.toString())
                }
            } catch (e: Exception) {
                longToast("Cannot add a comment because of: ${e.localizedMessage}")
                CrashSupport.reportError(e, commentRequest.toString())
            } finally {
                dialogHolder.hideProgressDialog()
            }
        }

    }

    private fun onEditTripComment(comment: TripComment) {
        val tripUuid = viewModel.tripUuid
        if (tripUuid.isBlank()) {
            return
        }
        // Build and display `Edit Comment` dialog
        DialogUtil.buildInputDialog(
            this,
            title = getString(R.string.general_comment_edit),
            positiveCode = { newText ->
                val updatedComment = comment.copyWithText(newText)
                val request = PostTripCommentRequest.build(updatedComment)
                postComment(request)
            },
            hint = getString(R.string.general_comment_hint_comment_text),
            defaultValue = comment.text,
            positiveCodeLabel = getString(R.string.general_comment_update),
            negativeCodeLabel = getString(R.string.cancel)
        ).show()
    }

    private fun onDeleteTripComment(comment: TripComment) {
        val tripUuid = viewModel.tripUuid
        if (tripUuid.isBlank()) {
            return
        }
        // Build and display `Delete Comment` dialog
        DialogUtil.buildYesNoDialog(
            this,
            title = getString(R.string.general_comment_delete),
            message = getString(R.string.general_comment_delete_dialog_message),
            positiveCode = {
                deleteComment(comment)
            },
            positiveCodeLabel = getString(R.string.general_delete),
            negativeCodeLabel = getString(R.string.general_cancel)
        ).show()
    }

    /**
     * Deletes passed [comment]
     */
    private fun deleteComment(comment: TripComment) {
        dialogHolder.displayProgressDialog(this, getString(R.string.trips_updating_trip))
        val commentUuid = comment.commentUuid

        lifecycleScope.launchWhenCreated {
            try {
                val service = ServiceFactory.getTripService(this@OnlineTripActivity)
                val result = service.deleteTripComment(commentUuid)
                if (result.isSuccess) {
                    // Let's reload comments
                    viewModel.loadTripComments(this@OnlineTripActivity, comment.tripUuid)
                    viewModel.updateTripCommentsCount(result.value?.commentsCount)
                    setResult(RESULT_COMMENTS_COUNT_UPDATED)
                } else {
                    longToast("Cannot delete a comment $commentUuid because of: ${result.errorMessage}")
                    CrashSupport.reportError(result, comment.toString())
                }
            } catch (e: Exception) {
                longToast("Cannot delete a comment $commentUuid because of: ${e.localizedMessage}")
                CrashSupport.reportError(e, comment.toString())
            } finally {
                dialogHolder.hideProgressDialog()
            }
        }
    }

    private fun setupLikes() {
        binding.activityOnlineTripLikesIcon.setOnClickListener {
            onLikesClicked()
        }
        binding.activityOnlineTripLikes.setOnClickListener {
            onLikesClicked()
        }
    }

    private fun onLikesClicked() {
        val service = ServiceFactory.getTripService(this)
        lifecycleScope.launchWhenCreated {
            val tripUuid = viewModel.tripUuid
            dialogHolder.displayProgressDialog(this@OnlineTripActivity, getString(R.string.trips_updating_trip))
            try {
                val trip = viewModel.trip.value
                if (trip == null) {
                    toast(getString(R.string.activity_online_trip_trip_not_loaded))
                    return@launchWhenCreated
                }
                val liked = !(viewModel.userData.value?.userLike ?: false)
                val response = service.setTripLiked(tripUuid, liked)
                if (response.isSuccess) {
                    response.value?.let { likes ->
                        viewModel.setUserLike(likes)
                    }
                } else {
                    val text = getString(
                        R.string.trips_trip_update_failed,
                        response.errorMessage ?: "Unknown error"
                    )
                    longToast(text)
                    CrashSupport.reportError(response)
                }
            } catch (e: Exception) {
                val message = "Error while liking the trip: $tripUuid"
                Log.e(TAG, message, e)
                CrashSupport.reportError(e, message)
            } finally {
                dialogHolder.hideProgressDialog()
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.activityOnlineTripToolbar.generalToolbar)
        binding.activityOnlineTripToolbar.generalToolbarTitle.text = getString(R.string.general_trip)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
    }

    private fun isCurrentUserTrip(): Boolean {
        val tripUserUuid = viewModel.trip.value?.userInfo?.driverId
        val currentUserUuid = runBlocking { DemoIdentityManager().getUserId(this@OnlineTripActivity) }
        return currentUserUuid == tripUserUuid
    }

    private fun onEditTrip() {
        DialogUtil.buildYesNoDialog(
            context = this,
            title = getString(R.string.activity_online_trip_edit_trip_title),
            message = getString(R.string.activity_online_trip_edit_trip_message),
            positiveCodeLabel = getString(R.string.general_edit),
            negativeCodeLabel = getString(R.string.general_cancel),
            positiveCode = {
                editTrip()
            }
        ).show()
    }

    private fun editTrip() {
        val dialog = DialogUtil.buildBlockingProgressDialog(
            context = this,
            title = getString(R.string.activity_online_trip_editing_trip)
        )
        dialog.show()
        lifecycleScope.launchWhenCreated {
            val service = ServiceFactory.getTripRecordingService(this@OnlineTripActivity)
            val trip = viewModel.trip.value
            if (trip == null) {
                toast(getString(R.string.activity_online_trip_edit_trip_is_null))
            } else {
                // Start trip editing - this will convert given `Trip` into `TripRecording`
                val result = service.startTripEditing(trip)
                dialog.dismiss()
                // Check if we were able to convert the `Trip` into `TripRecording`
                if (result.isSuccess) {
                    // We were able to convert the `Trip` into `TripRecording`
                    toast(getString(R.string.activity_online_trip_edit_success))
                    setResult(RESULT_EDIT_START)
                    // Navigate to trip editing screen
                    val intent = TripSaveActivity.createNavigateToIntent(this@OnlineTripActivity, trip.info.uuid)
                    startActivity(intent)
                    // Close the screen
                    finish()
                } else {
                    // We were not able to convert the `Trip` into `TripRecording`
                    val resultValue = result.value
                    longToast(getString(R.string.activity_online_trip_edit_failed, resultValue?.name ?: "Unknown"))
                    CrashSupport.reportError(result, "Trip UUID: ${viewModel.trip.value?.info}")
                }
            }
        }
    }

    private fun onDeleteTrip() {
        DialogUtil.buildYesNoDialog(
            context = this,
            title = getString(R.string.activity_online_trip_delete_trip_title),
            message = getString(R.string.activity_online_trip_delete_trip_message, viewModel.trip.value?.info?.name),
            positiveCodeLabel = getString(R.string.general_delete),
            negativeCodeLabel = getString(R.string.general_cancel),
            positiveCode = {
                deleteTrip()
            }
        ).show()
    }

    private fun deleteTrip() {
        val tripUuid = viewModel.trip.value?.info?.uuid
            ?: return
        val dialog = DialogUtil.buildBlockingProgressDialog(
            context = this,
            title = getString(R.string.activity_online_trip_deleting_trip)
        )
        dialog.show()

        lifecycleScope.launchWhenCreated {
            val service = ServiceFactory.getTripService(this@OnlineTripActivity)
            val result = service.deleteTrip(tripUuid)
            dialog.dismiss()
            if (result.isSuccess) {
                toast(getString(R.string.activity_online_trip_delete_success))
                setResult(RESULT_DELETED)
                finish() // Close the screen
            } else {
                longToast(getString(R.string.activity_online_trip_delete_failed, result.errorMessage ?: "Unknown"))
                CrashSupport.reportError(result, "Trip UUID: $tripUuid")
            }
        }
    }

    private fun onPromoteTrip() {
        DialogUtil.buildYesNoDialog(
            context = this,
            title = getString(R.string.activity_online_trip_promote_trip_title),
            message = getString(R.string.activity_online_trip_promote_trip_message, viewModel.trip.value?.info?.name),
            positiveCodeLabel = getString(R.string.general_promote),
            negativeCodeLabel = getString(R.string.general_cancel),
            positiveCode = {
                promoteTrip()
            }
        ).show()
    }

    private fun promoteTrip() {
        val tripUuid = viewModel.trip.value?.info?.uuid
            ?: return
        val dialog = DialogUtil.buildBlockingProgressDialog(
            context = this,
            title = getString(R.string.activity_online_trip_promoting_trip)
        )
        dialog.show()

        lifecycleScope.launchWhenCreated {
            val service = ServiceFactory.getTripService(this@OnlineTripActivity)
            val result = service.promoteTripToTrail(tripUuid)
            dialog.dismiss()
            if (result.isSuccess) {
                longToast(getString(R.string.activity_online_trip_promote_success, result.value?.promotionState?.getName() ?: "Unknown"))
            } else {
                longToast(getString(R.string.activity_online_trip_promote_failed, result.errorMessage ?: "Unknown"))
                CrashSupport.reportError(result, "Trip UUID: $tripUuid")
            }
        }

    }

}