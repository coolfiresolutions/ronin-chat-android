package com.coolfiresolutions.roninchat.server.callback

import com.coolfiresolutions.roninchat.server.model.MessageAttachment
import retrofit2.Response

class MessageAttachmentServiceCallback(private val listener: MessageAttachmentCallbackListener) : RoninAPICallback<MessageAttachment>() {
    interface MessageAttachmentCallbackListener {
        fun onRequestSuccess(messageAttachment: MessageAttachment?)
        fun onRequestFailure()
    }

    override fun onRequestSuccess(response: Response<*>, messageAttachment: MessageAttachment?) {
        listener.onRequestSuccess(messageAttachment)
    }

    override fun onRequestFailure(throwable: Throwable) {
        listener.onRequestFailure()
    }
}