package com.coolfiresolutions.roninchat.conversation

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.coolfiresolutions.roninchat.R
import com.coolfiresolutions.roninchat.common.BaseFragment
import com.coolfiresolutions.roninchat.common.GlideApp
import com.coolfiresolutions.roninchat.common.RoninMessageReceiver
import com.coolfiresolutions.roninchat.common.TwoButtonDialogFragment
import com.coolfiresolutions.roninchat.conversation.api.ConversationsServiceCallback
import com.coolfiresolutions.roninchat.conversation.model.Conversation
import com.coolfiresolutions.roninchat.conversation.model.GroupConversation
import com.coolfiresolutions.roninchat.conversation.model.UserConversation
import com.coolfiresolutions.roninchat.server.callback.GenericServiceCallback
import com.coolfiresolutions.roninchat.server.enums.EntityType
import com.coolfiresolutions.roninchat.server.model.LastMessage
import com.coolfiresolutions.roninchat.server.model.RoninMessage
import com.coolfiresolutions.roninchat.user.callback.UserServiceCallback
import com.coolfiresolutions.roninchat.user.model.User
import kotlinx.android.synthetic.main.fragment_conversations.*
import kotlinx.android.synthetic.main.toolbar_top_add.*

class ConversationsFragment : BaseFragment(), TwoButtonDialogFragment.TextTwoButtonDialogListener {
    lateinit var adapter: ConversationsAdapter
    lateinit var listener: ConversationsListener

    interface ConversationsListener {
        fun onAddConversationClicked()
        fun onUserConversationClicked(userConversation: UserConversation)
        fun onUserGroupConversationClicked(groupConversation: GroupConversation)
    }

    companion object {
        val TAG = ConversationsFragment::class.java.canonicalName!!

        const val IS_GROUP_CONVERSATION_KEY = "groupConversationKey"
        const val CONVERSATION_ID_KEY = "conversationIdKey"

        fun newInstance() = ConversationsFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_conversations, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        listener = activity as ConversationsListener

        adapter = ConversationsAdapter(messagesAdapterListener)
        rvConversations.layoutManager = LinearLayoutManager(activity)
        rvConversations.adapter = adapter

        GlideApp.with(activity!!).load(R.drawable.loader).into(ivLoader)
        getApplication().api.retrieveConversations(getApplication().networkProfile.network, conversationCallbackListener)

        ivToolbarAdd.setOnClickListener {
            listener.onAddConversationClicked()
        }
    }

    override fun onResume() {
        super.onResume()

        //Register both of these to listen for new text message and when groups are updated/created
        getApplication().localBroadcastManager.registerReceiver(roninTextMessageReceiver, RoninMessageReceiver.getTextMessageIntentFilter())
        getApplication().localBroadcastManager.registerReceiver(roninGroupMessageReceiver, RoninMessageReceiver.getGroupMessageIntentFilter())
    }

    override fun onPause() {
        super.onPause()

        getApplication().localBroadcastManager.unregisterReceiver(roninTextMessageReceiver)
        getApplication().localBroadcastManager.unregisterReceiver(roninGroupMessageReceiver)
    }

    private val roninTextMessageReceiver = RoninMessageReceiver(object : RoninMessageReceiver.MessageReceiverListener {
        override fun onMessageReceived(message: RoninMessage) {
            val lastMessage = LastMessage(message.sent!!, HashMap(message.data))
            var conversation: Conversation? = null

            if (message.targets[0].type == EntityType.USER.urlString) {
                //If the message is for a 1:1 user chat we need to get the id of the person that sent the message
                conversation = adapter.conversations.firstOrNull { it.id == message.actorId?.id }
            } else if (message.targets[0].type == EntityType.USERGROUP.urlString) {
                //If the message is for a userGroup chat we need to get the id of that userGroup
                conversation = adapter.conversations.firstOrNull { it.id == message.targets[0].id }
            }

            if (message.action == "create") {
                if (conversation != null) {
                    //Message is for an existing conversation so we just need to update the unread badge and last message
                    conversation.lastMessage = lastMessage
                    conversation.unreadMessages++
                    adapter.notifyDataSetChanged()
                } else {
                    //Message is a new conversation so we need to update the list accordingly
                    //NOTE: This will ALWAYS be a 1:1 user conversation since roninGroupMessageReceiver object handles new UserGroup creation
                    getApplication().api.retrieveUser(message.actorId!!.id, listener = object : UserServiceCallback.UserCallbackListener {
                        override fun onRequestSuccess(user: User?) {
                            user?.let {
                                val userConversation = UserConversation(it)
                                userConversation.lastMessage = lastMessage
                                userConversation.unreadMessages++
                                adapter.conversations.add(0, userConversation)
                                adapter.notifyDataSetChanged()
                            }
                        }

                        override fun onRequestFailure() {

                        }

                    })
                }
            }
            sortConversationsList(adapter.conversations)
        }
    })

