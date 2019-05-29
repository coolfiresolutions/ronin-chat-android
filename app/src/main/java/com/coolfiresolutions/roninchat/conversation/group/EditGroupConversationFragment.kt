package com.coolfiresolutions.roninchat.conversation.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.coolfiresolutions.roninchat.R
import com.coolfiresolutions.roninchat.common.BaseFragment
import com.coolfiresolutions.roninchat.conversation.model.GroupConversation
import com.coolfiresolutions.roninchat.server.callback.GenericServiceCallback
import kotlinx.android.synthetic.main.fragment_edit_group_conversation.*

class EditGroupConversationFragment : BaseFragment() {
    private lateinit var groupConversation: GroupConversation

    companion object {
        val TAG = EditGroupConversationFragment::class.java.canonicalName!!
        private const val KEY_GROUP_CONVERSATION = "keyGroupConversation"

        fun newInstance(groupConversation: GroupConversation): EditGroupConversationFragment {
            val bundle = Bundle()
            bundle.putParcelable(KEY_GROUP_CONVERSATION, groupConversation)

            val fragment = EditGroupConversationFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_edit_group_conversation, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        groupConversation = arguments!!.getParcelable(KEY_GROUP_CONVERSATION)!!

        etGroupName.setText(groupConversation.name)

        initClickListeners()
    }

    private fun initClickListeners() {
        ivToolbarBack.setOnClickListener {
            activity!!.onBackPressed()
        }

        ivSave.setOnClickListener {
            groupConversation.name = etGroupName.text.toString()
            getApplication().api.patchGroupConversationName(groupConversation.id, groupConversation.name, conversationPatchCallbackListener)
        }
    }

    private var conversationPatchCallbackListener = object : GenericServiceCallback.GenericCallbackListener {
        override fun onRequestSuccess() {
            if (!this@EditGroupConversationFragment.isVisible) {
                return
            }
            activity!!.onBackPressed()
        }

        override fun onRequestFailure() {
            if (!this@EditGroupConversationFragment.isVisible) {
                return
            }
            Toast.makeText(activity, "Error updating group's name", Toast.LENGTH_SHORT).show()
        }
    }
}