package org.hezistudio.itemSys.buildInItems

import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.message.data.MessageChainBuilder
import org.hezistudio.dataBase.DBTools
import org.hezistudio.itemSys.ItemEntity
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

object StrengthDrug:ItemEntity(
    "强壮药水","使用之后体力会提高那么一丢丢~","【使用者】使用了【道具名】，感到自己力的量奔涌而出",
    inPrice = 100, canBuy = true
) {
    override suspend fun active(db: Database?, user: Member, vararg exArg: String): Boolean {
        db?:return false
        val group = user.group
        val msgBuilder = MessageChainBuilder()
        val msgOrigin = this.useStr
        val itemName = this.name
        val dbR =  transaction {
            val dbUser = DBTools.getMember(db,user.id,group.id)?:return@transaction false
            dbUser.strength += 1.0
            val outMsg = msgOrigin.replace("【使用者】", dbUser.nickName).replace("【道具名】",itemName)
            msgBuilder.add(outMsg)
            true
        }
        if (dbR){
            group.sendMessage(msgBuilder.build())
        }else{
            group.sendMessage("哎呀，正在你要喝药的时候，有谁敲响了房门，你顺手又把药水放回了背包")
        }
        return dbR
    }
}