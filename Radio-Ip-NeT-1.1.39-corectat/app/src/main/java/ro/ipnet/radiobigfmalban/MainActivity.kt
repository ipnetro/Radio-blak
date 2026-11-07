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

private data class Station(val name: String, val subtitle: String, val url: String)
private val stations = listOf(
    Station("Radio Angel Hip-Hop", "HIP-HOP • LIVE", "https://radio.ip-net.ro/public/radio_angel_hip-hop/playlist.pls"),
    Station("Radio Big FM Alban", "ALBANIAN HITS • LIVE", "https://radio.ip-net.ro/public/radio_big_fm_alban/playlist.pls"),
    Station("Radio Tequila Club", "CLUB • LIVE", "https://radio.ip-net.ro/public/radio_tequila_club/playlist.pls")
)
private val Bg = Color(0xFF08090C)
private val Panel = Color(0xFF171820)
private val Red = Color(0xFFFF1744)

class MainActivity : ComponentActivity() {
    private var controller by mutableStateOf<MediaController?>(null)

    override fun onStart() {
        super.onStart()
        val token = SessionToken(this, ComponentName(this, RadioPlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        future.addListener({ controller = future.get() }, MoreExecutors.directExecutor())
    }

    override fun onStop() {
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
                RadioScreen(controller)
            }
        }
    }
}

