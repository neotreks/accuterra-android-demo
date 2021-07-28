package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.demo.databinding.FragmentRecordedTripPhotosBinding
import com.neotreks.accuterra.mobile.demo.ui.MediaDetailActivity
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMedia

/**
 * Recorded trip Map fragment
 */
class RecordedTripPhotosFragment : RecordedTripFragment() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var mediaListManager: TripRecordingMediaListManager

    private lateinit var binding: FragmentRecordedTripPhotosBinding

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    /** Provide view layout */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRecordedTripPhotosBinding.inflate(inflater, container, false)
        return binding.root
    }

    /** Load trip data into the UI */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup the photo manager
        mediaListManager = TripRecordingMediaListManager(
            requireContext(), lifecycleScope,
            binding.fragmentRecordedTripPhotos,
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