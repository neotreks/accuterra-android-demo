package com.neotreks.accuterra.mobile.demo.trip.online

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.neotreks.accuterra.mobile.demo.databinding.FragmentOnlineTripMapBinding
import com.neotreks.accuterra.mobile.demo.util.DialogUtil
import com.neotreks.accuterra.mobile.sdk.map.AccuTerraMapView
import com.neotreks.accuterra.mobile.sdk.map.AccuTerraStyle
import com.neotreks.accuterra.mobile.sdk.map.TrackingOption
import com.neotreks.accuterra.mobile.sdk.trip.model.Trip
import java.lang.ref.WeakReference

/**
 * Online trip Map fragment
 */
class OnlineTripMapFragment : OnlineTripFragment() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var binding: FragmentOnlineTripMapBinding

    private lateinit var accuTerraMapView: AccuTerraMapView

    private val accuTerraMapViewListener = AccuTerraMapViewListener(this)

    // Current style id
    private var currentStyle = AccuTerraStyle.VECTOR

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "OnlineTripMapFragment"
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    /** Provide view layout */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOnlineTripMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Load the map
        setupMap(savedInstanceState)
    }

    // MapBox stuff

    override fun onResume() {
        Log.i(TAG, "onResume()")
        super.onResume()
        accuTerraMapView.onResume()
    }

    override fun onPause() {
        Log.i(TAG, "onPause()")
        super.onPause()
        accuTerraMapView.onPause()
    }

    override fun onStart() {
        Log.i(TAG, "onStart()")
        super.onStart()
        accuTerraMapView.onStart()
    }

    override fun onStop() {
        Log.i(TAG, "onStop()")
        super.onStop()
        accuTerraMapView.onStop()
    }

    override fun onLowMemory() {
        Log.w(TAG, "onLowMemory()")
        super.onLowMemory()
        accuTerraMapView.onLowMemory()
    }

    override fun onDestroy() {
        Log.i(TAG, "onLowMemory()")
        super.onDestroy()
        accuTerraMapView.removeListener(getAccuTerraMapViewListener())
        accuTerraMapView.onDestroy()
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun getAccuTerraMapView(): AccuTerraMapView {
        return binding.fragmentOnlineTripMapMap
    }

    private fun getAccuTerraMapViewListener(): AccuTerraMapView.IAccuTerraMapViewListener {
        return accuTerraMapViewListener
    }

    private fun setupMap(savedInstanceState: Bundle?) {

        Log.d(TAG, "setupMap()")

        accuTerraMapView = getAccuTerraMapView()
        accuTerraMapView.onCreate(savedInstanceState)
        accuTerraMapView.addListener(getAccuTerraMapViewListener())

        accuTerraMapView.initialize(lifecycleScope, currentStyle)

    }

    private fun onAccuTerraMapViewReady() {
        Log.i(TAG, "onAccuTerraMapViewReady()")
        // Avoid map rotation by user
        getAccuTerraMapView().getMapboxMap().uiSettings.isRotateGesturesEnabled = false
        getAccuTerraMapView().getMapboxMap().uiSettings.isCompassEnabled = false
        // Try to register location observers to obtain device location

        lifecycleScope.launchWhenResumed {
            // Add trip layers to the map
            getAccuTerraMapView().tripLayersManager.addStandardTripLayers()
            // Load trip data into the UI
            viewModel.trip.observe(viewLifecycleOwner, Observer { trip ->
                if (trip == null) {
                    return@Observer
                }
                // Load trip into the map
                addTripToMap(trip)
                // Zoom to path area
                trip.location.mapBounds?.let { mapBounds ->
                    getAccuTerraMapView().zoomToBounds(mapBounds)
                }
            })
        }
    }

    private fun addTripToMap(trip: Trip) {
        lifecycleScope.launchWhenResumed {
            val tripLayersManager = getAccuTerraMapView().tripLayersManager
            val tripUuid = trip.info.uuid
            val path = trip.navigation.path.geometryGeoJSON
            val points = trip.navigation.points
            tripLayersManager.setVisibleTrip(tripUuid, path, points)
        }
    }

    /* * * * * * * * * * * * */
    /*     INNER CLASS       */
    /* * * * * * * * * * * * */

    private class AccuTerraMapViewListener(fragment: OnlineTripMapFragment)
        : AccuTerraMapView.IAccuTerraMapViewListener {

        private val weakFragment = WeakReference(fragment)

        override fun onInitialized(map: AccuTerraMapView) {
            Log.i(TAG, "AccuTerraMapViewListener.onInitialized()")

            val fragment = weakFragment.get()
                ?: return

            fragment.onAccuTerraMapViewReady()
        }

        override fun onInitializationFailed(map: AccuTerraMapView, errorMessage: String?) {
            weakFragment.get()?.let { fragment ->
                DialogUtil.buildOkDialog(
                    fragment.requireContext(), "Error",
                    errorMessage ?: "Unknown Error While Loading Map"
                ).show()
            }
        }

        override fun onStyleChanged(map: AccuTerraMapView) {
            weakFragment.get()?.let { fragment ->
                // Reload online trip path after the style has changed
                fragment.lifecycleScope.launchWhenResumed {
                    fragment.getAccuTerraMapView().tripLayersManager.setVisibleTripId(fragment.viewModel.tripUuid)
                }
            }
        }

        override fun onStyleChangeFailed(map: AccuTerraMapView, errorMessage: String?) {
        }

        override fun onStyleChangeStart(map: AccuTerraMapView) {
        }

        override fun onSignificantMapBoundsChange() {
            // nothing to do
        }

        override fun onTrackingModeChanged(mode: TrackingOption) {
            // nothing to do
        }
    }

}