package com.swordfish.lemuroid.app.utils.android

import android.content.Context
import android.os.Build
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import timber.log.Timber

/**
 * 安全访问 WorkManager 的守卫。
 *
 * 背景：某些定制 ROM（如江苏移动魔百盒 CM311-1a）会把所有第三方应用的
 * dataDir 强制放到 device-protected storage（/data/user_de/0）。此时
 * [Context.isDeviceProtectedStorage] 恒为 true，而 androidx.work 2.9.0 的
 * WorkManagerImpl 构造函数只要检测到该状态就会无条件抛出
 * IllegalStateException("Cannot initialize WorkManager in direct boot mode")。
 * 由于这些设备 ro.crypto.state=unencrypted，不存在"解锁后 storage 切换"的
 * 时刻，因此 WorkManager 永远无法初始化。
 *
 * 所有对 WorkManager 的访问都应经过本类；当 WorkManager 不可用时，返回
 * 安全的空实现，避免进程启动或运行期崩溃。
 */
object WorkManagerGuard {

    /**
     * 判断当前上下文是否处于 device-protected storage 状态（即 WorkManager 无法初始化）。
     */
    fun isUnavailable(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            context.isDeviceProtectedStorage
    }

    /**
     * 返回可用的 [WorkManager]，不可用时返回 null。
     * 调用方必须对 null 做空处理。
     */
    fun getInstanceOrNull(context: Context): WorkManager? {
        val appContext = context.applicationContext
        if (isUnavailable(appContext)) {
            Timber.w("WorkManager unavailable: device-protected storage detected, skipping")
            return null
        }
        return try {
            WorkManager.getInstance(appContext)
        } catch (t: Throwable) {
            Timber.w(t, "WorkManager initialization failed, skipping")
            null
        }
    }

    /**
     * 不可用时的空 [Flow]，用于替代 getWorkInfosForUniqueWorkFlow 等 Flow 接口。
     */
    fun emptyWorkInfoFlow(): Flow<List<WorkInfo>> = flowOf(emptyList())
}
