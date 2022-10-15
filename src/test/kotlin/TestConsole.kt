import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.console.MiraiConsole
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.enable
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.load
import net.mamoe.mirai.console.terminal.MiraiConsoleTerminalLoader
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.utils.BotConfiguration
import org.hezistudio.GroupmanagerHz
import java.io.File
import java.util.*

@OptIn(ConsoleExperimentalApi::class)
suspend fun main(){
    MiraiConsoleTerminalLoader.startAsDaemon()
    val pluginInstance = GroupmanagerHz
    pluginInstance.load() // 主动加载插件, Console 会调用 该插件.onLoad方法
    pluginInstance.enable() // 主动启用插件, Console 会调用 该插件.onEnable方法

    // 从沙盒配置文件中读取机器人信息
    val properties = Properties().apply {
        File("account.properties").inputStream().use {
            load(it)
        }
    }
    // 登录一个测试环境的 Bot
    val bot = MiraiConsole.addBot(properties.getProperty("id").toLong(), properties.getProperty("password"))
    bot.configuration.protocol =  BotConfiguration.MiraiProtocol.IPAD
    bot.alsoLogin()
    MiraiConsole.job.join()
}