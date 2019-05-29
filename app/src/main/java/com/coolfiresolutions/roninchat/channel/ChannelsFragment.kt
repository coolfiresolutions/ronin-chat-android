package com.coolfiresolutions.roninchat.channel

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.coolfiresolutions.roninchat.R
import com.coolfiresolutions.roninchat.common.BaseFragment
import com.coolfiresolutions.roninchat.common.GlideApp
import com.coolfiresolutions.roninchat.common.RoninMessageReceiver
import com.coolfiresolutions.roninchat.common.TwoButtonDialogFragment
import com.coolfiresolutions.roninchat.server.JSONMapperUtil
import com.coolfiresolutions.roninchat.server.callback.GenericServiceCallback
import com.coolfiresolutions.roninchat.server.model.LastMessage
import com.coolfiresolutions.roninchat.server.model.RoninMessage
import com.coolfiresolutions.roninchat.server.model.Session
import com.coolfiresolutions.roninchat.user.callback.UsersServiceCallback
import com.coolfiresolutions.roninchat.user.model.User
import kotlinx.android.synthetic.main.fragment_channels.*
import kotlinx.android.synthetic.main.toolbar_top_add.*

class ChannelsFragment : BaseFragment(), TwoButtonDialogFragment.TextTwoButtonDialogListener {
    lateinit var adapter: ChannelAdapter
    lateinit var listener: ChannelsFragmentListener
    var allChannels = ArrayList<Session>()

    interface ChannelsFragmentListener {
        fun onAddChannelClicked()
        fun onChannelClicked(channel: Session)
    }

    companion object {
        const val CHANNEL_ID_KEY = "channelIdKey"
        val TAG = ChannelsFragment::class.java.canonicalName!!

        fun newInstance() = ChannelsFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_channels, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        listener = activity as ChannelsFragmentListener

        adapter = ChannelAdapter(channelAdapterListener)
        rvChannels.layoutManager = LinearLayoutManager(activity)
        rvChannels.adapter = adapter

        GlideApp.with(activity!!).load(R.drawable.loader).into(ivLoader)

        getApplication().api.retrieveUsers(getApplication().networkProfile.network, usersServiceCallbackListener)
        getApplication().api.retrieveSessions(getApplication().networkProfile.network, sessionsServiceCallbackListener)

        ivToolbarAdd.setOnClickListener {
            listener.onAddChannelClicked()
        }

        etSearchChannels.addTextChangedListener(searchTextWatcher)
    }

    override fun onResume() {
        super.onResume()
        getApplication().localBroadcastManager.registerReceiver(roninSessionMessageReceiver, RoninMessageReceiver.getSessionMessageIntentFilter())
        getApplication().localBroadcastManager.registerReceiver(textMessageReceiver, RoninMessageReceiver.getTextMessageIntentFilter())
    }

    override fun onPause() {
        super.onPause()
        getApplication().localBroadcastManager.unregisterReceiver(roninSessionMessageReceiver)
        getApplication().localBroadcastManager.unregisterReceiver(textMessageReceiver)
    }

    override fun onStop() {
        super.onStop()
        etSearchChannels.setText("")
    }

    override fun onDialogAction(bundle: Bundle) {
        if (!bundle.getBoolean(TwoButtonDialogFragment.RESPONSE)) {
            return
        }
        val channelId = bundle.getString(CHANNEL_ID_KEY)!!
        getApplication().api.closeSession(channelId, object : GenericServiceCallback.GenericCallbackListener {
            override fun onRequestSuccess() {
                if (!this@ChannelsFragment.isVisible) {
                    return
                }
                val channel = adapter.channels.firstOrNull { it.id == channelId }
                adapter.channels.remove(channel)
                adapter.notifyDataSetChanged()
            }

            override fun onRequestFailure() {

            }

        })
    }

