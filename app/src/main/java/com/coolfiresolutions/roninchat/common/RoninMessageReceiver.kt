package com.coolfiresolutions.roninchat.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.coolfiresolutions.roninchat.server.model.RoninMessage

const val TEXT_INTENT_FILTER = "textMessageFilter"
const val GROUP_INTENT_FILTER = "groupMessageFilter"
const val SESSION_INTENT_FILTER = "sessionMessageFilter"

const val KEY_MESSAGE = "keyTextMessage"

class RoninMessageReceiver(val listener: MessageReceiverListener) : BroadcastReceiver() {
    interface MessageReceiverListener {
        fun onMessageReceived(message: RoninMessage)
    }

    companion object {
        fun getTextMessageIntent(message: RoninMessage): Intent {
            val intent = Intent(TEXT_INTENT_FILTER)
            intent.putExtra(KEY_MESSAGE, message)
            return intent
        }

        fun getGroupMessageIntent(message: RoninMessage): Intent {
            val intent = Intent(GROUP_INTENT_FILTER)
            intent.putExtra(KEY_MESSAGE, message)
            return intent
        }

        fun getSessionMessageIntent(message: RoninMessage): Intent {
            val intent = Intent(SESSION_INTENT_FILTER)
            intent.putExtra(KEY_MESSAGE, message)
            return intent
        }

        fun getTextMessageIntentFilter() = IntentFilter(TEXT_INTENT_FILTER)
        fun getGroupMessageIntentFilter() = IntentFilter(GROUP_INTENT_FILTER)
        fun getSessionMessageIntentFilter() = IntentFilter(SESSION_INTENT_FILTER)
    }

    override fun onReceive(context: Context, intent: Intent) {
        listener.onMessageReceived(intent.getParcelableExtra(KEY_MESSAGE))
    }
}