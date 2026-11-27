package com.doubao.tv

import android.app.Application
import android.content.Intent
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.doubao.tv.core.data.AppData
import com.doubao.tv.core.data.utils.BuiltinSources
import com.doubao.tv.core.data.utils.Globals
import kotlin.system.exitProcess

class MyTVApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()

        initDeviceId()
        crashHandle()
        AppData.init(applicationContext)
        BuiltinSources.install(this)
        UnsafeTrustManager.enableUnsafeTrustManager()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader(this).newBuilder()
            .logger(DebugLogger())
            .components {
                add(SvgDecoder.Factory())
            }
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(this)
                    // .maxSizePercent(0.25)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    // .maxSizeBytes(1024 * 1024 * 100)
                    .build()
            }
            .build()
    }

    private fun initDeviceId() {
        val androidId = android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
        Globals.deviceId = androidId ?: "Unknown"
    }

    private fun crashHandle() {
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            throwable.printStackTrace()

            val intent = Intent(this, CrashHandlerActivity::class.java).apply {
                putExtra("error_message", throwable.message)
                putExtra("error_stacktrace", throwable.stackTraceToString())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)

            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(1)
        }
    }
}
