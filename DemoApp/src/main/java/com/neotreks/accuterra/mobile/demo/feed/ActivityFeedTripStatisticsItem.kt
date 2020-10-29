package com.neotreks.accuterra.mobile.demo.feed

import com.neotreks.accuterra.mobile.sdk.ugc.model.ActivityFeedEntry

/**
 * Activity Feed Trip Statistics Item
 */
class ActivityFeedTripStatisticsItem(data: ActivityFeedEntry):
    BaseFeedItem<ActivityFeedEntry>(ActivityFeedItemType.ONLINE_TRIP_STATISTICS, data.trip.uuid, data)