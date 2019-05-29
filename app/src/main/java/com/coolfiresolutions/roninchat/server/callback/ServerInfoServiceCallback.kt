package com.coolfiresolutions.roninchat.server.callback

import com.coolfiresolutions.roninchat.server.model.ServerInfo
import retrofit2.Response

class ServerInfoServiceCallback(private val listener: ServerInfoCallbackListener) : RoninAPICallback<ServerInfo>() {
    interface ServerInfoCallbackListener {
        fun onRequestSuccess(serverInfo: ServerInfo?)
        fun onRequestFailure()
    }

    override fun onRequestSuccess(response: Response<*>, serverInfo: ServerInfo?) {
        listener.onRequestSuccess(serverInfo)
    }

    override fun onRequestFailure(throwable: Throwable) {
        listener.onRequestFailure()
    }
}