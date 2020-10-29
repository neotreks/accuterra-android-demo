package com.neotreks.accuterra.mobile.demo.trip.online

import android.content.Context
import com.neotreks.accuterra.mobile.demo.extensions.uuidToLong
import com.neotreks.accuterra.mobile.demo.ui.MutableListItemAdapter
import com.neotreks.accuterra.mobile.sdk.ugc.model.TripComment

/**
 * Trip Comment Adapter
 */
class OnlineTripCommentAdapter(
    context: Context,
    comments: MutableList<TripComment>
) : MutableListItemAdapter<TripComment>(context, comments, OnlineTripCommentViewBinder()) {

    override fun getItemId(item: TripComment): Long {
        return item.commentUuid.uuidToLong()
    }

}