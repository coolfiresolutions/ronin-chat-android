package com.coolfiresolutions.roninchat.server

import com.coolfiresolutions.roninchat.channel.CreateSessionBody
import com.coolfiresolutions.roninchat.conversation.group.CreateGroupConversationBody
import com.coolfiresolutions.roninchat.conversation.model.Conversation
import com.coolfiresolutions.roninchat.server.model.*
import com.coolfiresolutions.roninchat.user.model.User
import com.coolfiresolutions.roninchat.user.model.UserNetworkProfile
import com.coolfiresolutions.roninchat.user.model.UserProfile
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ApiEndpoints {
    @GET("server/info")
    fun getServerInfo(): Call<ServerInfo>

    @GET("api/v1/networks")
    fun getNetworks(): Call<List<Network>>

    @GET("api/v1/users/userprofile")
    fun getUserProfile(): Call<UserProfile>

    @GET("api/v1/networks/{networkId}/userprofile")
    fun getUserNetworkProfile(@Path("networkId") networkId: String): Call<UserNetworkProfile>

    @GET("api/v1/networks/{networkId}/userprofile/allConversations")
    fun getConversations(@Path("networkId") networkId: String): Call<List<Conversation>>

    @GET("api/v1/networks/{networkId}/users")
    fun getUsers(@Path("networkId") networkId: String): Call<List<User>>

    @GET("api/v1/users/{userId}")
    fun getUser(@Path("userId") userId: String): Call<User>

    //Retrieving messages via the userprofile endpoint automatically flags all the retrieved messages as read
    @GET("api/v1/networks/{networkId}/userprofile/users/{userId}/messages")
    fun getUserConversationMessages(@Path("networkId") networkId: String, @Path("userId") userId: String): Call<List<RoninMessage>>

    @GET("api/v1/networks/{networkId}/userprofile/usergroups/{userGroupId}/messages")
    fun getGroupConversationMessages(@Path("networkId") networkId: String, @Path("userGroupId") userGroupId: String): Call<List<RoninMessage>>

    @GET("api/v1/networks/{networkId}/sessions/withRecentActivity?status=open")
    fun getSessions(@Path("networkId") networkId: String): Call<List<Session>>

    @GET("api/v1/networks/{networkId}/userprofile/sessions/{sessionId}/messages")
    fun getSessionMessages(@Path("networkId") networkId: String, @Path("sessionId") sessionId: String): Call<List<RoninMessage>>

    @GET("api/users")
    fun getUsers(@Query("_id[]") ids: List<String>): Call<List<User>>

    @POST("api/v1/userGroups")
    fun createUserGroup(@Body body: CreateGroupConversationBody): Call<UserGroup>

    @POST("api/v1/sessions")
    fun createSession(@Body body: CreateSessionBody): Call<Session>

    @Multipart
    @POST("api/v1/files")
    fun uploadFile(@Part file: MultipartBody.Part): Call<MessageAttachment>

    @POST("logout")
    fun logout(@Header("deviceid") deviceId: String): Call<ResponseBody>

    @DELETE("api/v1/networks/{networkId}/userprofile/users/{userId}")
    fun deleteUserConversation(@Path("networkId") networkId: String, @Path("userId") userId: String): Call<ResponseBody>

    @PUT("api/v1/networks/{networkId}/userprofile/userGroups/{userGroupId}")
    fun hideUserGroupConversation(@Body body: HashMap<String, Any>, @Path("networkId") networkId: String, @Path("userGroupId") userGroupId: String): Call<ResponseBody>

    @PATCH("api/v1/sessions/{sessionId}")
    fun patchSession(@Path("sessionId") sessionId: String, @Body body: Array<PatchBody>): Call<ResponseBody>

    @PATCH("api/v1/userGroups/{id}")
    fun patchConversation(@Body patchProfile: List<PatchBody>, @Path("id") id: String): Call<ResponseBody>

    @PATCH("api/v1/sessions/{sessionId}")
    fun patchChannel(@Body patchProfile: List<PatchBody>, @Path("sessionId") sessionId: String): Call<ResponseBody>
}