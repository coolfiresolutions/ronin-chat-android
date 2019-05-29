package com.coolfiresolutions.roninchat.conversation.group

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.support.design.widget.BottomNavigationView
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import com.coolfiresolutions.roninchat.R
import com.coolfiresolutions.roninchat.common.BaseFragment
import com.coolfiresolutions.roninchat.common.RoninMessageReceiver
import com.coolfiresolutions.roninchat.common.createFile
import com.coolfiresolutions.roninchat.common.getMediaType
import com.coolfiresolutions.roninchat.conversation.MessagesAdapter
import com.coolfiresolutions.roninchat.conversation.api.MessagesServiceCallback
import com.coolfiresolutions.roninchat.conversation.model.GroupConversation
import com.coolfiresolutions.roninchat.server.callback.MessageAttachmentServiceCallback
import com.coolfiresolutions.roninchat.server.enums.EntityType
import com.coolfiresolutions.roninchat.server.enums.MessageActions
import com.coolfiresolutions.roninchat.server.model.MessageAttachment
import com.coolfiresolutions.roninchat.server.model.RoninMessage
import com.coolfiresolutions.roninchat.server.model.ServerActor
import com.coolfiresolutions.roninchat.server.model.ServerScope
import kotlinx.android.synthetic.main.fragment_group_conversation.*
import kotlinx.android.synthetic.main.toolbar_top_details.*
import org.joda.time.DateTime
import org.json.JSONObject
import java.io.File
import kotlin.math.absoluteValue

class GroupConversationFragment : BaseFragment() {
    lateinit var listener: GroupConversationListener
    private lateinit var groupConversation: GroupConversation
    private lateinit var adapter: MessagesAdapter
    private var isWritingAttachmentToDisk = false
    private var attachment: File? = null

    interface GroupConversationListener {
        fun onEditGroupConversationClicked(groupConversation: GroupConversation)
    }

