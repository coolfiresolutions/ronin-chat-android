package com.coolfiresolutions.roninchat.server.model

data class MessageAttachment(
        var id: String = "",
        var filename: String = "",
        var name: String = "",
        var contentType: String = "",
        var length: Int = 0,
        var uploadDate: String = "",
        var md5: String = "",
        var url: String = ""
)