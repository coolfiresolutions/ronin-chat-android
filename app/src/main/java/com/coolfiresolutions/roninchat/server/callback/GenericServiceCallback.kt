package com.coolfiresolutions.roninchat.server.callback

import okhttp3.ResponseBody
import retrofit2.Response

class GenericServiceCallback(private val listener: GenericCallbackListener) : RoninAPICallback<ResponseBody>() {
    interface GenericCallbackListener {
        fun onRequestSuccess()
        fun onRequestFailure()
    }

    override fun onRequestSuccess(response: Response<*>, responseBody: ResponseBody?) {
        listener.onRequestSuccess()
    }

    override fun onRequestFailure(throwable: Throwable) {
        listener.onRequestFailure()
    }
}