package mega.privacy.android.domain.entity.chat.messages.management

import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction

/**
 * Permission change message
 * @property privilege The privilege of the participant
 * @property handleOfAction The handle of the participant
 */
data class PermissionChangeMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val reactions: List<Reaction>,
    val privilege: ChatRoomPermission,
    val handleOfAction: Long,
) : ManagementMessage