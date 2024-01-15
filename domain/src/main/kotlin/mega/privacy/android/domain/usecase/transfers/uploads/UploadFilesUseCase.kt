package mega.privacy.android.domain.usecase.transfers.uploads

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.canceltoken.InvalidateCancelTokenUseCase
import mega.privacy.android.domain.usecase.transfers.AbstractTransferNodesUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.AddOrUpdateActiveTransferUseCase
import mega.privacy.android.domain.usecase.transfers.sd.HandleSDCardEventUseCase
import java.io.File
import javax.inject.Inject

/**
 * Uploads a list of files to the specified destination folder and returns a Flow to monitor the progress
 */
class UploadFilesUseCase @Inject constructor(
    cancelCancelTokenUseCase: CancelCancelTokenUseCase,
    invalidateCancelTokenUseCase: InvalidateCancelTokenUseCase,
    addOrUpdateActiveTransferUseCase: AddOrUpdateActiveTransferUseCase,
    handleSDCardEventUseCase: HandleSDCardEventUseCase,
    monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    private val transferRepository: TransferRepository,
) : AbstractTransferNodesUseCase<File, String>(
    cancelCancelTokenUseCase,
    invalidateCancelTokenUseCase,
    addOrUpdateActiveTransferUseCase,
    handleSDCardEventUseCase,
    monitorTransferEventsUseCase,
) {

    /**
     * Invoke
     *
     * @param files files and / or folders to be uploaded
     * @param destination destination folder where [files] will be uploaded
     * @param appData Custom app data to save in the MegaTransfer object.
     * @param isSourceTemporary Whether the temporary file or folder that is created for upload should be deleted or not
     * @param isHighPriority Whether the file or folder should be placed on top of the upload queue or not
     *
     * @return a flow of [MultiTransferEvent]s to monitor the download state and progress
     */
    operator fun invoke(
        files: List<File>,
        destination: TypedFolderNode,
        appData: TransferAppData?,
        isSourceTemporary: Boolean,
        isHighPriority: Boolean,
    ): Flow<MultiTransferEvent> {
        return super.commonInvoke(
            items = files,
            null,
        ) { file ->
            transferRepository.startUpload(
                localPath = file.absolutePath,
                parentNodeId = destination.id,
                fileName = file.name,
                modificationTime = file.lastModified() / 1000,
                appData = appData,
                isSourceTemporary = isSourceTemporary,
                shouldStartFirst = isHighPriority,
            )
        }
    }

    override fun generateIdFromItem(item: File): String =
        item.path + File.separator + item.name

    override fun generateIdFromTransferEvent(transferEvent: TransferEvent) =
        transferEvent.transfer.localPath + File.separator + transferEvent.transfer.fileName

}