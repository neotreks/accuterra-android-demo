package com.neotreks.accuterra.mobile.demo.feed

import com.neotreks.accuterra.mobile.sdk.ugc.model.ActivityFeedEntry

/**
 * Activity Feed Trip Header Item
 */
class ActivityFeedTripHeaderItem(info: ActivityFeedEntry):
    BaseFeedItem<ActivityFeedEntry>(ActivityFeedItemType.ONLINE_TRIP_HEADER, info.trip.uuid, info)