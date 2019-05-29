package com.coolfiresolutions.roninchat.server.model

data class PatchBody(
        var op: String = "",
        var path: String = "",
        var value: Any = Any()
)