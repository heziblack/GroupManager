package org.hezistudio.listenerHosts

import net.mamoe.mirai.console.plugin.version
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.PlainText
import org.hezistudio.GroupmanagerHz

object MenuListener:SimpleListenerHost() {

    private val menuList:ArrayList<String> = arrayListOf(
        """
            ·击剑：
               我要击剑(随机的对手)：随机挑选一位对手进行击剑(每日最多十次)
               击剑对决[at对象]：指定一位对手进行击剑
               我要打十个：一口气将每日十次击剑机会用尽
            """.trimIndent(),
        """
            ·游戏王：
               查卡 [卡名]：查询卡牌信息
            """.trimIndent(),
        """
            ·签到：日常签到，获取积分
            """.trimIndent(),
        """
            ·apex查询：
                派派查询 [originID]：根据你的Origin/EA账号查询你的apex段位信息
            """.trimIndent(),
        """
            ·查询类：
               角色卡：查看自己的信息
               查询积分：查看自己的积分
               排行榜：显示积分前十名
               地下室榜：显示积分后十名
            """.trimIndent(),
        """
            ·每日新闻\番剧：
               每天早上自动播报，也可以通过“每日新闻”与“每日番剧”手动获取
            """.trimIndent(),
        """
            ·道具系统：
               道具商店：展示系统内可购买的道具
               使用 [道具名]：使用道具
               对 [At对象] 使用 [道具名]：对他人使用道具
               [道具名]：查看道具说明
               购买 [道具名]：购买道具
               出售 [道具名]：出售道具
               背包：查看背包物品
            （更多功能敬请期待 版本号:${GroupmanagerHz.version}）
            """.trimIndent()
    )

    @EventHandler
    suspend fun onEvent(e:GroupMessageEvent){

        val fmb = ForwardMessageBuilder(e.group)
        for (m in menuList){
            fmb.add(e.bot, PlainText(m))
        }
        e.group.sendMessage(fmb.build())
    }
}