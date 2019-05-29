package com.coolfiresolutions.roninchat.server

import android.util.Log
import com.coolfiresolutions.roninchat.server.interfaces.ClientMessageListener
import com.coolfiresolutions.roninchat.server.interfaces.SocketStatusListener
import com.coolfiresolutions.roninchat.server.model.RoninMessage
import com.coolfiresolutions.roninchat.user.model.OnlineUser
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import io.socket.emitter.Emitter
import io.socket.engineio.client.Transport
import okhttp3.OkHttpClient
import org.json.JSONException
import org.json.JSONObject
import java.net.URISyntaxException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

class SocketIO<T>(
        private val socketRoute: String,
        private val messageType: Class<T>,
        private val clientMessageListener: ClientMessageListener,
        private val socketStatusListener: SocketStatusListener
) {

    private var socket: Socket? = null
    private var accessToken: String? = null
    private val queuedMessages = LinkedBlockingQueue<T>()
    private val activityListenerCallbackHolder =
            ConcurrentHashMap<String, RoninMessage.MessageResponseListener>()
    private var httpClient: OkHttpClient? = null
    private var currentlyEmptyingQueue: Boolean = false

    private val onTransport = Emitter.Listener { args ->
        val transport = args[0] as Transport
        transport.on(Transport.EVENT_REQUEST_HEADERS) { args ->
            val headers = args[0] as MutableMap<String, String>
            headers[HEADER_AUTHORIZATION] = String.format(HEADER_BEARER, accessToken)
        }
    }

    companion object {
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val HEADER_BEARER = "Bearer %s"
        private const val SOCKET_PATH = "/ronin/socket.io"
        private const val MESSAGE_KEY_ERROR = "error"
        private const val SOCKET_IO_EVENT_JOIN = "join"
        private const val SOCKET_IO_EVENT_JOIN_NETWORK = "join network"
        private const val SOCKET_IO_EVENT_MENTION = "mention"
        private const val SOCKET_IO_EVENT_USER_JOINED_NETWORK = "user joined network"
        private const val SOCKET_IO_EVENT_USER_LEFT_NETWORK = "user left network"
        private const val SOCKET_IO_EVENT_MESSAGE_READ = "messageRead"
        private const val RECONNECT_DELAY = 30000
        private const val TAG = "SocketIO"

        enum class MessageStatus {
            CONNECT,
            DISCONNECT,
            ERROR,
            NOT_CONNECTED
        }
    }

    private val onConnect = Emitter.Listener {
        clientMessageListener.onMessengerStatusReceived(Companion.MessageStatus.CONNECT)
        socketStatusListener.onSuccessfulConnection()
        sendQueuedMessages()
    }

    private val onDisconnect = Emitter.Listener {
        clientMessageListener.onMessengerStatusReceived(Companion.MessageStatus.DISCONNECT)
    }

    private val onConnectError = Emitter.Listener {
        clientMessageListener.onMessengerStatusReceived(Companion.MessageStatus.ERROR)
    }

    private val onUserJoinedNetwork = Emitter.Listener { args ->
        if (args.isEmpty()) {
            clientMessageListener.onMessengerStatusReceived(Companion.MessageStatus.ERROR)
        }

        val userId = args[0].toString()
        clientMessageListener.onUserJoinedNetwork(userId)
    }

    private val onUserLeftNetwork = Emitter.Listener { args ->
        if (args.isEmpty()) {
            clientMessageListener.onMessengerStatusReceived(Companion.MessageStatus.ERROR)
        }

        val userId = args[0].toString()
        clientMessageListener.onUserLeftNetwork(userId)
    }

    private val onMessageReceived = Emitter.Listener { args ->
        if (args.isEmpty()) {
            clientMessageListener.onMessengerStatusReceived(Companion.MessageStatus.ERROR)
        }

        if (args[0] is JSONObject) {
            val data = args[0] as JSONObject
            val message = data.toString()
            handleReceivedMessage(message)
        }
    }

    private val onMentionReceived = Emitter.Listener { args ->
        if (args.isEmpty()) {
            clientMessageListener.onMessengerStatusReceived(Companion.MessageStatus.ERROR)
        }

        if (args[0] is JSONObject) {
            val data = args[0] as JSONObject

            val message = data.toString()

            handleReceivedMention(message)
        }
    }

    private val onMessageError = Emitter.Listener {
        clientMessageListener.onMessengerStatusReceived(Companion.MessageStatus.ERROR)
    }

    private val isSocketConnected: Boolean
        get() = socket != null && socket!!.connected()

    fun setHttpClient(client: OkHttpClient) {
        httpClient = client
    }

    fun connect(url: String, accessToken: String, clientSecret: String, clientId: String) {
        //if we have passed in a custom client use it instead of a new one.
        if (httpClient != null) {
            IO.setDefaultOkHttpCallFactory(httpClient)
            IO.setDefaultOkHttpWebSocketFactory(httpClient)
        }

        val options = getOptions(accessToken, clientSecret, clientId)
        this.accessToken = accessToken

        try {
            socket = IO.socket(url + socketRoute, options)
        } catch (e: URISyntaxException) {
            Log.e(TAG, e.localizedMessage)
        }

        //establish events
        socket!!.on(Socket.EVENT_CONNECT, onConnect)
                .on(Socket.EVENT_RECONNECT, onConnect)
                .on(Socket.EVENT_DISCONNECT, onDisconnect)
                .on(Socket.EVENT_CONNECT_ERROR, onConnectError)
                .on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError)
                .on(Socket.EVENT_RECONNECT_ERROR, onConnectError)
                .on(Socket.EVENT_RECONNECT_FAILED, onConnectError)
                .on(SOCKET_IO_EVENT_MENTION, onMentionReceived)
                .on(Socket.EVENT_MESSAGE, onMessageReceived)
                .on(SOCKET_IO_EVENT_USER_JOINED_NETWORK, onUserJoinedNetwork)
                .on(SOCKET_IO_EVENT_USER_LEFT_NETWORK, onUserLeftNetwork)
                .on(Manager.EVENT_TRANSPORT, onTransport)
                .on(Socket.EVENT_ERROR, onMessageError)
        socket!!.connect()
    }

    fun disconnect() {
        if (socket != null) {
            socket!!.disconnect()
            socket!!.off(Socket.EVENT_CONNECT, onConnect)
                    .off(Socket.EVENT_RECONNECT, onConnect)
                    .off(Socket.EVENT_DISCONNECT, onDisconnect)
                    .off(Socket.EVENT_CONNECT_ERROR, onConnectError)
                    .off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError)
                    .off(Socket.EVENT_RECONNECT_ERROR, onConnectError)
                    .off(Socket.EVENT_RECONNECT_FAILED, onConnectError)
                    .off(SOCKET_IO_EVENT_MENTION, onMentionReceived)
                    .off(Socket.EVENT_MESSAGE, onMessageReceived)
                    .off(SOCKET_IO_EVENT_USER_JOINED_NETWORK, onUserJoinedNetwork)
                    .off(SOCKET_IO_EVENT_USER_LEFT_NETWORK, onUserLeftNetwork)
                    .off(Manager.EVENT_TRANSPORT, onTransport)
                    .off(Socket.EVENT_ERROR, onMessageError)

            socket!!.close()
        }
    }

    fun isSocketTokenExpired(accessToken: String): Boolean {
        return accessToken != this.accessToken
    }

    fun sendMessage(message: T, messageResponseListener: RoninMessage.MessageResponseListener) {
        if (message is RoninMessage) {
            sendRoninMessage(message as RoninMessage, messageResponseListener)
        }
    }

    private fun sendMessage(message: T) {
        if (message is RoninMessage) {
            sendRoninMessage(
                    message as RoninMessage,
                    activityListenerCallbackHolder[(message as RoninMessage).id]
            )
        }
    }

    fun joinRoom(room: String) {
        emitSocketEvent(
                room,
                "SocketIO Join Room",
                SOCKET_IO_EVENT_JOIN,
                "Tried to join room before socket was connected"
        )
    }

    fun joinNetwork(networkId: String) {
        if (isSocketConnected) {
            socket!!.emit(SOCKET_IO_EVENT_JOIN_NETWORK, networkId, Ack { args ->
                if (args != null && args[1] is JSONObject) {
                    try {
                        val callback = (args[1] as JSONObject).getString("users")
                        val onlineUsers =
                                JSONMapperUtil.createObjectByJSONString(
                                        callback,
                                        Array<OnlineUser>::class.java
                                )
                        onlineUsers?.let {
                            clientMessageListener.onJoinNetworkSuccess(it)
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            })
        } else {
            Log.d(TAG, "Tried to join network before socket was connected")
            clientMessageListener.onMessengerStatusReceived(Companion.MessageStatus.NOT_CONNECTED)
        }
    }

    // ===========================================================
    // Connect/Disconnect Listeners
    // ===========================================================

    fun markAsRead(messageId: String, networkId: String) {
        if (socket != null) {
            socket!!.emit(SOCKET_IO_EVENT_MESSAGE_READ, messageId, networkId)
        }
    }

    private fun sendRoninMessage(message: RoninMessage, messageResponseListener: RoninMessage.MessageResponseListener?) {
        if (isSocketConnected) {
            socket!!.emit(
                    Socket.EVENT_MESSAGE,
                    JSONMapperUtil.createJSONObjectByObject(message),
                    Ack { args ->
                        if (args != null && args[0] is JSONObject) {
                            val data = args[0] as JSONObject
                            if (data.has(MESSAGE_KEY_ERROR) && messageResponseListener != null) {
                                messageResponseListener.onMessageError(data, message)
                                Log.d(TAG, "SocketIO Message Send Failure: $data")
                            }
                        } else {
                            messageResponseListener!!.onMessageSuccess(message)
                        }
                    }
            )
            activityListenerCallbackHolder.remove(message.id)

            if (areMessagesQueued() && !currentlyEmptyingQueue) {
                sendQueuedMessages()
            }
        } else {
            Log.d(TAG, "Tried to send message before socket was connected")
            clientMessageListener.onMessengerStatusReceived(Companion.MessageStatus.NOT_CONNECTED)
            messageResponseListener?.let {
                activityListenerCallbackHolder[message.id] = it
            }
            queueMessage(messageType.cast(message))
        }
    }

    private fun emitSocketEvent(
            `object`: Any,
            LogLogMessage: String,
            socketIOEvent: String,
            failureMessage: String
    ) {
        Log.d(TAG, LogLogMessage)
        if (isSocketConnected) {
            socket!!.emit(socketIOEvent, `object`)
        } else {
            Log.d(TAG, failureMessage)
            clientMessageListener.onMessengerStatusReceived(Companion.MessageStatus.NOT_CONNECTED)
        }
    }

    // ===========================================================
    // Message Listeners
    // ===========================================================

    private fun getOptions(
            accessToken: String,
            clientSecret: String,
            clientId: String
    ): IO.Options {
        val options = IO.Options()
        options.path = SOCKET_PATH
        options.reconnectionDelay = RECONNECT_DELAY.toLong()
        options.query = String.format(
                "token=%s&clientSecret=%s&clientId=%s",
                accessToken,
                clientSecret,
                clientId
        )
        options.forceNew = true

        //if we have passed in a custom http client then use it
        if (httpClient != null) {
            options.webSocketFactory = httpClient
            options.callFactory = httpClient
        }

        return options
    }

    private fun queueMessage(message: T?) {
        try {
            queuedMessages.put(message)
            Log.d(
                    TAG,
                    "Message queued to be sent once socket is connected. Queue count: ${queuedMessages.size}"
            )
        } catch (e: InterruptedException) {
            Log.e(TAG, e.message)
        }
    }

    private fun sendQueuedMessages() {
        if (!areMessagesQueued()) {
            return
        }

        val messages = ArrayList<T>()
        queuedMessages.drainTo(messages)
        currentlyEmptyingQueue = true
        for (message in messages) {
            sendMessage(message)
        }
        currentlyEmptyingQueue = false
        Log.d(TAG, "Drained queue. Queue count: ${queuedMessages.size}")
    }

    private fun areMessagesQueued(): Boolean {
        return queuedMessages.size > 0
    }

    private fun handleReceivedMessage(message: String) {
        if (messageType == RoninMessage::class.java) {
            val roninMessage =
                    JSONMapperUtil.createObjectByJSONString(
                            message,
                            RoninMessage::class.java
                    )
            roninMessage?.let {
                clientMessageListener.onMessageReceived(it)
            }
        }
    }

    private fun handleReceivedMention(message: String) {
        if (messageType == RoninMessage::class.java) {
            val roninMessage =
                    JSONMapperUtil.createObjectByJSONString(
                            message,
                            RoninMessage::class.java
                    )
            roninMessage?.let {
                clientMessageListener.onMentionReceived(it)
            }
        }
    }
}
