package cn.wangce.lumi.music;

import android.media.session.MediaSession;
import android.os.Bundle;

// Kotlin K2(2.0.21) 对 android-35 SDK 中带 @android.annotation.NonNull 参数的
// MediaSession.Callback.onCustomAction 误报 "overrides nothing"（调用可见、覆写不可解析），
// 故该方法下沉到本 Java 桥接类实现（javac 无此解析问题），其余回调仍由 Kotlin 子类覆写。
abstract class MediaSessionCallbackBridge extends MediaSession.Callback {

    interface CustomActionHandler {
        void onAction(String action, Bundle extras);
    }

    private final CustomActionHandler handler;

    MediaSessionCallbackBridge(CustomActionHandler handler) {
        this.handler = handler;
    }

    @Override
    public void onCustomAction(String action, Bundle extras) {
        handler.onAction(action, extras);
    }
}
