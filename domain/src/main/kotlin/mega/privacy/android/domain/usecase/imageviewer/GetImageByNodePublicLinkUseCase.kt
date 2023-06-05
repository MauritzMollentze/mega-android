package mega.privacy.android.domain.usecase.imageviewer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.repository.ImageRepository
import mega.privacy.android.domain.usecase.node.AddImageTypeUseCase
import javax.inject.Inject

/**
 * The use case to get Image Result given Node Public Link
 */
class GetImageByNodePublicLinkUseCase @Inject constructor(
    private val addImageTypeUseCase: AddImageTypeUseCase,
    private val getImageUseCase: GetImageUseCase,
    private val imageRepository: ImageRepository,
) {
    /**
     * Invoke
     *
     * @param nodeFileLink          Public link to a file in MEGA
     * @param fullSize              Flag to request full size image despite data/size requirements
     * @param highPriority          Flag to request image with high priority
     * @param resetDownloads        Callback to reset downloads
     *
     * @return Flow<ImageResult>
     */
    operator fun invoke(
        nodeFileLink: String,
        fullSize: Boolean,
        highPriority: Boolean,
        resetDownloads: () -> Unit,
    ): Flow<ImageResult> = flow {
        val node = addImageTypeUseCase(imageRepository.getImageNodeForPublicLink(nodeFileLink))
        emitAll(getImageUseCase(node, fullSize, highPriority, resetDownloads))
    }
}