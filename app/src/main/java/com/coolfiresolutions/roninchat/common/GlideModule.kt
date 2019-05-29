package com.coolfiresolutions.roninchat.common

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.target.ViewTarget
import com.coolfiresolutions.roninchat.R

@GlideModule
class GlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        ViewTarget.setTagId(R.id.glide_request)
    }
}