    private val searchTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {

        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            s?.let {
                adapter.channels = allChannels.filter { it.name.contains(s, true) || it.description.contains(s, true) }.toMutableList()
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun displayLoader(shouldDisplay: Boolean) {
        ivLoader.visibility = if (shouldDisplay) View.VISIBLE else View.GONE
        ivLoaderText.visibility = if (shouldDisplay) View.VISIBLE else View.GONE
    }

    private fun displayErrorScreen(shouldDisplay: Boolean) {
        displayLoader(false)
        rvChannels.visibility = if (shouldDisplay) View.GONE else View.VISIBLE
        ivErrorMessage.visibility = if (shouldDisplay) View.VISIBLE else View.GONE
    }

    private fun sortChannelsList() {
        allChannels.sortByDescending { it.lastMessage?.sent }

        adapter.channels = allChannels
        adapter.notifyDataSetChanged()
    }

    private val textMessageReceiver = RoninMessageReceiver(object : RoninMessageReceiver.MessageReceiverListener {
        override fun onMessageReceived(message: RoninMessage) {
            val lastMessage = LastMessage(message.sent!!, HashMap(message.data))

            if (message.action == "create") {
                //Need to find out what channel the message is targeted at
                val channel = adapter.channels.firstOrNull { it.id == message.targets[0].id }
                channel?.let {
                    it.lastMessage = lastMessage
                    it.unreadMessages++
                    adapter.notifyDataSetChanged()
                }
            }
            sortChannelsList()
        }
    })

    private val roninSessionMessageReceiver = RoninMessageReceiver(object : RoninMessageReceiver.MessageReceiverListener {
        override fun onMessageReceived(message: RoninMessage) {
            val channelFilter = etSearchChannels.text.toString()

            if (message.action == "create") {
                val channel = JSONMapperUtil.createObjectByJSONMap(message.data, Session::class.java)
                allChannels.add(0, channel)
                adapter.channels.add(0, channel)
                adapter.channels = allChannels.filter { it.name.contains(channelFilter, true) || it.description.contains(channelFilter, true) }.toMutableList()
                adapter.notifyDataSetChanged()
            } else if (message.action == "update") {
                val channel = JSONMapperUtil.createObjectByJSONMap(message.data, Session::class.java)

                if (channel.status == "closed") {
                    //Channel was closed by someone else
                    adapter.channels.remove(adapter.channels.firstOrNull { it.id == channel.id })
                    adapter.notifyDataSetChanged()
                } else {
                    //Channel was updated (eg. name changed, severity updated, description changed, etc)
                    adapter.channels.remove(adapter.channels.firstOrNull { it.id == channel.id })
                    adapter.channels.add(0, channel)
                    adapter.channels = allChannels.filter { it.name.contains(channelFilter, true) || it.description.contains(channelFilter, true) }.toMutableList()
                    adapter.notifyDataSetChanged()
                }
            }
        }
    })

    private val channelAdapterListener = object : ChannelAdapter.ChannelAdapterListener {
        override fun onChannelLongPressed(channel: Session) {
            val dialog = TwoButtonDialogFragment.newInstance(
                    "Deleting this will remove it for all users.  Are you sure?",
                    "Delete",
                    "Cancel",
                    Bundle().apply {
                        putString(CHANNEL_ID_KEY, channel.id)
                    }
            )
            dialog.setTargetFragment(this@ChannelsFragment, 0)
            dialog.show(activity?.supportFragmentManager, TwoButtonDialogFragment.TAG)
        }

        override fun onChannelClicked(channel: Session) {
            listener.onChannelClicked(channel)
        }
    }

    private val usersServiceCallbackListener = object : UsersServiceCallback.UsersCallbackListener {
        override fun onRequestSuccess(users: List<User>?) {
            if (!this@ChannelsFragment.isVisible) {
                return
            }
            users?.let {
                adapter.users = HashMap(it.associate { user ->
                    user.id to user
                })
                adapter.notifyDataSetChanged()
            }
        }

        override fun onRequestFailure() {
            if (!this@ChannelsFragment.isVisible) {
                return
            }
            displayErrorScreen(true)
        }

    }

    private val sessionsServiceCallbackListener = object : SessionsServiceCallback.SessionsServiceCallbackListener {
        override fun onRequestSuccess(sessions: List<Session>?) {
            if (!this@ChannelsFragment.isVisible) {
                return
            }
            sessions?.let {
                for (session in it) {
                    //Joining a SocketIO room is how we trigger receiving real-time updates for targets scoped to that room
                    getApplication().serverClientManager.joinRoom(session.id)
                }
                allChannels = ArrayList(it)
                displayLoader(false)
                sortChannelsList()
            }
        }

        override fun onRequestFailure() {
            if (!this@ChannelsFragment.isVisible) {
                return
            }
            displayErrorScreen(true)
        }
    }
}