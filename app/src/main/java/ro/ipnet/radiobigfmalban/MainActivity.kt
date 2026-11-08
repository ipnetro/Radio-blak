package ro.ipnet.radiobigfmalban

import ro.ipnet.BuildConfig
import android.content.ComponentName
import android.net.Uri
import android.content.Intent
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.net.Uri as AndroidUri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private data class Station(val name: String, val subtitle: String, val url: String, val apiId: String)
private val defaultStations = listOf(
    Station("Radio Angel Hip-Hop", "HIP-HOP • LIVE", "https://radio.ip-net.ro/listen/radio_angel_hip-hop/radio.mp3", "radio_angel_hip-hop"),
    Station("Radio Angel Dance", "DANCE • LIVE", "https://radio.ip-net.ro/listen/radioangeldance/radio.mp3", "radioangeldance"),
    Station("Radio Angel Pop", "POP • LIVE", "https://radio.ip-net.ro/listen/radio_angel__pop/radio.mp3", "radio_angel__pop"),
    Station("Radio Tequila Club", "CLUB • LIVE", "https://radio.ip-net.ro/listen/radio_tequila_club/radio.mp3", "radio_tequila_club")
)
private val Bg = Color(0xFF08090C)
private val Panel = Color(0xFF171820)
private val Red = Color(0xFFFF1744)

class MainActivity : ComponentActivity() {
    private var controller by mutableStateOf<MediaController?>(null)
    private var isActivityVisible by mutableStateOf(false)

    override fun onStart() {
        super.onStart()
        isActivityVisible = true
        val token = SessionToken(this, ComponentName(this, RadioPlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        future.addListener({ controller = future.get() }, MoreExecutors.directExecutor())
    }

    override fun onStop() {
        isActivityVisible = false
        controller?.release()
        controller = null
        super.onStop()
    }

    private val updatePrefs by lazy { getSharedPreferences("radio_ipnet_updates", MODE_PRIVATE) }

    private val apkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            val expectedId = updatePrefs.getLong("pending_download_id", -1L)
            if (id == expectedId) finishDownloadAndInstall(id)
        }
    }

    @Suppress("DEPRECATION")
    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(apkReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(apkReceiver, filter)
        resumePendingUpdate()
    }

    override fun onPause() {
        try { unregisterReceiver(apkReceiver) } catch (_: Exception) { }
        super.onPause()
    }

    fun downloadAndInstallApk(url: String) {
        if (!url.endsWith(".apk", ignoreCase = true)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            updatePrefs.edit().putString("pending_apk_url", url).apply()
            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, AndroidUri.parse("package:$packageName")))
            return
        }
        enqueueApkDownload(url)
    }

    private fun enqueueApkDownload(url: String) {
        val request = DownloadManager.Request(AndroidUri.parse(url))
            .setTitle("Radio Ip-NeT")
            .setDescription("Se descarcă actualizarea aplicației")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "Radio-Ip-NeT-update.apk")
        val id = (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        updatePrefs.edit().putLong("pending_download_id", id).apply()
    }

    private fun resumePendingUpdate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) return
        val pendingUrl = updatePrefs.getString("pending_apk_url", null)
        if (!pendingUrl.isNullOrBlank()) {
            updatePrefs.edit().remove("pending_apk_url").apply()
            enqueueApkDownload(pendingUrl)
            return
        }
        val id = updatePrefs.getLong("pending_download_id", -1L)
        if (id <= 0L) return
        val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(id)
        manager.query(query).use { cursor ->
            if (cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                if (statusIndex >= 0 && cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                    finishDownloadAndInstall(id)
                }
            }
        }
    }

    private fun finishDownloadAndInstall(id: Long) {
        val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val uri = manager.getUriForDownloadedFile(id) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, AndroidUri.parse("package:$packageName")))
            return
        }
        updatePrefs.edit().remove("pending_download_id").apply()
        val install = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(install)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Red, background = Bg, surface = Panel,
                onPrimary = Color.White, onBackground = Color.White, onSurface = Color.White
            )) {
                RadioScreen(controller, isActivityVisible)
            }
        }
    }
}

