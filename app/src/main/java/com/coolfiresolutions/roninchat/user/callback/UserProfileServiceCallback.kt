package com.coolfiresolutions.roninchat.user.callback

import com.coolfiresolutions.roninchat.server.callback.RoninAPICallback
import com.coolfiresolutions.roninchat.user.model.UserProfile
import retrofit2.Response

class UserProfileServiceCallback(private val listener: UserProfileCallbackListener) : RoninAPICallback<UserProfile>() {
    interface UserProfileCallbackListener {
        fun onRequestSuccess(userProfile: UserProfile?)
        fun onRequestFailure()
    }

    override fun onRequestSuccess(response: Response<*>, userProfile: UserProfile?) {
        listener.onRequestSuccess(userProfile)
    }

    override fun onRequestFailure(throwable: Throwable) {
        listener.onRequestFailure()
    }
}