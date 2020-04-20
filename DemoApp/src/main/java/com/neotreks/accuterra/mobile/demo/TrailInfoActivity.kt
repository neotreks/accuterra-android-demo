package com.neotreks.accuterra.mobile.demo

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.neotreks.accuterra.mobile.demo.settings.Formatter
import com.neotreks.accuterra.mobile.demo.util.EnumUtil
import com.neotreks.accuterra.mobile.demo.util.RandomUtil
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trail.model.Trail
import com.neotreks.accuterra.mobile.sdk.trail.service.IMediaService
import kotlinx.android.synthetic.main.activity_trail_info.*
import kotlinx.android.synthetic.main.image_carousel_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.math.round


class TrailInfoActivity : AppCompatActivity() {

    companion object {

        private const val TAG = "TrailInfoActivity"

        private const val KEY_TRAIL_ID = "TRAIL_ID"

        fun createNavigateToIntent(context: Context, trailId: Long): Intent {
            return Intent(context, TrailInfoActivity::class.java)
                .apply {
                    putExtra(KEY_TRAIL_ID, trailId)
                }
        }
    }

    private lateinit var viewModel: TrailInfoViewModel

    private lateinit var imageAdapter: PictureAdapter

    private lateinit var mediaService: IMediaService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trail_info)
        //setSupportActionBar(activity_trail_info_toolbar)

        viewModel = ViewModelProvider(this).get(TrailInfoViewModel::class.java)
        viewModel.loadTechRatings(this)
        mediaService = ServiceFactory.getMediaService(this)

        parseIntent()

        setupToolbar()
        setupTabs()
        setupImageList()
        setupButtons()

        loadTrail()
        loadImages()
    }

    private fun setupToolbar() {
        activity_trail_info_back_button.setOnClickListener {
            onBackPressed()
        }

        /*
        Could not figure out how to show only the back arrow and do not make the toolbar to cast shadow
        So for now replaced with a custom image button.
        TODO Honza: Remove the image button and gix the toolbar to not cast the shadow

        supportActionBar!!.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }*/
    }

    private fun setupTabs() {
        activity_trail_discovery_tabs_content_view_pager.adapter =
            TrailInfoPagerAdapter(supportFragmentManager, this, viewModel.trailId)
    }

    private fun setupImageList() {
        imageAdapter = PictureAdapter(viewModel.imageS3Keys, mediaService, lifecycleScope, this)
        activity_trail_info_image_carousel.adapter = imageAdapter
    }

    private fun setupButtons() {
        activity_trail_info_get_start_icon.setOnClickListener {
            startActivity(DrivingActivity.createNavigateToIntent(this, viewModel.trailId))
        }

        activity_trail_info_get_download_icon.setOnClickListener {
            toast("TODO: Download trail")
        }

        activity_trail_info_get_saved_icon.setOnClickListener {
            toast("TODO: Mark trail as saved")
        }

        activity_trail_info_get_there_icon.setOnClickListener {
            toast("TODO: Navigate to trail")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun parseIntent() {
        require(intent.hasExtra(KEY_TRAIL_ID)) { "No intent key $KEY_TRAIL_ID provided." }

        viewModel.trailId = intent.getLongExtra(KEY_TRAIL_ID, Long.MIN_VALUE)
        require(viewModel.trailId != Long.MIN_VALUE) { "Invalid $KEY_TRAIL_ID value." }
    }

    private fun loadTrail() {
        lifecycleScope.launchWhenCreated {
            viewModel.loadTrail(viewModel.trailId, this@TrailInfoActivity)
            viewModel.trail.observe(this@TrailInfoActivity, Observer { trail ->
                lifecycleScope.launchWhenResumed {
                    if (trail != null) {
                        showTrail(trail)
                    }
                }
            })
        }
    }

    private fun loadImages() {
        lifecycleScope.launchWhenCreated {
            Log.i(TAG, "Loading images start")
            // Images hardcoded for now since we do not have media imported into the trail DB
            val newImages = listOf(
                "s3://accuterra-trail-data-dev/trail_media/test-media-01.png",
                "s3://accuterra-trail-data-dev/trail_media/test-media-02.png",
                "s3://accuterra-trail-data-dev/trail_media/test-media-03.png",
                "s3://accuterra-trail-data-dev/trail_media/test-media-04.png",
                "s3://accuterra-trail-data-dev/trail_media/test-media-05.png",
                "s3://accuterra-trail-data-dev/trail_media/test-media-06.png",
                "s3://accuterra-trail-data-dev/trail_media/test-media-07.png",
                "s3://accuterra-trail-data-dev/trail_media/test-media-08.png",
                "s3://accuterra-trail-data-dev/trail_media/test-media-09.png",
                "s3://accuterra-trail-data-dev/trail_media/test-media-10.png",
                "s3://accuterra-trail-data-dev/trail_media/test-media-11.png",
                "s3://accuterra-trail-data-dev/trail_media/test-media-12.png",
                "s3://accuterra-trail-data-dev/trail_media/test-media-13.jpg",
                "s3://accuterra-trail-data-dev/trail_media/test-media-14.jpg",
                "s3://accuterra-trail-data-dev/trail_media/test-media-16.jpg",
                "s3://accuterra-trail-data-dev/trail_media/test-media-17.jpg",
                "s3://accuterra-trail-data-dev/trail_media/test-media-18.jpg",
                "s3://accuterra-trail-data-dev/trail_media/test-media-19.jpg",
                "s3://accuterra-trail-data-dev/trail_media/test-media-20.jpg",
                "s3://accuterra-trail-data-dev/trail_media/test-media-21.jpg",
                "s3://accuterra-trail-data-dev/trail_media/test-media-22.jpg"
            )
            showImages(RandomUtil.getRandomList(newImages, 15))
        }
    }

    @UiThread
    fun showImages(newImages: List<String>) {
        Log.i(TAG, "showImages: start")
        viewModel.imageS3Keys.clear()
        viewModel.imageS3Keys.addAll(newImages)

        Log.i(TAG, "showImages: notifyDataSetChanged")
        imageAdapter.notifyDataSetChanged()
        Log.i(TAG, "showImages: end")
    }

    @UiThread
    private suspend fun showTrail(trail: Trail) {
        withContext(Dispatchers.Main) {
            activity_trail_info_title.text = trail.info.name
            activity_trail_info_length.text = Formatter.getDistanceFormatter()
                .formatDistance(this@TrailInfoActivity, trail.statistics.length * 1000)

            if (trail.userRating == null) {
                activity_trail_info_rating_value.visibility = View.GONE
                activity_trail_info_rating_stars.visibility = View.GONE
                activity_trail_info_ratings_count.visibility = View.GONE
            } else {
                val userRating = trail.userRating!!
                val rating = round(userRating.rating * 10) / 10.0

                activity_trail_info_rating_value.text = rating.toString()
                activity_trail_info_rating_stars.rating = rating.toFloat()
                activity_trail_info_ratings_count.text =
                    getString(R.string.rating_count_number, userRating.ratingCount)

                activity_trail_info_no_ratings.visibility = View.GONE
            }

            val techCode = trail.technicalRating.high.code
            val techRating = EnumUtil.getTechRatingForCode(techCode, this@TrailInfoActivity)
            activity_trail_info_difficulty.text = techRating!!.name
        }
    }

    class TrailInfoPagerAdapter(fm: FragmentManager,
                                private val context: Context,
                                private val trailId: Long)
        : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        private val tabNames = arrayOf(
            R.string.description,
            R.string.details,
            R.string.forecast,
            R.string.reviews
        )

        override fun getItem(position: Int): Fragment {
            val arguments = Bundle().apply {
                putLong(TrailInfoFragment.KEY_TRAIL_ID, trailId)
            }

            val fragment = when(position) {
                0 -> {
                    TrailDescriptionFragment()
                }
                1 -> {
                    TrailDetailsFragment()
                }
                else -> {
                    arguments.putString(DummyTrailInfoFragment.KEY_DUMMY_FRAGMENT_TITLE,
                        getPageTitle(position).toString())

                    DummyTrailInfoFragment()
                }
            }

            fragment.arguments = arguments
            return fragment
        }

        override fun getCount(): Int {
            return 4
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return context.getString(tabNames[position])
        }
    }

    class PictureAdapter(private val imageS3Keys: List<String>,
                         private val mediaService: IMediaService,
                         private val lifecycleScope: LifecycleCoroutineScope,
                         private val context: Context) :
        RecyclerView.Adapter<PictureAdapter.MyViewHolder>() {

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder.
        // Each data item is just a string in this case that is shown in a TextView.
        class MyViewHolder(frameLayout: FrameLayout)
            : RecyclerView.ViewHolder(frameLayout) {

            var url: URL? = null
            val imageView: AppCompatImageView = frameLayout.image_carousel_item_image
        }


        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(parent: ViewGroup,
                                        viewType: Int): MyViewHolder {
            // create a new view
            val frameLayout = LayoutInflater.from(parent.context)
                .inflate(R.layout.image_carousel_item, parent, false) as FrameLayout
            // set the view's size, margins, paddings and layout parameters
            return MyViewHolder(frameLayout)
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element

            lifecycleScope.launchWhenResumed {

                val s3Key = imageS3Keys[position]
                val file = mediaService.getMediaFile(s3Key)

                val options = RequestOptions()
                options.placeholder(android.R.drawable.ic_menu_info_details)
                options.fallback(android.R.drawable.ic_dialog_alert)

                Glide.with(context)
                    .applyDefaultRequestOptions(options)
                    .load(file)
                    .into(holder.imageView)

            }
        }

        private fun getImageBitmap(url: URL): Bitmap {
            val connection = url.openConnection()
            connection.connect()
            connection.getInputStream().use { inStream ->
                return BitmapFactory.decodeStream(inStream)
            }
        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = imageS3Keys.size
    }
}
