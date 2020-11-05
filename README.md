图挂了可以看这里：https://www.jianshu.com/p/e2eb509b04e7

# 前言

利用日志埋点，以排查由于逻辑出错而引发的Bug，是我们常用的排障手段。(什么？你还在通过Debug断点调试？)，但在生产环境下出于安全性考虑，往往Release包需要将日志输出到屏幕的功能关闭。

于是常会出现一种很尴尬的场景，你需要用电脑重新在手机上打一个Debug包覆盖才能重新看到日志输出，而如果恰巧事故发生在周末，你手上只有一台手机，一个APP，你该如何快速定位问题呢？

你是否会想，要是在手机上能有一个Logcat就好了，发生故障时立马就能快速定位问题，而这，正是本文想与你分享的。

偷偷先瞄一眼效果：
![lAHPBG1Q6B4q33nNAwzNAWg_360_780.gif](https://upload-images.jianshu.io/upload_images/5530180-1d70991e40ad946b.gif?imageMogr2/auto-orient/strip)

等不及要立即使用了？我已将该库上传到远程仓库了，可以通过以下方式引入：
在项目级build.gradle添加：
```
allprojects {
    repositories {
        ...
        maven{ url "https://dl.bintray.com/madchan/maven" }
    }
}
```
以及在模块级build.gradle添加：
```
implementation 'com.madchan.library:embeddedlogcat:1.0.0'
```
使用也很容易，以正常启动Activity的方式跳转即可：
```
startActivity(Intent(this, LogcatActivity::class.java))
```
同时示例源码也发布到GitHub了，如果对你有帮助，给点个Star^_^吧~
[https://github.com/madchan/EmbeddedLogcat](https://github.com/madchan/EmbeddedLogcat)

# 知识储备

#### Logcat窗口布局讲解

既然是要模仿Android Studio的Logcat功能，我们自然需要先分析Logcat窗口的功能布局，并评判哪一些功能是我们需要~~抄袭~~（参考）的，如图：
![clipboard.png](https://upload-images.jianshu.io/upload_images/5530180-d20692672594af0b.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

先从顶部栏开始分析，从左到右依次为：

**设备**：默认情况下，Logcat 仅显示在指定设备上运行的应用的日志消息。

**进程**：通常情况下，我们只关心自身应用进程的日志消息，但需考虑到应用可能存在多进程的情况。

**日志级别**：通常情况下，我们通过设置日志级别来表示对不同信息的关心程度。

具体的级别分布如下：
*   **Verbose**：显示所有日志消息（默认值）。
*   **Debug**：显示仅在开发期间有用的调试日志消息，以及此列表中较低的消息级别。
*   **Info**：显示常规使用情况的预期日志消息，以及此列表中较低的消息级别。
*   **Warn**：显示尚不是错误的潜在问题，以及此列表中较低的消息级别。
*   **Error**：显示已经引发错误的问题，以及此列表中较低的消息级别。
*   **Assert**：显示开发者预计绝不会发生的问题。

**搜索字段**：搜索包含特定字段的日志，支持正则表达式。

**过滤器**：过滤器菜单中，包含以下三个过滤选项：

*   **Show only selected application**：仅显示通过应用代码生成的消息（默认选项）。Logcat 使用正在运行的应用的 PID 来过滤日志消息。
*   **No Filters**：不应用过滤器。无论您选择哪个进程，logcat 都会显示设备中的所有日志消息。
*   **Edit Filter Configuration**：创建或修改自定义过滤器。例如，您可以创建一个过滤器，以同时查看两个应用中的日志消息。

再从左侧继续分析，从上到下。。。我们只挑几个常用的讲吧：

**清除日志**：通常是为了排除之前的日志消息的干扰。需加入。
**滚动到底部**：可以跳转到日志底部并查看最新的日志消息。需加入。

#### Logcat 命令行工具

可通过adb shell运行Logcat命令行，该命令行用于**转储系统消息日志**，包括**设备抛出错误时的堆栈轨迹**，以及**应用使用Log类写入的消息**。

Logcat包含了许多命令行选项，用以查看不同过滤条件下的日志输出，如需获取 logcat 在线帮助，可执行以下命令，此处不具体展开：
```
adb logcat --help
```
# 方案实现

聪明的同学可能已经猜到，Logcat命令行工具即是我们实现本主题的主要途径，而Logcat窗口布局控件的选择结果则是为命令行添加不同的过滤选项，下面我们来逐步实现。

首先，定义一个Command数据类，每一个过滤选项都作为该类的属性之一，toString方法负责将该类转换为一个完整的命令行。
```
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
```
#### -d选项

接着，介绍一个最基本的命令行，此命令行**将日志转储到屏幕并退出**  ：
```
adb logcat -d
```
我们在执行完该命令行后，逐行读取日志信息并输出到TextView：
```
// LogcatExecutor.kt
...
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
...
```
#### --pid=<pid>选项

此命令行**仅输出来自给定 PID 的日志**。由进程Spinner选中指定选项后，为Command类的pid属性赋值，并重新执行此命令行输出日志：
```
// LogcatActivity.kt
...
private lateinit var process: Spinner
...
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
...
```
#### *:S选项

此命令行用于**指示最低优先级，不低于指定优先级的标记的消息会写入日志**。与上面步骤相似，只不过赋值的是level属性。
```
// LogcatActivity.kt
...
private lateinit var level: Spinner
...
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
...
```
#### -e <expr>选项

此命令行**只输出日志消息与 <expr> 匹配的行**，其中 <expr> 是正则表达式。
```
// LogcatActivity.kt
...
private lateinit var search: EditText
...
search.addTextChangedListener (object : TextWatcher{

    override fun afterTextChanged(s: Editable?) {
        command.expr = s.toString().trim()
        startOutput()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

})
...
```
其他的如清除日志、滚动到底部、高亮ERROR级别日志是属于交互的优化，不在本文介绍的范围之内，感兴趣的可以阅读源码。

# 使用场景

可以参考我之前写的文章《Preference库：为你的应用快速搭建一个「开发者选项」》，为你的应用添加调试入口，并增加「进入日志调试页」的调试选项。

文章链接：[https://www.jianshu.com/p/6ae1794d8fca](https://www.jianshu.com/p/6ae1794d8fca)

# 总结

本文以Android Studio的Logcat功能为参考模板，使用Logcat 命令行工具搭配合适的交互控件，在应用内搭建了一个类似的功能，可帮助开发者根据日志信息快速定位问题，快跟我来一起使用吧！

# 参考

Logcat 命令行工具
[https://developer.android.google.cn/studio/command-line/logcat](https://developer.android.google.cn/studio/command-line/logcat)

使用 Logcat 写入和查看日志
[https://developer.android.google.cn/studio/debug/am-logcat](https://developer.android.google.cn/studio/debug/am-logcat)
