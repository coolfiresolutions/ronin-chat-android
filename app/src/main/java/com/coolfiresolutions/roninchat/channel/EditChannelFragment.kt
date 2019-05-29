package com.coolfiresolutions.roninchat.channel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.coolfiresolutions.roninchat.R
import com.coolfiresolutions.roninchat.common.BaseFragment
import com.coolfiresolutions.roninchat.server.callback.GenericServiceCallback
import com.coolfiresolutions.roninchat.server.model.Session
import kotlinx.android.synthetic.main.fragment_edit_channel.*

class EditChannelFragment : BaseFragment() {
    private lateinit var channel: Session

    companion object {
        val TAG = EditChannelFragment::class.java.canonicalName!!
        private const val KEY_CHANNEL = "keyChannel"

        fun newInstance(channel: Session): EditChannelFragment {
            val bundle = Bundle()
            bundle.putParcelable(KEY_CHANNEL, channel)

            val fragment = EditChannelFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_edit_channel, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        channel = arguments!!.getParcelable(KEY_CHANNEL)!!

        etChannelName.setText(channel.name)

        initClickListeners()
    }

    private fun initClickListeners() {
        ivToolbarBack.setOnClickListener {
            activity!!.onBackPressed()
        }

        ivSave.setOnClickListener {
            channel.name = etChannelName.text.toString()
            getApplication().api.patchChannelName(channel.id, channel.name, channelPatchCallbackListener)
        }
    }

    private var channelPatchCallbackListener = object : GenericServiceCallback.GenericCallbackListener {
        override fun onRequestSuccess() {
            if (!this@EditChannelFragment.isVisible) {
                return
            }
            activity!!.onBackPressed()
        }

        override fun onRequestFailure() {
            if (!this@EditChannelFragment.isVisible) {
                return
            }
            Toast.makeText(activity, "Error updating group's name", Toast.LENGTH_SHORT).show()
        }
    }
}