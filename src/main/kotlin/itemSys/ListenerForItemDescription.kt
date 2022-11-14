package org.hezistudio.itemSys

import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.content
import org.hezistudio.itemSys.BuildInItemList

object ListenerForItemDescription:ListenerHost {
    @EventHandler
    suspend fun onEvent(e:GroupMessageEvent){
        val dpt = BuildInItemList.get(e.message.content)?.description?:return
        e.group.sendMessage(dpt)
    }
}