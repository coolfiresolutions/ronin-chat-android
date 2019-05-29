package com.coolfiresolutions.roninchat.user.callback

import com.coolfiresolutions.roninchat.server.callback.RoninAPICallback
import com.coolfiresolutions.roninchat.user.model.User
import retrofit2.Response

class UserServiceCallback(private val listener: UserCallbackListener) : RoninAPICallback<User>() {
    interface UserCallbackListener {
        fun onRequestSuccess(user: User?)
        fun onRequestFailure()
    }

    override fun onRequestSuccess(response: Response<*>, user: User?) {
        listener.onRequestSuccess(user)
    }

    override fun onRequestFailure(throwable: Throwable) {
        listener.onRequestFailure()
    }
}