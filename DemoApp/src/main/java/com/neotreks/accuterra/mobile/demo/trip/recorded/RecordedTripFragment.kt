package com.neotreks.accuterra.mobile.demo.trip.recorded

import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels

/**
 * Base class for [RecordedTripActivity] fragments
 */
abstract class RecordedTripFragment : Fragment() {

    /** View model from the Activity */
    protected val viewModel: RecordedTripViewModel by activityViewModels()

}