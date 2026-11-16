package app.noctorium.tv

import app.noctorium.core.AppUiState
import app.noctorium.core.LibraryState
import app.noctorium.domain.HomeSection
import app.noctorium.domain.Playlist
import app.noctorium.domain.ProviderType
import app.noctorium.playback.QueueState
import app.noctorium.playlists.LocalPlaylist
import kotlin.test.Test
import kotlin.test.assertEquals

class TvRowsTest {
    @Test
    fun `home is pinned, then what was played last, then the services' rows of songs and of playlists`() {
        val ui = AppUiState(
            pinnedTracks = listOf(song("p")),
            recentTracks = listOf(song("r1"), song("r2")),
            homeSections = listOf(
                HomeSection("a", "New electronic", ProviderType.YOUTUBE_MUSIC, tracks = listOf(song("e1"))),
                HomeSection("b", "Mixes for you", ProviderType.YOUTUBE_MUSIC, tracks = emptyList(), playlists = listOf(Playlist("m", "Mix", ProviderType.YOUTUBE_MUSIC))),
                HomeSection("c", "Empty", ProviderType.YOUTUBE_MUSIC, tracks = emptyList()),
            ),
        )
        val rows = homeRows(ui)
        assertEquals(listOf("Pinned", "Jump back in", "New electronic", "Mixes for you"), rows.map { it.title })
        assertEquals(listOf("Song r1", "Song r2"), rows[1].tiles.map { it.title })
        assertEquals(TvTile.ServicePlaylist::class, rows[3].tiles.single()::class)
    }

    @Test
    fun `a song card plays among the songs of its row`() {
        val recent = listOf(song("r1"), song("r2"), song("r3"))
        val tile = homeRows(AppUiState(recentTracks = recent)).single().tiles[1] as TvTile.Song
        assertEquals(recent, tile.among)
    }

    @Test
    fun `the library is recently played, your playlists, and those on your services`() {
        val rows = libraryRows(
            AppUiState(recentTracks = listOf(song("r"))),
            LibraryState(
                localPlaylists = listOf(LocalPlaylist("l", "Road trip", listOf(song("a"), song("b")))),
                playlists = listOf(Playlist("s", "Liked", ProviderType.SOUNDCLOUD, trackCount = 1)),
            ),
        )
        assertEquals(listOf("Recently played", "Your playlists", "On your services"), rows.map { it.title })
        assertEquals("2 songs", rows[1].tiles.single().subtitle)
        assertEquals("1 song", rows[2].tiles.single().subtitle)
    }

    @Test
    fun `the queue starts at the song playing and ends with autoplay's`() {
        val lines = queueLines(QueueState(tracks = listOf(song("old"), song("now"), song("next")), currentIndex = 1, suggestions = listOf(song("s"))))
        assertEquals(listOf("now", "next", "s"), lines.map { it.track.id })
        assertEquals(listOf(true, false, false), lines.map { it.nowPlaying })
        assertEquals(listOf(1, 2, 0), lines.map { it.index })
        assertEquals(listOf(false, false, true), lines.map { it.suggestion })
    }

    @Test
    fun `times read as a clock does`() {
        assertEquals("0:00", clock(0))
        assertEquals("3:07", clock(187_000))
        assertEquals("1:02:45", clock(3_765_000))
    }
}
