package com.neotreks.accuterra.mobile.demo.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

/**
 * SDK preference storage
 */
abstract class AbstractPreferencesRepo(private val preferencesName: String) {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private var preferences: SharedPreferences? = null

    private var editor: SharedPreferences.Editor? = null

    /* * * * * * * * * * * * */
    /*        PUBLIC         */
    /* * * * * * * * * * * * */

    fun loadPreferences(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    open fun deletePreferences(context: Context) {
        loadPreferences(context)
        val keys = preferences?.all?.map { it.key }
        if (keys != null) {
            preferences?.edit()?.let { editor ->
                for (key in keys) {
                    editor.remove(key)
                }
                editor.clear()
                editor.commit()
            }
        }
    }

    fun getString(key: String, default: String? = null): String? {
        return getPreferences().getString(key, default)
    }

    fun getLong(key: String, default: Long): Long {
        return getPreferences().getLong(key, default)
    }

    fun setString(key: String, value: String?) {
        val editor = getEditor()

        if (value == null) {
            editor.remove(key)
        } else {
            editor.putString(key, value)
        }
    }

    fun setLong(key: String, value: Long?) {
        val editor = getEditor()

        if (value == null) {
            editor.remove(key)
        } else {
            editor.putLong(key, value)
        }
    }

    fun removeKey(key: String) {
        val editor = getEditor()
        editor.remove(key)
    }

    private fun getEditor(): SharedPreferences.Editor {
        return this.editor ?: throw IllegalStateException("You have to use `edit()` method first")
    }

    /* * * * * * * * * * * * */
    /*       PROTECTED       */
    /* * * * * * * * * * * * */

    @SuppressLint("CommitPrefEdits")
    protected fun edit(context: Context) {
        loadPreferences(context)
        editor = getPreferences().edit()
    }

    protected fun save() {
        editor?.commit()
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun getPreferences() : SharedPreferences {
        return preferences ?: throw IllegalStateException("Call load preferences first")
    }

}