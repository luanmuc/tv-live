package com.doubao.tv.core.data

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.doubao.tv.core.data.utils.Globals
import com.doubao.tv.core.data.utils.SP
import java.io.File

object AppData {
    fun init(context: Context) {
        Globals.cacheDir = context.cacheDir
        Globals.fileDir = context.filesDir
        Globals.nativeLibraryDir = File(context.applicationInfo.nativeLibraryDir)
        Globals.resources = context.resources
        Globals.deviceName = runCatching {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
                Settings.Secure.getString(context.contentResolver, "bluetooth_name")!!
            } else {
                Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)!!
            }
        }
            .recover {
                val manufacturer = Build.MANUFACTURER!!
                val model = Build.MODEL!!
                "$manufacturer ${model.removePrefix(manufacturer)}"
            }
            .getOrDefault("未知设备")

        SP.init(context)
    }
}