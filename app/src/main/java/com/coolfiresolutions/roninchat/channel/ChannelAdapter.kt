package com.coolfiresolutions.roninchat.channel

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.coolfiresolutions.roninchat.R
import com.coolfiresolutions.roninchat.common.GlideApp
import com.coolfiresolutions.roninchat.common.getGravatarUrlByEmail
import com.coolfiresolutions.roninchat.server.model.Session
import com.coolfiresolutions.roninchat.user.model.User
import kotlinx.android.synthetic.main.list_item_channel.view.*
import org.joda.time.DateTime

class ChannelAdapter(var listener: ChannelAdapterListener, var channels: MutableList<Session> = ArrayList()) :
        RecyclerView.Adapter<ChannelAdapter.ViewHolder>() {
    var users = HashMap<String, User>()

    interface ChannelAdapterListener {
        fun onChannelClicked(channel: Session)
        fun onChannelLongPressed(channel: Session)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_channel, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return channels.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindViewHolder(channels[position])
    }

    fun onChannelClicked(position: Int) {
        listener.onChannelClicked(channels[position])
    }

    fun onChannelLongPressed(position: Int) {
        listener.onChannelLongPressed(channels[position])
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        init {
            itemView.setOnClickListener {
                onChannelClicked(adapterPosition)
            }
            itemView.setOnLongClickListener {
                onChannelLongPressed(adapterPosition)
                true
            }
        }

        fun bindViewHolder(channel: Session) {
            itemView.ivUnreadBadge.visibility = if (channel.unreadMessages > 0) View.VISIBLE else View.GONE
            itemView.tvChannelName.text = channel.name
            itemView.tvChannelDescription.text = channel.description

            val user: User?
            if (channel.lastMessage == null) {
                user = users[channel.audit.createdBy.id]
                itemView.tvLastMessageText.text = "Created on ${DateTime(channel.startDate).toString("MM/dd/yy")}"
            } else {
                user = users[channel.lastMessage?.actorId?.id]
                itemView.tvLastMessageText.text = channel.lastMessage?.text
            }

            itemView.tvLastMessageUserName.text = "${user?.firstName} ${user?.lastName}"
            GlideApp.with(itemView)
                    .asBitmap()
                    .load(user?.email?.getGravatarUrlByEmail())
                    .circleCrop()
                    .into(object : SimpleTarget<Bitmap>(100, 100) {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            val emptyBitmap = Bitmap.createBitmap(resource.width, resource.height, resource.config)
                            if (resource.sameAs(emptyBitmap)) {
                                itemView.tvLastMessageUser.setBackgroundResource(R.drawable.ic_green_circle)
                                itemView.tvLastMessageUser.text = user?.firstName?.firstOrNull().toString()
                            } else {
                                itemView.tvLastMessageUser.background = BitmapDrawable(itemView.resources, resource)
                            }
                        }
                    })
        }
    }
}