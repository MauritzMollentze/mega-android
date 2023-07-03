package mega.privacy.android.app.constants

/**
 * The constants regarding Broadcast
 */
object BroadcastConstants {
    //    Broadcasts' IntentFilter
    const val BROADCAST_ACTION_INTENT_MANAGE_SHARE = "BROADCAST_ACTION_INTENT_MANAGE_SHARE"
    const val BROADCAST_ACTION_INTENT_TAKEN_DOWN_FILES = "INTENT_TAKEN_DOWN_FILES"
    const val BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE = "INTENT_FILTER_CONTACT_UPDATE"
    const val BROADCAST_ACTION_SHOW_SNACKBAR = "INTENT_SHOW_SNACKBAR"
    const val BROADCAST_ACTION_DESTROY_ACTION_MODE = "INTENT_DESTROY_ACTION_MODE"
    const val BROADCAST_ACTION_INTENT_RICH_LINK_SETTING_UPDATE = "INTENT_RICH_LINK_SETTING_UPDATE"
    const val BROADCAST_ACTION_RESUME_TRANSFERS = "INTENT_RESUME_TRANSFERS"
    const val BROADCAST_ACTION_REENABLE_CU_PREFERENCE = "BROADCAST_ACTION_REENABLE_CU_PREFERENCE"
    const val BROADCAST_ACTION_UPDATE_HISTORY_BY_RT = "ACTION_UPDATE_HISTORY_BY_RT"
    const val BROADCAST_ACTION_CHAT_TRANSFER_START = "INTENT_CHAT_TRANSFER_START"
    const val BROADCAST_ACTION_COOKIE_SETTINGS_SAVED = "BROADCAST_ACTION_COOKIE_SETTINGS_SAVED"
    const val BROADCAST_ACTION_ERROR_COPYING_NODES = "INTENT_ERROR_COPYING_NODES"
    const val BROADCAST_ACTION_RETRY_PENDING_MESSAGE = "INTENT_RETRY_PENDING_MESSAGE"

    //    Broadcasts' actions
    const val ACTION_UPDATE_CACHE_SIZE_SETTING = "ACTION_UPDATE_CACHE_SIZE_SETTING"
    const val ACTION_UPDATE_OFFLINE_SIZE_SETTING = "ACTION_UPDATE_OFFLINE_SIZE_SETTING"
    const val ACTION_RESET_VERSION_INFO_SETTING = "ACTION_RESET_VERSION_INFO_SETTING"
    const val ACTION_UPDATE_NICKNAME = "ACTION_UPDATE_NICKNAME"
    const val ACTION_UPDATE_FIRST_NAME = "ACTION_UPDATE_FIRST_NAME"
    const val ACTION_UPDATE_LAST_NAME = "ACTION_UPDATE_LAST_NAME"
    const val ACTION_UPDATE_CREDENTIALS = "ACTION_UPDATE_CREDENTIALS"
    const val ACTION_CLOSE_CHAT_AFTER_IMPORT = "ACTION_CLOSE_CHAT_AFTER_IMPORT"
    const val ACTION_CLOSE_CHAT_AFTER_OPEN_TRANSFERS = "ACTION_CLOSE_CHAT_AFTER_OPEN_TRANSFERS"
    const val ACTION_REFRESH_CAMERA_UPLOADS_SETTING = "ACTION_REFRESH_CAMERA_UPLOADS_SETTING"
    const val ACTION_DISABLE_MEDIA_UPLOADS_SETTING = "ACTION_DISABLE_MEDIA_UPLOADS_SETTING"
    const val ACTION_REFRESH_CLEAR_OFFLINE_SETTING = "ACTION_REFRESH_CLEAR_OFFLINE_SETTING"
    const val ACTION_UPDATE_RB_SCHEDULER = "ACTION_UPDATE_RB_SCHEDULER"
    const val ACTION_UPDATE_RETENTION_TIME = "ACTION_UPDATE_RETENTION_TIME"

    //    Broadcasts' extras
    const val TYPE_SHARE = "TYPE_SHARE"
    const val NUMBER_FILES = "NUMBER_FILES"
    const val EXTRA_USER_HANDLE = "USER_HANDLE"
    const val SNACKBAR_TEXT = "SNACKBAR_TEXT"
    const val CACHE_SIZE = "CACHE_SIZE"
    const val OFFLINE_SIZE = "OFFLINE_SIZE"
    const val ACTION_TYPE = "ACTION_TYPE"
    const val INVALID_ACTION = -1
    const val DAYS_COUNT = "DAYS_COUNT"
    const val KEY_REENABLE_WHICH_PREFERENCE = "REENABLE_WHICH_PREFERENCE"
    const val RETENTION_TIME = "RETENTION_TIME"
    const val PENDING_MESSAGE_ID = "PENDING_MESSAGE_ID"
    const val ERROR_MESSAGE_TEXT = "ERROR_MESSAGE_TEXT"
}
