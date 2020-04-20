package com.neotreks.accuterra.mobile.demo

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.sdk.trail.model.*
import kotlinx.android.synthetic.main.fragment_trail_description_content.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrailDetailsFragment: TrailInfoFragment() {

    private val viewModel: TrailInfoViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.trail.observe(this.viewLifecycleOwner, Observer { trail ->
            viewLifecycleOwner.lifecycleScope.launch {
                view.fragment_trail_info_description.text = getTrailDetails(trail)
            }
        })

    }

    /**
     * Make as suspend since this can take a while
     */
    private suspend fun getTrailDetails(trail: Trail): String {
        return withContext(Dispatchers.Main) {
            val builder = StringBuilder()
            appendInfo("Info", trail.info, builder)
            appendStatistics("Statistics", trail.statistics, builder)
            appendLocation("Location", trail.locationInfo, builder)
            appendTechRating("Tech. Rating", trail.technicalRating, builder)
            appendUserRating("User Rating", trail.userRating, builder)
            appendAccessInfo("Access Info", trail.accessInfo, builder)
            appendSystemAttributes("System Attributes", trail.systemAttributes, builder)
            appendNavigation("Navigation", trail.navigationInfo, builder)
            builder.toString()
        }
    }

    private fun appendSystemAttributes(name: String, info: TrailSystemAttributes, builder: StringBuilder) {
        appendSectionSeparator(name, builder)
        appendString("ID", info.id.toString(), builder)
        appendSectionFooter(builder)
    }

    private fun appendAccessInfo(name: String, info: TrailAccessInfo, builder: StringBuilder) {
        appendSectionSeparator(name, builder)
        appendString("Access Issue", info.accessIssue, builder)
        appendString("Access Issue Link", info.accessIssueLink, builder)
        appendString("Is Permit Required", info.isPermitRequired.toString(), builder)
        appendString("Permit Information", info.permitInformation, builder)
        appendString("Permit Information Link", info.permitInformationLink, builder)
        appendString("Seasonality Recommendation", info.seasonalityRecommendation, builder)
        appendString("Seasonality Recommendation Reason", info.seasonalityRecommendationReason, builder)
        appendList("Closure Periods", info.closurePeriods, builder)
        appendSectionFooter(builder)
    }

    private fun appendUserRating(name: String, info: UserRating?, builder: StringBuilder) {
        appendSectionSeparator(name, builder)
        if (info != null) {
            appendString("Rating", info.rating.toString(), builder)
            appendString("Rating Count", info.ratingCount.toString(), builder)
        }
        appendSectionFooter(builder)
    }

    private fun appendTechRating(name: String, info: TrailTechRating, builder: StringBuilder) {
        appendSectionSeparator(name, builder)
        appendString("Tech Rating High", info.high.toString(), builder)
        appendString("Tech Rating Low", info.low.toString(), builder)
        appendString("Tech Rating Summary", info.summary, builder)
        appendSectionFooter(builder)
    }

    private fun appendNavigation(name: String, info: TrailNavigationInfo, builder: StringBuilder) {
        appendSectionSeparator(name, builder)
        appendString("Nearest Town with Services", info.nearestTownWithServicesName, builder)
        appendString("Official Road name", info.officialRoadName, builder)
        appendString("Is One Way", info.isOneWay.toString(), builder)
        appendList("Map Points", info.mapPoints, builder)
        appendSectionFooter(builder)
    }

    private fun appendStatistics(name: String, info: TrailStatistics, builder: StringBuilder) {
        appendSectionSeparator(name, builder)
        appendString("Highest Elevation [m]", info.highestElevation.toString(), builder)
        appendString("Length [m]", info.length.toString(), builder)
        appendString("Estimated Drive Time Avg [s]", info.estimatedDriveTimeAvg.toString(), builder)
        appendString("Estimated Drive Time Min [s]", info.estimatedDriveTimeMin.toString(), builder)
        appendSectionFooter(builder)
    }

    private fun appendLocation(name: String, info: TrailLocationInfo, builder: StringBuilder) {
        appendSectionSeparator(name, builder)
        appendString("Country ISO3 code", info.countryIso3Code, builder)
        appendString("State code", info.stateCode, builder)
        appendString("County Name", info.countyName, builder)
        appendString("District Name", info.districtName, builder)
        appendString("Nearest Town Name", info.nearestTownName, builder)
        appendString("USDA Road Number", info.usdaFsRoadNumber, builder)
        appendString("USDA Park District Name", info.usdaFsParkDistrict, builder)
        appendString("Map Bounds", info.mapBounds.toString(), builder)
        appendSectionFooter(builder)
    }

    private fun appendInfo(name: String, info: TrailInfo, builder: StringBuilder) {
        appendSectionSeparator(name, builder)
        appendString("ID", info.id.toString(), builder)
        appendString("Name", info.name, builder)
        appendString("Highlights", info.highlights, builder)
        appendString("History", info.history, builder)
        appendString("Shape Type", info.shapeType.toString(), builder)
        appendList("Surface Type", info.surfaceTypes, builder)
        appendList("Access Concerns", info.accessConcerns, builder)
        appendString("Camping Options", info.campingOptions, builder)
        appendList("Vehicle Requirements", info.vehicleRequirements, builder)
        appendString("Status", info.status.toString(), builder)
        appendList("Tags", info.tags, builder)
        appendSectionFooter(builder)
    }

    private fun appendString(name: String, value: String?, builder: StringBuilder) {
        builder.appendln("---")
        builder.appendln("> $name: \n$value")
    }

    private fun appendSectionSeparator(name: String, builder: StringBuilder) {
        builder.appendln("= = = = = = = $name = = = = = = = =")
    }

    private fun appendSectionFooter(builder: StringBuilder) {
        builder.appendln()
        builder.appendln()
    }

    private fun appendList(name: String, collection: List<Any>?, builder: StringBuilder) {
        appendString(name, "", builder)
        if (collection == null) {
            return
        }
        for (item in collection) {
            builder.appendln("   $item")
        }
    }
}