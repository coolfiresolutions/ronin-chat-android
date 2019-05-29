package com.coolfiresolutions.roninchat.server.callback

import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response

abstract class RoninAPICallback<T> : Callback<T> {
    abstract fun onRequestSuccess(response: Response<*>, `object`: T?)
    abstract fun onRequestFailure(throwable: Throwable)

    override fun onResponse(call: Call<T>, response: Response<T>) {
        val code = response.code()
        if (code !in 200..299) {
            onRequestFailure(HttpException(response))
        } else {
            onRequestSuccess(response, response.body())
        }
    }

    override fun onFailure(call: Call<T>, t: Throwable) {
        val rawResponse = okhttp3.Response.Builder()
                .code(503)
                .message("Service Unavailable")
                .protocol(Protocol.HTTP_1_1)
                .request(Request.Builder().url("http://localhost/").build())
                .build()
        val mediaType = MediaType.get("application/json; charset=utf-8")
        val responseBody = ResponseBody.create(mediaType, "")
        val response = Response.error<T>(responseBody, rawResponse)
        onRequestFailure(HttpException(response))
    }
}
