package com.neotreks.accuterra.mobile.demo.trip.location

import android.location.Location

/**
 * Location Update Listener
 */
interface ILocationUpdateListener {

    /**
     * Method is called when new GPS location is received
     */
    fun onLocationUpdated(location: Location)

}