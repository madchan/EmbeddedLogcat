package com.madchan.comp.logcat

import android.text.TextUtils

/**
 * 命令行数据类
 * <p>
 * 每一个过滤选项都作为该类的属性之一，toString方法负责将该类转换为一个完整的命令行。
 */
data class Command(var level: String = " *:V") {    // 级别

    var pid: Int? = 0        // 进程ID
    var expr: String? = null    // 关键词

    override fun toString(): String {
        val builder = StringBuilder("logcat -d -v time $level")

        pid?.let {
            builder.append(" --pid=$pid")
        }

        if (!TextUtils.isEmpty(expr)) {
            builder.append(" -e $expr+")
        }

        return builder.toString()
    }
}