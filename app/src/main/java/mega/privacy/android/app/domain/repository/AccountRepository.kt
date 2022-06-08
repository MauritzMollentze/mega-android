package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.UserCredentials
import mega.privacy.android.app.domain.entity.UserAccount
import mega.privacy.android.app.domain.exception.MegaException
import mega.privacy.android.app.domain.entity.user.UserUpdate
import nz.mega.sdk.MegaNode

/**
 * Account repository
 */
interface AccountRepository {
    /**
     * Get user account
     *
     * @return the user account for the current user
     */
    suspend fun getUserAccount(): UserAccount

    /**
     * Is account data stale
     *
     * @return true if account data is stale. else false
     */
    fun isAccountDataStale(): Boolean

    /**
     * Request account
     * Sends a request to update account data asynchronously
     */
    fun requestAccount()

    /**
     * Is multi factor auth available
     *
     * @return true if multi-factor auth is available for the current user, else false
     */
    fun isMultiFactorAuthAvailable(): Boolean

    /**
     * Is multi factor auth enabled
     *
     * @return true if multi-factor auth is enabled for the current user, else false
     */
    @Throws(MegaException::class)
    suspend fun isMultiFactorAuthEnabled(): Boolean

    /**
     * Monitor multi factor auth changes
     *
     * @return a flow that emits changes to the multi-factor auth enabled state
     */
    fun monitorMultiFactorAuthChanges(): Flow<Boolean>

    /**
     * Request delete account link
     *
     * Sends a delete account link to the user's email address
     *
     */
    suspend fun requestDeleteAccountLink()

    /**
     * Monitor user updates
     *
     * @return a flow of all global user updates
     */
    fun monitorUserUpdates(): Flow<UserUpdate>


    /**
     * Gets user account credentials.
     *
     * @return User credentials if exists, null otherwise.
     */
    suspend fun getCredentials(): UserCredentials?

    /**
     * Refreshes DNS servers and retries pending connections.
     *
     * @param disconnect True if should disconnect megaChatApi, false otherwise.
     */
    fun retryPendingConnections(disconnect: Boolean)
}