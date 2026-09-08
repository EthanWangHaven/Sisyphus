package cn.wangce.lumi.ui.focus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import cn.wangce.lumi.MainActivity
import cn.wangce.lumi.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// 专注计时通知：黑白 RemoteViews 卡片 + Chronometer 系统自动走秒（无需每秒刷新）+ 暂停/完成遥控
@Singleton
class FocusNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun show(state: FocusSessionState, elapsedMillis: Long) {
        ensureChannel()

        val openApp = PendingIntent.getActivity(
            context, 100,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val togglePi = PendingIntent.getBroadcast(
            context, 101,
            Intent(context, FocusNotificationReceiver::class.java).setAction(ACTION_TOGGLE),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val finishPi = PendingIntent.getBroadcast(
            context, 102,
            Intent(context, FocusNotificationReceiver::class.java).setAction(ACTION_FINISH),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val view = RemoteViews(context.packageName, R.layout.notification_focus_card)
        // Chronometer 基准 = elapsedRealtime - 已计时长：运行时系统自动走秒，暂停时停走冻结
        view.setLong(R.id.chrono, "setBase", SystemClock.elapsedRealtime() - elapsedMillis)
        view.setBoolean(R.id.chrono, "setStarted", !state.paused)
        view.setOnClickPendingIntent(R.id.card_content, openApp)
        view.setOnClickPendingIntent(R.id.btn_toggle, togglePi)
        view.setOnClickPendingIntent(R.id.btn_finish, finishPi)
        view.setImageViewResource(
            R.id.btn_toggle,
            if (state.paused) R.drawable.ic_noti_play else R.drawable.ic_noti_pause,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_timer)
            .setCustomContentView(view)
            .setCustomBigContentView(view)
            .setContentIntent(openApp)
            .setOngoing(true) // 计时中防误划丢会话
            .setSilent(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
        } catch (_: SecurityException) {
            // 未授予通知权限时静默跳过，页面内计时不受影响
        }
    }

    fun cancel() {
        NotificationManagerCompat.from(context).cancel(NOTIF_ID)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.focus_notif_channel),
                        NotificationManager.IMPORTANCE_LOW,
                    ),
                )
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "focus_session"
        const val NOTIF_ID = 1003
        const val ACTION_TOGGLE = "cn.wangce.lumi.focus.TOGGLE"
        const val ACTION_FINISH = "cn.wangce.lumi.focus.FINISH"
    }
}
