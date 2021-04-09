package com.neotreks.accuterra.mobile.demo.trail

import android.content.Context
import android.util.Log
import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailMedia

/**
 * Utility class to manage the media list component
 *
 * @see [setupPhotoGrid]
 */
class TrailMediaListManager(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val view: RecyclerView,
    private val clickListener: MediaListClickListener,
    private val readOnly: Boolean = false
) {

    private lateinit var adapter: TrailMediaFileViewAdapter

    companion object {
        private const val TAG = "MediaListManager"
    }

    /**
     * PLEASE NOTE - this method must be called to make this component WORK!
     */
    fun setupPhotoGrid() {
        // Setup the component
        view.layoutManager = GridLayoutManager(context, 4)
    }

    fun refreshPhotoGridAdapter(mediaList: List<TrailMedia>) {
        // Setup the adapter
        adapter = TrailMediaFileViewAdapter(context, lifecycleScope, mediaList.toTypedArray(), readOnly)
        adapter.setClickListener(object: TrailMediaFileViewAdapter.ItemClickListener {
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

    interface MediaListClickListener {
        fun onItemClicked(media: TrailMedia)
        fun onDeleteItemClicked(media: TrailMedia)
    }

}