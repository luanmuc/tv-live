package com.doubao.tv.core.data.utils

import android.content.Context
import java.io.File

/**
 * 内置直播源释放器
 *
 * APK 里带着 assets 目录 sources 子目录下的 m3u（随仓库一起维护），首次启动时复制到应用私有目录，
 * 再配合 [Constants.IPTV_SOURCE_LIST] 注册成本地订阅源，做到「装完就能看、不依赖网络」。
 *
 * 幂等：文件已存在且非空就跳过；内置源内容更新时把 [SEEDED_VERSION] +1 即可覆盖。
 */
object BuiltinSources {
    private const val ASSET_DIR = "sources"
    private const val PREF_NAME = "builtin_sources"
    private const val KEY_VERSION = "seeded_version"
    private const val SEEDED_VERSION = 4

    fun install(context: Context) {
        val targetDir = File(Globals.fileDir, ASSET_DIR)
        if (!targetDir.exists()) targetDir.mkdirs()

        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val alreadySeeded = pref.getInt(KEY_VERSION, 0) >= SEEDED_VERSION

        context.assets.list(ASSET_DIR)
            ?.filter { it.endsWith(".m3u", ignoreCase = true) }
            ?.forEach { name ->
                val target = File(targetDir, name)
                if (alreadySeeded && target.exists() && target.length() > 0L) return@forEach
                runCatching {
                    context.assets.open("$ASSET_DIR/$name").use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }

        pref.edit().putInt(KEY_VERSION, SEEDED_VERSION).apply()
    }
}
