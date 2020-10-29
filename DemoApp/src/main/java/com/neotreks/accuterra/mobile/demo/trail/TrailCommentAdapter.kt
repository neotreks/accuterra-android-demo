package com.neotreks.accuterra.mobile.demo.trail

import android.content.Context
import com.neotreks.accuterra.mobile.demo.extensions.uuidToLong
import com.neotreks.accuterra.mobile.demo.ui.MutableListItemAdapter
import com.neotreks.accuterra.mobile.sdk.ugc.model.TrailComment

/**
 * Trail Comment Adapter
 */
class TrailCommentAdapter(
    context: Context,
    comments: MutableList<TrailComment>
) : MutableListItemAdapter<TrailComment>(context, comments, TrailCommentViewBinder()) {

    override fun getItemId(item: TrailComment): Long {
        return item.commentUuid.uuidToLong()
    }

}