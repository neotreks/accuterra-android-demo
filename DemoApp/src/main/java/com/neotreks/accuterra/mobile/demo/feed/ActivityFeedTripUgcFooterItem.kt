package com.neotreks.accuterra.mobile.demo.feed

import com.neotreks.accuterra.mobile.sdk.ugc.model.ActivityFeedEntry

/**
 * Activity Feed Trip Thumbnail Item
 */
class ActivityFeedTripUgcFooterItem(data: ActivityFeedEntry):
    BaseFeedItem<ActivityFeedEntry>(ActivityFeedItemType.ONLINE_TRIP_UGC_FOOTER, data.trip.uuid, data)