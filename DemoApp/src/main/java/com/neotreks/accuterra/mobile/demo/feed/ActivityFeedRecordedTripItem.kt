package com.neotreks.accuterra.mobile.demo.feed

import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingBasicInfo

/**
 * Activity Feed Local Recorded Trip Item
 */
class ActivityFeedRecordedTripItem(info: TripRecordingBasicInfo):
    BaseFeedItem<TripRecordingBasicInfo>(ActivityFeedItemType.LOCAL_RECORDED_TRIP, info.uuid, info.versionUuid, info)