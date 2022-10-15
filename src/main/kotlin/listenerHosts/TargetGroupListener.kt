package org.hezistudio.listenerHosts

import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListenerHost
import net.mamoe.mirai.event.events.BotJoinGroupEvent
import net.mamoe.mirai.event.events.MemberJoinEvent
import net.mamoe.mirai.event.events.MemberLeaveEvent

object TargetGroupListener:ListenerHost {
    @EventHandler
    suspend fun onEvent(e:MemberJoinEvent.Invite){
        e.group.sendMessage("${e.invitor.nameCardOrNick}邀请了${e.member.nameCardOrNick}加入, 欢迎大佬！")
    }
    @EventHandler
    suspend fun onEvent(e:MemberJoinEvent.Active){
        e.group.sendMessage("${e.member.nameCardOrNick}加入了我们, 欢迎大佬！")
    }
    @EventHandler
    suspend fun onEvent(e:MemberLeaveEvent.Kick){
        if (e.operator==null) return
        e.group.sendMessage("${e.operator!!.nameCardOrNick}把${e.member.nameCardOrNick}飞了！好可怕啊")
    }
    @EventHandler
    suspend fun onEvent(e:MemberLeaveEvent.Quit){
        e.group.sendMessage("${e.member.nameCardOrNick}离开了我们，呜呜")
    }
    @EventHandler
    suspend fun onEvent(e:BotJoinGroupEvent){
        e.group.sendMessage("爷来辣")
    }
}