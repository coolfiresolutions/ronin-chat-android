package com.coolfiresolutions.roninchat.server.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Audit(
        var createdBy: ServerActor = ServerActor()
) : Parcelable