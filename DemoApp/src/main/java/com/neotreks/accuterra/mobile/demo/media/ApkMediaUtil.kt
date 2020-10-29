package com.neotreks.accuterra.mobile.demo.media

import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMedia

/**
 * Utility class to handle APK media
 */
class ApkMediaUtil {

    companion object {

        /**
         * This is not mandatory. Media position is an optional field. If not set, then media
         * are ordered naturally in the same order as there were created.
         *
         * But it is possible to set the order of media to change the natural ordering.
         */
        fun updatePositions(allMedia: MutableList<TripRecordingMedia>): MutableList<TripRecordingMedia> {
            var position = 1
            val ordered = mutableListOf<TripRecordingMedia>()
            for (media in allMedia) {
                ordered.add(
                    // Set the optional position value
                    media.copy(position = position++)
                )
            }
            return ordered
        }

    }

}