package com.madchan.embeddedlogcat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.madchan.comp.logcat.LogcatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.textView).setOnClickListener{
            startActivity(Intent(this, LogcatActivity::class.java))
            startService(Intent(this, RemoteService::class.java))
        }

        var i = 0
        Thread{
            while (true) {
                Log.d("MainActivity", "onCreate: current is ${i++}")
                Thread.sleep(1000)
            }
        }.start()
    }
}