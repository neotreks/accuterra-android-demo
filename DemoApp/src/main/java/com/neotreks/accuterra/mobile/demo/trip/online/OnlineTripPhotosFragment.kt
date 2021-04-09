package com.neotreks.accuterra.mobile.demo.trip.online

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.neotreks.accuterra.mobile.demo.databinding.FragmentOnlineTripPhotosBinding
import com.neotreks.accuterra.mobile.demo.ui.GlideProgressIndicator
import com.neotreks.accuterra.mobile.demo.ui.UiUtils
import com.neotreks.accuterra.mobile.demo.util.visibility
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trip.model.TripMedia
import com.neotreks.accuterra.mobile.sdk.trip.service.ITripMediaService

/**
 * Online trip Map fragment
 */
class OnlineTripPhotosFragment : OnlineTripFragment() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var binding: FragmentOnlineTripPhotosBinding

    private lateinit var mediaListManager: OnlineTripMediaListManager

    private lateinit var mediaService: ITripMediaService

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    /** Provide view layout */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOnlineTripPhotosBinding.inflate(inflater, container, false)
        return binding.root
    }

    /** Load trip data into the UI */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mediaService = ServiceFactory.getTripMediaService(requireContext())

        // Setup the photo manager
        mediaListManager = buildMediaManager()
        mediaListManager.setupPhotoGrid()

        // Load trip photos into the UI
        viewModel.trip.observe(viewLifecycleOwner, { trip ->
            if (trip != null) {
                // We do merge trip photos with POI photos to show everything together
                val poiMedia = trip.navigation.points.flatMap { point ->
                    point.media.map { media -> media }
                }
                val mediaList = trip.media + poiMedia
                mediaListManager.refreshPhotoGridAdapter(mediaList)
                // Load first image into detail image view
                mediaList.firstOrNull()?.let { firstImage ->
                    loadDetailImage(firstImage.url)
                }
            }
        })

        // Image detail listener
        setupImageDetailListener()
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun buildMediaManager(): OnlineTripMediaListManager {
        return OnlineTripMediaListManager(
            requireContext(),
            lifecycleScope,
            binding.fragmentOnlineTripPhotos,
            object : OnlineTripMediaListManager.TripMediaListClickListener {
                override fun onItemClicked(media: TripMedia) {
                    viewModel.selectedMediaUrl = media.url
                    loadDetailImage(media.url)
                }
                override fun onDeleteItemClicked(media: TripMedia) {
                    throw IllegalStateException("Deletion is not supported")
                }
            },
            readOnly = true
        )
    }

    private fun setupImageDetailListener() {
        binding.fragmentOnlineTripPhotoDetail.setOnClickListener {
            val selectedUrl = viewModel.selectedMediaUrl
                ?: return@setOnClickListener
            val intent = OnlineTripMediaActivity.createNavigateToIntent(
                requireContext(),
                tripUuid = viewModel.tripUuid,
                selectedUrl = selectedUrl
            )
            startActivity(intent)
        }
    }

    private fun loadDetailImage(imageUrl: String) {
        lifecycleScope.launchWhenCreated {
            displayProgress()
            val options = UiUtils.getDefaultImageOptions()
            // Ge the URi Async to avoid UI freezing
            val uri = mediaService.getMediaFile(imageUrl).value
            Glide.with(this@OnlineTripPhotosFragment)
                .applyDefaultRequestOptions(options)
                .addDefaultRequestListener(GlideProgressIndicator(binding.fragmentOnlineTripPhotoDetailProgress))
                .load(uri)
                .into(binding.fragmentOnlineTripPhotoDetail)
        }
    }

    private fun displayProgress() {
        binding.fragmentOnlineTripPhotoDetailProgress.visibility = true.visibility
    }

}