    companion object {
        val TAG = GroupConversationFragment::class.java.canonicalName!!

        private const val KEY_GROUP_CONVERSATION = "keyGroupConversation"
        private const val SELECT_IMAGE = 4444

        fun newInstance(groupConversation: GroupConversation): GroupConversationFragment {
            val bundle = Bundle()
            bundle.putParcelable(KEY_GROUP_CONVERSATION, groupConversation)

            val fragment = GroupConversationFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_group_conversation, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        groupConversation = arguments!!.getParcelable(KEY_GROUP_CONVERSATION)!!
        listener = activity as GroupConversationListener

        adapter = MessagesAdapter(getApplication().networkProfile.userId, groupConversation.users)
        val layoutManager = LinearLayoutManager(activity)
        layoutManager.stackFromEnd = true
        rvMessages.layoutManager = layoutManager
        rvMessages.adapter = adapter

        getApplication().api.retrieveGroupConversationMessages(getApplication().networkProfile.network, groupConversation.id, messagesCallbackListener)
        tvDetailsToolbarText.text = groupConversation.getConversationName()

        initClickListeners()

        rvMessages.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            val y = oldBottom - bottom
            if (y.absoluteValue > 0) {
                rvMessages.post {
                    if (adapter.itemCount > 0) rvMessages.smoothScrollToPosition(adapter.itemCount)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.findViewById<BottomNavigationView>(R.id.nav_view)?.visibility = View.GONE
        getApplication().localBroadcastManager.registerReceiver(roninMessageReceiver, RoninMessageReceiver.getTextMessageIntentFilter())
    }

    override fun onPause() {
        super.onPause()
        getApplication().localBroadcastManager.unregisterReceiver(roninMessageReceiver)
    }

    override fun onStop() {
        super.onStop()
        activity?.findViewById<BottomNavigationView>(R.id.nav_view)?.visibility = View.VISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SELECT_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            //Clear if a previous attachment was attached but not sent
            attachment = null
            displayLoadingAttachmentView(true)
            AsyncTask.execute {
                val bitmap = MediaStore.Images.Media.getBitmap(activity!!.contentResolver, data.data)
                attachment = bitmap.createFile(activity!!)

                activity?.runOnUiThread {
                    displayLoadingAttachmentView(false, bitmap)
                }
            }

        } else if (resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(activity, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getWindowSoftInputMode(): Int {
        return WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
    }

    private fun initClickListeners() {
        ivSend.setOnClickListener {
            sendMessage()
        }

        ivToolbarCancel.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }

        ivAddAttachment.setOnClickListener {
            displayAttachmentChooser()
        }

        ivRemoveAttachment.setOnClickListener {
            if (attachment != null) {
                attachment = null
                displayAttachmentView(false)
            }
        }

        ivToolbarDetails.setOnClickListener {
            listener.onEditGroupConversationClicked(groupConversation)
        }
    }

    private fun displayAttachmentChooser() {
        if (isWritingAttachmentToDisk) {
            Toast.makeText(activity!!, "Please wait until current file is finished", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Image"), SELECT_IMAGE)
    }

    private fun sendMessage() {
        if (etMessage.text.isNullOrEmpty() && attachment == null) {
            Toast.makeText(activity!!, "Please enter a message first.", Toast.LENGTH_SHORT).show()
            return
        }

        isSendingMessage(true)
        if (attachment != null) {
            getApplication().api.uploadFile(attachment!!.name, attachment!!.getMediaType(), attachment!!, messageAttachmentCallbackListener)
        } else {
            getApplication().serverClientManager.sendMessage(getChatMessage(), messageResponseListener)
        }
    }

    private fun getChatMessage(messageAttachments: List<MessageAttachment>? = null): RoninMessage {
        val chatMessage = RoninMessage()
        //Actor ID should be our ID and a specified type of "user"
        chatMessage.actorId = ServerActor(getApplication().networkProfile.userId, EntityType.USER.urlString)
        //Data contains all text, attachment, and mentions within the message
        chatMessage.data?.set("body", etMessage.text.toString())
        //The Scope will be your target of the message and their object type (in this case we're sending a message to a "userGroup")
        chatMessage.targets.add(ServerScope(groupConversation.id, EntityType.USERGROUP.urlString))
        //The time of the message with the generic server format so it can be parsed properly by the server
        chatMessage.sent = DateTime().toString("yyyy-MM-dd'T'HH:mm:ss.SSS")
        chatMessage.type = EntityType.TEXT.urlString
        chatMessage.action = MessageActions.CREATE.urlString
        //A network must be set to correctly scope conversations to the current network
        chatMessage.data?.set(EntityType.NETWORK.urlString, getApplication().networkProfile.network)

        messageAttachments?.let {
            chatMessage.data?.set("attachments", it)
            attachment = null
        }

        return chatMessage
    }

    private fun addMessageToConversation(message: RoninMessage) {
        adapter.messages.add(message)
        adapter.notifyDataSetChanged()
        rvMessages.smoothScrollToPosition(adapter.itemCount)
    }

    //Real-time callback
    private var roninMessageReceiver = RoninMessageReceiver(object : RoninMessageReceiver.MessageReceiverListener {
        override fun onMessageReceived(message: RoninMessage) {
            //Check if the target is the current conversation
            if (message.targets[0].id == groupConversation.id) {
                addMessageToConversation(message)
                //Tells server that we've viewed the latest message
                getApplication().serverClientManager.markMessageAsRead(message.id, getApplication().networkProfile.network)
            }
        }
    })

    //<editor-fold desc="Private UI methods"
    private fun isSendingMessage(isSending: Boolean) {
        pbSendingProgress.visibility = if (isSending) View.VISIBLE else View.GONE
        ivSend.visibility = if (isSending) View.GONE else View.VISIBLE
    }

    private fun displayErrorScreen(shouldDisplay: Boolean) {
        rvMessages.visibility = if (shouldDisplay) View.GONE else View.VISIBLE
        ivErrorMessage.visibility = if (shouldDisplay) View.VISIBLE else View.GONE
    }

    private fun displayLoadingAttachmentView(isLoading: Boolean, bitmap: Bitmap? = null) {
        if (isLoading) {
            isWritingAttachmentToDisk = true
            displayAttachmentView(isLoading)
            pbAttachmentProgress.visibility = View.VISIBLE
            ivSend.isEnabled = false
        } else {
            isWritingAttachmentToDisk = false
            pbAttachmentProgress.visibility = View.GONE
            ivSend.isEnabled = true
        }
        ivAttachment.setImageBitmap(bitmap)
    }

    private fun displayAttachmentView(shouldDisplay: Boolean) {
        if (shouldDisplay) {
            ivAttachment.visibility = View.VISIBLE
            ivRemoveAttachment.visibility = View.VISIBLE
        } else {
            ivAttachment.setImageDrawable(null)
            ivAttachment.visibility = View.GONE
            ivRemoveAttachment.visibility = View.GONE
        }
    }
    //</editor-fold>

    //<editor-fold desc="API callbacks"
    //Callback for sending a message to the conversation
    private var messageResponseListener = object : RoninMessage.MessageResponseListener() {
        override fun onMessageError(errorJson: JSONObject, errorMessage: RoninMessage) {
            if (!this@GroupConversationFragment.isVisible) {
                return
            }

            activity?.runOnUiThread {
                Toast.makeText(activity, "Error sending message", Toast.LENGTH_SHORT).show()
                isSendingMessage(false)
            }
        }

        override fun onMessageSuccess(message: RoninMessage) {
            if (!this@GroupConversationFragment.isVisible) {
                return
            }

            activity?.runOnUiThread {
                displayAttachmentView(false)
                ivNoMessages.visibility = View.GONE
                etMessage.setText("")
                isSendingMessage(false)
                addMessageToConversation(message)
            }
        }
    }

    //Callback for when attachment has been uploaded to server
    private var messageAttachmentCallbackListener = object : MessageAttachmentServiceCallback.MessageAttachmentCallbackListener {
        override fun onRequestSuccess(messageAttachment: MessageAttachment?) {
            if (!this@GroupConversationFragment.isVisible) {
                return
            }

            messageAttachment?.let {
                getApplication().serverClientManager.sendMessage(getChatMessage(listOf(it)), messageResponseListener)
            }
        }

        override fun onRequestFailure() {

        }
    }

    //Callback for when we retrieve the initial list of messages in the conversation
    private var messagesCallbackListener = object : MessagesServiceCallback.MessagesCallbackListener {
        override fun onRequestSuccess(messages: List<RoninMessage>?) {
            if (!this@GroupConversationFragment.isVisible) {
                return
            }

            messages?.let {
                if (messages.isEmpty()) {
                    ivNoMessages.visibility = View.VISIBLE
                } else {
                    ivNoMessages.visibility = View.GONE
                }
                adapter.messages = ArrayList(it)
                adapter.notifyDataSetChanged()
                rvMessages.smoothScrollToPosition(adapter.itemCount)
            }
        }

        override fun onRequestFailure() {
            if (!this@GroupConversationFragment.isVisible) {
                return
            }
            displayErrorScreen(true)
        }
    }
    //</editor-fold>
}