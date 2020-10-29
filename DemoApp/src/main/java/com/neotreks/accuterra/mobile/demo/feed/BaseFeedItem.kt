package com.neotreks.accuterra.mobile.demo.feed

/**
 * General class for activity feed items.
 */
open class BaseFeedItem<T>(
    override val type: ActivityFeedItemType,
    override val tripUUID: String,
    override val data: T?
) : ActivityFeedItem {


}