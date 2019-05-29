package com.coolfiresolutions.roninchat.server.model

import com.coolfiresolutions.roninchat.user.model.User

data class UserGroup(
        var id: String = "",
        var name: String = "",
        var isHidden: Boolean = false,
        var users: ArrayList<User> = ArrayList(),
        var lastMessage: LastMessage? = null
)