package mega.privacy.android.domain.entity.chat.messages.management

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction

/**
 * Permission change message
 * @property privilege The privilege of the participant
 * @property handleOfAction The handle of the participant
 */
@Serializable
data class PermissionChangeMessage(
    override val chatId: Long,
    override val msgId: Long,
    override val time: Long,
    override val isDeletable: Boolean,
    override val isEditable: Boolean,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val reactions: List<Reaction>,
    override val status: ChatMessageStatus,
    override val content: String?,
    val privilege: ChatRoomPermission,
    val handleOfAction: Long,
) : ManagementMessage