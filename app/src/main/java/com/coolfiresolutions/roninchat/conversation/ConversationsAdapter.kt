package com.coolfiresolutions.roninchat.conversation

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.coolfiresolutions.roninchat.R
import com.coolfiresolutions.roninchat.common.GlideApp
import com.coolfiresolutions.roninchat.common.getGravatarUrlByEmail
import com.coolfiresolutions.roninchat.common.toFuzzyDateString
import com.coolfiresolutions.roninchat.conversation.model.Conversation
import com.coolfiresolutions.roninchat.conversation.model.GroupConversation
import com.coolfiresolutions.roninchat.conversation.model.UserConversation
import com.coolfiresolutions.roninchat.user.model.User
import kotlinx.android.synthetic.main.list_item_conversation.view.*
import org.joda.time.DateTime

class ConversationsAdapter(private var listener: ConversationsListener, var conversations: ArrayList<Conversation> = ArrayList()) : RecyclerView.Adapter<ConversationsAdapter.ViewHolder>() {
    interface ConversationsListener {
        fun onConversationClicked(conversation: Conversation)
        fun onConversationLongPressed(conversation: Conversation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return conversations.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindViewHolder(conversations[position])
    }

    private fun onConversationClicked(position: Int) {
        listener.onConversationClicked(conversations[position])
    }

    private fun onConversationLongPressed(position: Int) {
        listener.onConversationLongPressed(conversations[position])
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        init {
            itemView.setOnClickListener {
                onConversationClicked(adapterPosition)
            }

            itemView.setOnLongClickListener {
                onConversationLongPressed(adapterPosition)
                true
            }
        }

        fun bindViewHolder(conversation: Conversation) {
            itemView.tvConversationName.text = conversation.getConversationName()
            val unreadConversations = conversation.unreadMessages
            itemView.ivUnreadBadge.visibility = if (unreadConversations == 0) View.GONE else View.VISIBLE
            itemView.tvConversationDescription.text = conversation.lastMessage?.text

            if (unreadConversations > 0) {
                itemView.tvTimestamp.setTextColor(ContextCompat.getColor(itemView.context,
                        R.color.colorPrimary
                ))
            } else {
                itemView.tvTimestamp.setTextColor(ContextCompat.getColor(itemView.context,
                        R.color.offWhite
                ))
            }

            conversation.lastMessage?.let {
                itemView.tvTimestamp.text = DateTime(it.sent).toFuzzyDateString(false)
            } ?: run {
                itemView.tvTimestamp.text = ""
            }

            val numOfUsers = conversation.getUserCount()
            val users = (conversation as? GroupConversation)?.users

            if (conversation is UserConversation) {
                updateTextView(itemView.tvUser1, conversation.email.getGravatarUrlByEmail(), conversation.firstName, numOfUsers)
            } else {
                updateTextView(itemView.tvUser1, users?.getOrNull(0), numOfUsers)
            }

            updateTextView(itemView.tvUser2, users?.getOrNull(1), numOfUsers)
            updateTextView(itemView.tvUser3, users?.getOrNull(2), numOfUsers)
            updateTextView(itemView.tvUser4, users?.getOrNull(3), numOfUsers)
            updateTextView(itemView.tvUser5, users?.getOrNull(4), numOfUsers)
            updateTextView(itemView.tvUser6, users?.getOrNull(5), numOfUsers)
        }

        private fun updateTextView(textView: TextView, user: User?, numOfUsers: Int) {
            updateTextView(
                    textView,
                    user?.email.getGravatarUrlByEmail(),
                    user?.firstName,
                    numOfUsers)
        }

        private fun updateTextView(textView: TextView, email: String?, firstName: String?, numOfUsers: Int) {
            if (numOfUsers == 1 && (textView.tag as String).toInt() == 1) {
                textView.visibility = View.VISIBLE
            } else if (numOfUsers < (textView.tag as String).toInt()) {
                textView.visibility = View.INVISIBLE
                return
            } else {
                textView.visibility = View.VISIBLE
            }

            GlideApp.with(itemView)
                    .asBitmap()
                    .load(email)
                    .circleCrop()
                    .into(object : SimpleTarget<Bitmap>(100, 100) {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            val emptyBitmap = Bitmap.createBitmap(resource.width, resource.height, resource.config)
                            if (resource.sameAs(emptyBitmap)) {
                                textView.text = firstName!![0].toString().toUpperCase()
                                textView.background = itemView.context.getDrawable(R.drawable.ic_green_circle)
                            } else {
                                textView.text = ""
                                textView.background = BitmapDrawable(textView.resources, resource)
                            }
                        }
                    })
        }
    }
}