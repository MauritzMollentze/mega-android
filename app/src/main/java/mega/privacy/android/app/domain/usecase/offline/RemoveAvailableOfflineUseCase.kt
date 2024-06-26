package mega.privacy.android.app.domain.usecase.offline

import android.app.Activity
import mega.privacy.android.domain.entity.node.NodeId
import java.lang.ref.WeakReference

/**
 * Save or remove the file or folder represented by the given [NodeId] for offline use, if not already saved
 */
fun interface RemoveAvailableOfflineUseCase {
    /**
     * Save or remove the file or folder represented by the given [NodeId] for offline use, if not already saved
     * @param nodeId the [NodeId] of the node that will be saved offline
     * @param activity [WeakReference] of the activity to be used to show dialogs, access permission, etc.
     */
    suspend operator fun invoke(
        nodeId: NodeId,
        activity: WeakReference<Activity>,
    )
}