package ro.ipnet.radiobigfmalban

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class RadioPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private val handler = Handler(Looper.getMainLooper())
    private var retryAttempt = 0
    private var retryRunnable: Runnable? = null

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
                override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
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
        mediaSession = MediaSession.Builder(this, player)
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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        retryRunnable?.let(handler::removeCallbacks)
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
