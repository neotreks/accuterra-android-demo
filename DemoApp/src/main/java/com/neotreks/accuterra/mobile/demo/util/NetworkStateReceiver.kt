package com.neotreks.accuterra.mobile.demo.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.util.Log
import java.util.*

class NetworkStateReceiver(private val context: Context) : BroadcastReceiver() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private val mManager: ConnectivityManager
    private val mListeners: MutableList<NetworkStateReceiverListener>
    private var mConnected = true

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "NetworkStateReceiver"
    }

    /* * * * * * * * * * * * */
    /*      CONSTRUCTOR      */
    /* * * * * * * * * * * * */

    init {
        mListeners = ArrayList()
        mManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        checkStateChanged()
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.extras == null) return
        if (checkStateChanged()) notifyStateToAll()
    }

    /* * * * * * * * * * * * */
    /*        PUBLIC         */
    /* * * * * * * * * * * * */

    fun onResume() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        context.registerReceiver(this@NetworkStateReceiver, intentFilter)
    }

    fun onPause() {
        try {
            context.unregisterReceiver(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error while unregistering network state listener")
        }
    }

    fun isConnected() : Boolean {
        return mConnected
    }

    fun addListener(l: NetworkStateReceiverListener) {
        mListeners.add(l)
        notifyState(l)
    }

    fun removeListener(l: NetworkStateReceiverListener?) {
        mListeners.remove(l)
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun checkStateChanged(): Boolean {
        val prev = mConnected
        val activeNetwork = mManager.activeNetworkInfo
        mConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting
        return prev != mConnected
    }

    private fun notifyStateToAll() {
        for (listener in mListeners) {
            notifyState(listener)
        }
    }

    private fun notifyState(listener: NetworkStateReceiverListener?) {
        if (listener != null) {
            if (mConnected) listener.onNetworkAvailable() else listener.onNetworkUnavailable()
        }
    }

    /* * * * * * * * * * * * */
    /*     INNER CLASS       */
    /* * * * * * * * * * * * */

    interface NetworkStateReceiverListener {
        fun onNetworkAvailable()
        fun onNetworkUnavailable()
    }

}