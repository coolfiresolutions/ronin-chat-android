package com.coolfiresolutions.roninchat.conversation.model

import android.os.Parcelable
import com.coolfiresolutions.roninchat.user.model.User
import kotlinx.android.parcel.Parcelize

@Parcelize
class UserConversation(
        var firstName: String = "",
        var lastName: String = "",
        var email: String = ""
) : Conversation(), Parcelable {

    constructor(user: User) : this() {
        this.id = user.id
        this.firstName = user.firstName
        this.lastName = user.lastName
        this.unreadMessages = user.unreadMessages
        this.email = user.email
    }

    override fun getConversationName(): String {
        return "$firstName $lastName"
    }

    override fun getUserCount(): Int {
        return 1
    }
}