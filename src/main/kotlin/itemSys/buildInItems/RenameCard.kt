package org.hezistudio.itemSys.buildInItems

import net.mamoe.mirai.contact.Member
import org.hezistudio.dataBase.DBTools
import org.hezistudio.itemSys.ItemEntity
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

object RenameCard: ItemEntity(
    "改名卡","可以让你改变名字的道具, 发送'我要改名 [新名字]'使用",
    "改名成功！你现在的名字是【使用者】",
    inPrice = 500, canBuy = true, isSpecial = true
) {
    /**@param exArg 额外参数：0-新名字*/
    override suspend fun active(db: Database?, user: Member, vararg exArg: String): Boolean {
        if (db==null) return false
        return transaction{
            val mem = DBTools.getMember(db, user.id, user.group.id) ?: return@transaction false
            mem.nickName = exArg[0]
            true
        }
    }
}