package mega.privacy.android.app.mediaplayer.gateway

import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.mediaplayer.service.Metadata

/**
 * The media player service gateway
 */
interface MediaPlayerServiceGateway {
    /**
     *  Metadata update
     *
     *  @return Flow<Metadata>
     */
    fun metadataUpdate(): Flow<Metadata>

    /**
     * Stop audio player
     */
    fun stopAudioPlayer()

    /**
     * Close video player UI
     */
    fun mainPlayerUIClosed()

    /**
     * Judge the player whether is playing
     *
     * @return true is playing, otherwise is false.
     */
    fun playing(): Boolean

    /**
     * Seek to the index
     *
     * @param index the index that is sought to
     */
    fun seekTo(index: Int)

    /**
     * Set playWhenReady
     *
     * @param playWhenReady playWhenReady
     */
    fun setPlayWhenReady(playWhenReady: Boolean)

    /**
     * Remove the listener from player
     *
     * @param listener removed listener
     */
    fun removeListener(listener: Player.Listener)

    /**
     * Add the listener for player
     *
     * @param listener Player.Listener
     */
    fun addPlayerListener(listener: Player.Listener)

    /**
     * Get the playback state
     *
     * @return playback state
     */
    fun getPlaybackState(): Int

    /**
     * Get the current media item
     *
     * @return MediaItem
     */
    fun getCurrentMediaItem(): MediaItem?

    /**
     * Setup player view
     *
     * @param playerView PlayerView
     * @param useController useController
     * @param controllerShowTimeoutMs controllerShowTimeoutMs
     * @param repeatToggleModes repeatToggleModes
     * @param controllerHideOnTouch controllerHideOnTouch
     * @param showShuffleButton showShuffleButton
     */
    fun setupPlayerView(
        playerView: PlayerView,
        useController: Boolean = true,
        controllerShowTimeoutMs: Int = 0,
        controllerHideOnTouch: Boolean = false,
        repeatToggleModes: Int? = null,
        showShuffleButton: Boolean? = null,
    )
}