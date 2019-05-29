package com.coolfiresolutions.roninchat.channel

import com.coolfiresolutions.roninchat.server.callback.RoninAPICallback
import com.coolfiresolutions.roninchat.server.model.Session
import retrofit2.Response

class SessionsServiceCallback(val listener: SessionsServiceCallbackListener) : RoninAPICallback<List<Session>>() {
    interface SessionsServiceCallbackListener {
        fun onRequestSuccess(sessions: List<Session>?)
        fun onRequestFailure()
    }

    override fun onRequestSuccess(response: Response<*>, sessions: List<Session>?) {
        listener.onRequestSuccess(sessions)
    }

    override fun onRequestFailure(throwable: Throwable) {
        listener.onRequestFailure()
    }

}