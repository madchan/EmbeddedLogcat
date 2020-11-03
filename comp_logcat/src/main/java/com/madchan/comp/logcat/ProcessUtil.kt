package com.madchan.comp.logcat

import android.app.ActivityManager
import android.content.Context

class ProcessUtil {

    companion object {

        fun getProcessNames(context: Context): HashMap<String, Int> {
            var map = HashMap<String, Int>()
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (processInfo in activityManager.runningAppProcesses){
                if(processInfo.processName.contains(context.packageName)) {
                    map[processInfo.processName] = processInfo.pid
                }
            }
            return map
        }

    }
}