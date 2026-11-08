package ro.ipnet.radiobigfmalban

import ro.ipnet.R

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors

class RadioWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateOne(context, manager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PLAY_PAUSE -> withController(context) { controller ->
                if (controller.isPlaying) controller.pause() else controller.play()
                refresh(context)
            }
            ACTION_STOP -> withController(context) { controller ->
                controller.stop()
                refresh(context)
            }
            ACTION_PREVIOUS -> withController(context) { controller ->
                if (controller.hasPreviousMediaItem()) controller.seekToPreviousMediaItem()
                refresh(context)
            }
            ACTION_NEXT -> withController(context) { controller ->
                if (controller.hasNextMediaItem()) controller.seekToNextMediaItem()
                refresh(context)
            }
        }
    }

    private fun withController(context: Context, action: (MediaController) -> Unit) {
        val token = SessionToken(context, ComponentName(context, RadioPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            runCatching {
                future.get().also { controller ->
                    action(controller)
                    controller.release()
                }
            }
        }, MoreExecutors.directExecutor())
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "ro.ipnet.radiobigfmalban.WIDGET_PLAY_PAUSE"
        const val ACTION_STOP = "ro.ipnet.radiobigfmalban.WIDGET_STOP"
        const val ACTION_PREVIOUS = "ro.ipnet.radiobigfmalban.WIDGET_PREVIOUS"
        const val ACTION_NEXT = "ro.ipnet.radiobigfmalban.WIDGET_NEXT"

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, RadioWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { updateOne(context, manager, it) }
        }

        private fun updateOne(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.radio_widget)
            val prefs = context.getSharedPreferences("radio_ipnet_features", Context.MODE_PRIVATE)
            val station = prefs.getString("last_station_name", "Radio Ip-NeT") ?: "Radio Ip-NeT"
            views.setTextViewText(R.id.widget_station, station)
            val openIntent = PendingIntent.getActivity(
                context, 10, Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openIntent)
            views.setOnClickPendingIntent(R.id.widget_previous, broadcast(context, ACTION_PREVIOUS, 13))
            views.setOnClickPendingIntent(R.id.widget_play, broadcast(context, ACTION_PLAY_PAUSE, 11))
            views.setOnClickPendingIntent(R.id.widget_next, broadcast(context, ACTION_NEXT, 14))
            views.setOnClickPendingIntent(R.id.widget_stop, broadcast(context, ACTION_STOP, 12))
            manager.updateAppWidget(id, views)
        }

        private fun broadcast(context: Context, action: String, requestCode: Int): PendingIntent =
            PendingIntent.getBroadcast(
                context, requestCode, Intent(context, RadioWidgetProvider::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
    }
}
