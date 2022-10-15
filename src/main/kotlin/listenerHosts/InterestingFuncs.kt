package org.hezistudio.listenerHosts

import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.content

object InterestingFuncs:SimpleListenerHost() {
    @EventHandler
    suspend fun onEvent(e:GroupMessageEvent){
        val msgContent = e.message.content
        when(msgContent){
            "我要草十个"->{
                if (e.group.botPermission>e.sender.permission) {
                    e.group.sendMessage("？ 艹十个是吧？")
                    e.sender.mute(600)
                }
            }
        }
    }
}