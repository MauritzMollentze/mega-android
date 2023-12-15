package mega.privacy.android.app.presentation.meeting.chat.model.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.meeting.chat.view.message.AlterParticipantsMessageView
import mega.privacy.android.domain.entity.chat.messages.management.AlterParticipantsMessage

/**
 * Alter participants ui message
 *
 * @property message
 * @property showDate
 */
data class AlterParticipantsUiMessage(
    override val message: AlterParticipantsMessage,
    override val showDate: Boolean,
) : UiChatMessage {
    override val contentComposable: @Composable (RowScope.() -> Unit) = {
        AlterParticipantsMessageView(message = message, modifier = Modifier.padding(start = 32.dp))
    }

    override val avatarComposable: @Composable (RowScope.() -> Unit)? = null

    override val showAvatar: Boolean = false

    override val showTime: Boolean = true
}