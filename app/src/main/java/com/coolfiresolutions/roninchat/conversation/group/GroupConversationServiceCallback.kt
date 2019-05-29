package com.coolfiresolutions.roninchat.conversation.group

import com.coolfiresolutions.roninchat.server.callback.RoninAPICallback
import com.coolfiresolutions.roninchat.server.model.UserGroup
import retrofit2.Response

class GroupConversationServiceCallback(val listener: GroupConversationCallbackListener) : RoninAPICallback<UserGroup>() {
    interface GroupConversationCallbackListener {
        fun onRequestSuccess(userGroup: UserGroup?)
        fun onRequestFailure()
    }

    override fun onRequestSuccess(response: Response<*>, userGroup: UserGroup?) {
        listener.onRequestSuccess(userGroup)
    }

    override fun onRequestFailure(throwable: Throwable) {
        listener.onRequestFailure()
    }
}