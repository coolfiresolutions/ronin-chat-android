package com.coolfiresolutions.roninchat.channel

data class CreateSessionBody(
        var name: String = "",
        var description: String = "",
        var network: String = ""
)