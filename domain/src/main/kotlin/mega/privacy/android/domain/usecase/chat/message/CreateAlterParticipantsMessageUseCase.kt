package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.management.AlterParticipantsMessage
import javax.inject.Inject

internal class CreateAlterParticipantsMessageUseCase @Inject constructor() :
    CreateTypedMessageUseCase {

    override fun invoke(message: ChatMessage, isMine: Boolean) = AlterParticipantsMessage(
        message.msgId,
        message.timestamp,
        isMine = isMine
    )
}