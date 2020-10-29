package com.neotreks.accuterra.mobile.demo.trip.online

import android.view.View
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.ui.ListItemAdapterViewBinder
import com.neotreks.accuterra.mobile.sdk.ugc.model.TripComment
import kotlinx.android.synthetic.main.comment_item.view.*

/**
 * View Binder for the [OnlineTripCommentAdapter]
 */
class OnlineTripCommentViewBinder : ListItemAdapterViewBinder<TripComment> {

    override fun bindView(view: View, item: TripComment, isSelected: Boolean) {
        // User
        view.comment_item_user_name.text = item.user.userId
        // Text
        view.comment_item_comment.text = item.text
    }

    override fun getViewResourceId(): Int {
        return R.layout.comment_item
    }

}