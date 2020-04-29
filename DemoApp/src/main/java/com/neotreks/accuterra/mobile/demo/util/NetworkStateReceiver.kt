package com.neotreks.accuterra.mobile.demo.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import java.util.*

class NetworkStateReceiver(context: Context) :
    BroadcastReceiver() {
    private val mManager: ConnectivityManager
    private val mListeners: MutableList<NetworkStateReceiverListener>
    private var mConnected = true
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.extras == null) return
        if (checkStateChanged()) notifyStateToAll()
    }

    public fun isConnected() : Boolean {
        return mConnected
    }

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

    fun addListener(l: NetworkStateReceiverListener) {
        mListeners.add(l)
        notifyState(l)
    }

    fun removeListener(l: NetworkStateReceiverListener?) {
        mListeners.remove(l)
    }

    interface NetworkStateReceiverListener {
        fun onNetworkAvailable()
        fun onNetworkUnavailable()
    }

    init {
        mListeners = ArrayList()
        mManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val intentFilter = IntentFilter()
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        context.registerReceiver(this@NetworkStateReceiver, intentFilter)
        checkStateChanged()
    }
}