package app.noctorium.tv

import app.noctorium.core.AppState
import app.noctorium.core.AppUiState
import app.noctorium.core.LibraryState
import app.noctorium.domain.PlaybackOrigin
import app.noctorium.domain.Playlist
import app.noctorium.domain.Track
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.QueueState
import app.noctorium.playlists.LocalPlaylist
import kotlinx.coroutines.flow.StateFlow

/**
 * Noctorium, as the TV's screens see it: what they show, and what pressing something there does.
 *
 * The app's [AppState] is the one answer to this ([AppStateTvMusic]); the tests give the screens one of their own. The
 * screens read the app's own state and play through the same calls the phone's screens make, so a TV is the same
 * Noctorium as a phone with other screens, never a second player.
 */
interface TvMusic {
    val ui: StateFlow<AppUiState>
    val library: StateFlow<LibraryState>
    val playback: StateFlow<PlaybackState>
    val queue: StateFlow<QueueState>

    fun refreshHome()
    suspend fun search(text: String): List<Track>

    /** Radio stations: the most listened-to in the country chosen in the app, or those [query] finds. */
    suspend fun stations(query: String = ""): List<Track>

    /** A service playlist's songs, read on opening it. */
    suspend fun playlistTracks(playlist: Playlist): List<Track>

    fun playFromHome(track: Track, row: List<Track>)
    fun playFromSearch(track: Track, results: List<Track>)
    fun playRecent(track: Track, recent: List<Track>)
    fun playLocalPlaylist(playlist: LocalPlaylist, startAt: Track?)
    fun playPlaylist(playlist: Playlist, songs: List<Track>, startAt: Track?)
    fun playStation(station: Track)
    fun jumpToQueueItem(index: Int)
    fun playSuggestion(index: Int)

    fun togglePlayback()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)
    fun toggleShuffle()
    fun cycleRepeat()
}

/** The TV's view of the app's own state. */
class AppStateTvMusic(private val state: AppState) : TvMusic {
    override val ui get() = state.ui
    override val library get() = state.library
    override val playback get() = state.playback
    override val queue get() = state.shownQueue

    override fun refreshHome() = state.refreshHome()
    override suspend fun search(text: String) = state.searchQuietly(text)
    override suspend fun stations(query: String) = state.radioStationsQuietly(query)
    override suspend fun playlistTracks(playlist: Playlist) = playlist.tracks.ifEmpty { state.playlistTracks(playlist) }

    override fun playFromHome(track: Track, row: List<Track>) = state.playFromHome(track, row)
    override fun playFromSearch(track: Track, results: List<Track>) = state.playFromSearch(track, results)
    override fun playRecent(track: Track, recent: List<Track>) = state.play(track, PlaybackOrigin.LIBRARY, recent)
    override fun playLocalPlaylist(playlist: LocalPlaylist, startAt: Track?) = state.playLocalPlaylist(playlist, startAt)

    // A playlist's songs as read here, so a playlist opened on the TV plays what the TV showed of it.
    override fun playPlaylist(playlist: Playlist, songs: List<Track>, startAt: Track?) =
        state.playPlaylist(if (playlist.tracks.isEmpty() && songs.isNotEmpty()) playlist.copy(tracks = songs) else playlist, startAt)

    override fun playStation(station: Track) = state.playStation(station)
    override fun jumpToQueueItem(index: Int) = state.jumpToQueueItem(index)
    override fun playSuggestion(index: Int) = state.playSuggestion(index)

    override fun togglePlayback() = state.togglePlayback()
    override fun next() = state.next()
    override fun previous() = state.previous()
    override fun seekTo(positionMs: Long) = state.seekTo(positionMs)
    override fun toggleShuffle() = state.toggleShuffle()
    override fun cycleRepeat() = state.cycleRepeat()
}
