package com.coolfiresolutions.roninchat.conversation.api

import com.coolfiresolutions.roninchat.conversation.model.Conversation
import com.coolfiresolutions.roninchat.server.callback.RoninAPICallback
import retrofit2.Response

class ConversationsServiceCallback(private val listener: ConversationsCallbackListener) : RoninAPICallback<List<Conversation>>() {
    interface ConversationsCallbackListener {
        fun onRequestSuccess(conversations: List<Conversation>?)
        fun onRequestFailure()
    }

    override fun onRequestSuccess(response: Response<*>, conversations: List<Conversation>?) {
        listener.onRequestSuccess(conversations)
    }

    override fun onRequestFailure(throwable: Throwable) {
        listener.onRequestFailure()
    }
}