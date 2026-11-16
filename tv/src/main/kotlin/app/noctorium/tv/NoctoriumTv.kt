package app.noctorium.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import app.noctorium.domain.Track
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.PlaybackStatus
import app.noctorium.playback.RepeatMode
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

/** The pages a TV moves between, in the order the side of the screen lists them. */
enum class TvPage(val label: String, val icon: ImageVector) {
    NOW_PLAYING("Now playing", Icons.Filled.MusicNote),
    HOME("Home", Icons.Filled.Home),
    SEARCH("Search", Icons.Filled.Search),
    LIBRARY("Library", Icons.Filled.LibraryMusic),
    QUEUE("Queue", Icons.AutoMirrored.Filled.QueueMusic),
    RADIO("Radio", Icons.Filled.Radio),
    SETTINGS("Settings", Icons.Filled.Settings),
}

/** A list of songs opened from a card -- a playlist -- with what playing one of them does. */
private class OpenedList(
    val title: String,
    val subtitle: String?,
    val load: suspend () -> List<Track>,
    val play: (startAt: Track?, songs: List<Track>) -> Unit,
)

private val Night = Color(0xFF08070C)
private val Raised = Color(0xFF16141F)
private val Accent = Color(0xFF9D7BFF)
private val Quiet = Color(0xFFB4AFC4)

@Composable
private fun TvTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(
        primary = Accent,
        onPrimary = Color.Black,
        surface = Raised,
        onSurface = Color.White,
        background = Night,
        onBackground = Color.White,
        border = Accent,
    ),
    content = content,
)

/**
 * Noctorium on a television: a list of pages down the left, moved through with the remote's arrows, and the page on
 * the right. Back goes from an opened playlist to its page, and from any page to Home.
 *
 * @param openPhoneScreens Noctorium's own screens, for what a TV page does not do: signing in to the services, and
 * settings.
 */
@Composable
fun NoctoriumTv(music: TvMusic, openPhoneScreens: () -> Unit, modifier: Modifier = Modifier) = TvTheme {
    var page by rememberSaveable { mutableStateOf(TvPage.HOME) }
    var opened by remember { mutableStateOf<OpenedList?>(null) }
    val playback by music.playback.collectAsState()
    val search = remember { TvSearchState() }

    BackHandler(enabled = opened != null || page != TvPage.HOME) {
        if (opened != null) opened = null else page = TvPage.HOME
    }

    val go: (TvPage) -> Unit = { page = it; opened = null }
    val played: () -> Unit = { go(TvPage.NOW_PLAYING) }
    val openTile: (TvTile, (TvTile.Song) -> Unit) -> Unit = { tile, playSong ->
        when (tile) {
            is TvTile.Song -> { playSong(tile); played() }
            is TvTile.ServicePlaylist -> opened = OpenedList(tile.title, tile.subtitle, { music.playlistTracks(tile.playlist) }) { start, songs ->
                music.playPlaylist(tile.playlist, songs, start)
            }
            is TvTile.LocalList -> opened = OpenedList(tile.title, tile.subtitle, { tile.playlist.tracks }) { start, _ ->
                music.playLocalPlaylist(tile.playlist, start)
            }
        }
    }

    Row(modifier.fillMaxSize().background(Night)) {
        SideList(page, playback, go)
        Box(Modifier.weight(1f).fillMaxHeight()) {
            val list = opened
            if (list != null) {
                SongListPage(list) { start, songs -> list.play(start, songs); played() }
            } else {
                when (page) {
                    TvPage.NOW_PLAYING -> NowPlayingPage(music, go)
                    TvPage.HOME -> HomePage(music) { tile -> openTile(tile) { music.playFromHome(it.track, it.among) } }
                    TvPage.SEARCH -> SearchPage(music, search) { tile -> openTile(tile) { music.playFromSearch(it.track, it.among) } }
                    TvPage.LIBRARY -> LibraryPage(music) { tile -> openTile(tile) { music.playRecent(it.track, it.among) } }
                    TvPage.QUEUE -> QueuePage(music, played)
                    TvPage.RADIO -> RadioPage(music) { station -> music.playStation(station); played() }
                    TvPage.SETTINGS -> SettingsPage(openPhoneScreens)
                }
            }
        }
    }
}

