package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.exception.MegaException
import java.io.File

/**
 * The repository interface regarding thumbnail/preview feature.
 */
interface ImageRepository {

    /**
     * Check thumbnail from local
     * @param handle node handle
     * @return thumbnail file
     */
    suspend fun getThumbnailFromLocal(handle: Long): File?

    /**
     * Check thumbnail from server
     * @param handle node handle
     * @return thumbnail file
     */
    @Throws(MegaException::class)
    suspend fun getThumbnailFromServer(handle: Long): File?

    /**
     * Check preview from local
     * @param handle node handle
     * @return preview file
     */
    suspend fun getPreviewFromLocal(handle: Long): File?

    /**
     * Check preview from server
     * @param handle node handle
     * @return preview file
     */
    suspend fun getPreviewFromServer(handle: Long): File?

    /**
     * Download thumbnail
     *
     * @param handle
     * @param callback is download success
     */
    suspend fun downloadThumbnail(handle: Long, callback: (success: Boolean) -> Unit)

    /**
     * Download preview
     *
     * @param handle
     * @param callback is download success
     */
    suspend fun downloadPreview(handle: Long, callback: (success: Boolean) -> Unit)

    /**
     * Get Image Result given Offline File
     *
     * @param offlineNodeInformation    OfflineNodeInformation
     * @param file                      Image Offline File
     * @param highPriority              Flag to request image with high priority
     *
     * @return ImageResult
     */
    suspend fun getImageByOfflineFile(
        offlineNodeInformation: OfflineNodeInformation,
        file: File,
        highPriority: Boolean,
    ): ImageResult

    /**
     * Get Image Result given File
     *
     * @param file                      Image File
     * @param highPriority              Flag to request image with high priority
     *
     * @return ImageResult
     */
    suspend fun getImageFromFile(
        file: File,
        highPriority: Boolean,
    ): ImageResult

    /**
     * Get Thumbnail Cache Path
     */
    fun getThumbnailPath(): String

    /**
     * Get Preview Cache Path
     */
    fun getPreviewPath(): String

    /**
     * Get Full Image Cache Path
     */
    fun getFullImagePath(): String

    /**
     * Get ImageNode given Node Handle
     * @param handle                Image Node handle to request
     * @return ImageNode            Image Node
     */
    suspend fun getImageNodeByHandle(handle: Long): ImageNode

    /**
     * Get ImageNode given Public Link
     * @param nodeFileLink          Public link to a file in MEGA
     * @return ImageNode            Image Node
     */
    suspend fun getImageNodeForPublicLink(nodeFileLink: String): ImageNode

    /**
     * Get ImageNode given Chat Room Id and Chat Message Id
     * @param chatRoomId            Chat Room Id
     * @param chatMessageId         Chat Message Id
     * @return ImageNode            Image Node
     */
    suspend fun getImageNodeForChatMessage(
        chatRoomId: Long,
        chatMessageId: Long,
    ): ImageNode
}