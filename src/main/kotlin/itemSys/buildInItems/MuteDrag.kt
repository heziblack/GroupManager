package org.hezistudio.itemSys.buildInItems

import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.contact.nameCardOrNick
import org.hezistudio.GroupmanagerHz
import org.hezistudio.dataBase.DBTools
import org.hezistudio.dataBase.MemberMutes
import org.hezistudio.itemSys.ItemEntity
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object MuteDrag:ItemEntity(
    "禁言药水","可以让对象在一个小时之内无法说话的神秘道具，请勿对管理员使用",
    inPrice = 800,
    useToStr = "【使用对象】在接下来的一个小时内不能说话了",
    canBuy = true,
    target = ItemTarget.Someone,
) {
    /**@param exArg 0-禁言对象ID*/
    override suspend fun active(db: Database?, user: Member, vararg exArg: String): Boolean {
        db?:return false
        val group = user.group
        val bot = user.bot
        val target = group[exArg[0].toLong()] ?: return false
        if (target.permission >= group.botPermission) {
            group.sendMessage("Bot权限不够，也许我变成群主就让${target.nameCardOrNick}闭嘴呢")
        }
        if (group.botPermission >= MemberPermission.ADMINISTRATOR) {
            target.mute(3600)
            val eTime = LocalDateTime.now().plusHours(1)
            val formatter = DateTimeFormatter.ofPattern("yyMMddHHmmss")
            val endTimeLong = eTime.format(formatter).toLong()
            val r = transaction {
                val memBD = DBTools.getMember(this.db, target.id, group.id) ?: return@transaction false
                MemberMutes.insert {
                    it[member] = memBD.id
                    it[endTime] = endTimeLong
                }
                true
            }
            if (r) {
                val targetNick = DBTools.getMember(db, target.id, group.id)?.nickName ?: target.nameCardOrNick
                val userNick = DBTools.getMember(db, user.id, group.id)?.nickName ?: user.nameCardOrNick
                val outStr = this.useToStr.replace("【使用者】", userNick).replace("【使用对象】", targetNick)
                group.sendMessage(outStr)
            }
            return r
        } else {
            group.sendMessage("Bot权限不够，也许我变成群主就让${target.nameCardOrNick}闭嘴呢")
            return false
        }
    }
}