package ro.ipnet

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@OptIn(UnstableApi::class)
class RadioPlaybackService : MediaLibraryService() {
    private var mediaSession: MediaLibrarySession? = null
    private val handler = Handler(Looper.getMainLooper())
    private var retryAttempt = 0
    private var retryRunnable: Runnable? = null

    private data class Station(val id: String, val name: String, val subtitle: String, val url: String)
    private val stations = listOf(
        Station("radio_angel_hip-hop", "Radio Angel Hip-Hop", "HIP-HOP • LIVE", "https://radio.ip-net.ro/listen/radio_angel_hip-hop/radio.mp3"),
        Station("radioangeldance", "Radio Angel Dance", "DANCE • LIVE", "https://radio.ip-net.ro/listen/radioangeldance/radio.mp3"),
        Station("radio_angel__pop", "Radio Angel Pop", "POP • LIVE", "https://radio.ip-net.ro/listen/radio_angel__pop/radio.mp3"),
        Station("radio_tequila_club", "Radio Tequila Club", "CLUB • LIVE", "https://radio.ip-net.ro/listen/radio_tequila_club/radio.mp3")
    )

    private val libraryCallback = object : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(LibraryResult.ofItem(rootItem(), params))

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<com.google.common.collect.ImmutableList<MediaItem>>> {
            val children = if (parentId == ROOT_ID) {
                stations.map(::stationItem).drop(page * pageSize).take(pageSize)
            } else emptyList()
            return Futures.immediateFuture(LibraryResult.ofItemList(children, params))
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val item = if (mediaId == ROOT_ID) rootItem() else stations.firstOrNull { it.id == mediaId }?.let(::stationItem)
            return if (item == null) Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE, null))
            else Futures.immediateFuture(LibraryResult.ofItem(item, null))
        }
    }

    private fun rootItem() = MediaItem.Builder()
        .setMediaId(ROOT_ID)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle("Radio Ip-NeT")
                .setSubtitle("Posturi radio live")
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .build()
        )
        .build()

    private fun stationItem(station: Station) = MediaItem.Builder()
        .setMediaId(station.id)
        .setUri(station.url)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(station.name)
                .setSubtitle(station.subtitle)
                .setArtist("Radio Ip-NeT")
                .setArtworkUri(android.net.Uri.parse("android.resource://ro.ipnet/drawable/radio_ipnet_artwork"))
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .build()
        )
        .build()

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("radio_ipnet_features", Context.MODE_PRIVATE)
        val player = ExoPlayer.Builder(this).build().apply {
            volume = prefs.getFloat("player_volume", 1f).coerceIn(0f, 1f)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            setHandleAudioBecomingNoisy(true)
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) retryAttempt = 0
                    RadioWidgetProvider.refresh(this@RadioPlaybackService)
                }
                override fun onPlayerError(error: PlaybackException) {
                    scheduleReconnect(this@apply)
                }
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val index = stations.indexOfFirst { it.id == mediaItem?.mediaId }
                    if (index >= 0) {
                        prefs.edit().putInt("last_station_index", index)
                            .putString("last_station_name", stations[index].name).apply()
                    }
                    RadioWidgetProvider.refresh(this@RadioPlaybackService)
                }
            })
        }

        val openPlayerIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val sessionActivity = PendingIntent.getActivity(
            this, 1001, openPlayerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        mediaSession = MediaLibrarySession.Builder(this, player, libraryCallback)
            .setSessionActivity(sessionActivity)
            .build()
    }

    private fun scheduleReconnect(player: ExoPlayer) {
        retryRunnable?.let(handler::removeCallbacks)
        if (retryAttempt >= 6 || player.currentMediaItem == null) return
        val delays = longArrayOf(2000, 4000, 8000, 15000, 30000, 60000)
        val waitMs = delays[retryAttempt.coerceAtMost(delays.lastIndex)]
        retryAttempt++
        retryRunnable = Runnable {
            if (mediaSession?.player === player && player.currentMediaItem != null) {
                player.prepare()
                player.play()
            }
        }.also { handler.postDelayed(it, waitMs) }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

    override fun onDestroy() {
        retryRunnable?.let(handler::removeCallbacks)
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        private const val ROOT_ID = "radio_ipnet_root"
    }
}
