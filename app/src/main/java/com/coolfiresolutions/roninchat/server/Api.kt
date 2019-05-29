package com.coolfiresolutions.roninchat.server

import com.coolfiresolutions.roninchat.channel.CreateSessionBody
import com.coolfiresolutions.roninchat.channel.SessionServiceCallback
import com.coolfiresolutions.roninchat.channel.SessionsServiceCallback
import com.coolfiresolutions.roninchat.conversation.api.ConversationsServiceCallback
import com.coolfiresolutions.roninchat.conversation.api.MessagesServiceCallback
import com.coolfiresolutions.roninchat.conversation.group.CreateGroupConversationBody
import com.coolfiresolutions.roninchat.conversation.group.GroupConversationServiceCallback
import com.coolfiresolutions.roninchat.server.callback.GenericServiceCallback
import com.coolfiresolutions.roninchat.server.callback.MessageAttachmentServiceCallback
import com.coolfiresolutions.roninchat.server.callback.NetworkServiceCallback
import com.coolfiresolutions.roninchat.server.callback.ServerInfoServiceCallback
import com.coolfiresolutions.roninchat.server.model.PatchBody
import com.coolfiresolutions.roninchat.server.model.ServerConstants
import com.coolfiresolutions.roninchat.user.callback.UserNetworkProfileServiceCallback
import com.coolfiresolutions.roninchat.user.callback.UserProfileServiceCallback
import com.coolfiresolutions.roninchat.user.callback.UserServiceCallback
import com.coolfiresolutions.roninchat.user.callback.UsersServiceCallback
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.util.*

//Server key for uploading a file
const val FILE_KEY = "file"
//Server key for hiding UserGroups
const val USERGROUP_IS_HIDDEN = "isHidden"

class Api(serverClientManager: ServerClientManager) {
    private val apiEndpoints: ApiEndpoints =
            serverClientManager.registerAPIService(ApiEndpoints::class.java)

    fun retrieveServerInfo(listener: ServerInfoServiceCallback.ServerInfoCallbackListener) {
        val call = apiEndpoints.getServerInfo()
        call.enqueue(ServerInfoServiceCallback(listener))
    }

    fun retrieveNetworks(listener: NetworkServiceCallback.NetworkCallbackListener) {
        val call = apiEndpoints.getNetworks()
        call.enqueue(NetworkServiceCallback(listener))
    }

    fun retrieveUserNetworkProfile(networkId: String, listener: UserNetworkProfileServiceCallback.UserNetworkProfileCallbackListener) {
        val call = apiEndpoints.getUserNetworkProfile(networkId)
        call.enqueue(UserNetworkProfileServiceCallback(listener))
    }

    fun retrieveUserProfile(listener: UserProfileServiceCallback.UserProfileCallbackListener) {
        val call = apiEndpoints.getUserProfile()
        call.enqueue(UserProfileServiceCallback(listener))
    }

    fun retrieveConversations(networkId: String, listener: ConversationsServiceCallback.ConversationsCallbackListener) {
        val call = apiEndpoints.getConversations(networkId)
        call.enqueue(ConversationsServiceCallback(listener))
    }

    fun retrieveUsers(networkId: String, listener: UsersServiceCallback.UsersCallbackListener) {
        val call = apiEndpoints.getUsers(networkId)
        call.enqueue(UsersServiceCallback(listener))
    }

    fun retrieveUser(userId: String, listener: UserServiceCallback.UserCallbackListener) {
        val call = apiEndpoints.getUser(userId)
        call.enqueue(UserServiceCallback(listener))
    }

    fun retrieveUserConversationMessages(networkId: String, userId: String, listener: MessagesServiceCallback.MessagesCallbackListener) {
        val call = apiEndpoints.getUserConversationMessages(networkId, userId)
        call.enqueue(MessagesServiceCallback(listener))
    }

    fun retrieveGroupConversationMessages(networkId: String, groupId: String, listener: MessagesServiceCallback.MessagesCallbackListener) {
        val call = apiEndpoints.getGroupConversationMessages(networkId, groupId)
        call.enqueue(MessagesServiceCallback(listener))
    }

    fun patchGroupConversationName(groupId: String, value: Any, listener: GenericServiceCallback.GenericCallbackListener) {
        val body = PatchBody(ServerConstants.PATCH_VALUE_REPLACE, "/name", value)
        val bodies = arrayOf(body)
        val call = apiEndpoints.patchConversation(bodies.asList(), groupId)
        call.enqueue(GenericServiceCallback(listener))
    }

    fun patchChannelName(channelId: String, value: Any, listener: GenericServiceCallback.GenericCallbackListener) {
        val body = PatchBody(ServerConstants.PATCH_VALUE_REPLACE, "/name", value)
        val bodies = arrayOf(body)
        val call = apiEndpoints.patchChannel(bodies.asList(), channelId)
        call.enqueue(GenericServiceCallback(listener))
    }

    fun createUserGroup(body: CreateGroupConversationBody, listener: GroupConversationServiceCallback.GroupConversationCallbackListener) {
        val call = apiEndpoints.createUserGroup(body)
        call.enqueue(GroupConversationServiceCallback(listener))
    }

    fun retrieveSessions(networkId: String, listener: SessionsServiceCallback.SessionsServiceCallbackListener) {
        val call = apiEndpoints.getSessions(networkId)
        call.enqueue(SessionsServiceCallback(listener))
    }

    fun retrieveSessionMessages(networkId: String, sessionId: String, listener: MessagesServiceCallback.MessagesCallbackListener) {
        val call = apiEndpoints.getSessionMessages(networkId, sessionId)
        call.enqueue(MessagesServiceCallback(listener))
    }

    fun createSession(body: CreateSessionBody, listener: SessionServiceCallback.SessionServiceCallbackListener) {
        val call = apiEndpoints.createSession(body)
        call.enqueue(SessionServiceCallback(listener))
    }

    fun uploadFile(filename: String, mediaType: String, file: File, listener: MessageAttachmentServiceCallback.MessageAttachmentCallbackListener) {
        val requestBody = RequestBody.create(MediaType.parse(mediaType), file)
        val body = MultipartBody.Part.createFormData(FILE_KEY, filename, requestBody)

        val call = apiEndpoints.uploadFile(body)
        call.enqueue(MessageAttachmentServiceCallback(listener))
    }

    fun logout(deviceId: String, listener: GenericServiceCallback.GenericCallbackListener) {
        val call = apiEndpoints.logout(deviceId)
        call.enqueue(GenericServiceCallback(listener))
    }

    fun deleteUserConversation(networkId: String, userId: String, listener: GenericServiceCallback.GenericCallbackListener) {
        val call = apiEndpoints.deleteUserConversation(networkId, userId)
        call.enqueue(GenericServiceCallback(listener))
    }

    fun hideUserGroupConversation(networkId: String, userGroupId: String, listener: GenericServiceCallback.GenericCallbackListener) {
        val body = HashMap<String, Any>()
        body[USERGROUP_IS_HIDDEN] = true
        val call = apiEndpoints.hideUserGroupConversation(body, networkId, userGroupId)
        call.enqueue(GenericServiceCallback(listener))
    }

    fun closeSession(sessionId: String, listener: GenericServiceCallback.GenericCallbackListener) {
        val body = PatchBody(ServerConstants.PATCH_VALUE_REPLACE, "/status", "closed")
        val call = apiEndpoints.patchSession(sessionId, arrayOf(body))
        call.enqueue(GenericServiceCallback(listener))
    }
}