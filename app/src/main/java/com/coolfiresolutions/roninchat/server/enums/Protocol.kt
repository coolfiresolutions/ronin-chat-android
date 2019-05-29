package com.coolfiresolutions.roninchat.server.enums

enum class Protocol constructor(val value: String) {
    HTTP("http://"),
    HTTPS("https://"),
    INVALID("Invalid Protocol")
}