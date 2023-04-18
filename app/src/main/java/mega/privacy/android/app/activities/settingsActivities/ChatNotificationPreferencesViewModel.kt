package mega.privacy.android.app.activities.settingsActivities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import javax.inject.Inject

/**
 * Chat Notification Preferences ViewModel
 */
@HiltViewModel
class ChatNotificationPreferencesViewModel @Inject constructor(
    monitorUpdatePushNotificationSettingsUseCase: MonitorUpdatePushNotificationSettingsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatNotificationPreferencesState())

    /**
     * UI State ChatNotificationPreference
     * Flow of [ChatNotificationPreferencesState]
     */
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            monitorUpdatePushNotificationSettingsUseCase().collect {
                _state.update { it.copy(isPushNotificationSettingsUpdatedEvent = true) }
            }
        }
    }

    /**
     * on Consume Push notification settings updated event
     */
    fun onConsumePushNotificationSettingsUpdateEvent() {
        viewModelScope.launch {
            _state.update { it.copy(isPushNotificationSettingsUpdatedEvent = false) }
        }
    }
}