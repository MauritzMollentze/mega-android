package mega.privacy.android.feature.sync.data.gateway

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.suspendCancellableCoroutine
import mega.privacy.android.data.listener.OptionalMegaListenerInterface
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.feature.sync.data.model.MegaSyncListenerEvent
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaSync
import nz.mega.sdk.MegaSync.SyncType
import nz.mega.sdk.MegaSyncList
import nz.mega.sdk.MegaSyncStallList
import nz.mega.sdk.MegaSyncStats
import nz.mega.sdk.StalledIssuesReceiver
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Gateway implementation to access Sync API
 */
internal class SyncGatewayImpl @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    @ApplicationScope private val appScope: CoroutineScope,
) : SyncGateway {

    override val syncUpdate = callbackFlow {
        val listener = OptionalMegaListenerInterface(
            onSyncDeleted = {
                trySend(MegaSyncListenerEvent.OnSyncDeleted(it))
            },
            onSyncStatsUpdated = {
                trySend(MegaSyncListenerEvent.OnSyncStatsUpdated(it))
            },
            onSyncStateChanged = {
                trySend(MegaSyncListenerEvent.OnSyncStateChanged(it))
            }
        )
        megaApi.addListener(listener)
        awaitClose {
            megaApi.removeListener(listener)
        }
    }.shareIn(
        appScope,
        SharingStarted.WhileSubscribed()
    )

    private var stalledIssuesListener: StalledIssuesReceiver? = null

    override suspend fun syncFolderPair(
        syncType: SyncType,
        name: String?,
        localPath: String,
        remoteFolderId: Long,
    ): Boolean =
        suspendCancellableCoroutine { continuation ->
            val requestListener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { _: MegaRequest, error: MegaError ->
                    if (error.errorCode == MegaError.API_OK) {
                        continuation.resumeWith(Result.success(true))
                    } else {
                        continuation.resumeWith(Result.success(false))
                    }
                }
            )
            megaApi.syncFolder(
                syncType,
                localPath,
                name,
                remoteFolderId,
                null,
                requestListener
            )
        }

    override suspend fun getFolderPairs(): MegaSyncList =
        megaApi.syncs

    override suspend fun removeFolderPair(folderPairId: Long) {
        megaApi.removeSync(folderPairId)
    }

    override fun monitorOnSyncDeleted(): Flow<MegaSync> =
        syncUpdate
            .filterIsInstance<MegaSyncListenerEvent.OnSyncDeleted>()
            .map { it.sync }

    override fun monitorOnSyncStatsUpdated(): Flow<MegaSyncStats> =
        syncUpdate
            .filterIsInstance<MegaSyncListenerEvent.OnSyncStatsUpdated>()
            .map { it.syncStats }

    override fun monitorOnSyncStateChanged(): Flow<MegaSync> =
        syncUpdate
            .filterIsInstance<MegaSyncListenerEvent.OnSyncStateChanged>()
            .map { it.sync }

    override fun resumeSync(folderPairId: Long) {
        megaApi.resumeSync(folderPairId)
    }

    override fun pauseSync(folderPairId: Long) {
        megaApi.pauseSync(folderPairId)
    }

    override suspend fun getSyncStalledIssues(): MegaSyncStallList? {
        return if (megaApi.isSyncStalled) {
            suspendCancellableCoroutine { continuation ->
                stalledIssuesListener = StalledIssuesReceiver { megaSyncStallList ->
                    megaApi.removeRequestListener(stalledIssuesListener)
                    stalledIssuesListener = null
                    continuation.resume(megaSyncStallList)
                }
                megaApi.requestMegaSyncStallList(stalledIssuesListener)
            }
        } else {
            null
        }
    }

    override suspend fun isNodeSyncableWithError(megaNode: MegaNode): MegaError =
        megaApi.isNodeSyncableWithError(megaNode)
}