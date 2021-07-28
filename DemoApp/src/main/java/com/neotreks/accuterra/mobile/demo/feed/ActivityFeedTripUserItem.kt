package com.neotreks.accuterra.mobile.demo.feed

import com.neotreks.accuterra.mobile.sdk.ugc.model.ActivityFeedEntry

/**
 * Activity Feed Trip User Item
 */
class ActivityFeedTripUserItem(data: ActivityFeedEntry):
    BaseFeedItem<ActivityFeedEntry>(ActivityFeedItemType.ONLINE_TRIP_USER, data.trip.uuid, data.trip.versionUuid, data)