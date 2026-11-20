package com.swordfish.lemuroid.app.tv.shared

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment

object TVHelper {
    fun isSAFSupported(context: Context): Boolean {
        val packageManager = context.packageManager

        val isStandardHardware =
            listOf(
                !packageManager.hasSystemFeature("android.hardware.type.television"),
                !packageManager.hasSystemFeature("android.hardware.type.watch"),
                !packageManager.hasSystemFeature("android.hardware.type.automotive"),
            ).all { it }

        val isNotLegacyStorage =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !Environment.isExternalStorageLegacy()

        // 某些精简 ROM（如魔百盒）阉割了 DocumentsUI，即使硬件/存储条件满足，
        // 也没有任何组件能处理 ACTION_OPEN_DOCUMENT_TREE。此时应判定为不支持 SAF，
        // 回退到应用内置的目录选择器，否则点"目录"会无响应。
        val hasDocumentTreeHandler =
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).resolveActivity(packageManager) != null

        return hasDocumentTreeHandler && (isStandardHardware || isNotLegacyStorage)
    }

    fun isTV(context: Context): Boolean {
        val packageManager = context.packageManager
        return packageManager.hasSystemFeature("android.hardware.type.television")
    }
}
