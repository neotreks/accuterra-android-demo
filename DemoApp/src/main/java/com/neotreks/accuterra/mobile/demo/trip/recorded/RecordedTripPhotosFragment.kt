package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.ui.MediaDetailActivity
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMedia
import kotlinx.android.synthetic.main.fragment_recorded_trip_photos.view.*

/**
 * Recorded trip Map fragment
 */
class RecordedTripPhotosFragment : RecordedTripFragment() {

    private lateinit var mediaListManager: TripRecordingMediaListManager

    /** Provide view layout */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recorded_trip_photos, container, false)
    }

    /** Load trip data into the UI */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup the photo manager
        mediaListManager = TripRecordingMediaListManager(
            requireContext(),
            view.fragment_recorded_trip_photos,
            object : TripRecordingMediaListManager.TripRecordingMediaListClickListener {
                override fun onItemClicked(media: TripRecordingMedia) {
                    val intent = MediaDetailActivity.createNavigateToIntent(
                        requireContext(),
                        mediaUuid = media.uuid
                    )
                    startActivity(intent)
                }
                override fun onDeleteItemClicked(media: TripRecordingMedia) {
                    throw IllegalStateException("Deletion is not supported")
                }
            },
            readOnly = true
        )
        mediaListManager.setupPhotoGrid()

        // Load trip photos the UI
        viewModel.trip.observe(viewLifecycleOwner, Observer { trip ->
            if (trip == null) return@Observer
            mediaListManager.refreshPhotoGridAdapter(trip.media)
        })

    }

}