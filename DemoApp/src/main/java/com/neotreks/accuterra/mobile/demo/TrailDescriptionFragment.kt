package com.neotreks.accuterra.mobile.demo

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.core.text.HtmlCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_trail_description_content.view.*
import kotlinx.coroutines.launch

class TrailDescriptionFragment: TrailInfoFragment() {

    private val viewModel: TrailInfoViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.trail.observe(this.viewLifecycleOwner, Observer { trail ->
            viewLifecycleOwner.lifecycleScope.launch {
                val highlights = trail?.info?.highlights
                    ?: return@launch
                view.fragment_trail_info_description.text = HtmlCompat.fromHtml(highlights, HtmlCompat.FROM_HTML_MODE_LEGACY)
                view.fragment_trail_info_description.movementMethod = LinkMovementMethod.getInstance()

            }
        })

    }
}