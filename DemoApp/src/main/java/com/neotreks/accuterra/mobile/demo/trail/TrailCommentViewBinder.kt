package com.neotreks.accuterra.mobile.demo.trail

import android.view.View
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.CommentItemBinding
import com.neotreks.accuterra.mobile.demo.ui.ListItemAdapterViewBinder
import com.neotreks.accuterra.mobile.sdk.ugc.model.TrailComment

/**
 * View Binder for the [TrailCommentAdapter]
 */
class TrailCommentViewBinder : ListItemAdapterViewBinder<TrailComment> {

    override fun bindView(view: View, item: TrailComment, isSelected: Boolean, isFavorite: Boolean) {
        val binding = CommentItemBinding.bind(view)
        // User
        binding.commentItemUserName.text = item.user.userId
        // Text
        binding.commentItemComment.text = item.text
    }

    override fun getViewResourceId(): Int {
        return R.layout.comment_item
    }

}