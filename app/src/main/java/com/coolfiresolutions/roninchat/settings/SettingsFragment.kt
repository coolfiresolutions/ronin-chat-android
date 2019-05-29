package com.coolfiresolutions.roninchat.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.coolfiresolutions.roninchat.R
import com.coolfiresolutions.roninchat.common.BaseFragment
import com.coolfiresolutions.roninchat.common.GlideApp
import com.coolfiresolutions.roninchat.common.getGravatarUrlByEmail
import com.coolfiresolutions.roninchat.server.DeviceInfoUtility
import com.coolfiresolutions.roninchat.server.callback.GenericServiceCallback
import kotlinx.android.synthetic.main.fragment_settings.*

class SettingsFragment : BaseFragment() {
    companion object {
        val TAG = SettingsFragment::class.java.canonicalName!!
        fun newInstance() = SettingsFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val userProfile = getApplication().userProfile
        val serverInfo = getApplication().serverInfo

        tvUserName.text = getString(R.string.dual_string_placeholder, userProfile.firstName, userProfile.lastName)
        tvUserEmail.text = userProfile.email
        btnUserAvatar.text = userProfile.firstName.firstOrNull()?.toString()

        tvEnvironmentName.text = serverInfo.hostName
        tvVersionNumber.text = serverInfo.serverVersion

        btnSignOut.setOnClickListener {
            getApplication().api.logout(DeviceInfoUtility.getDeviceID(activity!!), logoutCallbackListener)
        }

        btnLinkGravatar.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://en.gravatar.com"))
            startActivity(browserIntent)
        }
    }

    override fun onResume() {
        super.onResume()
        //Doing this on resume in case a user adds a gravatar via the link
        GlideApp.with(activity!!).load(getApplication().userProfile.email.getGravatarUrlByEmail()).circleCrop().into(ivUserAvatar)
    }

    private val logoutCallbackListener = object : GenericServiceCallback.GenericCallbackListener {
        override fun onRequestSuccess() {
            if (!this@SettingsFragment.isVisible) {
                return
            }
            getApplication().serverClientManager.stopRealTime()
            getApplication().serverClientManager.logout()
            activity?.finish()
        }

        override fun onRequestFailure() {

        }
    }
}