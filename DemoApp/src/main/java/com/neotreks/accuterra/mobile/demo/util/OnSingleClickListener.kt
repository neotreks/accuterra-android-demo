package com.neotreks.accuterra.mobile.demo.util

import android.view.View
import java.util.concurrent.atomic.AtomicLong

/**
 * Listener avoiding multiple clicks
 */
class OnSingleClickListener(
    private val onClicked: (View?)->(Unit),
    private val intervalMs: Long = 1000
) : View.OnClickListener {

    private var lastClick = AtomicLong(0L)

    override fun onClick(v: View?) {
        val now = System.currentTimeMillis()
        val last = lastClick.get()
        val div = now - last
        synchronized(lastClick) {
            if (div > intervalMs) {
                if (lastClick.compareAndSet(last, now)) {
                    onClicked(v)
                }
            }
        }
    }
}