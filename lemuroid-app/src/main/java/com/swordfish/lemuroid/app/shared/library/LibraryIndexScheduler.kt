package com.swordfish.lemuroid.app.shared.library

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import com.swordfish.lemuroid.app.utils.android.WorkManagerGuard
import com.swordfish.lemuroid.lib.library.LemuroidLibrary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

object LibraryIndexScheduler {
    val CORE_UPDATE_WORK_ID: String = CoreUpdateWork::class.java.simpleName
    val LIBRARY_INDEX_WORK_ID: String = LibraryIndexWork::class.java.simpleName

    // 由 LemuroidApplication 注入，用于在 WorkManager 不可用（device-protected
    // storage ROM）时直接执行库扫描，绕过 WorkManager。
    @Volatile
    var directLibrary: LemuroidLibrary? = null

    // 直接扫描时使用的作用域，避免 WorkManager 不可用时空转。
    private val directScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun scheduleLibrarySync(applicationContext: Context) {
        val workManager = WorkManagerGuard.getInstanceOrNull(applicationContext)
        if (workManager != null) {
            workManager.beginUniqueWork(
                LIBRARY_INDEX_WORK_ID,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                OneTimeWorkRequestBuilder<LibraryIndexWork>().build(),
            ).enqueue()
            return
        }

        // WorkManager 不可用：直接同步执行扫描（在 IO 线程）。
        val library = directLibrary
        if (library == null) {
            Timber.w("WorkManager unavailable and no direct library, skipping index")
            return
        }
        directScope.launch {
            try {
                library.indexLibrary()
            } catch (e: Throwable) {
                Timber.e(e, "Direct library indexing failed")
            }
        }
    }

    fun scheduleCoreUpdate(applicationContext: Context) {
        WorkManagerGuard.getInstanceOrNull(applicationContext)?.beginUniqueWork(
            CORE_UPDATE_WORK_ID,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<CoreUpdateWork>().build(),
        )?.enqueue()
    }

    fun cancelLibrarySync(applicationContext: Context) {
        WorkManagerGuard.getInstanceOrNull(applicationContext)
            ?.cancelUniqueWork(LIBRARY_INDEX_WORK_ID)
    }

    fun cancelCoreUpdate(applicationContext: Context) {
        WorkManagerGuard.getInstanceOrNull(applicationContext)
            ?.cancelUniqueWork(CORE_UPDATE_WORK_ID)
    }
}
