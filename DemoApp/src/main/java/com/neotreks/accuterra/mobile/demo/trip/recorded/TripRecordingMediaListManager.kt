package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMedia

/**
 * Utility class to manage the media list component
 *
 * @see [setupPhotoGrid]
 */
class TripRecordingMediaListManager(
    private val context: Context,
    private val view: RecyclerView,
    private val clickListener: TripRecordingMediaListClickListener,
    private val readOnly: Boolean = false
) {

    private lateinit var adapter: TripRecordingMediaFileViewAdapter

    companion object {
        private const val TAG = "MediaListManager"
    }

    /**
     * PLEASE NOTE - this method must be called to make this component WORK!
     */
    fun setupPhotoGrid(columnsCount: Int = 3) {
        // Setup the component
        view.layoutManager = GridLayoutManager(context, columnsCount)
    }

    fun refreshPhotoGridAdapter(mediaList: List<TripRecordingMedia>) {
        // Setup the adapter
        adapter = TripRecordingMediaFileViewAdapter(context, mediaList.toTypedArray(), readOnly)
        adapter.setClickListener(object: TripRecordingMediaFileViewAdapter.ItemClickListener {
            override fun onItemClick(view: View?, position: Int) {
                val item = adapter.getItem(position)
                Log.d(TAG, "Image clicked: ${item.uri.path}")
                clickListener.onItemClicked(item)
            }
            override fun onDeleteItemClick(view: View?, position: Int) {
                val item = adapter.getItem(position)
                Log.d(TAG, "Image delete clicked: ${item.uri.path}")
                clickListener.onDeleteItemClicked(item)
            }
        })
        view.adapter = adapter
    }

    interface TripRecordingMediaListClickListener {
        fun onItemClicked(media: TripRecordingMedia)
        fun onDeleteItemClicked(media: TripRecordingMedia)
    }

}