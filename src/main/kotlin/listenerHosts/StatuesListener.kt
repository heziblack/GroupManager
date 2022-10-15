package org.hezistudio.listenerHosts

import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import org.hezistudio.GroupmanagerHz
import org.hezistudio.dataBase.DBTools
import org.jetbrains.exposed.sql.transactions.transaction

class StatuesListener:ListenerHost {
    @EventHandler
    suspend fun onEvent(e:GroupMessageEvent){

    }

    fun userStatues(user:Member):String{
        val dbc = GroupmanagerHz.getDBC(user.bot.id)
        val sb = StringBuilder()
        val dbMember = DBTools.getMember(dbc,user.id,user.group.id)?:return ""

        TODO()
    }

}