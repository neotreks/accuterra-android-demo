package com.neotreks.accuterra.mobile.demo.feed

/**
 * General interface for activity feed items.
 */
interface ActivityFeedItem {

    val type: ActivityFeedItemType

    val tripUUID: String

    val versionUuid: String

    val data: Any?

}