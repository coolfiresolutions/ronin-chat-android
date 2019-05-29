package com.coolfiresolutions.roninchat.conversation.group

data class CreateGroupConversationBody(val name: String, val network: String, val users: List<String>)