package com.coolfiresolutions.roninchat.server.enums

enum class EntityType(var urlString: String) {
    USER("user"),
    USERGROUP("userGroup"),
    NETWORK("network"),
    TEXT("text"),
    SESSION("session")
}