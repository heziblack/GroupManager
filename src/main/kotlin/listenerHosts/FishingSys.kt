package org.hezistudio.listenerHosts

import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.content
import org.hezistudio.GroupmanagerHz
import org.hezistudio.dataBase.*
import org.hezistudio.listenerHosts.FishingSys.RodState.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object FishingSys:ListenerHost{
    private val allowedDuration = Duration.ofMinutes(10)
    private val dtFormatter:DateTimeFormatter = DateTimeFormatter.ofPattern("yyMMddHHmm")

    init {
        buildInDropsToDB()
    }

    @EventHandler
    suspend fun onEvent(e:GroupMessageEvent){
        val msg = e.message.content

        when(e.message.content){
            "下杆"->{
                rodSwing(e.sender)
            }
            "提杆"->{
                rodPull(e.sender)
            }
        }
        if (msg=="天堂制造！"){
            val s = madeInHeaven(e.sender)
            if (s!=""){
                e.group.sendMessage(s)
            }
        }
    }

    val luckyList:ArrayList<Long> = arrayListOf(
        1718752796
    )

    /**提竿*/
    private suspend fun rodPull(member:Member){
        val db = GroupmanagerHz.getDBC(member.bot.id)
        val group = member.group
        val dbMember = DBTools.getMemberOrCreate(db,member)
        val state = if (member.id in luckyList){
            ItsTime
        }else{
            fishingState(member)
        }
        GroupmanagerHz.logger.info("${dbMember.nickName}:${state.name}")
        when(state){
            Waiting -> {
                val punish=(-3..-1).random()
                transaction {
                    val nm = DBTools.getMemberOrCreate(db,member)
                    nm.fraction += punish
                    val fishingMan = getFishManOrCreate(db,member)
                    fishingMan.rodState = false
                }
                group.sendMessage("看吧，鱼儿都被你吓跑了,积分${punish}")
            }
            ItsTime -> {
                val drop = awardFisher(dbMember)
                transaction {
                    val nm = DBTools.getMemberOrCreate(db,member)
                    nm.fraction += drop.awd
                    val fishingMan = getFishManOrCreate(db,member)
                    fishingMan.rodState = false
                }
                val reply = renderTextModel(drop,dbMember)
                group.sendMessage(reply)
            }
            Missed -> {
                val punish = PunishAndDescription.values().random()
                val drop = punishFisher(dbMember)
                transaction {
                    val nm = DBTools.getMemberOrCreate(db,member)
                    nm.fraction += drop.awd
                    val fishingMan = getFishManOrCreate(db,member)
                    fishingMan.rodState = false
                }
                val reply = renderTextModel(drop,dbMember)
                group.sendMessage(reply)
            }
            else -> {

            }
        }
    }
    /**挥杆*/
    private suspend fun rodSwing(member: Member){
        val db = GroupmanagerHz.getDBC(member.bot.id)
        val group = member.group
        val dbMember = DBTools.getMemberOrCreate(db,member)
        //        GroupmanagerHz.logger.info("${dbMember.nickName}:${state.name}")
//        GroupmanagerHz.logger.info("玩家积分:${dbMember.fraction}")
        when(fishingState(member)){
            Undo -> {
                if (dbMember.fraction<1){
                    group.sendMessage("非常遗憾的通知您，由于技术原因，在抹除您的债务之前您不能加入我们的钓鱼俱乐部")
                    return
                }
                val hour = (1L..3L).random()
                transaction {
                    val nm = DBTools.getMemberOrCreate(db,member)
                    nm.fraction -= 1
                    val fishingMan = getFishManOrCreate(db,member)
                    fishingMan.rodState = true
                    val nt = LocalDateTime.now()
                    val plusDuration = Duration.ofHours(hour)
                    val tt = nt.plus(plusDuration)
                    val tts = fromDateTimeToLong(tt)
                    fishingMan.targetTime = tts
                }
                group.sendMessage("扣除1点积分，请${hour}个小时后再发送:'提杆'")
            }
            Waiting,Missed,ItsTime ->{
                group.sendMessage("你已经放下了一支鱼竿，不能再放第二支了")
            }
            else -> {}
        }
//        GroupmanagerHz.logger.info("玩家积分：${dbMember.fraction}")
    }

    /**读取用户的钓鱼状态*/
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
    /**内置钓鱼状态*/
    private enum class RodState(){
        Undo(),
        Waiting(),
        ItsTime(),
        Missed()
    }
    /**内置钓鱼奖励*/
    private enum class FishDescription(val nameZh:String,val dscrp:String,val award:Int){
        Small("小鱼","小小的鱼",3),
        Mid("中鱼","看上去不错的鱼",5),
        Big("大鱼","好大的鱼",7)
    }
    /**内置钓鱼惩罚*/
    private enum class PunishAndDescription(val nameZh:String,val dscrp: String,val punish:Int){
        Small("靴子","不知道放了多久",-2),
        Mid("狐狸怪盗的留言","神秘",-4),
        Big("橘子","充满黄色内容物",-6)
    }

    /**如果数据库中的奖励为空，复制内置奖励到数据库*/
    private fun buildInDropsToDB(){
        if (GroupmanagerHz.pluginConfig.bot == -1L){
            return
        }
        val db = GroupmanagerHz.getDBC(GroupmanagerHz.pluginConfig.bot)
        transaction(db) {
            SchemaUtils.create(
                FishingDrops
            )
            val q = FishingDrop.all()
            if (q.empty()){
                FishDescription.values().forEach {
                    FishingDrop.new{
                        dropName = it.nameZh
                        strDesc = it.dscrp
                        awd = it.award
                    }
                }
                PunishAndDescription.values().forEach {
                    FishingDrop.new{
                        dropName = it.nameZh
                        strDesc = it.dscrp
                        awd = it.punish
                    }
                }
            }
        }
    }

    private fun awardFisher(fisher:org.hezistudio.dataBase.Member):FishingDrop{
        fisher.db
        val awds = transaction {
            FishingDrop.find {
                FishingDrops.fraction greater 0
            }.toList()
        }
        return if (awds.isEmpty()){
            transaction{
                FishingDrop.new {
                    strDesc = "虎纹鲨鱼"
                    awd = 5
                }
            }
        }else{
            awds.random()
        }
    }

    private fun punishFisher(fisher: org.hezistudio.dataBase.Member):FishingDrop{
        fisher.db
        val awds = transaction {
            FishingDrop.find {
                FishingDrops.fraction less 0
            }.toList()
        }
        return if (awds.isEmpty()){
            transaction {
                FishingDrop.new {
                    strDesc = "臭袜子"
                    awd = -3
                }
            }
        }else{
            awds.random()
        }
    }

    private val awdTextModel:List<String> = listOf(
        "【钓鱼人】虎躯一震，气沉丹田，大喝一声”鱼来“，钓上了【描述】的【渔获】，积分加【积分】",
        "看看你钓起来了什么：【描述】的【渔获】，积分加【积分】"
    )

    private val pnsTextModel:List<String> = listOf(
        "【钓鱼人】拉起鱼钩，高兴的看了看自己的收获，发现是个【描述】的【渔获】，大失所望，积分【积分】",
        "看看你钓起来了什么：【描述】的【渔获】，积分【积分】",
        "\'命里有时终须有，命里无时莫强求\'，钓起【描述】的【渔获】，积分【积分】"
    )
    private fun renderTextModel(drop: FishingDrop,dbMember:org.hezistudio.dataBase.Member):String{
        val str = if (drop.awd>0){
            awdTextModel.random()
        }else{
            pnsTextModel.random()
        }
        return str.replace("【钓鱼人】",dbMember.nickName).replace("【渔获】",drop.dropName).replace("【积分】",drop.awd.toString()).replace("【描述】",drop.strDesc)
    }

    private fun madeInHeaven(member:Member):String{
        val msg = StringBuilder()
        when(fishingState(member)){
            Waiting,ItsTime->{
                transaction {
                    val db = DBTools.db
                    val dbMember = DBTools.getMemberOrCreate(db,member)
                    dbMember.fraction -= 3
                    if (dbMember.fraction<0){
                        dbMember.fraction = 0
                    }
                    val fs = getFishManOrCreate(GroupmanagerHz.getDBC(member.bot.id),member)
                    val drt = Duration.ofMinutes(-3)
                    val t = LocalDateTime.now().plus(drt)
                    fs.targetTime = fromDateTimeToLong(t)
                    println(t)
                }
                msg.append("扣除3点积分，你的时间加速流动！")
            }
            else->{}
        }
        return msg.toString()
    }
}