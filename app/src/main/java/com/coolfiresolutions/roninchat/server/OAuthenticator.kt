package com.coolfiresolutions.roninchat.server

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.coolfiresolutions.roninchat.server.enums.ResponseCode
import com.coolfiresolutions.roninchat.server.model.AuthError
import com.coolfiresolutions.roninchat.server.model.ServerConfig
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

const val TAG = "OAuthenticator"

class OAuthenticator(private var context: Context, listener: AuthListener, private var serverPath: String) : Authenticator, Interceptor {

    companion object {
        private const val BEARER = "Bearer"

        //Form
        private const val FORM_CLIENT_SECRET = "client_secret"
        private const val FORM_GRANT_TYPE = "grant_type"
        private const val FORM_PASSWORD = "password"
        private const val FORM_REFRESH_TOKEN = "refresh_token"
        private const val FORM_CLIENT_ID = "client_id"
        private const val FORM_USERNAME = "username"

        //Header
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val HEADER_DEVICE_ID = "Device-ID"
        private const val HEADER_DEVICE_TYPE = "Device-Type"
        private const val HEADER_DEVICE_OS = "Device-OS"
        private const val HEADER_DEVICE_PLATFORM = "Device-Platform"
        private const val HEADER_PUSH_TOKEN = "Push-Token"
        private const val HEADER_APP_PACKAGE = "App-ID"
        private const val HEADER_DEVICE_NAME = "Device-Name"

        //Response
        private const val RESPONSE_ACCESS_TOKEN = "access_token"
        private const val RESPONSE_REFRESH_TOKEN = "refresh_token"
        private const val RESPONSE_USER_ID = "user_id"
        private const val RESPONSE_ERROR = "error"
    }

    private var accessToken: String? = null
    private var refreshToken: String? = ""
    private var authListener: AuthListener? = listener
    private var httpClient: OkHttpClient? = null
    private var serverConfig: ServerConfig? = null