@Composable
private fun RadioScreen(controller: MediaController?, isActivityVisible: Boolean) {
    val appContext = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { appContext.getSharedPreferences("radio_ipnet_features", Context.MODE_PRIVATE) }
    var selected by remember { mutableIntStateOf(prefs.getInt("last_station_index", 1).coerceIn(0, defaultStations.lastIndex)) }
    var playing by remember { mutableStateOf(false) }
    var playerVolume by remember { mutableFloatStateOf(prefs.getFloat("player_volume", 1f).coerceIn(0f, 1f)) }
    var muted by remember { mutableStateOf(playerVolume <= 0f) }
    var status by remember { mutableStateOf("Pregătit pentru redare") }
    var stations by remember { mutableStateOf(defaultStations) }
    var favoriteNames by remember { mutableStateOf(prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet()) }
    var historyNames by remember { mutableStateOf(prefs.getString("history", "")!!.split("\n").filter { it.isNotBlank() }) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var nowPlaying by remember { mutableStateOf("Așteptăm informațiile melodiei…") }
    var nowPlayingArtworkUrl by remember { mutableStateOf("") }
    var nowPlayingArtwork by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    val accent = Red
    var apkDownloadUrl by remember { mutableStateOf("https://github.com/ipnetro/Radio.Red-Black/releases/latest") }
    val installedVersion = BuildConfig.VERSION_NAME
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateAvailable by remember { mutableStateOf(false) }
    var updateMenuLabel by remember { mutableStateOf("Verifică actualizări") }
    var menuExpanded by remember { mutableStateOf(false) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var showSongRequests by remember { mutableStateOf(false) }
    var showHipHopHistory by remember { mutableStateOf(false) }
    var songRequests by remember { mutableStateOf<List<SongRequest>>(emptyList()) }
    var hipHopHistory by remember { mutableStateOf<List<String>>(emptyList()) }
    var songRequestsLoading by remember { mutableStateOf(false) }
    var hipHopHistoryLoading by remember { mutableStateOf(false) }
    var songRequestMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Observe MediaSession controls so the app UI follows notification/lock-screen actions.
    DisposableEffect(controller) {
        val activeController = controller
        if (activeController == null) {
            playing = false
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    playing = isPlaying
                    if (isPlaying) status = "Radio redă în fundal"
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
                        playing = false
                    }
                }
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val nextIndex = stations.indexOfFirst { it.apiId == mediaItem?.mediaId }
                    if (nextIndex >= 0) {
                        selected = nextIndex
                        prefs.edit().putInt("last_station_index", nextIndex)
                            .putString("last_station_name", stations[nextIndex].name).apply()
                        RadioWidgetProvider.refresh(appContext)
                    }
                }
            }
            activeController.addListener(listener)
            activeController.volume = playerVolume
            playing = activeController.isPlaying
            onDispose { activeController.removeListener(listener) }
        }
    }

    fun playStation(index: Int) {
        selected = index.coerceIn(0, stations.lastIndex)
        prefs.edit()
            .putInt("last_station_index", selected)
            .putString("last_station_name", stations[selected].name)
            .apply()
        RadioWidgetProvider.refresh(appContext)
        val c = controller ?: run { status = "Playerul se conectează..."; return }
        status = "Se conectează la ${stations[index].name}..."
        // Afișăm imediat postul selectat până sosește metadatele melodiei noi.
        nowPlaying = "Se încarcă melodia…"
        nowPlayingArtworkUrl = ""
        nowPlayingArtwork = null
        scope.launch {
            try {
                val playlist = stations.mapIndexed { stationIndex, station ->
                    val streamUrl = if (stationIndex == index) resolvePlayableUrl(station.url)
                        else runCatching { resolvePlayableUrl(station.url) }.getOrDefault(station.url)
                    val metadata = androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(station.name)
                        .setArtist("Radio Ip-NeT")
                        .setAlbumTitle("Radio Ip-NeT • LIVE")
                        .setArtworkUri(Uri.parse("android.resource://ro.ipnet/drawable/radio_ipnet_artwork"))
                        .build()
                    MediaItem.Builder()
                        .setMediaId(station.apiId)
                        .setUri(streamUrl)
                        .setMediaMetadata(metadata)
                        .build()
                }
                c.setMediaItems(playlist, index, 0L)
                c.prepare()
                c.play()
                playing = true
                status = "Se redă: ${stations[index].name}"
                nowPlaying = stations[index].name
                historyNames = (listOf(stations[index].name) + historyNames.filterNot { it == stations[index].name }).take(20)
                prefs.edit().putString("history", historyNames.joinToString("\n")).apply()
            } catch (e: Exception) {
                playing = false
                status = "Eroare la conectare: ${e.message ?: "playlist invalid"}"
            }
        }
    }

    LaunchedEffect(controller) {
        val activeController = controller ?: return@LaunchedEffect
        delay(600)
        if (prefs.contains("last_station_index") &&
            activeController.currentMediaItem == null && !activeController.isPlaying) {
            playStation(selected)
        }
    }

    suspend fun checkForUpdate(showUpToDate: Boolean = true) {
        checkingUpdate = true
        updateMenuLabel = "Se verifică actualizările..."
        try {
            val result = withContext(Dispatchers.IO) {
                val connection = (URL("https://update.ip-net.ro/version.json").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                    instanceFollowRedirects = true
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Cache-Control", "no-cache")
                    setRequestProperty("User-Agent", "Radio-Ip-NeT-Android/${BuildConfig.VERSION_NAME}")
                }
                try {
                    val code = connection.responseCode
                    if (code !in 200..299) throw IllegalStateException("Update server HTTP $code")
                    val json = JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
                    val latest = json.optString("latestVersion", "").trim()
                    val remoteCode = json.optInt("versionCode", 0)
                    val apk = json.optString("apkUrl", "").trim()
                    if (latest.isBlank() || remoteCode <= 0) throw IllegalStateException("version.json invalid")
                    Triple(latest, apk, remoteCode)
                } finally {
                    connection.disconnect()
                }
            }
            val latest = result.first
            val apkUrl = result.second
            val remoteVersionCode = result.third
            apkDownloadUrl = apkUrl
            val newer = remoteVersionCode > BuildConfig.VERSION_CODE
            updateAvailable = newer && apkUrl.endsWith(".apk", ignoreCase = true)
            when {
                latest.isBlank() -> updateMenuLabel = "Nu s-a găsit o versiune publicată"
                newer && apkUrl.endsWith(".apk", ignoreCase = true) -> {
                    updateMenuLabel = "Versiunea $latest — se descarcă actualizarea..."
                    (appContext as? MainActivity)?.downloadAndInstallApk(apkUrl)
                }
                newer -> updateMenuLabel = "Versiunea $latest există, dar linkul APK lipsește sau este invalid"
                showUpToDate -> updateMenuLabel = "Aplicația este la zi — nu trebuie actualizată"
            }
        } catch (e: Exception) {
            updateAvailable = false
            updateMenuLabel = "Nu s-a putut verifica. Încearcă din nou."
        } finally { checkingUpdate = false }
    }

    if (showHistoryDialog) {
        AlertDialog(onDismissRequest = { showHistoryDialog = false }, title = { Text("Istoric ascultare") },
            text = { if (historyNames.isEmpty()) Text("Nu există încă posturi în istoric.") else Column {
                historyNames.forEach { name -> Text("♫  $name", modifier = Modifier.fillMaxWidth().clickable {
                    stations.indexOfFirst { it.name == name }.takeIf { it >= 0 }?.let { playStation(it) }
                    showHistoryDialog = false
                }.padding(vertical = 7.dp)) }
            } }, confirmButton = { TextButton(onClick = { historyNames = emptyList(); prefs.edit().remove("history").apply() }) { Text("Șterge istoricul") } }, dismissButton = { TextButton(onClick = { showHistoryDialog = false }) { Text("Închide") } })
    }
    if (showSongRequests) {
        AlertDialog(
            onDismissRequest = { showSongRequests = false },
            title = { Text("Song Request • Hip-Hop") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    Text("Alege o melodie din lista disponibilă pentru Radio Angel Hip-Hop.",
                        color = Color(0xFFB9AAB1), fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    if (songRequestsLoading) CircularProgressIndicator(color = Red)
                    if (!songRequestsLoading && songRequests.isEmpty()) Text(
                        songRequestMessage.ifBlank { "Nu sunt melodii disponibile pentru cereri momentan." },
                        color = Color(0xFFE8DDE1), fontSize = 12.sp
                    )
                    songRequests.forEach { song ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            color = Color(0xFF211A22), shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(song.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(song.artist, color = Color(0xFFB9AAB1), fontSize = 10.sp)
                                }
                                TextButton(onClick = {
                                    scope.launch {
                                        songRequestMessage = submitHipHopRequest(stations.first().apiId, song.id)
                                    }
                                }) { Text("CERE", color = Red, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                    if (songRequestMessage.isNotBlank() && songRequests.isNotEmpty()) {
                        Text(songRequestMessage, color = Color(0xFFFF8299), fontSize = 11.sp)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSongRequests = false }) { Text("Închide") } },
            dismissButton = { TextButton(onClick = {
                scope.launch {
                    songRequestsLoading = true
                    songRequests = loadHipHopRequests(stations.first().apiId)
                    songRequestsLoading = false
                }
            }) { Text("Reîncarcă") } }
        )
    }
    if (showHipHopHistory) {
        AlertDialog(
            onDismissRequest = { showHipHopHistory = false },
            title = { Text("Song History • Hip-Hop") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    if (hipHopHistoryLoading) CircularProgressIndicator(color = Red)
                    if (!hipHopHistoryLoading && hipHopHistory.isEmpty()) Text(
                        "Istoricul nu este disponibil momentan.", color = Color(0xFFB9AAB1), fontSize = 12.sp
                    )
                    hipHopHistory.forEachIndexed { index, item ->
                        Text((index + 1).toString() + ".  " + item, color = Color.White, fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 7.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showHipHopHistory = false }) { Text("Închide") } },
            dismissButton = { TextButton(onClick = {
                scope.launch {
                    hipHopHistoryLoading = true
                    hipHopHistory = loadHipHopHistory(stations.first().apiId)
                    hipHopHistoryLoading = false
                }
            }) { Text("Reîncarcă") } }
        )
    }

    if (showVersionDialog) {
        AlertDialog(
            onDismissRequest = { showVersionDialog = false },
            title = { Text("Radio Ip-NeT") },
            text = { Text("Versiune instalată: $installedVersion") },
            confirmButton = {
                TextButton(onClick = { showVersionDialog = false }) { Text("Închide") }
            }
        )
    }


    LaunchedEffect(Unit) {
        // Fetch the live station list without allowing browser/hosting cache to return an old file.
        val configResult = withContext(Dispatchers.IO) {
            runCatching {
                val configUrl = "https://update.ip-net.ro/config.json?t=${System.currentTimeMillis()}"
                val connection = (URL(configUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 10000
                    requestMethod = "GET"
                    instanceFollowRedirects = true
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0")
                    setRequestProperty("Pragma", "no-cache")
                    setRequestProperty("User-Agent", "Radio-Ip-NeT-Android/${BuildConfig.VERSION_NAME}")
                }
                try {
                    val httpCode = connection.responseCode
                    if (httpCode !in 200..299) throw IllegalStateException("HTTP $httpCode")
                    val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val json = JSONObject(body)
                    val array = json.optJSONArray("stations") ?: throw IllegalStateException("lista stations lipsește")
                    buildList {
                        for (i in 0 until array.length()) {
                            val item = array.optJSONObject(i) ?: continue
                            val name = item.optString("name").trim()
                            val subtitle = item.optString("subtitle").trim()
                            val url = item.optString("url").trim()
                            val apiId = item.optString("apiId").trim()
                            if (name.isNotBlank() && url.startsWith("https://") && apiId.isNotBlank()) {
                                add(Station(name, subtitle, url, apiId))
                            }
                        }
                    }.also { if (it.isEmpty()) throw IllegalStateException("lista de posturi este goală") }
                } finally {
                    connection.disconnect()
                }
            }
        }
        configResult.onSuccess { remoteStations ->
            // Apply Compose state on the main thread after the network request completes.
            stations = remoteStations
            selected = prefs.getInt("last_station_index", selected).coerceIn(0, remoteStations.lastIndex)
            status = "Configurație live încărcată: ${remoteStations.size} posturi"
        }.onFailure { error ->
            status = "Config live: ${error.message ?: "eroare de conexiune"}"
        }
        checkForUpdate(showUpToDate = true)
    }

    LaunchedEffect(controller, selected, playing, isActivityVisible) {
        if (!isActivityVisible) return@LaunchedEffect
        // Poll while the app is visible; keep background playback free of UI metadata polling.
        // Do not wait for ExoPlayer to report isPlaying: the new station's metadata
        // can be fetched while the audio stream is still connecting.
        var lastSongKey = ""
        val stationIndex = selected
        val station = stations[stationIndex]
        while (selected == stationIndex) {
            val c = controller
            if (playing || c?.isPlaying == true) {
                val info = withContext(Dispatchers.IO) {
                    kotlinx.coroutines.withTimeoutOrNull(2200) {
                        fetchAzuraCastNowPlaying(station.apiId, station.name)
                    }
                }
                // Discard late replies from a previous station.
                if (selected == stationIndex && info != null && info.title.isNotBlank()) {
                    val display = if (info.artist.isNotBlank()) "${info.artist} — ${info.title}" else info.title
                    nowPlaying = display
                    if (nowPlayingArtworkUrl != info.artworkUrl) {
                        nowPlayingArtworkUrl = info.artworkUrl
                    }
                    val songKey = "${info.artist}|${info.title}|${info.artworkUrl}"
                    if (songKey != lastSongKey) {
                        lastSongKey = songKey

                        // Media3 uses this metadata for the system media notification
                        // and Android lock-screen controls. Keep the live stream playing.
                        val currentItem = c?.currentMediaItem
                        val currentIndex = c?.currentMediaItemIndex ?: -1
                        if (c != null && currentItem != null && currentIndex >= 0) {
                            val artwork = info.artworkUrl.takeIf { it.isNotBlank() }
                                ?.let { Uri.parse(it) }
                                ?: Uri.parse("android.resource://ro.ipnet/drawable/radio_ipnet_artwork")
                            val notificationMetadata = MediaMetadata.Builder()
                                .setTitle(info.title)
                                .setArtist(info.artist.ifBlank { station.name })
                                .setAlbumTitle("${station.name} • Radio Ip-NeT")
                                .setArtworkUri(artwork)
                                .build()
                            c.replaceMediaItem(
                                currentIndex,
                                currentItem.buildUpon()
                                    .setMediaMetadata(notificationMetadata)
                                    .build()
                            )
                        }
                    }
                }
            } else {
                // Even before playback starts, fetch the selected station once immediately.
                val info = withContext(Dispatchers.IO) {
                    kotlinx.coroutines.withTimeoutOrNull(2200) {
                        fetchAzuraCastNowPlaying(station.apiId, station.name)
                    }
                }
                if (selected == stationIndex && info != null && info.title.isNotBlank()) {
                    nowPlaying = if (info.artist.isNotBlank()) "${info.artist} — ${info.title}" else info.title
                    nowPlayingArtworkUrl = info.artworkUrl
                }
            }
            delay(5000)
        }
    }

    LaunchedEffect(nowPlayingArtworkUrl) {
        nowPlayingArtwork = if (nowPlayingArtworkUrl.isBlank()) null else withContext(Dispatchers.IO) {
            runCatching {
                val connection = (URL(nowPlayingArtworkUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 7000
                    readTimeout = 7000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Radio-Ip-NeT-Android/1.1.42")
                }
                try {
                    if (connection.responseCode !in 200..299) return@runCatching null
                    connection.inputStream.use { stream ->
                        val bytes = stream.readBytes()
                        if (bytes.size > 5 * 1024 * 1024) null
                        else BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    }
                } finally { connection.disconnect() }
            }.getOrNull()
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color(0xFF310D19), Color(0xFF111219), Color(0xFF08090C))
            )
        )
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header premium
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = { menuExpanded = true },
                        color = Color(0xFF21141A),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5A2635))
                    ) {
                        Text("☰", color = Color.White, fontSize = 21.sp,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column {
                        Text("IP•NET", color = Color.White, fontSize = 17.sp,
                            fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Text("RADIO PLAYER", color = Color(0xFFB7AAB0), fontSize = 9.sp,
                            letterSpacing = 1.8.sp)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(if (stations[selected].name in favoriteNames) "Elimină de la favorite" else "Adaugă la favorite") },
                            onClick = {
                                menuExpanded = false
                                val name = stations[selected].name
                                favoriteNames = if (name in favoriteNames) favoriteNames - name else favoriteNames + name
                                prefs.edit().putStringSet("favorites", favoriteNames).apply()
                            }
                        )
                        DropdownMenuItem(text = { Text("Istoric ascultare") }, onClick = { menuExpanded = false; showHistoryDialog = true })
                        DropdownMenuItem(text = { Text("Versiune") }, onClick = { menuExpanded = false; showVersionDialog = true })
                        DropdownMenuItem(text = { Text(updateMenuLabel) }, onClick = {
                            menuExpanded = false
                            scope.launch { checkForUpdate(showUpToDate = true) }
                        })
                        if (updateAvailable && apkDownloadUrl.endsWith(".apk", true)) {
                            DropdownMenuItem(text = { Text("Descarcă APK") }, onClick = {
                                menuExpanded = false
                                (appContext as? MainActivity)?.downloadAndInstallApk(apkDownloadUrl)
                            })
                        }
                    }
                }
                Surface(
                    color = Color(0xFF35121D),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF793044))
                ) {
                    Row(
                        Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(7.dp).background(Color(0xFFFF315B), CircleShape))
                        Spacer(Modifier.width(7.dp))
                        Text("LIVE", color = Color(0xFFFFA2B5), fontSize = 10.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            // Main player card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF17151C),
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A2630))
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ACUM SE REDĂ", color = Color(0xFFFF5575), fontSize = 10.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text("● ON AIR", color = Color(0xFFFF9BAD), fontSize = 9.sp,
                            letterSpacing = 1.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(116.dp).background(
                                Brush.linearGradient(listOf(Color(0xFF7B203B), Color(0xFF24131D), Color(0xFF111117))),
                                RoundedCornerShape(22.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (nowPlayingArtwork != null) {
                                Image(
                                    bitmap = nowPlayingArtwork!!,
                                    contentDescription = "Coperta melodiei curente",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("♫", color = Color(0xFFFF5274), fontSize = 43.sp)
                                    Text("IP•NET", color = Color.White, fontSize = 9.sp,
                                        fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                                }
                            }
                        }
                        Spacer(Modifier.width(15.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stations[selected].name, color = Color.White,
                                fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                            Spacer(Modifier.height(5.dp))
                            Text(stations[selected].subtitle, color = Color(0xFFFF8299),
                                fontSize = 9.sp, letterSpacing = 1.2.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("MELODIA CURENTĂ", color = Color(0xFF9E9299),
                                fontSize = 8.sp, letterSpacing = 1.2.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(nowPlaying, color = Color(0xFFE8DDE1), fontSize = 12.sp,
                                fontWeight = FontWeight.Medium, maxLines = 3)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Box(Modifier.fillMaxWidth().height(3.dp)
                        .background(Color(0xFF3A2931), RoundedCornerShape(10.dp))) {
                        Box(Modifier.fillMaxWidth(.38f).fillMaxHeight()
                            .background(Brush.horizontalGradient(listOf(Color(0xFFFF1744), Color(0xFFFF8299))),
                                RoundedCornerShape(10.dp)))
                    }
                    Spacer(Modifier.height(17.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { controller?.stop(); playing = false; status = "Redare oprită" },
                            color = Color(0xFF29232B), shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49313B))
                        ) {
                            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                Text("■", color = Color(0xFFE8DDE1), fontSize = 16.sp)
                            }
                        }
                        Spacer(Modifier.width(24.dp))
                        Surface(
                            onClick = {
                                val c = controller ?: return@Surface
                                if (c.isPlaying) { c.pause(); playing = false; status = "Pauză" }
                                else playStation(selected)
                            },
                            color = Red, shape = CircleShape
                        ) {
                            Box(Modifier.size(66.dp), contentAlignment = Alignment.Center) {
                                Text(if (playing) "Ⅱ" else "▶", color = Color.White, fontSize = 27.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.width(24.dp))
                        Surface(
                            onClick = {
                                muted = !muted
                                playerVolume = if (muted) 0f else 1f
                                controller?.volume = playerVolume
                                prefs.edit().putFloat("player_volume", playerVolume).apply()
                            },
                            color = Color(0xFF29232B), shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49313B))
                        ) {
                            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                Text(if (muted) "×" else "♫", color = Color(0xFFE8DDE1), fontSize = 19.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("VOL", color = Color(0xFFB9AAB1), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Slider(
                            value = playerVolume,
                            onValueChange = { value ->
                                playerVolume = value
                                muted = value <= 0f
                                controller?.volume = value
                                prefs.edit().putFloat("player_volume", value).apply()
                            },
                            valueRange = 0f..1f,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${(playerVolume * 100).toInt()}%", color = Color.White, fontSize = 10.sp)
                    }
                    Spacer(Modifier.height(13.dp))
                    Text(
                        if (controller == null) "Se pregătește playerul..." else status,
                        modifier = Modifier.fillMaxWidth(),
                        color = if (status.startsWith("Eroare")) Color(0xFFFF8A80) else Color(0xFFB9AAB1),
                        fontSize = 10.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 2
                    )
                }
            }

            Spacer(Modifier.height(25.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("POSTURI RADIO", color = Color.White, fontSize = 17.sp,
                        fontWeight = FontWeight.Black, letterSpacing = .5.sp)
                    Text("Alege atmosfera preferată", color = Color(0xFF9D9299), fontSize = 11.sp)
                }
                Surface(color = Color(0xFF29151E), shape = CircleShape) {
                    Text("${stations.size} POSTURI", color = Color(0xFFFF8299),
                        fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
                }
            }
            Spacer(Modifier.height(12.dp))

            stations.forEachIndexed { index, station ->
                val isSelected = selected == index
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                        .clickable { playStation(index) },
                    color = if (isSelected) Color(0xFF351522) else Color(0xFF17161C),
                    shape = RoundedCornerShape(19.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        if (isSelected) 1.5.dp else 1.dp,
                        if (isSelected) Red.copy(alpha = .9f) else Color(0xFF302831)
                    )
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(48.dp).background(
                                Brush.linearGradient(listOf(Color(0xFF692039), Color(0xFF28151F))),
                                RoundedCornerShape(15.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(listOf("HH", "DN", "PP", "TC").getOrElse(index) { "FM" },
                                color = Color(0xFFFF7892), fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(station.name, color = Color.White,
                                fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                            Spacer(Modifier.height(4.dp))
                            Text(station.subtitle, color = Color(0xFFAA9DA4),
                                fontSize = 9.sp, letterSpacing = .8.sp)
                        }
                        if (isSelected && playing) {
                            Text("● LIVE", color = Color(0xFFFF6684), fontSize = 9.sp,
                                fontWeight = FontWeight.Bold)
                        } else {
                            Text("▶", color = if (isSelected) Red else Color(0xFF9B8992),
                                fontSize = 15.sp)
                        }
                    }
                }
            }

            if (selected == 0) {
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showSongRequests = true
                            songRequestMessage = ""
                            scope.launch {
                                songRequestsLoading = true
                                songRequests = loadHipHopRequests(stations.first().apiId)
                                songRequestsLoading = false
                                if (songRequests.isEmpty()) songRequestMessage = "Lista indisponibilă sau cererile sunt dezactivate."
                            }
                        },
                        border = androidx.compose.foundation.BorderStroke(1.dp, Red),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8299))
                    ) { Text("♫  SONG REQUEST", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showHipHopHistory = true
                            scope.launch {
                                hipHopHistoryLoading = true
                                hipHopHistory = loadHipHopHistory(stations.first().apiId)
                                hipHopHistoryLoading = false
                            }
                        },
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF794054)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE8DDE1))
                    ) { Text("♫  SONG HISTORY", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }
            }

            if (favoriteNames.isNotEmpty()) {
                Spacer(Modifier.height(17.dp))
                Text("♥  FAVORITELE TALE", modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFFF8299), fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                favoriteNames.forEach { favorite ->
                    Text("♥   $favorite", modifier = Modifier.fillMaxWidth().clickable {
                        stations.indexOfFirst { it.name == favorite }.takeIf { it >= 0 }?.let { playStation(it) }
                    }.padding(vertical = 8.dp), color = Color.White, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF30252C)))
            Spacer(Modifier.height(13.dp))
            Text("RADIO IP•NET", color = Color(0xFFE8DDE1), fontSize = 11.sp,
                fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Spacer(Modifier.height(4.dp))
            Text("FEEL THE MUSIC  •  VERSION $installedVersion",
                color = Color(0xFF81757C), fontSize = 9.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(12.dp))
        }
    }
}

