package com.neotreks.accuterra.mobile.demo.profile

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.ActivityDbPasscodeBinding
import com.neotreks.accuterra.mobile.demo.longToast
import com.neotreks.accuterra.mobile.demo.security.DemoDbEncryptProvider
import com.neotreks.accuterra.mobile.demo.settings.UserSettingsActivity
import com.neotreks.accuterra.mobile.demo.toast
import com.neotreks.accuterra.mobile.demo.ui.RegExInputFilter
import com.neotreks.accuterra.mobile.demo.util.DialogUtil
import com.neotreks.accuterra.mobile.demo.util.InputDialogOptions
import com.neotreks.accuterra.mobile.sdk.DBPasscodeBuilder
import com.neotreks.accuterra.mobile.sdk.SdkManager
import com.neotreks.accuterra.mobile.sdk.db.DbDomain
import kotlinx.coroutines.*


class DbPasscodeActivity : AppCompatActivity() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var binding: ActivityDbPasscodeBinding

    private val viewModel: DbPasscodeViewModel by viewModels()

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        fun createNavigateToIntent(context: Context): Intent {
            return Intent(context, DbPasscodeActivity::class.java)
        }
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDbPasscodeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setupButton()
        setupObservers()
        lifecycleScope.launchWhenCreated {
            refreshPasscode()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun setupToolbar() {

        setSupportActionBar(binding.activityDbPasscodeToolbar.generalToolbar)
        binding.activityDbPasscodeToolbar.generalToolbarTitle.text = getString(R.string.activity_db_passcode_title)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
    }

    private fun setupButton() {
        binding.activityDbPasscodeSetPasscode.setOnClickListener {
            showSetPasscodeDialog()
        }
    }

    private fun setupObservers() {
        viewModel.currentPassword.observe(this, { curPassword ->
            binding.activityDbPasscodeCurrentPasscode.text = curPassword ?: "-"
        })
    }

    private fun showSetPasscodeDialog() {

        DialogUtil.buildInputDialog(
            context = this,
            title = getString(R.string.activity_db_passcode_set_passcode_dialog_title),
            message = getString(R.string.activity_db_passcode_set_passcode_dialog_message),
            positiveCodeLabel = getString(R.string.activity_db_passcode_set_passcode_dialog_yes),
            negativeCodeLabel = getString(R.string.general_cancel),
            positiveCode = { newPasscode ->
                val oldPasscode = viewModel.currentPassword.value
                changePasscode(oldPasscode ?: "", newPasscode)
            },
            options = InputDialogOptions(
                // Let's limit just to basic characters for testing - but in real it can be any UTF-8 character
                inputFilters = arrayOf(RegExInputFilter("[a-zA-Z0-9_+-]")),
                minInputLength = 1,
            )
        ).show()

    }

    private fun changePasscode(oldPasscode: String, newPasscode: String) {
        GlobalScope.launch {
            var progressDialog: Dialog? = null
            try {
                supervisorScope {
                    lifecycleScope.launchWhenCreated {
                        // Display progress dialog
                        progressDialog = DialogUtil.buildBlockingProgressDialog(
                            context = this@DbPasscodeActivity,
                            title = getString(R.string.activity_db_passcode_set_passcode_set_passcode_progress)
                        )
                        progressDialog?.show()
                    }
                    // Flag indicating if the DB was already encrypted
                    val isInitialEncryption = oldPasscode.isEmpty()
                    withContext(Dispatchers.Main) {
                        // Build DB passcode provider - with new password.
                        val passcodeProvider = DemoDbEncryptProvider(
                            DBPasscodeBuilder.buildPasscodeBytes(newPasscode)
                        )
                        // Change the DB passcode in the SDK
                        if (!isInitialEncryption) {
                            // We can call `change DB passcode` only if the DB is already encrypted
                            SdkManager.changeDbPasscode(
                                DbDomain.TRIP, DBPasscodeBuilder.buildPasscodeBytes(oldPasscode),
                                passcodeProvider
                            )
                        }
                        // Save passcode - this is just demo so we can save it into preferences
                        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this@DbPasscodeActivity)
                        val editor = sharedPreferences.edit()
                        editor.putString(UserSettingsActivity.KEY_TRIP_DB_PASSCODE, newPasscode)
                        editor.apply()
                        // Now remove the temporary passcode from the provider so the passcode is not visible for anyone else
                        passcodeProvider.discardPassword()
                        lifecycleScope.launchWhenCreated {
                            toast(getString(R.string.activity_db_passcode_password_set))
                            if (isInitialEncryption) {
                                showRestartApkDialog()
                            }
                        }
                    }
                    lifecycleScope.launchWhenCreated {
                        refreshPasscode()
                        // Close progress dialog
                        progressDialog?.dismiss()
                    }
                }
            } catch (e: Exception) {
                lifecycleScope.launchWhenCreated {
                    progressDialog?.dismiss()
                    longToast(getString(R.string.activity_db_passcode_password_not_set, e.localizedMessage))
                }
            }
        }
    }

    private fun showRestartApkDialog() {
        DialogUtil.buildOkDialog(
            context = this,
            title = getString(R.string.activity_db_passcode_restart_dialog_title),
            message = getString(R.string.activity_db_passcode_restart_dialog_message) ,
        ).show()
    }

    private suspend fun refreshPasscode() {
        withContext(Dispatchers.Main) {
            // Read from shared preferences
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this@DbPasscodeActivity)
            // We need to use default value also here since
            val tripDbPasscode = sharedPreferences.getString(UserSettingsActivity.KEY_TRIP_DB_PASSCODE, "")
            viewModel.currentPassword.value = tripDbPasscode
        }
    }

}