    private var authCallback: Callback = object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            processFailedRequest(call, null)
        }

        @Throws(IOException::class)
        override fun onResponse(call: Call, response: Response) {
            val responseBody = response.body()!!.string()
            if (response.isSuccessful) {
                processSuccessfulRequest(call, responseBody)
            } else {
                processFailedRequest(call, responseBody)
            }
            response.close()
        }
    }

    interface AuthListener {
        fun onAuthSuccess(accessToken: String?, refreshToken: String?, userId: String)

        fun onAuthError(authError: AuthError)
    }


    // ===========================================================
    // Overrides
    // ===========================================================

    @Synchronized
    @Throws(IOException::class)
    override fun authenticate(route: Route?, response: Response?): Request? {
        if (route == null || response == null) {
            return null
        }

        var request: Request? = null
        if (response.code() == ResponseCode.UNAUTHORIZED.code && Endpoints.OAUTH_TOKEN.getRelativePath(serverPath).equals(
                        response.request().url().encodedPath()
                )
        ) {
            return null
        } else if (accessToken == null) {
            return null
        } else {
            if (isAuthHeaderExist(response.request())) {
                if (response.code() == ResponseCode.UNAUTHORIZED.code) {
                    val requestAccessToken: String?
                    val requestAccessTokenSplit =
                            response.request().header(HEADER_AUTHORIZATION)!!.split(" ".toRegex())
                                    .dropLastWhile { it.isEmpty() }
                                    .toTypedArray()
                    requestAccessToken =
                            if (requestAccessTokenSplit.size == 2) requestAccessTokenSplit[1] else null

                    if (accessToken == requestAccessToken) {
                        val refreshRequest = buildRefreshRequest(serverConfig!!)
                        val refreshResponse = httpClient!!.newCall(refreshRequest).execute()

                        if (!refreshResponse.isSuccessful) {
                            Log.e(TAG, "unsuccessful refresh")
                            authListener!!.onAuthError(
                                    AuthError(
                                            "Unauthorized",
                                            "Unauthorized"
                                    )
                            )
                            return null
                        }

                        refreshToken(refreshResponse)
                    }

                    request = response.request().newBuilder()
                            .header(HEADER_AUTHORIZATION, "$BEARER $accessToken").build()
                }
            } else {
                request = response.request().newBuilder()
                        .header(HEADER_AUTHORIZATION, "$BEARER $accessToken")
                        .build()
            }

            return request
        }
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var originalRequest = chain.request()
        if (accessToken != null) {
            //add our access token onto this call, the interceptor should catch the bad header and refresh
            originalRequest = originalRequest.newBuilder()
                    .header(HEADER_AUTHORIZATION, "$BEARER $accessToken")
                    .build()
        }
        return chain.proceed(originalRequest)
    }

    // ===========================================================
    // Public Methods
    // ===========================================================

    fun setHttpClient(httpClient: OkHttpClient) {
        this.httpClient = httpClient
    }

    fun authByServerConfig(
            httpClient: OkHttpClient,
            serverConfig: ServerConfig,
            pushToken: String
    ) {

        val authRequest = buildAuthRequest(serverConfig, pushToken)
        httpClient.newCall(authRequest).enqueue(authCallback)
        this.serverConfig = serverConfig
    }

    fun logout() {
        if (serverConfig == null) {
            return  //null check for serverConfig being released before this call is made
        }

        val logoutRequest = Request.Builder()
                .url(Endpoints.LOG_OUT.getPath(serverConfig!!.getUrlWithProtocol(), serverPath))
                .post(RequestBody.create(null, ByteArray(0)))
                .build()
        httpClient!!.newCall(logoutRequest).enqueue(authCallback)
    }

    // ===========================================================
    // Process auth callbacks
    // ===========================================================

    private fun processFailedRequest(call: Call, responseBody: String?) {
        val requestUrl = call.request().url().toString()
        if (requestUrl.contains(Endpoints.OAUTH_TOKEN.getRelativePath(serverPath))) {
            processFailedTokenRequest(responseBody)
        }
    }

    private fun processSuccessfulRequest(call: Call, responseBody: String) {
        val requestUrl = call.request().url().toString()
        if (requestUrl.contains(Endpoints.OAUTH_TOKEN.getRelativePath(serverPath))) {
            try {
                val jsonObject = JSONObject(responseBody)
                if (jsonObject.has(RESPONSE_ERROR)) {
                    processFailedRequest(call, responseBody)
                } else {
                    processTokenSuccessRequest(responseBody)
                }
            } catch (e: JSONException) {
                Log.e(TAG, e.message)
            }

        } else if (requestUrl.contains(Endpoints.LOG_OUT.getRelativePath(serverPath))) {
            processLogoutSuccess()
        }
    }

    // ===========================================================
    // Private Methods
    // ===========================================================

    private fun processTokenSuccessRequest(responseBody: String) {
        val obj: JSONObject
        try {
            obj = JSONObject(responseBody)
            accessToken = obj.getString(RESPONSE_ACCESS_TOKEN)
            refreshToken = obj.getString(RESPONSE_REFRESH_TOKEN)
            val userId = obj.getString(RESPONSE_USER_ID)

            //Notify client
            if (authListener != null) {
                authListener!!.onAuthSuccess(accessToken, refreshToken, userId)
            }
        } catch (e: JSONException) {
            Log.e(TAG, e.message)
        }
    }

    private fun processFailedTokenRequest(responseBody: String?) {
        var authError: AuthError? =
                AuthError("Unknown", "Unknown Error")
        if (responseBody != null) {
            authError = JSONMapperUtil.createObjectByJSONString(
                    responseBody,
                    AuthError::class.java
            )
        }

        if (authListener != null) {
            authListener!!.onAuthError(
                    authError ?: AuthError(
                            "Unknown",
                            "Unknown Error"
                    )
            )
        }
    }

    private fun processLogoutSuccess() {
        accessToken = null
        refreshToken = null
    }

    @Throws(IOException::class)
    private fun refreshToken(refreshResponse: Response) {
        val obj: JSONObject
        try {
            obj = JSONObject(refreshResponse.body()!!.string())
            accessToken = obj.getString(RESPONSE_ACCESS_TOKEN)
            refreshToken = obj.getString(RESPONSE_REFRESH_TOKEN)
            val userId = obj.getString(RESPONSE_USER_ID)

            if (authListener != null) {
                authListener!!.onAuthSuccess(accessToken, refreshToken, userId)
            }
        } catch (e: JSONException) {
            Log.e(TAG, e.message)
        } catch (e: IOException) {
            Log.e(TAG, e.message)
        }
    }

    private fun buildAuthRequest(serverConfig: ServerConfig, pushToken: String?): Request {
        val formBody: RequestBody

        if (serverConfig.customHeaders == null || serverConfig.customHeaders!!.size == 0) {
            //this is our standard password login
            formBody = FormBody.Builder()
                    .add(FORM_GRANT_TYPE, FORM_PASSWORD)
                    .add(FORM_USERNAME, serverConfig.username)
                    .add(FORM_PASSWORD, serverConfig.password)
                    .add(FORM_CLIENT_ID, serverConfig.clientId)
                    .add(FORM_CLIENT_SECRET, serverConfig.clientSecret)
                    .build()
        } else {
            //custom logic to use custom headers
            val builder = FormBody.Builder()
                    .add(FORM_USERNAME, serverConfig.username)
                    .add(FORM_CLIENT_ID, serverConfig.clientId)
                    .add(FORM_CLIENT_SECRET, serverConfig.clientSecret)

            for (entry in serverConfig.customHeaders!!.entries) {
                builder.add(entry.key, entry.value)
            }

            formBody = builder.build()
        }


        return Request.Builder()
                .url(Endpoints.OAUTH_TOKEN.getPath(serverConfig.getUrlWithProtocol(), serverPath))
                .post(formBody)
                .header(HEADER_DEVICE_ID, DeviceInfoUtility.getDeviceID(context))
                .header(HEADER_DEVICE_TYPE, DeviceInfoUtility.deviceName)
                .header(HEADER_DEVICE_OS, DeviceInfoUtility.deviceOS)
                .header(HEADER_DEVICE_PLATFORM, "Android")
                .header(HEADER_APP_PACKAGE, context.applicationContext.packageName)
                .header(HEADER_DEVICE_NAME, DeviceInfoUtility.userDeviceName)
                .header(HEADER_PUSH_TOKEN, pushToken ?: "").build()
    }

    private fun buildRefreshRequest(serverConfig: ServerConfig): Request {
        val formBody = FormBody.Builder()
                .add(FORM_GRANT_TYPE, FORM_REFRESH_TOKEN)
                .add(FORM_CLIENT_ID, serverConfig.clientId)
                .add(FORM_CLIENT_SECRET, serverConfig.clientSecret)
                .add(FORM_REFRESH_TOKEN, refreshToken!!)
                .build()

        return Request.Builder()
                .url(Endpoints.OAUTH_TOKEN.getPath(serverConfig.getUrlWithProtocol(), serverPath))
                .post(formBody).build()
    }

    private fun isAuthHeaderExist(request: Request): Boolean {
        val header = request.header(HEADER_AUTHORIZATION)
        return !(header == null || TextUtils.isEmpty(header))
    }
}

private enum class Endpoints constructor(private var relativePath: String) {
    LOG_OUT("logout"),
    OAUTH_TOKEN("oauth2/token");

    fun getRelativePath(serverPath: String): String {
        return "/$serverPath$relativePath"
    }

    fun getPath(url: String, serverExtension: String): String {
        return "$url/$serverExtension$relativePath"
    }
}
