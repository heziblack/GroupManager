package org.hezistudio.listenerHosts

import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.content
import org.hezistudio.GroupmanagerHz
import org.hezistudio.dataBase.DBTools
import org.hezistudio.dataBase.MemberFishing
import org.hezistudio.dataBase.MemberFishings
import org.hezistudio.listenerHosts.FishingSys.RodState.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object FishingSys:ListenerHost{

    private val allowedDuration = Duration.ofMinutes(10)
    private val dtFormatter:DateTimeFormatter = DateTimeFormatter.ofPattern("yyMMddHHmm")
    private val oneHour = Duration.ofHours(1)
    @EventHandler
    suspend fun onEvent(e:GroupMessageEvent){
        when(e.message.content){
            "下杆"->{
                rodSwing(e.sender)
            }
            "提杆"->{
                rodPull(e.sender)
            }
        }
    }
    /**提竿*/
    private suspend fun rodPull(member:Member){
        val db = GroupmanagerHz.getDBC(member.bot.id)
        val group = member.group
        val dbMember = DBTools.getMemberOrCreate(db,member)
//        val fms = fishingState(member)
//        GroupmanagerHz.logger.info("${fms.name}")
        when(fishingState(member)){
            Waiting -> {
                val punish=(-3..-1).random()
                transaction {
                    dbMember.fraction += punish
                    val fishingMan = getFishManOrCreate(db,member)
                    fishingMan.rodState = false
                }
                group.sendMessage("看吧，鱼儿都被你吓跑了,积分${punish}")
            }
            ItsTime -> {
                val award = FishDescription.values().random()
                transaction {
                    dbMember.fraction += award.award
                    val fishingMan = getFishManOrCreate(db,member)
                    fishingMan.rodState = false
                }
                group.sendMessage("${dbMember.nickName}钓起来${award.dscrp}, 获得${award.award}点积分")
            }
            Missed -> {
                transaction {
                    val fishingMan = getFishManOrCreate(db,member)
                    fishingMan.rodState = false
                }
                group.sendMessage("你把鱼竿抬起来，发现鱼饵早就被吃掉了")
            }
            else -> {

            }
        }
    }

    private suspend fun rodSwing(member: Member){
        val db = GroupmanagerHz.getDBC(member.bot.id)
        val group = member.group
        val dbMember = DBTools.getMemberOrCreate(db,member)
        when(fishingState(member)){
            Undo -> {
                if (dbMember.fraction<1){
                    return
                }
                transaction {
                    dbMember.fraction -= 1
                    val fishingMan = getFishManOrCreate(db,member)
                    fishingMan.rodState = true
                    val nt = LocalDateTime.now()
                    val tt = nt.plus(oneHour)
                    val tts = fromDateTimeToLong(tt)
                    fishingMan.targetTime = tts
                }
                group.sendMessage("扣除1点积分，请一个小时后再发送:'提杆'")
            }
            else -> {

            }
        }
    }

    private fun fishingState(member:Member):RodState{
        val db = GroupmanagerHz.getDBC(member.bot.id)
        val fishMam = getFishManOrCreate(db,member)
        if (fishMam.rodState){
            val bestTime = fromLongToDateTime(fishMam.targetTime)
            val durations = Duration.between(bestTime,LocalDateTime.now())
            return if (durations.isNegative){
                // 为负，时间未到
                Waiting
            }else if (durations > allowedDuration){
                // 超出最佳时间
                Missed
            }else{
                // 最佳时间内
                ItsTime
            }
        }else{
            return Undo
        }
    }

    private fun fromLongToDateTime(long: Long): LocalDateTime {
        val s = long.toString()
        return LocalDateTime.from(dtFormatter.parse(s))
    }

    private fun fromDateTimeToLong(dt:LocalDateTime):Long{
        return dt.format(dtFormatter).toLong()
    }

    private fun getFishManOrCreate(db: Database,member:Member):MemberFishing{
        val dbMember = DBTools.getMemberOrCreate(db,member)
        return transaction {
            val q = MemberFishing.find { MemberFishings.memId eq dbMember.id.value }
            if (q.empty()){
                MemberFishing.new {
                    memId = dbMember.id.value
                }
            }else{
                q.first()
            }
        }
    }

    private enum class RodState(){
        Undo(),
        Waiting(),
        ItsTime(),
        Missed()
    }

    private enum class FishDescription(val dscrp:String,val award:Int){
        Small("一条小小的鱼",1),
        Mid("一条看上去不错的鱼",2),
        Big("好大一条鱼",3)
    }

}