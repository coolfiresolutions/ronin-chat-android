package com.coolfiresolutions.roninchat.server.callback

import com.coolfiresolutions.roninchat.server.model.Network
import retrofit2.Response

class NetworkServiceCallback(private val listener: NetworkCallbackListener) : RoninAPICallback<List<Network>>() {
    interface NetworkCallbackListener {
        fun onRequestSuccess(networks: List<Network>?)
        fun onRequestFailure()
    }

    override fun onRequestSuccess(response: Response<*>, networks: List<Network>?) {
        listener.onRequestSuccess(networks)
    }

    override fun onRequestFailure(throwable: Throwable) {
        listener.onRequestFailure()
    }
}