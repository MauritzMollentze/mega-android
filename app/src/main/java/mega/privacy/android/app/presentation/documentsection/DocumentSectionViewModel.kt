package mega.privacy.android.app.presentation.documentsection

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.documentsection.model.DocumentSectionUiState
import mega.privacy.android.app.presentation.documentsection.model.DocumentUiEntity
import mega.privacy.android.app.presentation.documentsection.model.DocumentUiEntityMapper
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetFileUrlByNodeHandleUseCase
import mega.privacy.android.domain.usecase.documentsection.GetAllDocumentsUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * The view model for the document section
 */
@HiltViewModel
class DocumentSectionViewModel @Inject constructor(
    private val getAllDocumentsUseCase: GetAllDocumentsUseCase,
    private val documentUiEntityMapper: DocumentUiEntityMapper,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
    private val monitorViewType: MonitorViewType,
    private val setViewType: SetViewType,
    private val getNodeByHandle: GetNodeByHandle,
    private val getFingerprintUseCase: GetFingerprintUseCase,
    private val megaApiHttpServerIsRunningUseCase: MegaApiHttpServerIsRunningUseCase,
    private val megaApiHttpServerStartUseCase: MegaApiHttpServerStartUseCase,
    private val getFileUrlByNodeHandleUseCase: GetFileUrlByNodeHandleUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DocumentSectionUiState())
    internal val uiState = _uiState.asStateFlow()

    private var searchQuery = ""
    private val originalData = mutableListOf<DocumentUiEntity>()

    /**
     * Is network connected
     */
    internal val isConnected: Boolean
        get() = isConnectedToInternetUseCase()

    init {
        checkViewType()
        viewModelScope.launch {
            merge(
                monitorNodeUpdatesUseCase(),
                monitorOfflineNodeUpdatesUseCase()
            ).conflate()
                .catch {
                    Timber.e(it)
                }.collect {
                    refreshDocumentNodes()
                }
        }
    }

    internal suspend fun refreshDocumentNodes() =
        runCatching {
            getDocumentUIEntityList().updateOriginalData().filterDocumentsBySearchQuery()
        }.onSuccess { documentList ->
            val sortOrder = getCloudSortOrder()
            _uiState.update {
                it.copy(
                    allDocuments = documentList,
                    sortOrder = sortOrder,
                    isLoading = false,
                    scrollToTop = false,
                )
            }
        }.onFailure {
            Timber.e(it)
        }

    private suspend fun getDocumentUIEntityList() = getAllDocumentsUseCase().map {
        documentUiEntityMapper(it)
    }

    private fun List<DocumentUiEntity>.updateOriginalData() = also { data ->
        originalData.clear()
        originalData.addAll(data)
    }

    private fun List<DocumentUiEntity>.filterDocumentsBySearchQuery() =
        filter { document ->
            document.name.contains(searchQuery, true)
        }

    private fun checkViewType() {
        viewModelScope.launch {
            monitorViewType().collect { viewType ->
                _uiState.update { it.copy(currentViewType = viewType) }
            }
        }
    }

    internal fun shouldShowSearchMenu() = _uiState.value.allDocuments.isNotEmpty()

    internal fun searchReady() {
        if (_uiState.value.searchMode)
            return

        _uiState.update { it.copy(searchMode = true) }
        searchQuery = ""
    }

    internal fun searchQuery(query: String) {
        if (searchQuery == query)
            return

        searchQuery = query
        searchNodeByQueryString()
    }

    internal fun exitSearch() {
        _uiState.update { it.copy(searchMode = false) }
        searchQuery = ""
        searchNodeByQueryString()
    }

    private fun searchNodeByQueryString() {
        val documents = originalData.filterDocumentsBySearchQuery()
        _uiState.update {
            it.copy(
                allDocuments = documents,
                scrollToTop = true
            )
        }
    }

    internal fun onChangeViewTypeClicked() {
        viewModelScope.launch {
            when (_uiState.value.currentViewType) {
                ViewType.LIST -> setViewType(ViewType.GRID)
                ViewType.GRID -> setViewType(ViewType.LIST)
            }
        }
    }

    internal fun refreshWhenOrderChanged() =
        viewModelScope.launch {
            val sortOrder = getCloudSortOrder()
            _uiState.update {
                it.copy(
                    sortOrder = sortOrder,
                    isLoading = true
                )
            }
            refreshDocumentNodes()
        }

    /**
     * Detect the node whether is local file
     *
     * @param handle node handle
     * @return true is local file, otherwise is false
     */
    internal suspend fun getLocalFilePath(handle: Long): String? =
        getNodeByHandle(handle)?.let { node ->
            val localPath = FileUtil.getLocalFile(node)
            val file = File(FileUtil.getDownloadLocation(), node.name)
            if (localPath != null && ((FileUtil.isFileAvailable(file) && file.length() == node.size)
                        || (node.fingerprint == getFingerprintUseCase(localPath)))
            ) {
                localPath
            } else {
                null
            }
        }

    /**
     * Update intent
     *
     * @param handle node handle
     * @param name node name
     * @param intent Intent
     * @return updated intent
     */
    internal suspend fun updateIntent(
        handle: Long,
        name: String,
        intent: Intent,
    ): Intent {
        if (megaApiHttpServerIsRunningUseCase() == 0) {
            megaApiHttpServerStartUseCase()
            intent.putExtra(Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
        }

        getFileUrlByNodeHandleUseCase(handle)?.let { url ->
            Uri.parse(url)?.let { uri ->
                intent.setDataAndType(uri, MimeTypeList.typeForName(name).type)
            }
        }

        return intent
    }

    internal suspend fun getDocumentNodeByHandle(handle: Long) = getNodeByHandle(handle)
}