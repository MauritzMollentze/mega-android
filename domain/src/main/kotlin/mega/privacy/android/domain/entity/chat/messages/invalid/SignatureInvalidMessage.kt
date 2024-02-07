package mega.privacy.android.domain.entity.chat.messages.invalid

import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction

/**
 * Signature invalid message
 *
 * @property msgId
 * @property time
 * @property isMine
 * @property userHandle
 */
data class SignatureInvalidMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val reactions: List<Reaction>,
) : InvalidMessage