package com.doubao.tv

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.doubao.tv.ui.utils.Configs
import com.doubao.tv.utlis.HttpServer

class HttpServerService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Configs.debugDeveloperMode) {
            stopSelf()
            return START_NOT_STICKY
        }
        HttpServer.start(applicationContext)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        HttpServer.stop()
        super.onDestroy()
    }
}