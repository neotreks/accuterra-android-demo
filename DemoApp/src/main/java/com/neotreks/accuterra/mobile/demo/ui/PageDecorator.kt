package com.neotreks.accuterra.mobile.demo.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Bottom Dot Decorator
 */
class RecycleViewBottomDotDecorator : RecyclerView.ItemDecoration() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private var normalIndicator: Paint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.GRAY
    }

    private val currentIndicator = Paint().apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    // save the center coordinates of all indicators
    private val indicators = mutableListOf<Pair<Float, Float>>()

    private val indicatorRadius = (DP * 7)
    private val verticalPadding = (DP * 10)
    private val indicatorHeight = indicatorRadius + verticalPadding
    private val indicatorPadding = (DP * 20)
    private val bottomOffset = indicatorHeight * 2

    private var activeIndicator = 0
    private var isInitialized = false

    val indicatorTouchListener = object: RecyclerView.OnItemTouchListener {
        override fun onInterceptTouchEvent(rv: RecyclerView, motionEvent: MotionEvent): Boolean {
            return isIndicatorPressing(motionEvent, rv)
        }
        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) { }
        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) { }
    }

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private val DP: Float = android.content.res.Resources.getSystem().displayMetrics.density
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    // create three indicators for three slides
    override fun onDrawOver(canvas: Canvas,
                            parent: RecyclerView,
                            state: RecyclerView.State) {

        if(!isInitialized) {
            setupIndicators(parent)
        }

        // draw indicators with stroke style
        val itemCount = parent.adapter?.itemCount ?: 0
        parent.adapter?.let {

            val visibleItem = (parent.layoutManager as LinearLayoutManager)
                .findFirstCompletelyVisibleItemPosition()
            activeIndicator = visibleItem

            with(canvas) {
                for (i  in 0 until itemCount) {
                    drawCircle(indicators[i].first, indicators[i].second, activeIndicator == i)
                }
            }

            if(activeIndicator >= 0) {
                // paint over the needed circle
                canvas.drawCircle(indicators[activeIndicator].first, indicators[activeIndicator].second, true)
            }

        }
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.bottom = bottomOffset.toInt()
    }

    /* * * * * * * * * * * * */
    /*        PUBLIC         */
    /* * * * * * * * * * * * */

    fun reset() {
        isInitialized = false
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun Canvas.drawCircle(x: Float, y: Float, isCurrent: Boolean = false) {
        drawCircle(x, y, indicatorRadius, if(isCurrent) currentIndicator else normalIndicator)
    }

    private fun setupIndicators(recyclerView: RecyclerView) {
        isInitialized = true

        val itemCount = recyclerView.adapter?.itemCount ?: 0

        val indicatorTotalWidth = (indicatorRadius + indicatorPadding) * itemCount
        val indicatorPosX = (recyclerView.width - indicatorTotalWidth) / 2f
        val indicatorPosY = recyclerView.height - ((bottomOffset / 2) + (indicatorRadius / 2))

        for (i in 0 until itemCount) {
            indicators.add((indicatorPosX + (indicatorRadius + indicatorPadding) * i ) to indicatorPosY)
        }

    }

    private fun isIndicatorPressing(motionEvent: MotionEvent, recyclerView: RecyclerView): Boolean {
        when(motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                checkIfIndicatorPressing(motionEvent.x, motionEvent.y)?.let { position ->
                    recyclerView.scrollToPosition(position)
                }
            }
        }
        return false
    }

    private fun checkIfIndicatorPressing(touchX: Float, touchY: Float): Int?{
        indicators.indices.forEach {
            // point belongs to a circle or not
            // sqrt((x0-x1)*(x0-x1)+(y0-y1)*(y0-y1))<=r
            // but the radius value is increased to make it easier to click
            if(sqrt(
                    ((indicators[it].first - touchX).toDouble()).pow(2.0)
                        + ((indicators[it].second - touchY).toDouble()).pow(2.0)
                )
                            <= indicatorRadius * 2) {
                return it
            }
        }
        return null
    }

}