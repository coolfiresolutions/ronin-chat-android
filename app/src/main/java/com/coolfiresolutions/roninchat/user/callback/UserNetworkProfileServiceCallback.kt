package com.coolfiresolutions.roninchat.user.callback

import com.coolfiresolutions.roninchat.server.callback.RoninAPICallback
import com.coolfiresolutions.roninchat.user.model.UserNetworkProfile
import retrofit2.Response

class UserNetworkProfileServiceCallback(private val listener: UserNetworkProfileCallbackListener) : RoninAPICallback<UserNetworkProfile>() {
    interface UserNetworkProfileCallbackListener {
        fun onRequestSuccess(userNetworkProfile: UserNetworkProfile?)
        fun onRequestFailure()
    }

    override fun onRequestSuccess(response: Response<*>, userNetworkProfile: UserNetworkProfile?) {
        listener.onRequestSuccess(userNetworkProfile)
    }

    override fun onRequestFailure(throwable: Throwable) {
        listener.onRequestFailure()
    }
}