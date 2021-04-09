package com.neotreks.accuterra.mobile.demo.trip.online

import android.content.Context
import android.util.Log
import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.neotreks.accuterra.mobile.demo.media.ApkMediaVariant
import com.neotreks.accuterra.mobile.sdk.trip.model.TripMedia

/**
 * Utility class to manage the media list component
 *
 * @see [setupPhotoGrid]
 */
class OnlineTripMediaListManager(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val view: RecyclerView,
    private val clickListener: TripMediaListClickListener,
    private val readOnly: Boolean = false
) {

    private lateinit var adapter: OnlineTripMediaFileViewAdapter

    companion object {
        private const val TAG = "MediaListManager"
    }

    /**
     * PLEASE NOTE - this method must be called to make this component WORK!
     */
    fun setupPhotoGrid() {
        // Setup the component
        view.layoutManager = GridLayoutManager(context, 1, GridLayoutManager.HORIZONTAL, false)
    }

    fun refreshPhotoGridAdapter(mediaList: List<TripMedia>) {
        // Setup the adapter
        adapter = OnlineTripMediaFileViewAdapter(
            context,
            lifecycleScope,
            mediaList.toTypedArray(),
            mediaVariant = ApkMediaVariant.THUMBNAIL,
            readOnly = readOnly
        )
        adapter.setClickListener(object: OnlineTripMediaFileViewAdapter.ItemClickListener {
            override fun onItemClick(view: View?, position: Int) {
                val item = adapter.getItem(position)
                Log.d(TAG, "Image clicked: ${item.url}")
                clickListener.onItemClicked(item)
            }
            override fun onDeleteItemClick(view: View?, position: Int) {
                val item = adapter.getItem(position)
                Log.d(TAG, "Image delete clicked: ${item.url}")
                clickListener.onDeleteItemClicked(item)
            }
        })
        view.adapter = adapter
    }

    interface TripMediaListClickListener {
        fun onItemClicked(media: TripMedia)
        fun onDeleteItemClicked(media: TripMedia)
    }

}