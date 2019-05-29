package com.coolfiresolutions.roninchat.user

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.coolfiresolutions.roninchat.R
import com.coolfiresolutions.roninchat.common.getGravatarUrlByEmail
import com.coolfiresolutions.roninchat.user.model.User
import kotlinx.android.synthetic.main.list_item_user.view.*

class UsersAdapter(var users: List<User> = ArrayList()) : RecyclerView.Adapter<UsersAdapter.ViewHolder>() {
    val selectedUsers = HashMap<String, User>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_user, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return users.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindViewHolder(users[position])
    }

    fun onUserClicked(position: Int) {
        val user = users[position]
        if (selectedUsers.containsKey(user.id)) {
            selectedUsers.remove(user.id)
        } else {
            selectedUsers[user.id] = users[position]
        }
        notifyItemChanged(position)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        init {
            itemView.setOnClickListener {
                onUserClicked(adapterPosition)
            }
        }

        fun bindViewHolder(user: User) {
            itemView.tvUserName.text = itemView.context.getString(R.string.dual_string_placeholder, user.firstName, user.lastName)
            itemView.clUserListItem.setBackgroundColor(if (selectedUsers.contains(user.id)) itemView.resources.getColor(R.color.darkGrey) else itemView.resources.getColor(R.color.colorPrimaryDark))

            Glide.with(itemView)
                    .asBitmap()
                    .load(user.email.getGravatarUrlByEmail())
                    .circleCrop()
                    .into(itemView.ivUserAvatar)
        }
    }
}