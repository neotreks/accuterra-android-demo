package com.neotreks.accuterra.mobile.demo.util

import com.neotreks.accuterra.mobile.sdk.model.Result

/**
 * This class is has an empty implementation in this Demo App!
 *
 * Utility class to report errors or crashes
 * intended to centralize handling of such reporting
 */
object CrashSupport {

    /**
     * Provide some custom data to Crashlytics
     * */
    internal fun logSdkInfo() {
        // No implementation in the Demo App
        // You can log e.g.:
        //      "sdk_version" => SdkInfo().versionName
        //      "sdk_db_model" => SdkInfo().dbModelVersion
    }

    /**
     * Report exception to crash reporting tool
     * */
    internal fun reportError(e: Throwable, logMessage: String? = null) {
        // No implementation in the Demo App
    }

    /**
     * Report exception to crash reporting tool
     * */
    fun <T>reportError(result: Result<T>, logMessage: String? = null) {
        // No implementation in the Demo App
    }

    /**
     * Report exception to crash reporting tool
     * */
    fun reportError(errorMessage: String?) {
        // No implementation in the Demo App
    }

    /**
     * Put into the `log` before reporting the error or crash
     */
    fun log(logMessage: String) {
        // No implementation in the Demo App
    }

}