package cn.wangce.lumi.ui.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// 专注通知栏遥控入口：暂停/继续/完成均委托全局会话管理器
@AndroidEntryPoint
class FocusNotificationReceiver : BroadcastReceiver() {

    @Inject lateinit var manager: FocusSessionManager

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            FocusNotifier.ACTION_TOGGLE -> manager.toggle()
            FocusNotifier.ACTION_FINISH -> manager.finish()
        }
    }
}
