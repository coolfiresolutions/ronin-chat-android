package com.coolfiresolutions.roninchat.server.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

@Parcelize
data class LastMessage(
        var sent: String = "",
        var data: @RawValue HashMap<String, Any?> = HashMap(),
        var actorId: ServerActor = ServerActor()
) : Parcelable {

    val text: String
        get() {
            val lastText = data["body"] as? String
            return when {
                //Last message has attachments but no text
                !(data["attachments"] as List<Any?>?).isNullOrEmpty() && lastText.isNullOrEmpty() -> "Attachment"
                else -> lastText ?: ""
            }
        }
}