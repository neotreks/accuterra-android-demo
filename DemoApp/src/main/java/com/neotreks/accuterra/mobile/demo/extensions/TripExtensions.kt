package com.neotreks.accuterra.mobile.demo.extensions

import com.neotreks.accuterra.mobile.sdk.trip.model.TripLocationInfo

/**
 * Extensions related to Trip
 */


fun TripLocationInfo.getLocationLabelString(): String {
    val firstPart = nearestTownName ?: countyName ?: districtName
    val secondPart = stateCode ?: countryIso3Code

    return if (firstPart.isNullOrBlank() && secondPart.isNotNullNorBlank()) {
        secondPart!!
    } else if (firstPart.isNotNullNorBlank() && secondPart.isNullOrBlank()) {
        firstPart!!
    } else {
        "$firstPart, $secondPart"
    }

}