@Composable
private fun RadioScreen(controller: MediaController?) {
    var selected by remember { mutableIntStateOf(1) }
    var playing by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Pregătit pentru redare") }
    var apkDownloadUrl by remember { mutableStateOf("https://github.com/ipnetro/Radio.Red-Black/releases/latest") }
    val installedVersion = BuildConfig.VERSION_NAME
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateAvailable by remember { mutableStateOf(false) }
    var updateMenuLabel by remember { mutableStateOf("Verifică actualizări") }
    var menuExpanded by remember { mutableStateOf(false) }
    var showVersionDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val appContext = androidx.compose.ui.platform.LocalContext.current

    fun playStation(index: Int) {
        selected = index
        val c = controller ?: run { status = "Playerul se conectează..."; return }
        status = "Se conectează la ${stations[index].name}..."
        scope.launch {
            try {
                val streamUrl = resolvePlayableUrl(stations[index].url)
                val metadata = androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(stations[index].name)
                    .setArtist("Radio Ip-NeT")
                    .setAlbumTitle("Radio Ip-NeT • LIVE")
                    .setArtworkUri(Uri.parse("android.resource://ro.ipnet/drawable/radio_ipnet_artwork"))
                    .build()
                val mediaItem = MediaItem.Builder()
                    .setUri(streamUrl)
                    .setMediaMetadata(metadata)
                    .build()
                c.setMediaItem(mediaItem)
                c.prepare()
                c.play()
                playing = true
                status = "Se redă: ${stations[index].name}"
            } catch (e: Exception) {
                playing = false
                status = "Eroare la conectare: ${e.message ?: "playlist invalid"}"
            }
        }
    }

    suspend fun checkForUpdate(showUpToDate: Boolean = true) {
        checkingUpdate = true
        updateMenuLabel = "Se verifică actualizările..."
        try {
            val result = withContext(Dispatchers.IO) {
                val conn = (URL("https://api.github.com/repos/ipnetro/Radio.Red-Black/releases/latest").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "Radio-Ip-NeT-Android")
                }
                try {
                    val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                    val tag = json.optString("tag_name", "").removePrefix("v")
                    val assets = json.optJSONArray("assets")
                    var apkUrl: String? = null
                    if (assets != null) for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        if (asset.optString("name").endsWith(".apk", true)) {
                            apkUrl = asset.optString("browser_download_url")
                            break
                        }
                    }
                    Triple(tag, apkUrl, "")
                } finally { conn.disconnect() }
            }
            val latest = result.first
            apkDownloadUrl = result.second.orEmpty()
            val newer = compareVersions(latest, installedVersion) > 0
            updateAvailable = newer && result.second != null
            updateMenuLabel = when {
                latest.isBlank() -> "Nu s-a găsit o versiune"
                newer && result.second != null -> "Actualizare disponibilă: $latest"
                newer -> "Versiunea $latest este publicată, dar APK-ul lipsește din Release"
                else -> "Aplicația este la zi — nu trebuie actualizată"
            }
            when {
                latest.isBlank() -> updateMenuLabel = "Nu s-a găsit o versiune publicată"
                newer && result.second != null -> {
                    updateMenuLabel = "Versiunea $latest — se descarcă actualizarea..."
                    (appContext as? MainActivity)?.downloadAndInstallApk(result.second)
                }
                newer -> updateMenuLabel = "Versiunea $latest există, dar APK-ul lipsește din Release"
                showUpToDate -> updateMenuLabel = "Aplicația este la zi — nu trebuie actualizată"
            }
        } catch (e: Exception) {
            updateAvailable = false
            updateMenuLabel = "Verificarea actualizărilor a eșuat"
            updateMenuLabel = "Nu s-a putut verifica. Încearcă din nou."
        } finally { checkingUpdate = false }
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
        // La fiecare pornire confirmăm și când aplicația este deja la zi.
        checkForUpdate(showUpToDate = true)
    }

    LaunchedEffect(controller) {
        while (true) {
            playing = controller?.isPlaying == true
            delay(500)
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF260B13), Color(0xFF101117), Bg))
        )
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        Text(
                            "☰",
                            color = Red,
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { menuExpanded = true }.padding(end = 10.dp)
                        )
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Versiune") },
                                onClick = {
                                    menuExpanded = false
                                    showVersionDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(updateMenuLabel) },
                                onClick = {
                                    scope.launch { checkForUpdate(showUpToDate = true) }
                                }
                            )
                            if (updateAvailable && apkDownloadUrl.endsWith(".apk", true)) {
                                DropdownMenuItem(
                                    text = { Text("Descarcă APK") },
                                    onClick = {
                                        menuExpanded = false
                                        (appContext as? MainActivity)?.downloadAndInstallApk(apkDownloadUrl)
                                    }
                                )
                            }
                        }
                    }
                    Text("IP•NET RADIO", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                }
                Surface(color = Color(0xFF35121D), shape = CircleShape) {
                    Text("● LIVE PLAYER", color = Color(0xFFFF6B86),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        fontSize = 10.sp, letterSpacing = 1.sp)
                }
            }

            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.size(142.dp).background(
                    Brush.radialGradient(listOf(Color(0xFF33343D), Color(0xFF111218), Bg)),
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    Modifier.size(122.dp), shape = CircleShape, color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(2.dp, Red.copy(alpha = .55f))
                ) {}
                Text("Radio\nIp-NeT", color = Red, fontSize = 19.sp, lineHeight = 21.sp, fontWeight = FontWeight.Black, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            Spacer(Modifier.height(12.dp))
            Text("Radio Ip-NeT", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("FEEL THE MUSIC  •  LIVE", color = Color.LightGray,
                fontSize = 10.sp, letterSpacing = 2.sp)

            Spacer(Modifier.height(16.dp))
            Surface(
                color = Panel, shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30313A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("ACUM ASCULTI", color = Red, fontSize = 9.sp, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(stations[selected].name, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(stations[selected].subtitle, color = Color.LightGray, fontSize = 10.sp)
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                OutlinedButton(
                    onClick = { controller?.stop(); playing = false },
                    shape = CircleShape, contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(50.dp)
                ) { Text("■") }
                Button(
                    onClick = {
                        val c = controller ?: return@Button
                        if (c.isPlaying) { c.pause(); playing = false }
                        else playStation(selected)
                    },
                    shape = CircleShape, contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(68.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) { Text(if (playing) "Ⅱ" else "▶", fontSize = 25.sp) }
                OutlinedButton(
                    onClick = { muted = !muted; controller?.volume = if (muted) 0f else 1f },
                    shape = CircleShape, contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(50.dp)
                ) { Text(if (muted) "×" else "♫", fontSize = 18.sp) }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                if (controller == null) "Pregătire player..." else status,
                color = if (status.startsWith("Eroare")) Color(0xFFFF8A80) else Color(0xFFFF718A),
                fontSize = 10.sp, maxLines = 2
            )

            Spacer(Modifier.height(16.dp))
            Text("ALEGE POSTUL", modifier = Modifier.fillMaxWidth(),
                color = Color.LightGray, fontSize = 11.sp, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(8.dp))

            stations.forEachIndexed { index, station ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .clickable { playStation(index) },
                    color = if (selected == index) Color(0xFF35131F) else Panel,
                    shape = RoundedCornerShape(17.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, if (selected == index) Red else Color(0xFF30313A)
                    )
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(44.dp).background(
                                Brush.linearGradient(listOf(Color(0xFF54182A), Color(0xFF211018))),
                                RoundedCornerShape(13.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(listOf("AH", "BF", "TC")[index], color = Red,
                                fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(station.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(station.subtitle, color = Color.LightGray, fontSize = 9.sp,
                                letterSpacing = .6.sp)
                        }
                        Text(
                            if (selected == index && playing) "● LIVE" else "▶",
                            color = Red, fontSize = 11.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text("RADIO IP-NET • VERSION $installedVersion", color = Color.Gray,
                fontSize = 9.sp, letterSpacing = 1.sp)
        }
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
