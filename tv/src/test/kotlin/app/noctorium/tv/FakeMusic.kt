package app.noctorium.tv

import app.noctorium.core.AppUiState
import app.noctorium.core.LibraryState
import app.noctorium.domain.Artist
import app.noctorium.domain.Playlist
import app.noctorium.domain.ProviderType
import app.noctorium.domain.Track
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.QueueState
import app.noctorium.playlists.LocalPlaylist
import kotlinx.coroutines.flow.MutableStateFlow

fun song(id: String, provider: ProviderType = ProviderType.YOUTUBE_MUSIC) =
    Track(provider, id, "Song $id", listOf(Artist("a$id", "Artist $id", provider)), durationMs = 187_000, sourceUrl = "https://example.org/$id")

/** The TV's music, noting what it was asked to do. */
class FakeMusic : TvMusic {
    override val ui = MutableStateFlow(AppUiState(homeLoading = false))
    override val library = MutableStateFlow(LibraryState())
    override val playback = MutableStateFlow(PlaybackState())
    override val queue = MutableStateFlow(QueueState())
    val did = mutableListOf<String>()
    var found: List<Track> = listOf(song("x1"), song("x2"))
    var playlistSongs: List<Track> = listOf(song("p1"), song("p2"), song("p3"))

    override fun refreshHome() { did += "refresh home" }
    override suspend fun search(text: String): List<Track> { did += "search $text"; return found }
    override suspend fun stations(query: String) = listOf(song("fm", ProviderType.RADIO))
    override suspend fun playlistTracks(playlist: Playlist) = playlistSongs
    override fun playFromHome(track: Track, row: List<Track>) { did += "home ${track.id} of ${row.size}" }
    override fun playFromSearch(track: Track, results: List<Track>) { did += "search ${track.id} of ${results.size}" }
    override fun playRecent(track: Track, recent: List<Track>) { did += "recent ${track.id}" }
    override fun playLocalPlaylist(playlist: LocalPlaylist, startAt: Track?) { did += "local ${playlist.id} at ${startAt?.id}" }
    override fun playPlaylist(playlist: Playlist, songs: List<Track>, startAt: Track?) { did += "playlist ${playlist.id} at ${startAt?.id} of ${songs.size}" }
    override fun playStation(station: Track) { did += "station ${station.id}" }
    override fun jumpToQueueItem(index: Int) { did += "jump $index" }
    override fun playSuggestion(index: Int) { did += "suggestion $index" }
    override fun togglePlayback() { did += "play/pause" }
    override fun next() { did += "next" }
    override fun previous() { did += "previous" }
    override fun seekTo(positionMs: Long) { did += "seek $positionMs" }
    override fun toggleShuffle() { did += "shuffle" }
    override fun cycleRepeat() { did += "repeat" }
}