    override fun onDialogAction(bundle: Bundle) {
        if (!bundle.getBoolean(TwoButtonDialogFragment.RESPONSE)) {
            return
        }

        val conversationId = bundle.getString(CONVERSATION_ID_KEY)!!
        if (bundle.getBoolean(IS_GROUP_CONVERSATION_KEY)) {
            //hide the convo if it's a group since other people are in it
            getApplication().api.hideUserGroupConversation(getApplication().networkProfile.network, conversationId, object : GenericServiceCallback.GenericCallbackListener {
                override fun onRequestSuccess() {
                    if (!this@ConversationsFragment.isVisible) {
                        return
                    }
                    val conversation = adapter.conversations.firstOrNull { it.id == conversationId }
                    adapter.conversations.remove(conversation)
                    adapter.notifyDataSetChanged()
                }

                override fun onRequestFailure() {

                }
            })
        } else {
            // delete the convo if it's only 1:1
            getApplication().api.deleteUserConversation(getApplication().networkProfile.network, conversationId, object : GenericServiceCallback.GenericCallbackListener {
                override fun onRequestSuccess() {
                    if (!this@ConversationsFragment.isVisible) {
                        return
                    }
                    val conversation = adapter.conversations.firstOrNull { it.id == conversationId }
                    adapter.conversations.remove(conversation)
                    adapter.notifyDataSetChanged()
                }

                override fun onRequestFailure() {

                }
            })
        }
    }

    private fun displayLoader(shouldDisplay: Boolean) {
        ivLoader.visibility = if (shouldDisplay) View.VISIBLE else View.GONE
        ivLoaderText.visibility = if (shouldDisplay) View.VISIBLE else View.GONE
    }

    private fun displayErrorScreen(shouldDisplay: Boolean) {
        displayLoader(false)
        rvConversations.visibility = if (shouldDisplay) View.GONE else View.VISIBLE
        ivErrorMessage.visibility = if (shouldDisplay) View.VISIBLE else View.GONE
    }

    private fun sortConversationsList(conversations: ArrayList<Conversation>) {
        conversations.sortByDescending { it.lastMessage?.sent }

        adapter.conversations = conversations
        adapter.notifyDataSetChanged()
    }

    private val roninGroupMessageReceiver = RoninMessageReceiver(object : RoninMessageReceiver.MessageReceiverListener {
        override fun onMessageReceived(message: RoninMessage) {
            if (message.action == "create") {
                adapter.conversations.add(0, GroupConversation(message))
                adapter.notifyItemChanged(0)
            }
        }
    })

    private val conversationCallbackListener = object : ConversationsServiceCallback.ConversationsCallbackListener {
        override fun onRequestSuccess(conversations: List<Conversation>?) {
            if (!this@ConversationsFragment.isVisible) {
                return
            }

            conversations?.let {
                for (conversation in conversations) {
                    if (conversation is GroupConversation) {
                        //Joining a SocketIO room is how we trigger receiving real-time updates for targets scoped to that room
                        try {
                            getApplication().serverClientManager.joinRoom(conversation.id)
                        } catch (ex: TypeCastException) {
                            Log.e(TAG, "Fragment is no longer attached to an activity")
                        }
                    }
                }
                displayLoader(false)
                sortConversationsList(ArrayList(it))
            }
        }

        override fun onRequestFailure() {
            if (!this@ConversationsFragment.isVisible) {
                return
            }

            displayErrorScreen(true)
        }
    }

    private val messagesAdapterListener = object : ConversationsAdapter.ConversationsListener {
        override fun onConversationLongPressed(conversation: Conversation) {
            val dialog = TwoButtonDialogFragment.newInstance(
                    "Remove this conversation?",
                    "Remove",
                    "Cancel",
                    Bundle().apply {
                        putBoolean(IS_GROUP_CONVERSATION_KEY, conversation is GroupConversation)
                        putString(CONVERSATION_ID_KEY, conversation.id)
                    }
            )
            dialog.setTargetFragment(this@ConversationsFragment, 0)
            dialog.show(activity?.supportFragmentManager, TwoButtonDialogFragment.TAG)
        }

        override fun onConversationClicked(conversation: Conversation) {
            if (conversation is UserConversation) {
                listener.onUserConversationClicked(conversation)
            } else if (conversation is GroupConversation) {
                listener.onUserGroupConversationClicked(conversation)
            }
        }
    }
}