package com.neotreks.accuterra.mobile.demo.settings

import android.content.Context

interface HeadingFormatter {
    fun formatHeading(context: Context, heading: Float): String
}