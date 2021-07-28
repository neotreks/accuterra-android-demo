package com.neotreks.accuterra.mobile.demo.feed

import com.neotreks.accuterra.mobile.sdk.ugc.model.ActivityFeedEntry

/**
 * Activity Feed Trip Thumbnail Item
 */
class ActivityFeedTripThumbnailItem(data: ActivityFeedEntry):
    BaseFeedItem<ActivityFeedEntry>(ActivityFeedItemType.ONLINE_TRIP_THUMBNAIL, data.trip.uuid, data.trip.versionUuid, data)