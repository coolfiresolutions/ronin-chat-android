package com.coolfiresolutions.roninchat.server.interfaces

import com.coolfiresolutions.roninchat.server.model.AuthError

interface ServerClientListener {
    /**
     * Callback to provide tokens and user id after successfully authenticating to server.
     *
     * @param accessToken  Provided for convenience. Authentication orchestration is handled in the library.
     * @param refreshToken  Provided to cache for auto-logging in.
     * @param userId  User id of account that was logged in.
     */
    fun onAuthSuccess(accessToken: String, refreshToken: String, userId: String)

    /**
     * Callback to notify of authentication issue with server.
     *
     * @param authError  Error provided from server on issue with authentication.
     */
    fun onAuthError(authError: AuthError)

    /**
     * Callback to SocketIO has connected successfully
     */
    fun onSocketConnected()
}