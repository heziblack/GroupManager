package org.hezistudio.listenerHosts

import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.content
import org.hezistudio.GroupmanagerHz
import org.hezistudio.dataBase.DBTools
import org.hezistudio.dataBase.Member
import org.hezistudio.itemSys.ItemEntity
import org.hezistudio.itemSys.buildInItems.RenameCard
import org.jetbrains.exposed.sql.Database

object SpecialItemUseHost:ListenerHost {
    @EventHandler
    suspend fun onEvent(e:GroupMessageEvent){
        val item = filterForSpecialItemCmd(e.message)?:return
        if (item.name == "改名卡"){
            val newName = e.message.content.replace("我要改名","").trim()
            val db = GroupmanagerHz.getDBC(e.bot.id)
            val mem = DBTools.getMember(db,e.sender.id,e.group.id)?:return
            val itemDB = DBTools.getItem(db,item.name)?:return
            if(DBTools.memberHasItem(db,mem,itemDB)){
                (item as RenameCard).active(db,e.sender,newName)
                e.group.sendMessage(item.useStr.replace("【使用者】", newName))
                val mi = DBTools.getMemberItem(db,mem,itemDB)!!
                DBTools.deleteMemberItem(db,mi)
            }else{
                e.group.sendMessage("你没有${item.name}, 不能改名字哦~")
            }
        }
    }

    private val specialItemCmdList:ArrayList<String> = arrayListOf(
        """我要改名 ?\S+"""
    )

    private fun filterForSpecialItemCmd(msg: MessageChain):ItemEntity?{
        if(Regex(specialItemCmdList[0]).matches(msg.content)){
            return RenameCard
        }else{
            return null
        }
        return null
    }

}