private data class SongRequest(val id: String, val title: String, val artist: String)

private suspend fun loadHipHopRequests(stationId: String): List<SongRequest> = withContext(Dispatchers.IO) {
    val endpoint = "https://radio.ip-net.ro/api/station/" + Uri.encode(stationId) + "/requests"
    val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
        connectTimeout = 8000
        readTimeout = 8000
        requestMethod = "GET"
        setRequestProperty("Accept", "application/json")
    }
    try {
        if (connection.responseCode !in 200..299) return@withContext emptyList()
        val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val array = if (body.trimStart().startsWith("[")) org.json.JSONArray(body) else {
            val root = JSONObject(body)
            root.optJSONArray("requests") ?: root.optJSONArray("songs") ?: org.json.JSONArray()
        }
        buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val song = item.optJSONObject("song") ?: item
                val id = item.optString("request_id").ifBlank { item.optString("id").ifBlank { song.optString("id") } }
                val title = song.optString("title").trim()
                val artist = song.optString("artist").trim()
                if (id.isNotBlank() && title.isNotBlank()) add(SongRequest(id, title, artist))
            }
        }
    } finally { connection.disconnect() }
}

private suspend fun submitHipHopRequest(stationId: String, requestId: String): String = withContext(Dispatchers.IO) {
    val endpoint = "https://radio.ip-net.ro/api/station/" + Uri.encode(stationId) + "/request/" + Uri.encode(requestId)
    val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
        connectTimeout = 8000
        readTimeout = 8000
        requestMethod = "POST"
        doOutput = true
        setRequestProperty("Accept", "application/json")
    }
    try {
        val code = connection.responseCode
        if (code in 200..299) "Cererea a fost trimisă către Radio Angel Hip-Hop!"
        else "Cererea nu a fost acceptată (HTTP " + code + ")."
    } catch (_: Exception) {
        "Nu am putut trimite cererea. Încearcă din nou."
    } finally { connection.disconnect() }
}

