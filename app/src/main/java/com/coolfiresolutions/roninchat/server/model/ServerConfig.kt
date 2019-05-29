package com.coolfiresolutions.roninchat.server.model

import android.os.Parcelable
import com.coolfiresolutions.roninchat.server.enums.Protocol
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
class ServerConfig : Parcelable {
    var protocol: Protocol? = null
    var url: String? = null
    var username: String? = null
    var password: String? = null
    var clientSecret: String? = null
    var clientId: String? = null
    var customHeaders: HashMap<String, String>? = null
    var accessToken: String? = null
    var refreshToken: String? = null
    var userId: String? = null
    var id: Long = 0

    fun getUrlWithProtocol(): String {
        return protocol?.value + url
    }
}