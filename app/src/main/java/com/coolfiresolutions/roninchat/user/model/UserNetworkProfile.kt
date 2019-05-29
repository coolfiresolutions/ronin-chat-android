package com.coolfiresolutions.roninchat.user.model

import com.coolfiresolutions.roninchat.server.model.Session
import com.coolfiresolutions.roninchat.server.model.UserGroup

data class UserNetworkProfile(
        var userId: String = "",
        var unreadSessionMessageCount: Int = 0,
        var unreadConversationMessageCount: Int = 0,
        var users: ArrayList<User> = ArrayList(),
        var userGroups: ArrayList<UserGroup> = ArrayList(),
        var sessions: ArrayList<Session> = ArrayList(),
        var network: String = ""
)