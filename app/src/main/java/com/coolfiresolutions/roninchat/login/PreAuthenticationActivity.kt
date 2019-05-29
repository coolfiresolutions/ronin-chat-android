package com.coolfiresolutions.roninchat.login

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.coolfiresolutions.roninchat.R
import com.coolfiresolutions.roninchat.RoninChatApplication

class PreAuthenticationActivity : AppCompatActivity(), LoginFragment.LoginFragmentListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preauthentication)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.flMainContent, LoginFragment.newInstance(), LoginFragment.TAG)
                    .commit()
        }
    }

    ////////////////////////////////////////////////////////////////////////
    // Fragment callbacks
    ////////////////////////////////////////////////////////////////////////

    override fun onLoginClicked(instanceUrl: String, username: String, password: String, listener: RoninChatApplication.LoginCallback) {
        (application as RoninChatApplication).updateEnvironment(instanceUrl)
        (application as RoninChatApplication).login(username, password, listener)
    }
}