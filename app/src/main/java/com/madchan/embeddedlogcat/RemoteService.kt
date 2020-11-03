package com.madchan.embeddedlogcat

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class RemoteService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        var i = 0
        Thread{
            while (true) {
                Log.d("RemoteService", "onBind: current is ${i++}")
                Thread.sleep(5000)
            }
        }.start()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

}