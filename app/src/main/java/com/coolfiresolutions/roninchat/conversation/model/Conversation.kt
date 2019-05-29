package com.coolfiresolutions.roninchat.conversation.model

import com.coolfiresolutions.roninchat.server.model.LastMessage
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.joda.time.DateTime

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tgtType", defaultImpl = UserConversation::class)
@JsonSubTypes(
        JsonSubTypes.Type(value = UserConversation::class, name = "user"),
        JsonSubTypes.Type(value = GroupConversation::class, name = "userGroup")
)
abstract class Conversation(var id: String = "",
                            var unreadMessages: Int = 0,
                            var lastMessage: LastMessage? = LastMessage(),
                            var tgtType: String? = null,
                            var sentDate: DateTime = DateTime()) {


    abstract fun getConversationName(): String
    abstract fun getUserCount(): Int
}