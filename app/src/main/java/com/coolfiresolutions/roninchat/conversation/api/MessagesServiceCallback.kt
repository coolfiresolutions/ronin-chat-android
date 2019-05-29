package com.coolfiresolutions.roninchat.conversation.api

import com.coolfiresolutions.roninchat.server.callback.RoninAPICallback
import com.coolfiresolutions.roninchat.server.model.RoninMessage
import retrofit2.Response

class MessagesServiceCallback(val listener: MessagesCallbackListener) : RoninAPICallback<List<RoninMessage>>() {
    interface MessagesCallbackListener {
        fun onRequestSuccess(messages: List<RoninMessage>?)
        fun onRequestFailure()
    }

    override fun onRequestSuccess(response: Response<*>, messages: List<RoninMessage>?) {
        listener.onRequestSuccess(messages)
    }

    override fun onRequestFailure(throwable: Throwable) {
        listener.onRequestFailure()
    }
}