// --- The side ---

@Composable
private fun SideList(page: TvPage, playback: PlaybackState, go: (TvPage) -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Column(
        Modifier.width(232.dp).fillMaxHeight().background(Raised.copy(alpha = .55f)).padding(horizontal = 16.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Noctorium", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(start = 12.dp, bottom = 20.dp))
        val pages = TvPage.entries.filter { it != TvPage.NOW_PLAYING || playback.track != null }
        pages.forEach { entry ->
            Surface(
                onClick = { go(entry) },
                modifier = Modifier.fillMaxWidth().testTag("page:${entry.name}").let { if (entry == TvPage.HOME) it.focusRequester(first) else it },
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (entry == page) Accent.copy(alpha = .22f) else Color.Transparent,
                    contentColor = if (entry == page) Color.White else Quiet,
                    focusedContainerColor = Color.White,
                    focusedContentColor = Color.Black,
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(entry.icon, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(entry.label, fontSize = 17.sp)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        playback.track?.let { track ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
                Cover(track.artworkUrl, Modifier.size(44.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(track.title, fontSize = 13.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(statusLine(playback), fontSize = 12.sp, color = Quiet, maxLines = 1)
                }
            }
        }
    }
}

internal fun statusLine(playback: PlaybackState): String = when (playback.status) {
    PlaybackStatus.PLAYING -> "Playing"
    PlaybackStatus.PAUSED -> "Paused"
    PlaybackStatus.RESOLVING -> "Loading…"
    PlaybackStatus.ERROR -> "Couldn't play"
    PlaybackStatus.IDLE -> "Stopped"
}

// --- Pieces ---

@Composable
private fun Cover(url: String?, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(Raised), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Quiet.copy(alpha = .5f))
        if (url != null) AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun PageTitle(text: String, subtitle: String? = null) {
    Column(Modifier.padding(bottom = 18.dp)) {
        Text(text, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)
        subtitle?.let { Text(it, fontSize = 16.sp, color = Quiet) }
    }
}

@Composable
private fun Note(text: String) = Text(text, fontSize = 18.sp, color = Quiet, modifier = Modifier.padding(vertical = 12.dp))

@Composable
private fun TileCard(tile: TvTile, onClick: () -> Unit, size: Int = 176, modifier: Modifier = Modifier) {
    Column(Modifier.width(size.dp)) {
        Card(
            onClick = onClick,
            modifier = modifier.size(size.dp).testTag("tile:${tile.key}"),
            shape = CardDefaults.shape(RoundedCornerShape(14.dp)),
            scale = CardDefaults.scale(focusedScale = 1.08f),
            border = CardDefaults.border(focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = RoundedCornerShape(14.dp))),
        ) {
            Cover(tile.artworkUrl, Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(10.dp))
        Text(tile.title, fontSize = 15.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        tile.subtitle?.let { Text(it, fontSize = 13.sp, color = Quiet, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
private fun TileRow(row: TvRow, onTile: (TvTile) -> Unit) {
    Column(Modifier.padding(bottom = 26.dp)) {
        Text(row.title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        row.subtitle?.let { Text(it, fontSize = 14.sp, color = Quiet) }
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp), contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)) {
            items(row.tiles, key = { it.key }) { tile -> TileCard(tile, { onTile(tile) }) }
        }
    }
}

@Composable
private fun RowsPage(title: String, rows: List<TvRow>, empty: String, onTile: (TvTile) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 40.dp, end = 40.dp, top = 32.dp, bottom = 48.dp)) {
        item { PageTitle(title) }
        if (rows.isEmpty()) item { Note(empty) }
        items(rows, key = { it.title }) { row -> TileRow(row, onTile) }
    }
}

@Composable
private fun SongLine(number: String, track: Track, note: String?, highlighted: Boolean, onClick: () -> Unit, tag: String) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag(tag),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (highlighted) Accent.copy(alpha = .18f) else Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(number, fontSize = 15.sp, modifier = Modifier.width(36.dp))
            Cover(track.artworkUrl, Modifier.size(48.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(note, track.artistLine.ifBlank { null }).joinToString(" · "), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            track.durationMs?.let { Text(clock(it), fontSize = 14.sp) }
        }
    }
}

// --- Pages ---

@Composable
private fun HomePage(music: TvMusic, onTile: (TvTile) -> Unit) {
    val ui by music.ui.collectAsState()
    LaunchedEffect(Unit) { if (ui.homeSections.isEmpty() && !ui.homeLoading) music.refreshHome() }
    val rows = homeRows(ui)
    RowsPage(
        "Home",
        rows,
        if (ui.homeLoading) "Loading Home…" else "Nothing on Home yet. Sign in to a service under Settings, and Home fills with its music.",
        onTile,
    )
}

@Composable
private fun LibraryPage(music: TvMusic, onTile: (TvTile) -> Unit) {
    val ui by music.ui.collectAsState()
    val library by music.library.collectAsState()
    RowsPage("Library", libraryRows(ui, library), "Nothing here yet: songs you play, and your playlists, show here.", onTile)
}

@Composable
private fun SongListPage(list: OpenedList, play: (Track?, List<Track>) -> Unit) {
    val songs by produceState<List<Track>?>(null, list) { value = runCatching { list.load() }.getOrDefault(emptyList()) }
    val first = remember { FocusRequester() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 40.dp, end = 40.dp, top = 32.dp, bottom = 48.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { PageTitle(list.title, list.subtitle) }
        val loaded = songs
        when {
            loaded == null -> item { Note("Loading…") }
            loaded.isEmpty() -> item { Note("No songs in this playlist could be read.") }
            else -> {
                item {
                    Surface(
                        onClick = { play(null, loaded) },
                        modifier = Modifier.padding(bottom = 14.dp).focusRequester(first).testTag("play-all"),
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
                        colors = ClickableSurfaceDefaults.colors(containerColor = Accent, contentColor = Color.Black, focusedContainerColor = Color.White, focusedContentColor = Color.Black),
                    ) {
                        Row(Modifier.padding(horizontal = 22.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Play", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
                }
                items(loaded.withIndex().toList(), key = { (i, t) -> "$i:${t.queueKey}" }) { (i, track) ->
                    SongLine("${i + 1}", track, null, false, { play(track, loaded) }, "song:${track.queueKey}")
                }
            }
        }
    }
}

/** What was searched for and found, kept while the TV moves to other pages and back. */
internal class TvSearchState {
    var query by mutableStateOf("")
    var searched by mutableStateOf<String?>(null)
    var results by mutableStateOf<List<Track>>(emptyList())
    var loading by mutableStateOf(false)

    /** Counts the searches, so the first result is given the remote's focus once for each, when it arrives. */
    var runs by mutableStateOf(0)
    var focusedRun = 0
}

@Composable
private fun SearchPage(music: TvMusic, search: TvSearchState, onTile: (TvTile) -> Unit) {
    val scope = rememberCoroutineScope()
    val field = remember { FocusRequester() }
    val firstResult = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val run = {
        val text = search.query.trim()
        if (text.isNotEmpty()) {
            // Out of the way of what was found: the TV's keyboard covers half the screen.
            keyboard?.hide()
            search.loading = true
            search.searched = text
            scope.launch {
                search.results = runCatching { music.search(text) }.getOrDefault(emptyList())
                search.loading = false
                search.runs++
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(start = 40.dp, end = 40.dp, top = 32.dp)) {
        PageTitle("Search")
        BasicTextField(
            value = search.query,
            onValueChange = { search.query = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge.copy(color = Color.White),
            cursorBrush = SolidColor(Accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { run() }),
            modifier = Modifier.fillMaxWidth().focusRequester(field).testTag("search-field"),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Raised).padding(horizontal = 20.dp, vertical = 16.dp)) {
                    if (search.query.isEmpty()) Text("Songs, artists…", color = Quiet, style = MaterialTheme.typography.titleLarge)
                    inner()
                }
            },
        )
        Spacer(Modifier.height(24.dp))
        val tiles = search.results.map { TvTile.Song(it, search.results) }
        when {
            search.loading -> Note("Searching…")
            search.searched != null && tiles.isEmpty() -> Note("Nothing found for “${search.searched}”.")
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(176.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                contentPadding = PaddingValues(4.dp, 8.dp, 4.dp, 48.dp),
            ) {
                itemsIndexed(tiles, key = { _, tile -> tile.key }) { i, tile ->
                    TileCard(tile, { onTile(tile) }, modifier = if (i == 0) Modifier.focusRequester(firstResult) else Modifier)
                }
            }
        }
    }
    LaunchedEffect(Unit) { if (search.query.isEmpty()) runCatching { field.requestFocus() } }
    // The remote's focus onto what was found: hiding the keyboard leaves it nowhere, and the arrows with it.
    LaunchedEffect(search.runs) {
        if (search.runs != search.focusedRun && search.results.isNotEmpty()) {
            search.focusedRun = search.runs
            withFrameNanos { }
            runCatching { firstResult.requestFocus() }
        }
    }
}

@Composable
private fun QueuePage(music: TvMusic, played: () -> Unit) {
    val queue by music.queue.collectAsState()
    val lines = queueLines(queue)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 40.dp, end = 40.dp, top = 32.dp, bottom = 48.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { PageTitle("Queue") }
        if (lines.isEmpty()) item { Note("Nothing queued. Pick a song from Home, Search or your Library.") }
        var headed = false
        lines.forEachIndexed { n, line ->
            if (line.suggestion && !headed) {
                headed = true
                item(key = "autoplay") {
                    Text("Autoplay" + (queue.suggestionsFrom?.let { " · from $it" } ?: ""), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
                }
            }
            item(key = "${line.suggestion}:${line.index}:${line.track.queueKey}") {
                SongLine(
                    if (line.nowPlaying) "▶" else "${n + 1}",
                    line.track,
                    if (line.nowPlaying) "Playing now" else null,
                    line.nowPlaying,
                    {
                        if (line.suggestion) music.playSuggestion(line.index) else music.jumpToQueueItem(line.index)
                        played()
                    },
                    "queue:${line.suggestion}:${line.index}",
                )
            }
        }
    }
}

@Composable
private fun RadioPage(music: TvMusic, play: (Track) -> Unit) {
    val stations by produceState<List<Track>?>(null) { value = runCatching { music.stations() }.getOrDefault(emptyList()) }
    Column(Modifier.fillMaxSize().padding(start = 40.dp, end = 40.dp, top = 32.dp)) {
        PageTitle("Radio", "The most listened-to stations where you are")
        val list = stations
        when {
            list == null -> Note("Finding stations…")
            list.isEmpty() -> Note("No stations could be found. Is the TV online?")
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(176.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                contentPadding = PaddingValues(4.dp, 8.dp, 4.dp, 48.dp),
            ) {
                items(list, key = { it.queueKey }) { station -> TileCard(TvTile.Song(station, list), { play(station) }) }
            }
        }
    }
}

@Composable
private fun NowPlayingPage(music: TvMusic, go: (TvPage) -> Unit) {
    val playback by music.playback.collectAsState()
    val queue by music.queue.collectAsState()
    val track = playback.track
    val playButton = remember { FocusRequester() }
    Box(Modifier.fillMaxSize()) {
        if (track?.artworkUrl != null) {
            AsyncImage(model = track.artworkUrl, contentDescription = null, contentScale = ContentScale.Crop, alpha = .16f, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Night.copy(alpha = .4f), Night))))
        }
        if (track == null) {
            Column(Modifier.padding(40.dp)) {
                PageTitle("Now playing")
                Note("Nothing is playing.")
            }
            return@Box
        }
        Row(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 40.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(track.artworkUrl, Modifier.size(260.dp))
            Spacer(Modifier.width(36.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(track.artistLine, fontSize = 22.sp, color = Quiet, maxLines = 1, overflow = TextOverflow.Ellipsis)
                track.album?.title?.let { Text(it, fontSize = 17.sp, color = Quiet.copy(alpha = .8f), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Spacer(Modifier.height(30.dp))
                Progress(playback)
                if (playback.status == PlaybackStatus.ERROR) {
                    Text(playback.errorMessage ?: "This song couldn't be played.", fontSize = 15.sp, color = Color(0xFFFF8A80), modifier = Modifier.padding(top = 10.dp))
                }
                Spacer(Modifier.height(28.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundButton(Icons.Filled.Shuffle, "Shuffle", lit = queue.shuffleEnabled) { music.toggleShuffle() }
                    RoundButton(Icons.Filled.SkipPrevious, "Previous") { music.previous() }
                    RoundButton(
                        if (playback.status == PlaybackStatus.PLAYING) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        if (playback.status == PlaybackStatus.PLAYING) "Pause" else "Play",
                        big = true,
                        modifier = Modifier.focusRequester(playButton),
                    ) { music.togglePlayback() }
                    RoundButton(Icons.Filled.SkipNext, "Next") { music.next() }
                    RoundButton(
                        if (queue.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        when (queue.repeatMode) { RepeatMode.OFF -> "Repeat"; RepeatMode.ALL -> "Repeating all"; RepeatMode.ONE -> "Repeating this song" },
                        lit = queue.repeatMode != RepeatMode.OFF,
                    ) { music.cycleRepeat() }
                }
                Spacer(Modifier.height(24.dp))
                queueLines(queue).firstOrNull { !it.nowPlaying }?.let { next ->
                    Surface(
                        onClick = { go(TvPage.QUEUE) },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                        colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, contentColor = Quiet, focusedContainerColor = Color.White, focusedContentColor = Color.Black),
                    ) {
                        Text("Up next: ${next.track.title} · ${next.track.artistLine}", fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
            }
        }
    }
    LaunchedEffect(track != null) { if (track != null) runCatching { playButton.requestFocus() } }
}

@Composable
private fun Progress(playback: PlaybackState) {
    val duration = playback.durationMs.takeIf { it > 0 } ?: playback.track?.durationMs ?: 0
    val fraction = if (duration > 0) (playback.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f
    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = .18f))) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(Accent))
    }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Text(if (playback.status == PlaybackStatus.RESOLVING) "Loading…" else clock(playback.positionMs), fontSize = 14.sp, color = Quiet)
        Spacer(Modifier.weight(1f))
        if (duration > 0) Text(clock(duration), fontSize = 14.sp, color = Quiet)
    }
}

@Composable
private fun RoundButton(icon: ImageVector, label: String, big: Boolean = false, lit: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(if (big) 66.dp else 50.dp).testTag("button:$label"),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (big) Accent else Color.White.copy(alpha = .1f),
            contentColor = if (big) Color.Black else if (lit) Accent else Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.12f),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(if (big) 34.dp else 24.dp))
        }
    }
}

@Composable
private fun SettingsPage(openPhoneScreens: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(start = 40.dp, end = 40.dp, top = 32.dp)) {
        PageTitle("Settings")
        Text(
            "Signing in to your services, and Noctorium's settings, are on Noctorium's full screens. They work with the remote too.",
            fontSize = 18.sp, color = Quiet, modifier = Modifier.width(720.dp),
        )
        Spacer(Modifier.height(20.dp))
        Surface(
            onClick = openPhoneScreens,
            modifier = Modifier.testTag("open-phone-screens"),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
            colors = ClickableSurfaceDefaults.colors(containerColor = Accent, contentColor = Color.Black, focusedContainerColor = Color.White, focusedContentColor = Color.Black),
        ) {
            Text("Open sign-in and settings", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
        }
        Spacer(Modifier.height(36.dp))
        Text("From your phone", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Text(
            "Noctorium on a phone or computer on the same network finds this TV under Connect, and can play on it and drive it from there.",
            fontSize = 16.sp, color = Quiet, modifier = Modifier.width(720.dp).padding(top = 6.dp),
        )
    }
}
