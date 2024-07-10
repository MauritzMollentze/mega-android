package mega.privacy.android.app.presentation.clouddrive

import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.presentation.fileinfo.model.getNodeIcon
import mega.privacy.android.app.presentation.filelink.model.FileLinkState
import mega.privacy.android.app.presentation.mapper.UrlDownloadException
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.textEditor.TextEditorViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.PublicNodeException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.filelink.GetFileUrlByPublicLinkUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.publiclink.CheckPublicNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.publiclink.CopyPublicNodeUseCase
import mega.privacy.android.domain.usecase.node.publiclink.MapNodeToPublicLinkUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model class for [mega.privacy.android.app.presentation.filelink.FileLinkComposeActivity]
 */
@HiltViewModel
class FileLinkViewModel @Inject constructor(
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val hasCredentialsUseCase: HasCredentialsUseCase,
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    private val getPublicNodeUseCase: GetPublicNodeUseCase,
    private val checkPublicNodesNameCollisionUseCase: CheckPublicNodesNameCollisionUseCase,
    private val copyPublicNodeUseCase: CopyPublicNodeUseCase,
    private val httpServerStart: MegaApiHttpServerStartUseCase,
    private val httpServerIsRunning: MegaApiHttpServerIsRunningUseCase,
    private val getFileUrlByPublicLinkUseCase: GetFileUrlByPublicLinkUseCase,
    private val mapNodeToPublicLinkUseCase: MapNodeToPublicLinkUseCase,
    private val fileTypeIconMapper: FileTypeIconMapper,
) : ViewModel() {

    private val _state = MutableStateFlow(FileLinkState())

    /**
     * The FileLink UI State accessible outside the ViewModel
     */
    val state: StateFlow<FileLinkState> = _state.asStateFlow()

    /**
     * Is connected
     */
    val isConnected: Boolean
        get() = isConnectedToInternetUseCase()

    /**
     * Check if login is required
     */
    fun checkLoginRequired() {
        viewModelScope.launch {
            val hasCredentials = hasCredentialsUseCase()
            val shouldLogin = hasCredentials && !rootNodeExistsUseCase()
            _state.update { it.copy(shouldLogin = shouldLogin, hasDbCredentials = hasCredentials) }
        }
    }

    /**
     * Handle intent
     */
    fun handleIntent(intent: Intent) {
        intent.dataString?.let { link ->
            _state.update { it.copy(url = link) }
            getPublicNode(link)
        } ?: Timber.w("url NULL")
    }

    /**
     * Get node from public link
     */
    fun getPublicNode(link: String, decryptionIntroduced: Boolean = false) = viewModelScope.launch {
        runCatching { getPublicNodeUseCase(link) }
            .onSuccess { node ->
                val iconResource = getNodeIcon(
                    typedNode = node,
                    originShares = false,
                    fileTypeIconMapper = fileTypeIconMapper
                )
                _state.update {
                    it.copyWithTypedNode(node, iconResource)
                }
                resetJobInProgressState()
            }
            .onFailure { exception ->
                resetJobInProgressState()
                when (exception) {
                    is PublicNodeException.InvalidDecryptionKey -> {
                        if (decryptionIntroduced) {
                            Timber.w("Incorrect key, ask again!")
                            _state.update { it.copy(askForDecryptionDialog = true) }
                        } else {
                            _state.update {
                                it.copy(fetchPublicNodeError = exception)
                            }
                        }
                    }

                    is PublicNodeException.DecryptionKeyRequired -> {
                        _state.update { it.copy(askForDecryptionDialog = true) }
                    }

                    else -> {
                        _state.update {
                            it.copy(fetchPublicNodeError = exception as? PublicNodeException)
                        }
                    }
                }
            }
    }

    /**
     * Get combined url with key for fetching link content
     */
    fun decrypt(mKey: String?) {
        val url = state.value.url
        mKey?.let { key ->
            if (key.isEmpty()) return
            var urlWithKey = ""
            if (url.contains("#!")) {
                // old folder link format
                urlWithKey = if (key.startsWith("!")) {
                    Timber.d("Decryption key with exclamation!")
                    url + key
                } else {
                    "$url!$key"
                }
            } else if (url.contains(Constants.SEPARATOR + "file" + Constants.SEPARATOR)) {
                // new folder link format
                urlWithKey = if (key.startsWith("#")) {
                    Timber.d("Decryption key with hash!")
                    url + key
                } else {
                    "$url#$key"
                }
            }
            Timber.d("File link to import: $urlWithKey")
            getPublicNode(urlWithKey, true)
        }
    }

    /**
     * Handle select import folder result
     */
    fun handleSelectImportFolderResult(result: ActivityResult) {
        val resultCode = result.resultCode
        val intent = result.data

        if (resultCode != AppCompatActivity.RESULT_OK || intent == null) {
            return
        }

        if (!isConnected) {
            resetJobInProgressState()
            setErrorMessage(R.string.error_server_connection_problem)
            return
        }

        val toHandle = intent.getLongExtra("IMPORT_TO", 0)
        handleImportNode(toHandle)
    }

    /**
     * Handle import node
     *
     * @param targetHandle
     */
    fun handleImportNode(targetHandle: Long) {
        checkNameCollision(targetHandle)
    }

    private fun checkNameCollision(targetHandle: Long) = viewModelScope.launch {
        val fileNode = state.value.fileNode ?: run {
            Timber.e("Invalid File node")
            resetJobInProgressState()
            return@launch
        }
        runCatching {
            checkPublicNodesNameCollisionUseCase(
                listOf(fileNode),
                targetHandle,
                NodeNameCollisionType.COPY
            )
        }.onSuccess { result ->
            if (result.noConflictNodes.isNotEmpty()) {
                copy(targetHandle)
            } else if (result.conflictNodes.isNotEmpty()) {
                val collision = NameCollision.Copy.fromNodeNameCollision(result.conflictNodes[0])
                _state.update {
                    it.copy(collision = collision, jobInProgressState = null)
                }
            }
        }.onFailure { throwable ->
            resetJobInProgressState()
            setErrorMessage(R.string.general_error)
            Timber.e(throwable)
        }
    }

    private fun copy(targetHandle: Long) = viewModelScope.launch {
        val fileNode = state.value.fileNode ?: run {
            Timber.e("Invalid File node")
            resetJobInProgressState()
            return@launch
        }
        runCatching { copyPublicNodeUseCase(fileNode, NodeId(targetHandle), null) }
            .onSuccess { _state.update { it.copy(copySuccess = true, jobInProgressState = null) } }
            .onFailure { copyThrowable ->
                resetJobInProgressState()
                handleCopyError(copyThrowable)
                Timber.e(copyThrowable)
            }
    }

    private fun handleCopyError(throwable: Throwable) {
        when (throwable) {
            is QuotaExceededMegaException -> {
                _state.update { it.copy(overQuotaError = triggered(StorageState.Red)) }
            }

            is NotEnoughQuotaMegaException -> {
                _state.update { it.copy(overQuotaError = triggered(StorageState.Orange)) }
            }

            is ForeignNodeException -> {
                _state.update { it.copy(foreignNodeError = triggered) }
            }

            else -> {
                setErrorMessage(R.string.context_no_copied)
            }
        }
    }

    /**
     * Handle save to device
     */
    fun handleSaveFile() {
        viewModelScope.launch {
            val linkNodes = listOfNotNull(
                (_state.value.fileNode as? UnTypedNode)?.let {
                    runCatching {
                        mapNodeToPublicLinkUseCase(it, null) as? TypedNode
                    }.onFailure {
                        Timber.e(it)
                    }.getOrNull()
                })
            _state.update {
                it.copy(
                    downloadEvent = triggered(TransferTriggerEvent.StartDownloadNode(linkNodes))
                )
            }
        }
    }

    /**
     * Reset collision
     */
    fun resetCollision() {
        _state.update { it.copy(collision = null) }
    }

    /**
     * Reset the askForDecryptionKeyDialog boolean
     */
    fun resetAskForDecryptionKeyDialog() {
        _state.update { it.copy(askForDecryptionDialog = false) }
    }

    /**
     * Reset the job in progress state value
     */
    private fun resetJobInProgressState() {
        _state.update { it.copy(jobInProgressState = null) }
    }

    /**
     * update intent values for image
     */
    fun updateImageIntent(intent: Intent) {
        _state.update { it.copy(openFile = triggered(intent)) }
    }

    /**
     * Update intent values for audio/video
     */
    fun updateAudioVideoIntent(intent: Intent, nameType: MimeTypeList) {
        viewModelScope.launch {
            runCatching {
                with(state.value) {
                    intent.apply {
                        putExtra(
                            Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE,
                            Constants.FILE_LINK_ADAPTER
                        )
                        putExtra(Constants.INTENT_EXTRA_KEY_IS_PLAYLIST, false)
                        putExtra(Constants.URL_FILE_LINK, url)
                        putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, handle)
                        putExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME, title)
                        putExtra(Constants.EXTRA_SERIALIZE_STRING, serializedData)
                    }

                    startHttpServer(intent)
                    val path = getFileUrlByPublicLinkUseCase(url) ?: throw UrlDownloadException()
                    intent.setDataAndType(Uri.parse(path), nameType.type)

                    if (nameType.isVideoNotSupported || nameType.isAudioNotSupported) {
                        val s = title.split("\\.".toRegex())
                        if (s.size > 1 && s[s.size - 1] == "opus") {
                            intent.setDataAndType(intent.data, "audio/*")
                        }
                    }
                    intent
                }
            }.onSuccess { intent ->
                intent.let { _state.update { it.copy(openFile = triggered(intent)) } }
            }.onFailure {
                Timber.e("itemClick:ERROR:httpServerGetLocalLink")
            }
        }
    }

    /**
     * Update intent values for pdf
     */
    fun updatePdfIntent(pdfIntent: Intent, mimeType: String) {
        viewModelScope.launch {
            runCatching {
                with(state.value) {
                    pdfIntent.apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                        putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, handle)
                        putExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME, title)
                        putExtra(Constants.URL_FILE_LINK, url)
                        putExtra(Constants.EXTRA_SERIALIZE_STRING, serializedData)
                        putExtra(Constants.INTENT_EXTRA_KEY_INSIDE, true)
                        putExtra(
                            Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE,
                            Constants.FILE_LINK_ADAPTER
                        )
                    }
                    startHttpServer(pdfIntent)
                    val path = getFileUrlByPublicLinkUseCase(url) ?: throw UrlDownloadException()
                    pdfIntent.setDataAndType(Uri.parse(path), mimeType)
                }
            }.onSuccess { intent ->
                intent.let { _state.update { it.copy(openFile = triggered(intent)) } }
            }.onFailure {
                Timber.e("itemClick:ERROR:httpServerGetLocalLink")
            }
        }
    }

    /**
     * Update intent value for text editor
     */
    fun updateTextEditorIntent(intent: Intent) {
        with(state.value) {
            intent.apply {
                putExtra(Constants.URL_FILE_LINK, url)
                putExtra(Constants.EXTRA_SERIALIZE_STRING, serializedData)
                putExtra(TextEditorViewModel.MODE, TextEditorViewModel.VIEW_MODE)
                putExtra(
                    Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE,
                    Constants.FILE_LINK_ADAPTER
                )
            }
            _state.update { it.copy(openFile = triggered(intent)) }
        }
    }

    /**
     * Start the server if not started
     * also setMax buffer size based on available buffer size
     * @param intent [Intent]
     *
     * @return intent
     */
    private suspend fun startHttpServer(intent: Intent): Intent {
        if (httpServerIsRunning() == 0) {
            httpServerStart()
            intent.putExtra(Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
        }
        return intent
    }

    /**
     * Reset and notify that openFile event is consumed
     */
    fun resetOpenFile() = _state.update { it.copy(openFile = consumed()) }

    /**
     * Reset and notify that downloadFile event is consumed
     */
    fun resetDownloadFile() = _state.update {
        it.copy(
            downloadEvent = consumed(),
        )
    }

    /**
     * Set and notify that errorMessage event is triggered
     */
    private fun setErrorMessage(message: Int) =
        _state.update { it.copy(errorMessage = triggered(message)) }

    /**
     * Reset and notify that errorMessage event is consumed
     */
    fun resetErrorMessage() = _state.update { it.copy(errorMessage = consumed()) }

    /**
     * Reset and notify that overQuotaError event is consumed
     */
    fun resetOverQuotaError() = _state.update { it.copy(overQuotaError = consumed()) }

    /**
     * Reset and notify that foreignNodeError event is consumed
     */
    fun resetForeignNodeError() = _state.update { it.copy(foreignNodeError = consumed) }
}
