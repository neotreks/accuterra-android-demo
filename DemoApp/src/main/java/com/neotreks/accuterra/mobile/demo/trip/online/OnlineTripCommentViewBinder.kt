package com.neotreks.accuterra.mobile.demo.trip.online

import android.view.View
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.CommentItemBinding
import com.neotreks.accuterra.mobile.demo.ui.ListItemAdapterViewBinder
import com.neotreks.accuterra.mobile.sdk.ugc.model.TripComment

/**
 * View Binder for the [OnlineTripCommentAdapter]
 */
class OnlineTripCommentViewBinder : ListItemAdapterViewBinder<TripComment> {

    override fun bindView(view: View, item: TripComment, isSelected: Boolean, isFavorite: Boolean) {
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