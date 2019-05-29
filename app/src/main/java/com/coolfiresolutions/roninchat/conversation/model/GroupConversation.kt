package com.coolfiresolutions.roninchat.conversation.model

import android.os.Parcelable
import com.coolfiresolutions.roninchat.server.JSONMapperUtil
import com.coolfiresolutions.roninchat.server.model.RoninMessage
import com.coolfiresolutions.roninchat.server.model.UserGroup
import com.coolfiresolutions.roninchat.user.model.User
import kotlinx.android.parcel.Parcelize

@Parcelize
class GroupConversation(
        var name: String = "",
        var users: ArrayList<User> = ArrayList()
) : Conversation(), Parcelable {

    constructor(userGroup: UserGroup) : this() {
        this.id = userGroup.id
        this.name = userGroup.name
        this.users = userGroup.users
        this.lastMessage = userGroup.lastMessage
    }

    //Full UserGroup is returned in the RoninMessage data
    constructor(message: RoninMessage) :
            this(JSONMapperUtil.createObjectByJSONMap(message.data, UserGroup::class.java))

    override fun getConversationName(): String {
        return name
    }

    override fun getUserCount(): Int {
        return users.size
    }
}
