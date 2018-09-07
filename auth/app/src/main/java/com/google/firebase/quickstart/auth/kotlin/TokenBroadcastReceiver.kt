package com.google.firebase.quickstart.auth.kotlin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log


/**
 * Receiver to capture tokens broadcast via ADB and insert them into the
 * running application to facilitate easy testing of custom authentication.
 */
abstract class TokenBroadcastReceiver : BroadcastReceiver() {

    private val TAG = "TokenBroadcastReceiver"


    val EXTRA_KEY_TOKEN = "key_token"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive:$intent")

        if (ACTION_TOKEN == intent.action) {
            val token = intent.extras?.getString(EXTRA_KEY_TOKEN)
            onNewToken(token)
        }
    }

    abstract fun onNewToken(token: String?)

    companion object {
        val ACTION_TOKEN = "com.google.example.ACTION_TOKEN"
        val filter: IntentFilter
            get() = IntentFilter(ACTION_TOKEN)
    }

}
