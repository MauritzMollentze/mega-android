package mega.privacy.android.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.UpdateApp
import mega.privacy.android.domain.usecase.logging.GetZippedLogsUseCase
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class QASettingViewModel @Inject constructor(
    private val updateApp: UpdateApp,
    private val getLogFile: GetZippedLogsUseCase,
) : ViewModel() {

    fun checkUpdatePressed() {
        viewModelScope.launch {
            updateApp()
                .onCompletion {
                    it?.let { error ->
                        Timber.e(error, "Auto update failed")
                    }
                }
                .collect()
        }
    }

    fun exportLogs(onLogCreated: (File) -> Unit) {
        viewModelScope.launch {
            try {
                val logFile = getLogFile()
                onLogCreated(logFile)
            } catch (e: Exception) {
                Timber.e(e, "Error exporting logs")
            }
        }
    }
}