package com.neotreks.accuterra.mobile.demo

import android.location.Location
import com.neotreks.accuterra.mobile.sdk.map.NextWayPoint

class NavigatorStatus private constructor(val status: NavigatorStatusType,
                                          val location: Location?,
                                          val nextWayPoint: NextWayPoint?) {

    companion object {
        fun createNotReady(): NavigatorStatus {
            return NavigatorStatus(NavigatorStatusType.NOT_READY, null, null)
        }
        fun createNavigating(location: Location, situation: NextWayPoint): NavigatorStatus {
            return NavigatorStatus(NavigatorStatusType.NAVIGATING, location, situation)
        }
        fun createFinished(location: Location, situation: NextWayPoint): NavigatorStatus {
            return NavigatorStatus(NavigatorStatusType.FINISHED, location, situation)
        }
        fun createWrongDirection(location: Location): NavigatorStatus {
            return NavigatorStatus(NavigatorStatusType.WRONG_DIRECTION, location, null)
        }
        fun createTrailLost(location: Location): NavigatorStatus {
            return NavigatorStatus(NavigatorStatusType.TRAIL_LOST, location, null)
        }
    }

}