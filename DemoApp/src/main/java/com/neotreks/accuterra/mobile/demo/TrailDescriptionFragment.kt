package com.neotreks.accuterra.mobile.demo

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.core.text.HtmlCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.demo.databinding.FragmentTrailDescriptionContentBinding
import kotlinx.coroutines.launch

class TrailDescriptionFragment: TrailInfoFragment() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private val viewModel: TrailInfoViewModel by activityViewModels()

    private lateinit var binding: FragmentTrailDescriptionContentBinding

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.trail.observe(this.viewLifecycleOwner, Observer { trail ->
            viewLifecycleOwner.lifecycleScope.launch {
                val highlights = trail?.info?.highlights
                    ?: return@launch
                val binding = FragmentTrailDescriptionContentBinding.bind(view)
                binding.fragmentTrailInfoDescription.text = HtmlCompat.fromHtml(highlights, HtmlCompat.FROM_HTML_MODE_LEGACY)
                binding.fragmentTrailInfoDescription.movementMethod = LinkMovementMethod.getInstance()

            }
        })

    }
}