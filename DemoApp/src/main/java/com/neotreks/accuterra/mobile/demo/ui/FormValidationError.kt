package com.neotreks.accuterra.mobile.demo.ui

import android.view.View

/**
 * Form Validation error
 */
data class FormValidationError(

    /**
     * Error message
     */
    val errorMessage: String,

    /**
     * UI component related to the error
     */
    val component: View? = null

)