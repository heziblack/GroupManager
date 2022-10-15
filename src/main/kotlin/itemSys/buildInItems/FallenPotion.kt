package org.hezistudio.itemSys.buildInItems

import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.message.data.MessageChainBuilder
import org.hezistudio.dataBase.DBTools
import org.hezistudio.itemSys.ItemEntity
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

object FallenPotion: ItemEntity(
    "堕落魔药","使用后可以使自己属性值归零的药剂",
    canBuy = true, inPrice = 0
) {
    override suspend fun active(db: Database?, user: Member, vararg exArg: String): Boolean {
        db?:return false
        val mb = MessageChainBuilder()
        val rot = transaction {
            val dbMem = DBTools.getMember(db,user.id,user.group.id)?:return@transaction false
            dbMem.attack = 0.0
            dbMem.strength = 0.0
            dbMem.defence = 0.0
            mb.add("${dbMem.nickName}喝下了堕落魔药，属性值归零了！")
            true
        }
        if (rot){
            user.group.sendMessage(mb.build())
        }else{
            user.group.sendMessage("哇喔, 由于不可抗力，您并没有喝下堕落药水")
        }
        return rot
    }
}