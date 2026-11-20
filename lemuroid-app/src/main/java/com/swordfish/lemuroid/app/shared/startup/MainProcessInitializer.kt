package com.swordfish.lemuroid.app.shared.startup

import android.content.Context
import androidx.startup.Initializer
import com.swordfish.lemuroid.app.shared.library.LibraryIndexScheduler
import com.swordfish.lemuroid.app.shared.savesync.SaveSyncWork
import timber.log.Timber

class MainProcessInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        Timber.i("Requested initialization of main process tasks")
        SaveSyncWork.enqueueAutoWork(context, 0)
        LibraryIndexScheduler.scheduleCoreUpdate(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        // 注意：此处不能依赖 WorkManagerInitializer。
        // 在 device-protected storage 的 ROM（如魔百盒）上，WorkManager 初始化会抛
        // IllegalStateException("Cannot initialize WorkManager in direct boot mode")，
        // 导致 Application.onCreate 阶段崩溃。所有 WorkManager 访问已改为经过
        // WorkManagerGuard 的惰性安全调用。
        return listOf(DebugInitializer::class.java)
    }
}
