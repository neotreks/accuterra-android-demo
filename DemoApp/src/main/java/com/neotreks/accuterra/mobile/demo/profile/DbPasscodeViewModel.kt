package com.neotreks.accuterra.mobile.demo.profile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * View Model for the [DbPasscodeActivity]
 */
class DbPasscodeViewModel: ViewModel() {

    var currentPassword: MutableLiveData<String> = MutableLiveData("")

}