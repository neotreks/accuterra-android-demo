package com.neotreks.accuterra.mobile.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.neotreks.accuterra.mobile.demo.databinding.FragmentTrailDescriptionContentBinding

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
        val binding = FragmentTrailDescriptionContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        trailId = arguments?.getLong(KEY_TRAIL_ID)
            ?: throw IllegalStateException("This fragment requires attributes with a '$KEY_TRAIL_ID' key.")
    }
}