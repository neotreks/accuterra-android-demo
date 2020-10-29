package com.neotreks.accuterra.mobile.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.demo.trail.TrailCommentAdapter
import com.neotreks.accuterra.mobile.demo.ui.OnListItemLongClickListener
import com.neotreks.accuterra.mobile.sdk.ugc.model.TrailComment
import kotlinx.android.synthetic.main.fragment_trail_ugc.*

class TrailUgcFragment(private val listener: Listener): TrailInfoFragment() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private val viewModel: TrailInfoViewModel by activityViewModels()

    private lateinit var commentAdapter: TrailCommentAdapter

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    /** Provide view layout */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trail_ugc, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupComments()
        setupRating()
        setupObservers()

    }

    private fun setupObservers() {
        viewModel.comments.observe(this.viewLifecycleOwner, Observer { comments ->
            commentAdapter.setItems(comments ?: listOf())
        })
        viewModel.commentsCount.observe(this.viewLifecycleOwner, Observer { commentsCount ->
            fragment_trail_ugc_comments.text = this.getString(R.string.general_comments_number, commentsCount)
        })
        viewModel.userData.observe(this.viewLifecycleOwner, Observer { userData ->
            if (userData == null) return@Observer
            val rating = userData.userRating
            if (rating == null) {
                fragment_trail_ugc_rating_value.text = getString(R.string.no_rating)
                fragment_trail_ugc_rating_stars.rating = 0F
            } else {
                fragment_trail_ugc_rating_value.text = rating.toString()
                fragment_trail_ugc_rating_stars.rating = rating
            }
        })
    }

    private fun setupComments() {
        // Comments Adapter
        commentAdapter = TrailCommentAdapter(requireContext(), mutableListOf())
        commentAdapter.setOnListItemLongClickListener(object : OnListItemLongClickListener<TrailComment> {
            override fun onListItemLongClick(item: TrailComment, view: View) {
                showCommentItemMenu(item, view)
            }
        })
        fragment_trail_comments_list.adapter = commentAdapter
        // Comments sending
        fragment_trail_ugc_add_icon.setOnClickListener {
            listener.onAddTrailComment()
        }
    }

    private fun showCommentItemMenu(comment: TrailComment, view: View) {
        val context = context ?: return
        val menu = PopupMenu(context, view)
        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_edit_list_item_edit -> {
                    listener.onEditTrailComment(comment)
                }
                R.id.menu_edit_list_item_delete -> {
                    listener.onDeleteTrailComment(comment)
                }
            }
            true
        }
        menu.inflate(R.menu.menu_edit_list_item)
        menu.show()
    }

    private fun setupRating() {
        fragment_trail_ugc_rating_stars.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (!fromUser) return@setOnRatingBarChangeListener
            lifecycleScope.launchWhenCreated {
                listener.onUserRatingSet(rating)
            }
        }
    }

    /* * * * * * * * * * * * */
    /*       INTERNAL        */
    /* * * * * * * * * * * * */

    interface Listener {
        fun onAddTrailComment()
        fun onEditTrailComment(comment: TrailComment)
        fun onDeleteTrailComment(comment: TrailComment)
        fun onUserRatingSet(rating: Float)
    }

}