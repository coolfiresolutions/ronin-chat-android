package com.coolfiresolutions.roninchat.server.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ServerScope(var id: String = "", var type: String = "") : Parcelable