package com.neotreks.accuterra.mobile.demo.trip.online

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.neotreks.accuterra.mobile.demo.DemoApplication

/**
 * Base class for [OnlineTripActivity] fragments
 */
abstract class OnlineTripFragment : Fragment() {

    /** View model from the Activity */
    protected val viewModel: OnlineTripViewModel by lazy {
        // Please note it is taken from ViewModelStore because it is shared between more activities
        val viewModelStore = (requireActivity().application as DemoApplication).onlineTripVMStore
        ViewModelProvider(viewModelStore, defaultViewModelProviderFactory).get(OnlineTripViewModel::class.java)
    }

}