package com.coolfiresolutions.roninchat.user.callback

import com.coolfiresolutions.roninchat.server.callback.RoninAPICallback
import com.coolfiresolutions.roninchat.user.model.User
import retrofit2.Response

class UsersServiceCallback(private val listener: UsersCallbackListener) : RoninAPICallback<List<User>>() {
    interface UsersCallbackListener {
        fun onRequestSuccess(users: List<User>?)
        fun onRequestFailure()
    }

    override fun onRequestSuccess(response: Response<*>, users: List<User>?) {
        listener.onRequestSuccess(users)
    }

    override fun onRequestFailure(throwable: Throwable) {
        listener.onRequestFailure()
    }
}