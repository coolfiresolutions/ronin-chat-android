package com.coolfiresolutions.roninchat.conversation

import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.coolfiresolutions.roninchat.R
import com.coolfiresolutions.roninchat.common.GlideApp
import com.coolfiresolutions.roninchat.common.getGravatarUrlByEmail
import com.coolfiresolutions.roninchat.common.toFuzzyDateString
import com.coolfiresolutions.roninchat.server.model.RoninMessage
import com.coolfiresolutions.roninchat.user.model.User
import kotlinx.android.synthetic.main.list_item_message.view.*
import org.joda.time.DateTime

class MessagesAdapter(val myUserId: String, var users: List<User> = ArrayList(), var messages: ArrayList<RoninMessage> = ArrayList()) : RecyclerView.Adapter<MessagesAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_message, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.onBindViewHolder(messages[position])
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun onBindViewHolder(message: RoninMessage) {
            if (myUserId == message.actorId?.id) {
                //I sent the message
                itemView.ivUserAvatar.visibility = View.INVISIBLE
                itemView.tvMessageSender.visibility = View.INVISIBLE
                itemView.tvMessageText.setTextColor(ContextCompat.getColor(itemView.context, R.color.colorPrimary))
                itemView.tvMessageText.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
                itemView.tvMessageTimeStamp.setTextColor(ContextCompat.getColor(itemView.context, R.color.colorPrimary))
            } else {

                //Someone else sent the message
                itemView.ivUserAvatar.visibility = View.VISIBLE
                itemView.tvMessageSender.visibility = View.VISIBLE
                itemView.tvMessageText.setTextColor(ContextCompat.getColor(itemView.context, R.color.offWhite))
                itemView.tvMessageText.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                itemView.tvMessageTimeStamp.setTextColor(ContextCompat.getColor(itemView.context, R.color.offWhite))

                val userMap = users.associateBy({ it.id }, { it })
                val userWhoSentMessage = userMap[message.actorId?.id]

                itemView.tvMessageSender.text = itemView.context.getString(R.string.dual_string_placeholder, userWhoSentMessage?.firstName, userWhoSentMessage?.lastName)

                GlideApp.with(itemView)
                        .asBitmap()
                        .load(userWhoSentMessage?.email.getGravatarUrlByEmail())
                        .circleCrop()
                        .into(itemView.ivUserAvatar)
            }

            if (!message.getAttachments().isNullOrEmpty()) {
                itemView.ivMessageAttachment.visibility = View.VISIBLE
                GlideApp.with(itemView)
                        .asBitmap()
                        .placeholder(R.drawable.ic_insert_drive_file_grey_24dp)
                        .load(message.getAttachments()!![0].url)
                        .into(itemView.ivMessageAttachment)
            } else {
                itemView.ivMessageAttachment.visibility = View.GONE
            }

            itemView.tvMessageText.text = message.data?.get("body") as String?
            itemView.tvMessageTimeStamp.text = DateTime(message.sent).toFuzzyDateString(true)
        }
    }
}