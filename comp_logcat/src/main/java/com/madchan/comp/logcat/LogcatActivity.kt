package com.madchan.comp.logcat

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AppCompatActivity

/**
 * 日志输出页面
 */
class LogcatActivity : AppCompatActivity() {

    private lateinit var process: Spinner
    private lateinit var level: Spinner
    private lateinit var search: EditText

    private lateinit var scroller: ScrollView
    private lateinit var content: TextView

    private var command = Command()
    private var processMap = HashMap<String, Int>()

    /** 支持自动滚动到底部 */
    private var supportFullScroll = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logcat)

        processMap = ProcessUtil.getProcessNames(this)

        process = findViewById(R.id.process)
        process.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, android.R.id.text1, ArrayList<String>(processMap.keys))
        process.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View,
                position: Int,
                l: Long
            ) {
                command.pid = processMap[(process.adapter.getItem(position) as String)]
                startOutput()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }

        level = findViewById(R.id.level)
        level.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View,
                i: Int,
                l: Long
            ) {
                when (i) {
                    0 -> command.level = "*:V"
                    1 -> command.level = "*:D"
                    2 -> command.level = "*:I"
                    3 -> command.level = "*:W"
                    4 -> command.level = "*:E"
                    else -> {
                    }
                }
                startOutput()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }

        search = findViewById(R.id.search)
        search.addTextChangedListener (object : TextWatcher{

            override fun afterTextChanged(s: Editable?) {
                command.expr = s.toString().trim()
                startOutput()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        })

        scroller = findViewById(R.id.scroller)
        scroller.setOnTouchListener(OnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> supportFullScroll = false //查看日志时停止自动滚动到底部
            }
            false
        })
        content = findViewById(R.id.content)

        findViewById<ImageView>(R.id.clear).setOnClickListener {
            content.text = ""
            LogcatExecutor.clear()
        }
        findViewById<ImageView>(R.id.scroll_to_end).setOnClickListener{
            scroller.fullScroll(ScrollView.FOCUS_DOWN)
            supportFullScroll = true
        }

    }

    override fun onResume() {
        super.onResume()
        startOutput()
    }

    private fun startOutput() {
        LogcatExecutor.startOutput(command, object : LogcatExecutor.Callback {
            override fun onLogOutput(log: String) {
                runOnUiThread {
                    content.text = log
                    if(supportFullScroll) scroller.fullScroll(ScrollView.FOCUS_DOWN)
                }
            }
        })
    }

    override fun onPause() {
        super.onPause()
        LogcatExecutor.stopOutput()
    }

}