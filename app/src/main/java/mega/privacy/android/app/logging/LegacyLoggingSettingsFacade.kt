package mega.privacy.android.app.logging

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.ResetSdkLogger
import mega.privacy.android.domain.usecase.SetChatLogsEnabled
import mega.privacy.android.domain.usecase.SetSdkLogsEnabled
import mega.privacy.android.domain.usecase.logging.AreChatLogsEnabledUseCase
import mega.privacy.android.domain.usecase.logging.AreSdkLogsEnabledUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Legacy logging settings facade implementation of [LegacyLoggingSettings]
 *
 * @property setSdkLogsEnabled
 * @property setChatLogsEnabled
 * @property resetSdkLogger
 * @property coroutineScope
 * @constructor
 *
 * @param areSdkLogsEnabledUseCase
 * @param areChatLogsEnabledUseCase
 */
class LegacyLoggingSettingsFacade @Inject constructor(
    private val setSdkLogsEnabled: SetSdkLogsEnabled,
    private val setChatLogsEnabled: SetChatLogsEnabled,
    private val resetSdkLogger: ResetSdkLogger,
    @ApplicationScope private val coroutineScope: CoroutineScope,
    areSdkLogsEnabledUseCase: AreSdkLogsEnabledUseCase,
    areChatLogsEnabledUseCase: AreChatLogsEnabledUseCase,
) : LegacyLoggingSettings {

    @OptIn(DelicateCoroutinesApi::class)
    private val sdkLogStatus =
        areSdkLogsEnabledUseCase().stateIn(GlobalScope, SharingStarted.Eagerly, false)

    @OptIn(DelicateCoroutinesApi::class)
    private val chatLogStatus =
        areChatLogsEnabledUseCase().stateIn(GlobalScope, SharingStarted.Eagerly, false)

    override fun setStatusLoggerSDK(context: Context, enabled: Boolean) {
        coroutineScope.launch {
            setSdkLogsEnabled(enabled)
        }.invokeOnCompletion {
            if (enabled) {
                Timber.i("SDK logs are now enabled - App Version: ${Util.getVersion(context)}")
                Util.showSnackbar(context, context.getString(R.string.settings_enable_logs))
            } else {
                Timber.i("SDK logs are now disabled - App Version: ${Util.getVersion(context)}")
                Util.showSnackbar(context, context.getString(R.string.settings_disable_logs))
            }
        }
    }

    override fun setStatusLoggerKarere(context: Context, enabled: Boolean) {
        coroutineScope.launch {
            setChatLogsEnabled(enabled)
        }.invokeOnCompletion {
            if (enabled) {
                Timber.i("Karere logs are now enabled - App Version: ${Util.getVersion(context)}")
                Util.showSnackbar(context, context.getString(R.string.settings_enable_logs))
            } else {
                Timber.i("Karere logs are now disabled - App Version: ${Util.getVersion(context)}")
                Util.showSnackbar(context, context.getString(R.string.settings_disable_logs))
            }
        }
    }

    override fun resetLoggerSDK() {
        resetSdkLogger()
    }

    override fun areSDKLogsEnabled() = sdkLogStatus.value

    override fun updateSDKLogs(enabled: Boolean) {
        coroutineScope.launch {
            setSdkLogsEnabled(enabled)
        }
    }

    override fun areKarereLogsEnabled() = chatLogStatus.value

    override fun updateKarereLogs(enabled: Boolean) {
        coroutineScope.launch {
            setChatLogsEnabled(enabled)
        }
    }

}