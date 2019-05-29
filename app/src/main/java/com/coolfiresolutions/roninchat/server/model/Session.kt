package com.coolfiresolutions.roninchat.server.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Session(
        var id: String = "",
        var name: String = "",
        var description: String = "",
        var status: String = "",
        var users: List<String> = ArrayList(),
        var lastMessage: LastMessage? = LastMessage(),
        var unreadMessages: Int = 0,
        var audit: Audit = Audit(),
        var startDate: String = ""
) : Parcelable