package com.coolfiresolutions.roninchat.server.interfaces

import com.coolfiresolutions.roninchat.server.SocketIO
import com.coolfiresolutions.roninchat.server.model.RoninMessage
import com.coolfiresolutions.roninchat.user.model.OnlineUser

interface ClientMessageListener {
    /**
     * Receive real-time messages to notify you of entity creation, update, or deletion
     *
     * @param message
     */
    fun onMessageReceived(message: RoninMessage)

    /**
     * Receive real-time messages to notify you of entity creation, update, or deletion
     *
     * @param message
     */
    fun onMentionReceived(message: RoninMessage)

    /**
     * State of real-time socket communication.
     *
     * @param messageStatus  Current state of socket. States include connect, disconnect, and error
     */
    fun onMessengerStatusReceived(messageStatus: SocketIO.Companion.MessageStatus)

    /**
     * Receive real-time messages to notify you of a user joining a network
     *
     * @param userId  Id of user that has joined network
     */
    fun onUserJoinedNetwork(userId: String)

    /**
     * Receive callback full of online users and devices
     *
     * @param onlineRoster Array of online users and their respective devices
     */
    fun onJoinNetworkSuccess(onlineRoster: Array<OnlineUser>)

    /**
     * Receive real-time messages to notify you of a user leaving a network
     *
     * @param userId  Id of user that has left network
     */
    fun onUserLeftNetwork(userId: String)
}
