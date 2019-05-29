package com.coolfiresolutions.roninchat.server

import android.content.Context
import com.coolfiresolutions.roninchat.server.enums.Protocol
import com.coolfiresolutions.roninchat.server.interfaces.ClientMessageListener
import com.coolfiresolutions.roninchat.server.interfaces.ServerClientListener
import com.coolfiresolutions.roninchat.server.interfaces.SocketStatusListener
import com.coolfiresolutions.roninchat.server.model.AuthError
import com.coolfiresolutions.roninchat.server.model.RoninMessage
import com.coolfiresolutions.roninchat.server.model.ServerConfig
import okhttp3.Cache
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Construct for Server Library
 *
 * @param context  Android context. Can't be null
 * @param serverClientListener  Listener to handle communication of authentication and socketIO connection. Can't be null
 * @param clientMessageListener  Listener to handle Ronin messages and connection state.
 */
class ServerClientManager(
        private val context: Context?, private val serverClientListener: ServerClientListener?,
        clientMessageListener: ClientMessageListener?
) {

    lateinit var serverConfig: ServerConfig
    private var retrofit: Retrofit? = null
    /**
     * Gets the OkHTTP Server Client, we exposed this to follow proper coding practice of a single OkHTTP instance in the application, you can use this instance with glide to retrieve images and it will provide auth to the server. Returns null if you have not provided an environment
     * @return OkHttpClient
     */
    var serverHttpClient: OkHttpClient? = null
        private set
    private var oauth: OAuthenticator? = null
    private lateinit var socketIO: SocketIO<RoninMessage>

    private val joinedRooms: MutableList<String>
    private var httpTimeout: Int = 0
    private var serverPath: String? = null

    companion object {
        const val INVALID_URL = "Invalid URl"
        private const val CACHE_SIZE = 20 * 1024 * 1024 //20 mb
    }

    private val socketStatusListener = object : SocketStatusListener {
        override fun onSuccessfulConnection() {
            rejoinRooms()
            serverClientListener!!.onSocketConnected()
        }
    }

    private val authListener = object : OAuthenticator.AuthListener {
        override fun onAuthSuccess(accessToken: String?, refreshToken: String?, userId: String) {
            if (accessToken == null || refreshToken == null) {
                return
            }

            //Update server config with latest
            serverConfig.accessToken = (accessToken)
            serverConfig.refreshToken = (refreshToken)
            serverConfig.userId = (userId)

            //Socket io needs access token;
            if (socketIO.isSocketTokenExpired(accessToken)) {
                stopRealTime()
                startRealTime()
            }

            if (serverClientListener != null) {
                serverClientListener.onAuthSuccess(accessToken, refreshToken, userId)
            }
        }

        override fun onAuthError(authError: AuthError) {
            serverClientListener?.onAuthError(authError)
        }
    }

    init {
        if (context == null || serverClientListener == null || clientMessageListener == null) {
            throw IllegalArgumentException("Please provide context and valid listeners")
        }
        serverConfig = ServerConfig()
        socketIO =
                SocketIO(
                        "/",
                        RoninMessage::class.java,
                        clientMessageListener,
                        socketStatusListener
                )
        this.joinedRooms = ArrayList()
    }

    /**
     * Point the library to the correct server environment
     *
     * @param protocol  Protocol such as HTTP or HTTPS, not null
     * @param url  url of the endpoint without protocol, not null
     * @param clientId  Specified client id given by administrator of server, not null
     * @param clientSecret  Specified client secret given by administrator of server, not null
     * @param httpTimeout Set the HTTP timeout in seconds
     */
    fun setEnvironment(
            protocol: Protocol?,
            url: String?,
            clientId: String,
            clientSecret: String,
            httpTimeout: Int
    ) {
        var url = url
        if (protocol == null || url == null || HttpUrl.parse(protocol.value + url) == null) {
            serverConfig.protocol = (Protocol.INVALID)
            serverConfig.url = (INVALID_URL)
            throw IllegalArgumentException("Please provide a protocol and a valid URL")
        }
        url = url.replace(" ", "")
        this.httpTimeout = httpTimeout

        //set up config
        serverConfig.protocol = (protocol)
        serverConfig.url = (url)
        serverConfig.clientId = (clientId)
        serverConfig.clientSecret = (clientSecret)

        this.serverPath = "ronin/"
        oauth = OAuthenticator(context!!, authListener, serverPath!!)

        setupClients()
    }

    /**
     * Point the library to the correct server environment
     *
     * @param service  Generic type of service
     * @return  API service that has been registered
     */
    fun <T> registerAPIService(service: Class<T>): T {
        if (retrofit == null) {
            throw IllegalStateException("Environment must be set")
        }
        return retrofit!!.create(service)
    }

    /**
     * Start the socket real-time communication. The environment must be set and access token must be given at
     * authentication to be started.
     *
     */
    fun startRealTime() {
        if (serverConfig.accessToken == null) {
            throw IllegalStateException("Login successfully before starting")
        }
        socketIO.connect(
                serverConfig.getUrlWithProtocol(),
                serverConfig.accessToken!!,
                serverConfig.clientSecret!!,
                serverConfig.clientId!!
        )
    }

    /**
     * Stop the socket real-time communication
     *
     */
    fun stopRealTime() {
        socketIO.disconnect()
    }

    /**
     * Login to the server with username and password. Account can be created on the web or through an API post.
     *
     * @param username  Username of account, not null.
     * @param password  Password of account, not null.
     */
    fun login(username: String?, password: String?, pushToken: String) {
        if (serverConfig.url == null) {
            throw IllegalStateException("Set environment before logging in")
        }

        if (username == null || password == null) {
            throw IllegalArgumentException("Please provide valid username and password")
        }

        if (serverConfig.url.equals(INVALID_URL)) {
            serverClientListener!!.onAuthError(
                    AuthError(
                            "Invalid URL",
                            "Invalid URL"
                    )
            )
            return
        }

        serverConfig.username = (username)
        serverConfig.password = (password)

        oauth!!.authByServerConfig(serverHttpClient!!, serverConfig, pushToken)
    }

    /**
     * Logout from server and stop real-time communication.
     */
    fun logout() {
        socketIO.disconnect()
        oauth!!.logout()
    }

    /**
     * Reconnect ronin server, this will skip the auth calls, but will recreate the okhttp and socket.io
     */
    fun reconnect() {
        stopRealTime()
        startRealTime()
    }

    /**
     * Send real time generic message to create, update, and delete entities
     * @param message  Generic message to be sent
     * @param messageResponseListener  Callback to notify the app what happened with the last message you sent
     */
    fun sendMessage(message: RoninMessage, messageResponseListener: RoninMessage.MessageResponseListener) {
        socketIO.sendMessage(message, messageResponseListener)
    }

    /**
     * Join a network, session, or user room id. This is required to receive messages for user-to-user and session chat.
     *
     * @param roomId  Id of room. Could be a user id or session id
     */
    fun joinRoom(roomId: String) {
        if (!joinedRooms.contains(roomId)) {
            socketIO.joinRoom(roomId)
            joinedRooms.add(roomId)
        }
    }

    fun joinNetwork(networkId: String) {
        socketIO.joinNetwork(networkId)
    }

    /**
     * Mark message as Read.
     *
     * @param messageId represents the message's id to be marked as read
     */
    fun markMessageAsRead(messageId: String, networkId: String) {
        socketIO.markAsRead(messageId, networkId)
    }

    private fun rejoinRooms() {
        for (i in joinedRooms.indices) {
            socketIO.joinRoom(joinedRooms[i])
        }
    }

    private fun setupClients() {
        serverHttpClient = OkHttpClient.Builder()
                .connectTimeout(httpTimeout.toLong(), TimeUnit.SECONDS)
                .writeTimeout(httpTimeout.toLong(), TimeUnit.SECONDS)
                .readTimeout(httpTimeout.toLong(), TimeUnit.SECONDS)
                .addInterceptor(oauth!!)
                .authenticator(oauth!!)
                .cache(Cache(context?.cacheDir, CACHE_SIZE.toLong()))
                .build()
        oauth!!.setHttpClient(serverHttpClient!!)
        socketIO.setHttpClient(serverHttpClient!!)

        //Initialize new retrofit endpoint
        retrofit = Retrofit.Builder()
                .baseUrl(String.format("%s/%s", serverConfig.getUrlWithProtocol(), serverPath))
                .addConverterFactory(JacksonConverterFactory.create(JSONMapperUtil.defaultMapper()))
                .client(serverHttpClient!!)
                .build()
    }
}
