package app.noctorium.tv

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.assertIsDisplayed
import app.noctorium.core.AppUiState
import app.noctorium.core.LibraryState
import app.noctorium.domain.HomeSection
import app.noctorium.domain.Playlist
import app.noctorium.domain.ProviderType
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.PlaybackStatus
import app.noctorium.playback.QueueState
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The remote's OK, on whatever is pressed: focus moves there, as the arrows would take it, and OK is pressed. */
@OptIn(ExperimentalTestApi::class)
fun SemanticsNodeInteraction.press() {
    requestFocus()
    performKeyInput { pressKey(Key.DirectionCenter) }
}

/** The TV's screens, drawn by Robolectric at a TV's size, pressed as the remote's OK presses them. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-land-television")
class TvScreensTest {
    @get:Rule
    val compose = createComposeRule()
    private val music = FakeMusic()
    private var openedPhoneScreens = 0

    private fun show() = compose.setContent { NoctoriumTv(music, openPhoneScreens = { openedPhoneScreens++ }) }

    @Test
    fun `a song on Home plays among its row, and the TV goes to what is playing`() {
        music.ui.value = AppUiState(homeLoading = false, homeSections = listOf(HomeSection("a", "New electronic", ProviderType.YOUTUBE_MUSIC, tracks = listOf(song("e1"), song("e2")))))
        show()
        compose.onNodeWithText("New electronic").assertIsDisplayed()
        compose.onNodeWithTag("tile:song:YOUTUBE_MUSIC:e2").press()
        assertEquals(listOf("home e2 of 2"), music.did - "refresh home")
        music.playback.value = PlaybackState(status = PlaybackStatus.PLAYING, track = song("e2"), durationMs = 187_000)
        compose.onNodeWithText("Artist e2").assertIsDisplayed()
        compose.onNodeWithTag("button:Pause").press()
        compose.onNodeWithTag("button:Next").press()
        assertEquals(listOf("home e2 of 2", "play/pause", "next"), music.did - "refresh home")
    }

    @Test
    fun `a playlist on Home opens to its songs, and plays from the one picked`() {
        music.ui.value = AppUiState(homeLoading = false, homeSections = listOf(HomeSection("b", "Mixes", ProviderType.YOUTUBE_MUSIC, tracks = emptyList(), playlists = listOf(Playlist("m", "My mix", ProviderType.YOUTUBE_MUSIC)))))
        show()
        compose.onNodeWithTag("tile:playlist:YOUTUBE_MUSIC:m").press()
        compose.onNodeWithText("My mix").assertIsDisplayed()
        compose.onNodeWithTag("song:YOUTUBE_MUSIC:p2").press()
        assertEquals(listOf("playlist m at p2 of 3"), music.did - "refresh home")
    }

    @Test
    fun `search finds songs to play`() {
        show()
        compose.onNodeWithTag("page:SEARCH").press()
        compose.onNodeWithTag("search-field").performTextInput("daft punk")
        compose.onNodeWithTag("search-field").performImeAction()
        compose.onNodeWithTag("tile:song:YOUTUBE_MUSIC:x2").press()
        assertEquals(listOf("search daft punk", "search x2 of 2"), music.did - "refresh home")
    }

    @Test
    fun `the queue marks the song playing, and a pick there jumps to it`() {
        music.queue.value = QueueState(tracks = listOf(song("a"), song("b")), currentIndex = 0, suggestions = listOf(song("s")), suggestionsFrom = "YouTube Music radio")
        show()
        compose.onNodeWithTag("page:QUEUE").press()
        compose.onNodeWithText("Playing now · Artist a").assertIsDisplayed()
        compose.onNodeWithText("Autoplay · from YouTube Music radio").assertIsDisplayed()
        compose.onNodeWithTag("queue:false:1").press()
        compose.onNodeWithTag("page:QUEUE").press()
        compose.onNodeWithTag("queue:true:0").press()
        assertEquals(listOf("jump 1", "suggestion 0"), music.did - "refresh home")
    }

    @Test
    fun `radio lists stations to play`() {
        show()
        compose.onNodeWithTag("page:RADIO").press()
        compose.onNodeWithTag("tile:song:RADIO:fm").press()
        assertEquals(listOf("station fm"), music.did - "refresh home")
    }

    @Test
    fun `the library opens a Noctorium playlist`() {
        music.library.value = LibraryState(localPlaylists = listOf(app.noctorium.playlists.LocalPlaylist("l", "Road trip", listOf(song("a"), song("b")))))
        show()
        compose.onNodeWithTag("page:LIBRARY").press()
        compose.onNodeWithTag("tile:local:l").press()
        compose.onNodeWithTag("play-all").press()
        assertEquals(listOf("local l at null"), music.did - "refresh home")
    }

    @Test
    fun `settings opens the full screens, for signing in`() {
        show()
        compose.onNodeWithTag("page:SETTINGS").press()
        compose.onNodeWithTag("open-phone-screens").press()
        assertTrue(openedPhoneScreens == 1)
    }

    @Test
    fun `the artist on Now playing opens their songs`() {
        music.playback.value = PlaybackState(status = PlaybackStatus.PLAYING, track = song("e2"), durationMs = 187_000)
        show()
        compose.onNodeWithTag("page:NOW_PLAYING").press()
        compose.onNodeWithTag("artist").press()
        compose.onNodeWithText("Artist e2").assertIsDisplayed()
        compose.onNodeWithTag("song:YOUTUBE_MUSIC:p1").press()
        assertEquals(listOf("playlist artist-named:Artist e2 at p1 of 3"), music.did - "refresh home")
    }

    @Test
    fun `search lists artists above the songs`() {
        music.artistsFound = listOf(Playlist("artist-named:Daft Punk", "Daft Punk", ProviderType.YOUTUBE_MUSIC))
        show()
        compose.onNodeWithTag("page:SEARCH").press()
        compose.onNodeWithTag("search-field").performTextInput("daft punk")
        compose.onNodeWithTag("search-field").performImeAction()
        compose.onNodeWithText("Artist · YouTube Music").assertIsDisplayed()
        compose.onNodeWithTag("tile:playlist:YOUTUBE_MUSIC:artist-named:Daft Punk").press()
        compose.onNodeWithTag("play-all").press()
        assertEquals(listOf("search daft punk", "playlist artist-named:Daft Punk at null of 3"), music.did - "refresh home")
    }

    @Test
    fun `a link typed into search plays at once when the setting says so`() {
        music.linkPlays = true
        show()
        compose.onNodeWithTag("page:SEARCH").press()
        compose.onNodeWithTag("search-field").performTextInput("https://soundcloud.com/flume/never-be-like-you")
        compose.onNodeWithTag("search-field").performImeAction()
        assertEquals(listOf("link https://soundcloud.com/flume/never-be-like-you"), music.did - "refresh home")
    }
}
