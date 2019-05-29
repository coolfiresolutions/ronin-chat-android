package com.coolfiresolutions.roninchat.channel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.coolfiresolutions.roninchat.R
import com.coolfiresolutions.roninchat.common.BaseFragment
import com.coolfiresolutions.roninchat.server.model.Session
import kotlinx.android.synthetic.main.fragment_new_channel.*
import kotlinx.android.synthetic.main.toolbar_top_cancel.*

class NewChannelFragment : BaseFragment() {
    companion object {
        val TAG = NewChannelFragment::class.java.canonicalName!!

        fun newInstance() = NewChannelFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_channel, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        ivToolbarConfirm.setOnClickListener {
            createChannel()
        }

        ivToolbarCancel.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }
    }

    override fun onPause() {
        super.onPause()
        hideKeyboard()
    }

    private fun createChannel() {
        if (etChannelName.text.toString().isEmpty()) {
            Toast.makeText(activity, "Please enter a channel name", Toast.LENGTH_SHORT).show()
            return
        } else if (etChannelDescription.text.toString().isEmpty()) {
            Toast.makeText(activity, "Please enter a channel description", Toast.LENGTH_SHORT).show()
            return
        }

        getApplication().api.createSession(
                CreateSessionBody(
                        etChannelName.text.toString(),
                        etChannelDescription.text.toString(),
                        getApplication().networkProfile.network), sessionServiceCallbackListener)
    }

    private var sessionServiceCallbackListener = object : SessionServiceCallback.SessionServiceCallbackListener {
        override fun onRequestSuccess(session: Session?) {
            if (!this@NewChannelFragment.isVisible) {
                return
            }
            activity?.supportFragmentManager?.popBackStack()
        }

        override fun onRequestFailure() {

        }

    }
}