package mega.privacy.android.navigation

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.TypedFileNode
import java.io.File

/**
 * App module navigator
 *
 */
interface AppNavigator {
    /**
     * Navigates to the Settings Camera Uploads page
     *
     * @param activity The Activity
     */
    fun openSettingsCameraUploads(activity: Activity)

    /**
     * Navigates to the Backups page to load the contents of the Backup Folder
     *
     * @param activity the Activity
     * @param backupsHandle The Backups Handle used to load its contents
     * @param errorMessage The [StringRes] of the message to display in the error banner
     */
    fun openNodeInBackups(activity: Activity, backupsHandle: Long, @StringRes errorMessage: Int?)

    /**
     * Navigates to the Cloud Drive page to view the selected Node
     *
     * @param activity the Activity
     * @param nodeHandle The Node Handle to view the selected Node. The Root Node will be accessed
     * if no Node Handle is specified
     * @param errorMessage The [StringRes] of the message to display in the error banner
     */
    fun openNodeInCloudDrive(
        activity: Activity,
        nodeHandle: Long = -1L,
        @StringRes errorMessage: Int?,
    )

    /**
     * Open chat
     *
     * @param context
     * @param chatId chat id of the chat room
     * @param action action of the intent
     * @param link chat link
     * @param text text to show in snackbar
     * @param messageId message id
     * @param isOverQuota is over quota int value
     */
    fun openChat(
        context: Context,
        chatId: Long,
        action: String? = null,
        link: String? = null,
        text: String? = null,
        messageId: Long? = null,
        isOverQuota: Int? = null,
        flags: Int = 0,
    )

    /**
     * Navigates to the new [mega.privacy.android.app.presentation.meeting.managechathistory.view.screen.ManageChatHistoryActivityV2]
     *
     * @param context The context that call this method
     * @param chatId The chat ID of the chat or meeting room
     * @param email The email of the current user
     */
    fun openManageChatHistoryActivity(
        context: Context,
        chatId: Long = -1L,
        email: String? = null,
    )

    /**
     * Open upgrade account screen.
     * This screen allows users to upgrade to a paid plan
     */
    fun openUpgradeAccount(context: Context)

    /**
     * Navigates to the Syncs page
     *
     * @param activity      The Activity
     * @param deviceName    The device name
     * @param openNewSync   True to directly open New Sync screen, False otherwise.
     */
    fun openSyncs(activity: Activity, deviceName: String? = null, openNewSync: Boolean = false)

    /**
     * Open zip browser
     *
     * @param context Context
     * @param zipFilePath zip file path
     * @param nodeHandle the node handle of zip file
     * @param onError Callback called when zip file format check is not passed
     */
    fun openZipBrowserActivity(
        context: Context,
        zipFilePath: String,
        nodeHandle: Long? = null,
        onError: () -> Unit,
    )

    /**
     * Navigates to the new [InviteContactActivityV2].
     *
     * @param context The context that call this method.
     * @param isFromAchievement Whether the entry point is [InviteFriendsRoute].
     */
    fun openInviteContactActivity(context: Context, isFromAchievement: Boolean)

    /**
     * Navigate to [TransfersActivity]
     */
    fun openTransfers(context: Context, tab: Int)

    /**
     * Open media player by file node
     *
     * @param context Context
     * @param contentUri NodeContentUri
     * @param fileNode TypedFileNode
     * @param sortOrder SortOrder
     * @param viewType the adapter type of the view
     * @param isFolderLink whether the file is a folder link
     * @param isMediaQueueAvailable whether the media queue is available
     * @param searchedItems the list of searched items, this is only used under the search mode
     * @param mediaQueueTitle the title of the media queue
     * @param onError Callback called when error occurs
     */
    fun openMediaPlayerActivityByFileNode(
        context: Context,
        contentUri: NodeContentUri,
        fileNode: TypedFileNode,
        viewType: Int,
        sortOrder: SortOrder = SortOrder.ORDER_NONE,
        isFolderLink: Boolean = false,
        isMediaQueueAvailable: Boolean = true,
        searchedItems: List<Long>? = null,
        mediaQueueTitle: String? = null,
        onError: () -> Unit = {},
    )

    /**
     * Open media player by local file
     *
     * @param context Context
     * @param localFile File
     * @param fileTypeInfo FileTypeInfo
     * @param viewType the adapter type of the view
     * @param handle the handle of the node
     * @param parentId the parent id of the node
     * @param sortOrder SortOrder
     * @param isFolderLink whether the file is a folder link
     * @param isMediaQueueAvailable whether the media queue is available
     * @param searchedItems the list of searched items, this is only used under the search mode
     * @param onError Callback called when error occurs
     */
    fun openMediaPlayerActivityByLocalFile(
        context: Context,
        localFile: File,
        fileTypeInfo: FileTypeInfo,
        viewType: Int,
        handle: Long,
        parentId: Long,
        sortOrder: SortOrder = SortOrder.ORDER_NONE,
        isFolderLink: Boolean = false,
        isMediaQueueAvailable: Boolean = true,
        searchedItems: List<Long>? = null,
        onError: () -> Unit = {},
    )

    /**
     * Open media player from Chat
     *
     * @param context Context
     * @param contentUri [NodeContentUri]
     * @param message [NodeAttachmentMessage]
     * @param fileNode [FileNode]
     * @param onError Callback called when error occurs
     */
    fun openMediaPlayerActivityFromChat(
        context: Context,
        contentUri: NodeContentUri,
        message: NodeAttachmentMessage,
        fileNode: FileNode,
        onError: (Throwable) -> Unit = {},
    )
}
