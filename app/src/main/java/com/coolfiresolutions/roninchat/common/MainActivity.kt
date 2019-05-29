package com.coolfiresolutions.roninchat.common

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import com.coolfiresolutions.roninchat.R
import com.coolfiresolutions.roninchat.RoninChatApplication
import com.coolfiresolutions.roninchat.channel.ChannelFragment
import com.coolfiresolutions.roninchat.channel.ChannelsFragment
import com.coolfiresolutions.roninchat.channel.EditChannelFragment
import com.coolfiresolutions.roninchat.channel.NewChannelFragment
import com.coolfiresolutions.roninchat.conversation.ConversationsFragment
import com.coolfiresolutions.roninchat.conversation.NewConversationFragment
import com.coolfiresolutions.roninchat.conversation.UserConversationFragment
import com.coolfiresolutions.roninchat.conversation.group.EditGroupConversationFragment
import com.coolfiresolutions.roninchat.conversation.group.GroupConversationFragment
import com.coolfiresolutions.roninchat.conversation.model.GroupConversation
import com.coolfiresolutions.roninchat.conversation.model.UserConversation
import com.coolfiresolutions.roninchat.server.DeviceInfoUtility
import com.coolfiresolutions.roninchat.server.callback.GenericServiceCallback
import com.coolfiresolutions.roninchat.server.model.Session
import com.coolfiresolutions.roninchat.settings.SettingsFragment
import com.coolfiresolutions.roninchat.user.model.User

class MainActivity : AppCompatActivity(),
        ConversationsFragment.ConversationsListener,
        NewConversationFragment.NewMessageListener,
        ChannelsFragment.ChannelsFragmentListener,
        GroupConversationFragment.GroupConversationListener,
        ChannelFragment.ChannelListener,
        TwoButtonDialogFragment.TextTwoButtonDialogListener {

    companion object {
        fun getMainIntent(caller: Context): Intent {
            val intent = Intent(caller, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        if (savedInstanceState == null) {
            displayConversations()
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 0) {
            TwoButtonDialogFragment.newInstance("Would you like to logout?", "Logout", "Cancel", Bundle()).show(supportFragmentManager, TwoButtonDialogFragment.TAG)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDialogAction(bundle: Bundle) {
        if (!bundle.getBoolean(TwoButtonDialogFragment.RESPONSE)) {
            return
        }

        (application as RoninChatApplication).api.logout((DeviceInfoUtility.getDeviceID(this)), logoutCallbackListener)
    }

    override fun onAddConversationClicked() {
        var fragment = supportFragmentManager.findFragmentByTag(NewConversationFragment.TAG)
        if (fragment == null) {
            fragment = NewConversationFragment.newInstance()
        }

        replaceMainFragmentWithBackStack(fragment, NewConversationFragment.TAG)
    }

    override fun onUserConversationCreated(user: User) {
        navigateToUserConversation(UserConversation(user))
    }

    override fun onUserGroupConversationCreated(groupConversation: GroupConversation) {
        var fragment = supportFragmentManager.findFragmentByTag(GroupConversationFragment.TAG)
        if (fragment == null) {
            fragment = GroupConversationFragment.newInstance(groupConversation)
        }

        replaceMainFragmentWithBackStack(fragment, GroupConversationFragment.TAG)
    }

    override fun onEditGroupConversationClicked(groupConversation: GroupConversation) {
        var fragment = supportFragmentManager.findFragmentByTag(EditGroupConversationFragment.TAG)
        if (fragment == null) {
            fragment = EditGroupConversationFragment.newInstance(groupConversation)
        }

        replaceMainFragmentWithBackStack(fragment, EditGroupConversationFragment.TAG)
    }

    override fun onEditChannelClicked(channel: Session) {
        var fragment = supportFragmentManager.findFragmentByTag(EditChannelFragment.TAG)
        if (fragment == null) {
            fragment = EditChannelFragment.newInstance(channel)
        }

        replaceMainFragmentWithBackStack(fragment, EditChannelFragment.TAG)
    }

    override fun onUserGroupConversationClicked(groupConversation: GroupConversation) {
        var fragment = supportFragmentManager.findFragmentByTag(GroupConversationFragment.TAG)
        if (fragment == null) {
            fragment = GroupConversationFragment.newInstance(groupConversation)
        }

        replaceMainFragmentWithBackStack(fragment, GroupConversationFragment.TAG)
    }

    override fun onUserConversationClicked(userConversation: UserConversation) {
        navigateToUserConversation(userConversation)
    }

    override fun onAddChannelClicked() {
        var fragment = supportFragmentManager.findFragmentByTag(NewChannelFragment.TAG)
        if (fragment == null) {
            fragment = NewChannelFragment.newInstance()
        }

        replaceMainFragmentWithBackStack(fragment, NewChannelFragment.TAG)
    }

    override fun onChannelClicked(channel: Session) {
        var fragment = supportFragmentManager.findFragmentByTag(ChannelFragment.TAG)
        if (fragment == null) {
            fragment = ChannelFragment.newInstance(channel)
        }

        replaceMainFragmentWithBackStack(fragment, ChannelFragment.TAG)
    }

    private fun navigateToUserConversation(userConversation: UserConversation) {
        var fragment = supportFragmentManager.findFragmentByTag(UserConversationFragment.TAG)
        if (fragment == null) {
            fragment = UserConversationFragment.newInstance(userConversation)
        }

        replaceMainFragmentWithBackStack(fragment, UserConversationFragment.TAG)
    }

    private fun displayConversations() {
        var fragment = supportFragmentManager.findFragmentByTag(ConversationsFragment.TAG)
        if (fragment == null) {
            fragment = ConversationsFragment.newInstance()
        }

        replaceMainFragment(fragment, ConversationsFragment.TAG)
    }

    private fun displayChannels() {
        var fragment = supportFragmentManager.findFragmentByTag(ChannelsFragment.TAG)
        if (fragment == null) {
            fragment = ChannelsFragment.newInstance()
        }

        replaceMainFragment(fragment, ChannelsFragment.TAG)
    }

    private fun displaySettings() {
        var fragment = supportFragmentManager.findFragmentByTag(SettingsFragment.TAG)
        if (fragment == null) {
            fragment = SettingsFragment.newInstance()
        }

        replaceMainFragment(fragment, SettingsFragment.TAG)
    }

    private fun replaceMainFragmentWithBackStack(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.flMainContent, fragment, tag)
                .addToBackStack(tag)
                .commit()
    }

    private fun replaceMainFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.flMainContent, fragment, tag)
                .commit()
    }

    private val logoutCallbackListener = object : GenericServiceCallback.GenericCallbackListener {
        override fun onRequestSuccess() {
            (application as RoninChatApplication).serverClientManager.stopRealTime()
            (application as RoninChatApplication).serverClientManager.logout()
            finish()
        }

        override fun onRequestFailure() {

        }
    }

    ////////////////////////////////////////////////////////////////////////
    // Navigation callbacks
    ////////////////////////////////////////////////////////////////////////

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_messages -> {
                displayConversations()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_channels -> {
                displayChannels()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_settings -> {
                displaySettings()
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }
}
