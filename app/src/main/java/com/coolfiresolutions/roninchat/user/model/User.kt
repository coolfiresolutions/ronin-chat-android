package com.coolfiresolutions.roninchat.user.model

import android.os.Parcelable
import com.coolfiresolutions.roninchat.conversation.model.UserConversation
import kotlinx.android.parcel.Parcelize

@Parcelize
data class User(
        var id: String = "",
        var firstName: String = "",
        var lastName: String = "",
        var unreadMessages: Int = 0,
        var email: String = ""
) : Parcelable {

    constructor(userConversation: UserConversation) :
            this(userConversation.id,
                    userConversation.firstName,
                    userConversation.lastName,
                    userConversation.unreadMessages,
                    userConversation.email)
}