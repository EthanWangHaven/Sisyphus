package cn.wangce.lumi

import android.app.Application
import androidx.core.app.NotificationManagerCompat
import cn.wangce.lumi.ui.focus.FocusNotifier
import dagger.hilt.android.HiltAndroidApp

// 应用入口：Hilt 依赖注入根
@HiltAndroidApp
class LumiApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // 进程重启后专注会话态已随内存丢失，清掉可能残留的通知卡片（计时不可恢复，卡片无意义）
        NotificationManagerCompat.from(this).cancel(FocusNotifier.NOTIF_ID)
    }
}
