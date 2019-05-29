package com.coolfiresolutions.roninchat

import android.app.Application
import android.os.Handler
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.Toast
import com.coolfiresolutions.roninchat.common.MainActivity
import com.coolfiresolutions.roninchat.common.RoninMessageReceiver
import com.coolfiresolutions.roninchat.server.Api
import com.coolfiresolutions.roninchat.server.ServerClientManager
import com.coolfiresolutions.roninchat.server.SocketIO
import com.coolfiresolutions.roninchat.server.callback.NetworkServiceCallback
import com.coolfiresolutions.roninchat.server.callback.ServerInfoServiceCallback
import com.coolfiresolutions.roninchat.server.enums.EntityType
import com.coolfiresolutions.roninchat.server.enums.Protocol
import com.coolfiresolutions.roninchat.server.interfaces.ClientMessageListener
import com.coolfiresolutions.roninchat.server.interfaces.ServerClientListener
import com.coolfiresolutions.roninchat.server.model.AuthError
import com.coolfiresolutions.roninchat.server.model.Network
import com.coolfiresolutions.roninchat.server.model.RoninMessage
import com.coolfiresolutions.roninchat.server.model.ServerInfo
import com.coolfiresolutions.roninchat.user.callback.UserNetworkProfileServiceCallback
import com.coolfiresolutions.roninchat.user.callback.UserProfileServiceCallback
import com.coolfiresolutions.roninchat.user.model.OnlineUser
import com.coolfiresolutions.roninchat.user.model.UserNetworkProfile
import com.coolfiresolutions.roninchat.user.model.UserProfile
import net.danlew.android.joda.JodaTimeAndroid

const val CLIENT_ID = "{{PUT_YOUR_GENERATED_CLIENT_ID_HERE}}"
const val CLIENT_SECRET = "{{PUT_YOUR_GENERATED_CLIENT_SECRET_HERE}}"
const val HTTP_TIMEOUT = 10

class RoninChatApplication : Application() {
    lateinit var serverClientManager: ServerClientManager
    lateinit var api: Api
    lateinit var serverInfo: ServerInfo
    lateinit var networkProfile: UserNetworkProfile
    lateinit var userProfile: UserProfile
    lateinit var localBroadcastManager: LocalBroadcastManager
    lateinit var loginCallback: LoginCallback

    interface LoginCallback {
        fun onLoginSuccess()
        fun onLoginFailure()
    }

    override fun onCreate() {
        super.onCreate()
        JodaTimeAndroid.init(this)
        serverClientManager = ServerClientManager(this, clientAuthListener, clientMessageListener)
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
    }

    fun updateEnvironment(instanceUrl: String) {
        serverClientManager.setEnvironment(
                Protocol.HTTPS,
                instanceUrl,
                CLIENT_ID,
                CLIENT_SECRET,
                HTTP_TIMEOUT)
    }

    fun login(username: String, password: String, listener: LoginCallback) {
        this.loginCallback = listener
        serverClientManager.login(username, password, "")
    }

    private fun onLoginSuccess() {
        //Need to wait for valid retrofit connection before creating the Api service
        api = Api(serverClientManager)
        //Start real-time communications on a successful login
        serverClientManager.startRealTime()

        api.retrieveNetworks(object : NetworkServiceCallback.NetworkCallbackListener {
            override fun onRequestSuccess(networks: List<Network>?) {
                networks?.let {
                    if (it.isNotEmpty()) {
                        connectToNetwork(it[0].id)
                    }
                }
            }

            override fun onRequestFailure() {
                loginCallback.onLoginFailure()
                showToast("Error retrieving networks")
            }

        })

        api.retrieveUserProfile(object : UserProfileServiceCallback.UserProfileCallbackListener {
            override fun onRequestSuccess(userProfile: UserProfile?) {
                userProfile?.let {
                    this@RoninChatApplication.userProfile = it
                }
            }

            override fun onRequestFailure() {
                loginCallback.onLoginFailure()
                showToast("Error retrieving networks")
            }
        })

        api.retrieveServerInfo(object : ServerInfoServiceCallback.ServerInfoCallbackListener {
            override fun onRequestSuccess(serverInfo: ServerInfo?) {
                serverInfo?.let {
                    this@RoninChatApplication.serverInfo = it
                }
            }

            override fun onRequestFailure() {
                loginCallback.onLoginFailure()
            }
        })
    }

    private fun connectToNetwork(networkId: String) {
        //Joining a network allows us to receive real-time messages from that network
        api.retrieveUserNetworkProfile(networkId, object : UserNetworkProfileServiceCallback.UserNetworkProfileCallbackListener {
            override fun onRequestSuccess(userNetworkProfile: UserNetworkProfile?) {
                userNetworkProfile?.let {
                    networkProfile = it
                    serverClientManager.joinNetwork(networkId)

                    startActivity(MainActivity.getMainIntent(applicationContext))
                    loginCallback.onLoginSuccess()
                }
            }

            override fun onRequestFailure() {
                loginCallback.onLoginFailure()
                showToast("Unable to connect to network")
            }
        })
    }

    private fun showToast(message: String) {
        Handler(mainLooper).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT)
                    .show()
        }
    }

    private val clientAuthListener = object : ServerClientListener {
        override fun onAuthSuccess(accessToken: String, refreshToken: String, userId: String) {
            onLoginSuccess()
        }

        override fun onAuthError(authError: AuthError) {
            loginCallback.onLoginFailure()
            showToast("Unable to authenticate. Verify you are connected to the internet and try again.")
        }

        override fun onSocketConnected() {
            if (::networkProfile.isInitialized) {
                serverClientManager.joinNetwork(networkProfile.network)
            }
        }
    }

    private fun parseRoninMessage(message: RoninMessage) {
        val type = EntityType.values().firstOrNull { it.urlString == message.type }

        when (type) {
            EntityType.TEXT -> localBroadcastManager.sendBroadcast(RoninMessageReceiver.getTextMessageIntent(message))
            EntityType.USERGROUP -> localBroadcastManager.sendBroadcast(RoninMessageReceiver.getGroupMessageIntent(message))
            EntityType.SESSION -> localBroadcastManager.sendBroadcast(RoninMessageReceiver.getSessionMessageIntent(message))
            else -> Log.w("Unhandled RoninMessage: %s", message.type)
        }
    }

    private val clientMessageListener = object : ClientMessageListener {
        override fun onMessageReceived(message: RoninMessage) {
            parseRoninMessage(message)
        }

        override fun onMentionReceived(message: RoninMessage) {
            //Used for when a mention is received from a text message
        }

        override fun onMessengerStatusReceived(messageStatus: SocketIO.Companion.MessageStatus) {

        }

        override fun onUserJoinedNetwork(userId: String) {

        }

        override fun onJoinNetworkSuccess(onlineRoster: Array<OnlineUser>) {

        }

        override fun onUserLeftNetwork(userId: String) {

        }
    }
}