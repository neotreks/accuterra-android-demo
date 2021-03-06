package com.neotreks.accuterra.mobile.demo

import android.os.Bundle
import android.view.View
import com.neotreks.accuterra.mobile.demo.databinding.FragmentTrailDescriptionContentBinding

class DummyTrailInfoFragment: TrailInfoFragment() {

    companion object {
        const val KEY_DUMMY_FRAGMENT_TITLE = "FRAGMENT_TITLE"
    }

    private lateinit var tabName: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabName = arguments?.getString(KEY_DUMMY_FRAGMENT_TITLE)
            ?: throw IllegalStateException("This fragment requires attributes with a '$KEY_DUMMY_FRAGMENT_TITLE' key.")

        val dummyText = "Just a sample line of content in $tabName tab for trail #$trailId."
        val binding = FragmentTrailDescriptionContentBinding.bind(view)
        binding.fragmentTrailInfoDescription.text = dummyText

    }
}