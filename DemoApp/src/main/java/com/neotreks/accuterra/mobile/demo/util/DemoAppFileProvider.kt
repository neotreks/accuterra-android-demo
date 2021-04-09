package com.neotreks.accuterra.mobile.demo.util

import androidx.core.content.FileProvider

/**
 * Demo APK File Provider
 */
class DemoAppFileProvider: FileProvider() {

    companion object {
        const val AUTHORITY = "com.neotreks.accuterra.mobile.demo.fileprovider"
    }

}