package com.coolfiresolutions.roninchat.conversation

import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.coolfiresolutions.roninchat.R
import com.coolfiresolutions.roninchat.common.BaseFragment
import com.coolfiresolutions.roninchat.common.GlideApp
import com.coolfiresolutions.roninchat.conversation.group.CreateGroupConversationBody
import com.coolfiresolutions.roninchat.conversation.group.GroupConversationServiceCallback
import com.coolfiresolutions.roninchat.conversation.model.GroupConversation
import com.coolfiresolutions.roninchat.server.model.UserGroup
import com.coolfiresolutions.roninchat.user.UsersAdapter
import com.coolfiresolutions.roninchat.user.callback.UsersServiceCallback
import com.coolfiresolutions.roninchat.user.model.User
import kotlinx.android.synthetic.main.fragment_new_message.*
import kotlinx.android.synthetic.main.toolbar_top_cancel.*

class NewConversationFragment : BaseFragment() {
    private lateinit var adapter: UsersAdapter
    private lateinit var listener: NewMessageListener
    private var allUsers = ArrayList<User>()

    interface NewMessageListener {
        fun onUserConversationCreated(user: User)
        fun onUserGroupConversationCreated(groupConversation: GroupConversation)
    }

    companion object {
        val TAG = NewConversationFragment::class.java.canonicalName!!

        fun newInstance(): NewConversationFragment = NewConversationFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_message, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        listener = activity as NewMessageListener

        adapter = UsersAdapter()
        rvUsers.layoutManager = LinearLayoutManager(activity)
        rvUsers.adapter = adapter

        getApplication().api.retrieveUsers(getApplication().networkProfile.network, userServiceCallback)
        tvCancelToolbarText.text = getString(R.string.new_message)
        ivToolbarCancel.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }

        ivToolbarConfirm.setOnClickListener {
            if (adapter.selectedUsers.size < 1) {
                Toast.makeText(activity, "Please select at least one user", Toast.LENGTH_SHORT).show()
            } else {
                createConversation()
            }
        }

        etSearchUsers.addTextChangedListener(searchTextWatcher)
        GlideApp.with(activity!!).load(R.drawable.loader).into(ivLoader)
    }

    override fun onResume() {
        super.onResume()
        activity?.findViewById<BottomNavigationView>(R.id.nav_view)?.visibility = View.GONE
    }

    override fun onStop() {
        super.onStop()
        etSearchUsers.setText("")
        activity?.findViewById<BottomNavigationView>(R.id.nav_view)?.visibility = View.VISIBLE
    }

    private val searchTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {

        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            s?.let {
                adapter.users = allUsers.filter { it.firstName.contains(s, true) || it.lastName.contains(s, true) }
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun displayLoader(shouldDisplay: Boolean) {
        ivLoader.visibility = if (shouldDisplay) View.VISIBLE else View.GONE
        ivLoaderText.visibility = if (shouldDisplay) View.VISIBLE else View.GONE
    }

    private val userServiceCallback = object : UsersServiceCallback.UsersCallbackListener {
        override fun onRequestSuccess(users: List<User>?) {
            if (!this@NewConversationFragment.isVisible) {
                return
            }
            users?.let {
                //Remove ourselves from the list of current users
                allUsers = ArrayList(it.filter { user ->
                    user.id != getApplication().networkProfile.userId
                })

                displayLoader(false)
                adapter.users = allUsers
                adapter.notifyDataSetChanged()
            }
        }

        override fun onRequestFailure() {

        }
    }

    private fun createConversation() {
        when {
            adapter.selectedUsers.size == 0 -> return
            adapter.selectedUsers.size > 1 -> createUserGroupConversation()
            else -> listener.onUserConversationCreated(adapter.selectedUsers.values.elementAt(0))
        }
    }

    private fun createUserGroupConversation() {
        val users = adapter.selectedUsers.map { it.value.id }.toMutableList()
        val userGroupName = getUserGroupName()

        //Need to add ourselves to the group
        users.add(getApplication().networkProfile.userId)
        getApplication().api.createUserGroup(CreateGroupConversationBody(userGroupName, getApplication().networkProfile.network, users), object : GroupConversationServiceCallback.GroupConversationCallbackListener {
            override fun onRequestSuccess(userGroup: UserGroup?) {
                userGroup?.let {
                    listener.onUserGroupConversationCreated(GroupConversation(it))
                }
            }

            override fun onRequestFailure() {
            }
        })
    }

    private fun getUserGroupName(): String {
        var userNames = ""
        adapter.selectedUsers.values.forEach {
            userNames += it.firstName + ", "
        }
        //Add our name to the list
        userNames += " ${getApplication().userProfile.firstName}"
        return userNames
    }
}