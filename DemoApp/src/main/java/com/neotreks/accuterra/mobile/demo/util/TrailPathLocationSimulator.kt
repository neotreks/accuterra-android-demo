package com.neotreks.accuterra.mobile.demo.util

import android.location.Location
import android.os.Handler
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.turf.TurfMeasurement
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailPath

class TrailPathLocationSimulator(trailPath: TrailPath,
                                 callback: LocationEngineCallback<LocationEngineResult>) {

    private var callback: LocationEngineCallback<LocationEngineResult>? = callback

    private val trailPoints: List<Point>
    private var nextPointIndex = 0
    private val handler = Handler()

    private val initialDelayMillis = 5000L
    private val delayMillis = 500L

    init {
        val trailGeometry = Feature.fromJson(trailPath.geojson).geometry() as? LineString
            ?: throw IllegalArgumentException("Unexpected trail path format.")

        trailPoints = trailGeometry.coordinates()

        if (trailPoints.isEmpty()) {
            throw IllegalArgumentException("The trailPath does not contain any points.")
        }
    }

    fun start() {
        handler.postDelayed({
            reportFakeLocation(nextPointIndex++)
            scheduleNextFakeLocation()
        }, initialDelayMillis)
    }

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        callback = null
    }

    private fun scheduleNextFakeLocation() {
        if (nextPointIndex >= trailPoints.size) {
            return
        }

        handler.postDelayed({
            reportFakeLocation(nextPointIndex++)
            scheduleNextFakeLocation()
        }, delayMillis)
    }


    private fun reportFakeLocation(pointIndex: Int) {
        val point = trailPoints[pointIndex]
        val bearing = if (pointIndex + 1 < trailPoints.size) {
            val nextPoint = trailPoints[pointIndex + 1]
            calculateBearing(point, nextPoint)
        } else {
            null
        }

        val location = Location("trailPathSimulator")
        location.latitude = point.latitude()
        location.longitude = point.longitude()
        location.accuracy = 1f
        if (bearing != null) {
            location.bearing = bearing.toFloat()
        }

        callback?.onSuccess(LocationEngineResult.create(location))
    }

    private fun calculateBearing(currentPoint: Point, nextPoint: Point): Double {
        val turfBearing = TurfMeasurement.bearing(currentPoint, nextPoint)
        return if (turfBearing >= 0) {
            turfBearing
        } else {
            360 + turfBearing // Turf returns value from (-180 to 180) while Android needs [0 to 360)
        }
    }

}