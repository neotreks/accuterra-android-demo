package com.neotreks.accuterra.mobile.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

abstract class TrailInfoFragment: Fragment() {

    protected var trailId: Long = Long.MIN_VALUE

    companion object {
        const val KEY_TRAIL_ID = "TRAIL_ID"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trail_description_content, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        trailId = arguments?.getLong(KEY_TRAIL_ID)
            ?: throw IllegalStateException("This fragment requires attributes with a '$KEY_TRAIL_ID' key.")
    }
}