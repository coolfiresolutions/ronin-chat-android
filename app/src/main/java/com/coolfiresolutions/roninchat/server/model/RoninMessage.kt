package com.coolfiresolutions.roninchat.server.model

import android.os.Parcelable
import com.coolfiresolutions.roninchat.server.JSONMapperUtil
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

@Parcelize
class RoninMessage(
        var id: String = UUID.randomUUID().toString(),
        var actorId: ServerActor? = null,
        var targets: ArrayList<ServerScope> = ArrayList(),
        var sent: String? = null,
        var type: String = "",
        var action: String = "",
        var data: @RawValue MutableMap<String, Any>? = HashMap()
) : Parcelable {
    enum class Action(action: String) {
        CREATE("create"),
        UPDATE("update"),
        DELETE("delete"),
        ADD_USER("addUser"),
        REMOVE_USER("removeUser");

        var action: String
            internal set

        init {
            this.action = action
        }
    }

    /**
     *
     * @param user - User who created this message
     * @param targetId - Id of the type being targeted
     * @param targetType - Type being targeted
     * @param type - Type of entity ("location, session, etc")
     * @param action - Action on date ("Update, Delete, Create")
     * @param data - Data converted to JSON String
     */
    constructor(
            user: ServerActor,
            targetId: String,
            targetType: String,
            type: String,
            action: String,
            data: MutableMap<String, Any>
    ) : this() {
        this.id = UUID.randomUUID().toString()
        this.actorId = user
        this.targets = ArrayList()
        this.targets.add(ServerScope(targetId, targetType))
        this.type = type
        this.action = action
        this.data = data

        val df = JSONMapperUtil.dateFormatWithTimeZone
        this.sent = df.format(Date())
    }

    fun getAttachments(): List<MessageAttachment>? {
        var messageAttachments = ArrayList<MessageAttachment>()

        (data?.get("attachments") as List<*>?)?.map {
            try {
                (it as HashMap<String, *>?).apply {
                    messageAttachments.add(MessageAttachment(url = it?.get("url") as String))
                }
            } catch (ex: ClassCastException) {
                //List of 1 was returned
                messageAttachments.add(it as MessageAttachment)
            }
        }

        return messageAttachments
    }

    abstract class MessageResponseListener {
        abstract fun onMessageError(errorJson: JSONObject, errorMessage: RoninMessage)

        abstract fun onMessageSuccess(message: RoninMessage)
    }
}
