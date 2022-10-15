package org.hezistudio.listenerHosts

import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.content
import org.hezistudio.GroupmanagerHz
import org.hezistudio.dataBase.DBTools
import org.jetbrains.exposed.sql.transactions.transaction

object StatuesListener:ListenerHost {
    @EventHandler
    suspend fun onEvent(e:GroupMessageEvent){
        val msg = e.message.content
        if (msg == "角色卡"){
            val outMsg = userStatues(e.sender)
            if (outMsg != "") {
                e.group.sendMessage(outMsg)
            }
        }else if (msg == "查询积分" || msg == "积分查询"){
            val outMsg = userFraction(e.sender)
            if (outMsg != "") {
                e.group.sendMessage(outMsg)
            }
        }
    }

    private fun userStatues(user:Member):String{
        val dbc = GroupmanagerHz.getDBC(user.bot.id)
        val sb = StringBuilder()
        val dbMember = DBTools.getMember(dbc,user.id,user.group.id)?:return ""
        val gender = if (dbMember.gender) {"男"} else {"女"}
        val outString = """
            ${dbMember.nickName} 性别：$gender
            积分：${dbMember.fraction}
            攻\防\力：${dbMember.attack}\${dbMember.defence}\${dbMember.strength}
        """.trimIndent()
        sb.append(outString)
        return sb.toString()
    }

    private fun userFraction(user: Member):String{
        val dbc = GroupmanagerHz.getDBC(user.bot.id)
        val dbMember = DBTools.getMember(dbc,user.id,user.group.id)?:return ""
        return "您当前的积分为：${dbMember.fraction}"
    }

}