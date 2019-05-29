package com.coolfiresolutions.roninchat.channel

import com.coolfiresolutions.roninchat.server.callback.RoninAPICallback
import com.coolfiresolutions.roninchat.server.model.Session
import retrofit2.Response

class SessionServiceCallback(val listener: SessionServiceCallbackListener) : RoninAPICallback<Session>() {
    interface SessionServiceCallbackListener {
        fun onRequestSuccess(session: Session?)
        fun onRequestFailure()
    }

    override fun onRequestSuccess(response: Response<*>, session: Session?) {
        listener.onRequestSuccess(session)
    }

    override fun onRequestFailure(throwable: Throwable) {
        listener.onRequestFailure()
    }

}