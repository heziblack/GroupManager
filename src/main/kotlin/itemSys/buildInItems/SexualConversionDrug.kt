package org.hezistudio.itemSys.buildInItems

import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.message.data.MessageChainBuilder
import org.hezistudio.dataBase.DBTools
import org.hezistudio.itemSys.ItemEntity
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

object SexualConversionDrug:ItemEntity(
    "性转药水","看名字的话就不需要多做说明了吧",
    inPrice = 200,
    canBuy = true,
    canBeConsumed = true
) {
    override suspend fun active(db: Database?, user: Member, vararg exArg: String): Boolean {
        db?:return false
        val group = user.group
        val msgBuilder = MessageChainBuilder()
        val itemName = this.name
        val dbR =  transaction {
            val dbUser = DBTools.getMember(db,user.id,group.id)?:return@transaction false
            if (dbUser.gender){
                dbUser.gender = false
                msgBuilder.add("${dbUser.nickName}喝下了${itemName}, 变成了娇滴滴的女孩纸")
            }else{
                dbUser.gender = true
                msgBuilder.add("${dbUser.nickName}喝下了${itemName}, 变成了娇滴滴的男孩纸")
            }
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