private suspend fun loadHipHopHistory(stationId: String): List<String> = withContext(Dispatchers.IO) {
    val endpoint = "https://radio.ip-net.ro/api/nowplaying/" + Uri.encode(stationId)
    val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
        connectTimeout = 8000
        readTimeout = 8000
        requestMethod = "GET"
        setRequestProperty("Accept", "application/json")
    }
    try {
        if (connection.responseCode !in 200..299) return@withContext emptyList()
        val json = JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
        val history = json.optJSONArray("song_history") ?: return@withContext emptyList()
        buildList {
            for (i in 0 until history.length()) {
                val entry = history.optJSONObject(i) ?: continue
                val song = entry.optJSONObject("song") ?: continue
                val title = song.optString("title").trim()
                val artist = song.optString("artist").trim()
                val line = if (artist.isBlank()) title else artist + " — " + title
                if (line.isNotBlank()) add(line)
            }
        }
    } finally { connection.disconnect() }
}

private data class NowPlayingInfo(val title: String, val artist: String, val artworkUrl: String)

/** Public AzuraCast Now Playing API. Supports station shortcode or numeric station ID. */
private suspend fun fetchAzuraCastNowPlaying(stationId: String, stationName: String): NowPlayingInfo? =
    withContext(Dispatchers.IO) {
        val endpoint = "https://radio.ip-net.ro/api/nowplaying/${Uri.encode(stationId)}"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            connectTimeout = 3500
            readTimeout = 3500
            requestMethod = "GET"
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("User-Agent", "Radio-Ip-NeT-Android/1.1.41")
        }
        try {
            if (connection.responseCode !in 200..299) return@withContext null
            val json = JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
            val nowPlaying = json.optJSONObject("now_playing") ?: json
            val song = nowPlaying.optJSONObject("song")
                ?: json.optJSONObject("current_song")
                ?: return@withContext null
            var title = song.optString("title", "").trim()
            var artist = song.optString("artist", "").trim()
            if (title.isBlank()) {
                val text = song.optString("text", "").trim()
                val split = text.split(" - ", limit = 2)
                if (split.size == 2) {
                    artist = artist.ifBlank { split[0].trim() }
                    title = split[1].trim()
                } else title = text
            }
            val art = song.optString("art", "").trim()
                .ifBlank { song.optString("artwork", "").trim() }
            val safeArt = art.takeIf { it.startsWith("https://", true) || it.startsWith("http://", true) }.orEmpty()
            if (title.isBlank() || title.equals(stationName, ignoreCase = true)) null
            else NowPlayingInfo(title, artist, safeArt)
        } finally {
            connection.disconnect()
        }
    }

