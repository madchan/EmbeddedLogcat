package com.madchan.comp.logcat

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Logcat执行器
 */
object LogcatExecutor {

    private var handler: LogcatHandler
    private var callback: Callback? = null

    private var command: Command? = null

    init {
        val thread = HandlerThread("Logcat")
        thread.start()
        handler = LogcatHandler(thread.looper)
    }

    fun startOutput(command: Command, callback: Callback) {
        this.command = command
        this.callback = callback
        handler.startOutputThread()
    }

    fun stopOutput() {
        handler.stopOutputThread()
    }

    fun clear() {
        handler.startClearThread()
    }

    class LogcatHandler(looper: Looper) : Handler(looper) {

        companion object {
            const val MSG_START = 1
            const val MSG_CLEAR = 2
            const val INTERVAL_TIME = 1000L
        }

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_START -> {
                    execOutputCommand(command)
                    sendEmptyMessageDelayed(MSG_START, INTERVAL_TIME)
                }
                MSG_CLEAR -> {
                    execClearCommand()
                }
                else -> throw IllegalArgumentException("Unknown what = $msg.what")
            }
        }

        fun startOutputThread() {
            sendEmptyMessage(MSG_START)
        }

        fun stopOutputThread() {
            removeMessages(MSG_START)
        }

        fun startClearThread() {
            sendEmptyMessage(MSG_CLEAR)
        }

        /**
         * 执行日志输出命令行
         */
        private fun execOutputCommand(command: Command?) {
            try {
                val command = command?.toString() ?: "logcat -d"
                val process = Runtime.getRuntime().exec(command)
                val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))

                val log = StringBuilder()
                var line: String? = bufferedReader.readLine()
                while (line != null) {
                    log.append(line)
                    log.append("\n\n")

                    line = bufferedReader.readLine()
                }

                callback?.onLogOutput(log.toString())

            } catch (e: IOException) {
                Log.e("LogcatHandler", "执行Logcat命令行失败：" + e.message)
            }

        }

        /**
         * 执行日志清除命令行
         */
        private fun execClearCommand() {
            Runtime.getRuntime().exec("logcat -c")
        }
    }

    interface Callback {
        fun onLogOutput(log: String)
    }
}