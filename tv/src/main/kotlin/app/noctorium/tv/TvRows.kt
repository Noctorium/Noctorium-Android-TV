package app.noctorium.tv

import app.noctorium.core.AppUiState
import app.noctorium.core.LibraryState
import app.noctorium.domain.Playlist
import app.noctorium.domain.isLikedCollection
import app.noctorium.domain.isArtistPage
import app.noctorium.domain.Track
import app.noctorium.playback.QueueState
import app.noctorium.playlists.LocalPlaylist

/** One card on a TV screen: a song that plays, or a playlist that opens. */
sealed interface TvTile {
    val key: String
    val title: String
    val subtitle: String?
    val artworkUrl: String?

    /** A song, and the songs it was shown among: what plays after it. */
    data class Song(val track: Track, val among: List<Track>) : TvTile {
        override val key get() = "song:${track.queueKey}"
        override val title get() = track.title
        override val subtitle get() = track.artistLine.ifBlank { null }
        override val artworkUrl get() = track.artworkUrl
    }

    /** A playlist on a service, whose songs are read on opening it. */
    data class ServicePlaylist(val playlist: Playlist) : TvTile {
        override val key get() = "playlist:${playlist.provider.name}:${playlist.id}"
        override val title get() = playlist.title
        override val subtitle get() = if (playlist.isArtistPage()) "Artist · ${playlist.provider.displayName}"
            else (playlist.trackCount ?: playlist.tracks.size.takeIf { it > 0 })?.let(::songs) ?: playlist.ownerName
        override val artworkUrl get() = playlist.artworkUrl ?: playlist.tracks.firstOrNull()?.artworkUrl
    }

    /** A Noctorium playlist, kept on this device. */
    data class LocalList(val playlist: LocalPlaylist) : TvTile {
        override val key get() = "local:${playlist.id}"
        override val title get() = playlist.title
        override val subtitle get() = songs(playlist.tracks.size)
        override val artworkUrl get() = playlist.tracks.firstNotNullOfOrNull { it.artworkUrl }
    }
}

/** A row of cards, under its title. */
data class TvRow(val title: String, val subtitle: String?, val tiles: List<TvTile>)

internal fun songs(count: Int) = if (count == 1) "1 song" else "$count songs"

private fun songRow(title: String, subtitle: String?, tracks: List<Track>) =
    TvRow(title, subtitle, tracks.distinctBy { it.queueKey }.map { TvTile.Song(it, tracks) })

/**
 * Home, as the TV shows it: what is pinned, what was played last, then the services' own rows -- of songs, or of
 * playlists and albums that open.
 */
fun homeRows(ui: AppUiState): List<TvRow> = buildList {
    if (ui.pinnedTracks.isNotEmpty()) add(songRow("Pinned", null, ui.pinnedTracks))
    if (ui.recentTracks.isNotEmpty()) add(songRow("Jump back in", "What you played last", ui.recentTracks))
    ui.homeSections.forEach { section ->
        when {
            section.tracks.isNotEmpty() -> add(songRow(section.title, section.subtitle, section.tracks))
            section.playlists.isNotEmpty() -> add(TvRow(section.title, section.subtitle, section.playlists.map { TvTile.ServicePlaylist(it) }))
        }
    }
}

/** Your library: what was played last, your Noctorium playlists, and your playlists on the services. */
fun libraryRows(ui: AppUiState, library: LibraryState): List<TvRow> = buildList {
    if (ui.recentTracks.isNotEmpty()) add(songRow("Recently played", null, ui.recentTracks))
    if (library.localPlaylists.isNotEmpty()) add(TvRow("Your playlists", "Kept in Noctorium", library.localPlaylists.map { TvTile.LocalList(it) }))
    // Each service's liked songs first, in a row of their own: what is opened most, found first.
    val liked = library.playlists.filter { it.isLikedCollection() }
    if (liked.isNotEmpty()) add(TvRow("Liked songs", null, liked.map { TvTile.ServicePlaylist(it) }))
    val rest = library.playlists.filterNot { it.isLikedCollection() }
    if (rest.isNotEmpty()) add(TvRow("On your services", null, rest.map { TvTile.ServicePlaylist(it) }))
}

/** A line of the queue as the TV lists it. */
data class TvQueueLine(val track: Track, val nowPlaying: Boolean, val suggestion: Boolean, val index: Int)

/** The queue from the song playing on (what was played before it is left off), then autoplay's songs after it. */
fun queueLines(queue: QueueState): List<TvQueueLine> {
    val from = queue.currentIndex.coerceAtLeast(0)
    val queued = queue.tracks.withIndex().drop(from).map { (i, track) -> TvQueueLine(track, i == queue.currentIndex, false, i) }
    val suggested = queue.suggestions.withIndex().map { (i, track) -> TvQueueLine(track, false, true, i) }
    return queued + suggested
}

/** "3:07", "1:02:45": how a length or a place in a song reads. */
fun clock(ms: Long): String {
    val seconds = (ms.coerceAtLeast(0) / 1000)
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