private fun compareVersions(left: String, right: String): Int {
    fun parts(value: String) = value.split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
    val a = parts(left)
    val b = parts(right)
    for (i in 0 until maxOf(a.size, b.size)) {
        val diff = (a.getOrElse(i) { 0 }).compareTo(b.getOrElse(i) { 0 })
        if (diff != 0) return diff
    }
    return 0
}

/** Resolve an Internet radio playlist (.pls) to its actual audio stream URL. */
private suspend fun resolvePlayableUrl(playlistUrl: String): String = withContext(Dispatchers.IO) {
    if (!playlistUrl.substringBefore('?').endsWith(".pls", ignoreCase = true)) return@withContext playlistUrl
    val connection = (URL(playlistUrl).openConnection() as HttpURLConnection).apply {
        connectTimeout = 12000
        readTimeout = 12000
        instanceFollowRedirects = true
        setRequestProperty("User-Agent", "Mozilla/5.0 (Android Radio Player)")
        setRequestProperty("Accept", "audio/x-scpls, audio/*, */*")
    }
    try {
        val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val stream = body.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("File", ignoreCase = true) && it.contains('=') }
            ?.substringAfter('=')?.trim()?.trim('\"')
            ?.takeIf { it.startsWith("http://", true) || it.startsWith("https://", true) }
        stream ?: throw IllegalStateException("Nu am găsit adresa File1 în playlistul PLS")
    } finally {
        connection.disconnect()
    }
}
