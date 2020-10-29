package com.neotreks.accuterra.mobile.demo.trail

import android.view.View
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.ui.ListItemAdapterViewBinder
import com.neotreks.accuterra.mobile.sdk.ugc.model.TrailComment
import kotlinx.android.synthetic.main.comment_item.view.*

/**
 * View Binder for the [TrailCommentAdapter]
 */
class TrailCommentViewBinder : ListItemAdapterViewBinder<TrailComment> {

    override fun bindView(view: View, item: TrailComment, isSelected: Boolean) {
        // User
        view.comment_item_user_name.text = item.user.userId
        // Text
        view.comment_item_comment.text = item.text
    }

    override fun getViewResourceId(): Int {
        return R.layout.comment_item
    }

}