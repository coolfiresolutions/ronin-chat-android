package com.coolfiresolutions.roninchat.server.model

data class Network(
        var id: String = "",
        var name: String = "",
        var description: String = "",
        var users: ArrayList<String> = ArrayList(),
        var isHidden: Boolean = false,
        var iconUrl: String = "",
        var isPrivate: Boolean = false,
        var geofenceId: String = "",
        var color: String = ""
)