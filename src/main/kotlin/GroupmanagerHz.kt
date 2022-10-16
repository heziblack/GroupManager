package org.hezistudio

import com.beust.klaxon.Klaxon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.extension.PluginComponentStorage
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.plugin.version
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.ListeningStatus
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.info
import org.hezistudio.apexSearch.ListenerOfApexSearch
import org.hezistudio.dataBase.*
import org.hezistudio.itemSys.BuildInItemList
import org.hezistudio.itemSys.ListenerOfItemSys
import org.hezistudio.itemSys.ListenerForItemDescription
import org.hezistudio.klaxonModels.MessageModel
import org.hezistudio.listenerHosts.*
import org.hezistudio.stockSys.StockListener
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import javax.imageio.ImageIO
import kotlin.math.absoluteValue

object GroupmanagerHz : KotlinPlugin(
    JvmPluginDescription(
        id = "org.hezistudio.groupmanager-hz",
        name = "我的群管插件",
        version = "0.0.5",
    ) {
        author("HeziBlack")
    }
) {
    private val scheduler = Executors.newScheduledThreadPool(3)

    private fun clearSchedule(){
        Schedule.clear()
    }

    private fun cycleClear(cycleTime:Long = 24*60*60*1000){
        scheduler.submit {
            GroupmanagerHz.launch {
                delay(cycleTime)
                clearSchedule()
                cycleClear()
            }
        }
    }
    // 我的游戏群：795327860L
    // 测试群：116143851L
    private var groupID = 795327860L
    var pluginConfig = Config()
    var messageModel = MessageModel()

    override fun onEnable() {
        logger.info { "Plugin loading" }
        pluginConfig = loadConfig() // 加载配置
        messageModel = loadMessageModel() // 加载消息模板
        groupID = pluginConfig.group // 读取群id
//        cycleTimer() // 循环计时器

        // 指定群聊消息
        val channel = globalEventChannel().filter {
            it is GroupMessageEvent && it.group.id == pluginConfig.group
                    && it.bot.id == pluginConfig.bot
        }

        // 哔哩哔哩解析
        channel.registerListenerHost(BiliVideoPhase)

        // 签到功能
        channel.filter {
            it is GroupMessageEvent && it.message.content == "签到"
        }.subscribeAlways<GroupMessageEvent> { signIn(sender) }

        // 查询积分
        channel.registerListenerHost(StatuesListener)

        // 狐狸要的击剑功能：随机击剑
        channel.filter { it is GroupMessageEvent && it.message.content == "我要击剑" }.subscribe<GroupMessageEvent> {
                val msgBuilder = MessageChainBuilder()
                getDBC(bot.id)
                transaction {
                    addLogger(StdOutSqlLogger)
                    val theSender = getDBMember(db,sender.id)?:return@transaction
                    val target = getRandomMemberExcept(db,sender.id)?:return@transaction
                    msgBuilder.add(jijianAction(db,theSender,target))
                }
                group.sendMessage(msgBuilder.build())
                ListeningStatus.LISTENING
            }

        // 击剑对决
        channel.filter {
            val patter = """击剑对决\[mirai:at:[0-9]+] ?"""
            val regex = Regex(patter)
            val serializedMiraiCode = (it as GroupMessageEvent).message.serializeToMiraiCode()
            regex.matches(serializedMiraiCode)
        }.subscribe<GroupMessageEvent> {
            getDBC(bot.id)
            val mb = MessageChainBuilder()
            transaction {
                val challenger = getDBMember(db,sender.id)?:return@transaction
                val target = getDBMember(db, (message[2] as At).target)?:return@transaction
                mb.add(jijianAction(db,challenger,target))
            }
            group.sendMessage(mb.build())
            ListeningStatus.LISTENING
        }

        // 击剑十连
        channel.filter { it is GroupMessageEvent && it.message.content == "我要打十个" }.subscribeAlways<GroupMessageEvent> {
            getDBC(bot.id)
            val msgBuilder = MessageChainBuilder()
            transaction {
                val self = getDBMember(db,sender.id)?:return@transaction
                msgBuilder.add(jijianAction10(db,self))
            }
            group.sendMessage(msgBuilder.build())
        }
        // 股票系统
        channel.registerListenerHost(StockListener)

        // 排行榜
        channel.filter {
            if (it !is GroupMessageEvent) return@filter false
            val msg = it.message.content
            val cmdList = listOf<String>("排行榜","地下室榜")
            msg in cmdList
        }.subscribeAlways<GroupMessageEvent> {
            getDBC(bot.id)
            val sb = StringBuilder("排行榜\n  昵称       积分")

            if (message.content == "排行榜") {
                transaction {
                    val mems = Member.all().filter { it.fraction > 0 }
                    val sortedMembers = mems.sortedByDescending { it.fraction }
                    val topTen = if (sortedMembers.size >= 10) 10 else sortedMembers.size
                    val leaderBoard: ArrayList<Member> = arrayListOf()
                    for (idx in (0 until topTen)) {
                        leaderBoard.add(sortedMembers[idx])
                    }
                    for (mb in leaderBoard) {
                        val nick = mb.nickName
                        val nickLen = getTextLength(nick)
                        val dealedNick =
                            if (nickLen > 8) {
                                val nl = nick.length
                                "${nick.substring(0, 2)}..${nick.substring(nl - 2)}"
                            } else {
                                nick
                            }
                        val dealedNickLen = getTextLength(dealedNick)
                        val row =
                            if (dealedNickLen <= 4) {
                                "\n${dealedNick}           ${mb.fraction}"
                            } else {
                                "\n${dealedNick}  ${mb.fraction}"
                            }
                        sb.append(row)
                    }
                }
                group.sendMessage(sb.toString())
            }else{
                transaction {
                    val mems = Member.all().filter { it.fraction < 0 }
                    val sortedMembers = mems.sortedBy { it.fraction }
                    val topTen = if (sortedMembers.size >= 10) 10 else sortedMembers.size
                    val leaderBoard: ArrayList<Member> = arrayListOf()
                    for (idx in (0 until topTen)) {
                        leaderBoard.add(sortedMembers[idx])
                    }
                    for (mb in leaderBoard) {
                        val nick = mb.nickName
                        val nickLen = getTextLength(nick)
                        val dealedNick =
                            if (nickLen > 8) {
                                val nl = nick.length
                                "${nick.substring(0, 2)}..${nick.substring(nl - 2)}"
                            } else {
                                nick
                            }
                        val dealedNickLen = getTextLength(dealedNick)
                        val row =
                            if (dealedNickLen <= 4) {
                                "\n${dealedNick}           ${mb.fraction}"
                            } else {
                                "\n${dealedNick}  ${mb.fraction}"
                            }
                        sb.append(row)
                    }
                }
                group.sendMessage(sb.toString())
            }
        }

        // 道具系统
        channel.filter { it is GroupMessageEvent }.registerListenerHost(ListenerOfItemSys)

        // 特殊道具使用
        channel.registerListenerHost(SpecialItemUseHost)

        // 道具说明
        channel.filter {
            it is GroupMessageEvent && it.message.content in BuildInItemList.nameSet
        }.registerListenerHost(ListenerForItemDescription)

        // 禁言监听
        globalEventChannel().filter {
            val rr = (it as MemberUnmuteEvent).group.id == pluginConfig.group
            rr
        }.subscribeAlways<MemberUnmuteEvent> {
            getDBC(bot.id)
            val target = this.member
            logger.info(target.nick)
            var leftTime:Long = 0L
            transaction {
                val mem = DBTools.getMember(db,target.id,it.groupId)
                if (mem!=null){
                    val mm = MemberMutes.select {
                        MemberMutes.member eq mem.id
                    }
                    if (!mm.empty()){
                        val tm = mm.last()
                        val formatter = DateTimeFormatter.ofPattern("yyMMddHHmmss")
                        val cTime = LocalDateTime.now()
                        val endTime = LocalDateTime.from(formatter.parse(tm[MemberMutes.endTime].toString()))
                        val duration = Duration.between(cTime,endTime)
                        if (!duration.isNegative){
                            leftTime = duration.seconds
                        }
                    }
                }
            }
            if (leftTime>0){
                if(group.botPermission > MemberPermission.MEMBER && target.permission < group.botPermission){
                    target.mute(leftTime.toInt())
                }
            }
        }

        // 利用bot上线事件读取bot数据
        globalEventChannel().subscribe<BotOnlineEvent> {
            logger.warning("登录bot功能还未实现")
            ListeningStatus.LISTENING
        }

        // bot管理员私聊
        globalEventChannel().filter {
            it is FriendMessageEvent && it.sender.id == pluginConfig.owner
        }.subscribe<FriendMessageEvent> {
            when (message.content){
                "数据更新"->{

                }
            }
            ListeningStatus.LISTENING
        }

        // apex查询
        channel.filter {
            if (it !is GroupMessageEvent) return@filter false
            val msg = it.message.content
            val starts = msg.startsWith("派派查询")
            starts
        }.registerListenerHost(ListenerOfApexSearch)

        // 监视群成员变化
        globalEventChannel().filter {
            val t1 = it is MemberJoinEvent && it.groupId == pluginConfig.group
            val t2 = it is MemberLeaveEvent && it.groupId == pluginConfig.group
            val t3 = it is BotJoinGroupEvent && it.groupId == pluginConfig.group
            t1 || t2 || t3
        }.registerListenerHost(TargetGroupListener)

        // 测试
        channel.filter { it is GroupMessageEvent && it.message.content=="测试" }.registerListenerHost(ListenerForTest)

        // 菜单
        channel.filter { it is GroupMessageEvent && it.message.content=="菜单" }.registerListenerHost(MenuListener)

        // 隐藏小功能
        channel.registerListenerHost(InterestingFuncs)

        logger.info { "Plugin loaded" }
    }

    /**加载完成后行动*/
    override fun PluginComponentStorage.onLoad() {
        runAfterStartup {
            val s = Bot.instances.size
            initDataBase(Bot.instances)
            println("在线Bot：$s")
        }
    }

    /**循环计时器，用于每日清空游戏日程*/
    private fun cycleTimer(){
        val d = Duration.between(LocalTime.of(23,59),LocalTime.now())
            .abs()
            .toMillis()
        cycleClear(d)
    }

    /**加载配置*/
    private fun loadConfig():Config{
        val file = File(configFolder,"config.json")
        if (!file.exists()){
            if (!configFolder.exists()){
                configFolder.mkdirs()
            }
            file.createNewFile()
        }
        val parser = Klaxon()
        val config = try{
            parser.parse<Config>(file.readText()) ?: Config()
        }catch (e:Exception){
            logger.warning("警告：获取配置异常，请重新设置配置文件")
            Config()
        }
        // 覆写配置文件
        val jsonString = parser.toJsonString(config)
        file.writeText(jsonString)
        logger.info("当前配置：$jsonString")
        return config
    }

    /**加载消息模板*/
    private fun loadMessageModel():MessageModel{
        val file = File(dataFolder,"消息模板.json")
        val i = !file.exists()
        if (i){
            if (!dataFolder.exists()){
                dataFolder.mkdirs()
            }
            file.createNewFile()
        }
        val parser = Klaxon()
        val model = try{
            parser.parse<MessageModel>(file.readText())?:MessageModel()
        }catch (e:Exception){
            logger.warning("警告：获取消息模板异常，请重新设置文件：${file.toURI()}")
            MessageModel()
        }
        val jsonString = parser.toJsonString(model)
        if(i){
            file.writeText(jsonString)
        }
        logger.info("""
            win[${model.winStr.size}]:${model.winStr[0]}
            draw[${model.drawStr.size}]:${model.drawStr[0]}
            loss[${model.lossStr.size}]:${model.lossStr[0]}
            """.trimEnd())
        return model
    }

    /**创建数据库表*/
    private fun crateTables(db: Database){
        transaction {
            SchemaUtils.create(
                Groups, Users, Items,
                BlueMaps, BlueMapInputItems, BlueMapOutputItems,
                GroupMembers, GroupMemberBlueMaps, MemberItems,
                MemberSignIns, JiJians, ScheduleMembers,
                GameSchedules,MemberMutes
            )
        }
    }

    /**启动时更新用户信息*/
    private fun updateDB(group:Group, file: File, db:Database? = null){
        db ?: Database.connect("jdbc:sqlite:${file.toURI()}","org.sqlite.JDBC")
        transaction {
//            addLogger(StdOutSqlLogger)
            crateTables(this.db)
            val allItems = Item.all()
            for (item in BuildInItemList){
                var hadInDB = false
                allItems.forEach{
                    if (it.name == item.name){
                        hadInDB = true
                    }
                }
                if (!hadInDB){
                    Item.new {
                        name = item.name
                        description = item.description
                        inPrice = item.inPrice
                        outPrice = item.outPrice
                        usage = item.equipStr
                    }
                }
            }
            val grs = org.hezistudio.dataBase.Group.find { Groups.number eq group.id }
            val gr = if (grs.empty()) {
                org.hezistudio.dataBase.Group.new { number = group.id; isWorking = true }
            }else{
                grs.first()
            }
            // 遍历群内成员，检查有无新成员并更新
            for (mem in group.members){
                val members = User.find{ Users.userNum.eq(mem.id) }
                if (members.empty()){
                    logger.info("未查询到")
                    val newUser = User.new { userNum = mem.id }
                    Member.new {
                        member = newUser
                        this.group = gr
                        nickName = mem.nameCardOrNick
                    }
                }
            }
        }
    }

    /**初始化数据库*/
    private fun initDataBase(bList:List<Bot>){
        logger.info("进入初始化数据库")
        for (bot in bList){
            val group = bot.groups[groupID]?:continue
            logger.info("在群里面")
            val file = File(dataFolder,"db${bot.id}.db3")
            if (!file.exists()){
                if (!dataFolder.exists()) dataFolder.mkdirs()
                file.createNewFile()
                val db = Database.connect("jdbc:sqlite:${file.toURI()}","org.sqlite.JDBC")
                logger.info(db.url)
                transaction {
//                    addLogger(StdOutSqlLogger)
                    crateTables(db)
                    val groupEntity = org.hezistudio.dataBase.Group.new {
                        number = groupID
                        isWorking = true
                    }
                    for (member in group.members){
                        val user = User.new{
                            userNum = member.id
                        }
                        Member.new {
                            this.member = user
                            this.group = groupEntity
                            nickName = member.nameCardOrNick
                        }
                    }
                }
            }else{
                updateDB(group, file)
            }
        }
    }

    /**处理签到*/
    private suspend fun signIn(member:net.mamoe.mirai.contact.Member){
        val contactGroup = member.group
        val botID = contactGroup.bot.id
        getDBC(botID)
        val msgBuilder = MessageChainBuilder()
        transaction {
            // 添加签到
            fun addSignIn(m:Member,ts:String){
                MemberSignIn.new {
                    this.member = m
                    timeStamp = ts
                }
            }
            // 添加签到
            fun addSignIn(m:Member,ts:String,gift:Int){
                addSignIn(m,ts)
                m.fraction += gift
            }
            // 获取群员
            val m = Member.all().filter {
                it.member.userNum == member.id
            }
            if (m.isNotEmpty()){
                // rms:resultOfMemberSignIn
                val rms = MemberSignIn.find {
                    MemberSignIns.member eq m.first().id
                }
                // 没有签到记录
                if (rms.empty()){
                    val mem = m.first()
                    val gift = (3..15).random()
                    val healthFra = memberSignIn(gift, msgBuilder)
                    addSignIn(mem, todayStamp().toString(), gift+healthFra)
                }else{
                    // 有签到记录，获取最新签到记录
                    val latest = rms.last().timeStamp.toLong()
                    logger.info("最近签到：$latest, 今日期限：${todayLine()}")
                    if (latest < todayLine()){
                        // 未签过到
                        val mem = m.first()
                        val gift = (3..15).random()
                        val healthFra = memberSignIn(gift, msgBuilder)
                        addSignIn(mem, todayStamp().toString(), gift+healthFra)
                    }else{
                        msgBuilder.add(PlainText("你今天已经签过到了"))
                    }
                }
            }
        }
        contactGroup.sendMessage(msgBuilder.build())
    }

    /**检查健康打卡时间*/
    private fun memberSignIn(gift:Int, msgBuilder:MessageChainBuilder):Int{
        val health = isHealthTime()
        msgBuilder.add(PlainText("签到成功,奖励${gift}点积分"))
        return if (!health){
            val a = (-5..-2).random()
            msgBuilder.add("\n这个时间签到，作息不太正常呢，${a}点积分")
            a
        }else 0
    }

    /**获取数据库连接
     *
     * 使用时确认数据库已经完成初始化*/
    fun getDBC(botID:Long):Database{
        val dbFile = File(dataFolder,"db${botID}.db3")
        return Database.connect("jdbc:sqlite:${dbFile.toURI()}", "org.sqlite.JDBC")
    }

    /**更新击剑记录
     *
     * 找到关联击剑记录，并在此记录上+1
     *
     * 若无相关记录，则创建一个次数为1的记录*/
    private fun addJijian(db: Database, member: Member):Jijian{
        return transaction {
            val jijians = Jijian.all().filter {
                it.member == member
            }
            if (jijians.isEmpty()){
                return@transaction addNewJijian(db,member)
            }
            val latest = jijians.last()
            if (latest.timeStamp > todayLine()){
                // 有今天的记录
                latest.counter += 1
                latest
            }else{
                // 没有当日记录
                addNewJijian(db,member)
            }
        }
    }

    /**添加一行新击剑记录*/
    private fun addNewJijian(db:Database, member:Member):Jijian{
        return transaction {
            return@transaction Jijian.new {
                this.member = member
                timeStamp = todayStamp()
            }
        }
    }

    /**获取今日时间戳*/
    fun todayStamp():Long{
        val currentTime = LocalDateTime.now()
        val fmt = DateTimeFormatter.ofPattern("yyMMddHHmm")
        return currentTime.format(fmt).toLong()
    }

    /**获取今日时间线*/
    fun todayLine():Long{
        val currentTime = LocalDateTime.now()
        val fmt = DateTimeFormatter.ofPattern("yyMMdd0000")
        return currentTime.format(fmt).toLong()
    }

    /**获取字符长度，英文占1 中文占2*/
    private fun getTextLength(s:String):Int{
        var len = 0
        for (c in s){
            len += when(c.code){
                in (19968..40869) -> 2
                else -> 1
            }
        }
        return len
    }

    /**根据id获取db实体*/
    private fun getDBMember(db: Database, memberID:Long):Member?{
        return transaction {
            val query = GroupMembers.innerJoin(Users).innerJoin(Groups)
                .slice(GroupMembers.columns)
                .select(
                    Users.userNum eq memberID and (Groups.number eq groupID)
                )
            val mems = Member.wrapRows(query).toList()
            if (mems.isEmpty()){
                return@transaction null
            }else{
                return@transaction mems[0]
            }
        }
    }

    /**获取除了指定id外的随机db实体*/
    private fun getRandomMemberExcept(db: Database, except: Long):Member?{
        return transaction {
            val query = GroupMembers.innerJoin(Users).innerJoin(Groups)
                .slice(GroupMembers.columns)
                .select(
                    (Users.userNum neq except) and (Groups.number eq groupID)
                )
            val mems = Member.wrapRows(query).toList()
            if (mems.isEmpty()){
                return@transaction null
            }else{
                return@transaction mems.random()
            }
        }
    }

    /**击剑*/
    private fun jijianAction(db: Database, challenger: Member, target: Member):MessageChain{
        val msgBuilder = MessageChainBuilder()
        transaction {
            /**内部方法，用于替换文字模板*/
            fun replace(s:String, sender:Member, target:Member):String{
                return s.replace("【发起人】", sender.nickName)
                    .replace("【击剑目标】", target.nickName)
            }

            val jijianCount = addJijian(db,challenger)
            if (jijianCount.counter > 10){
                jijianCount.counter -= 1
                msgBuilder.add("你今天击剑次数已经超过10次了，注意身体喔")
                return@transaction
            }
            msgBuilder.add("${challenger.nickName}找上了对手：${target.nickName} ")
            when(jijianCompare2(challenger, target)){
                true->{
                    val msg = replace(
                        messageModel.winStr.random(),challenger,target)
                    msgBuilder.add(msg)
                    msgBuilder.add("\n积分+2")
                    challenger.fraction += 2
                }
                false->{
                    val msg = replace(
                        messageModel.lossStr.random(),challenger,target)
                    msgBuilder.add(msg)
                    msgBuilder.add("\n积分-3")
                    challenger.fraction -= 3
                }
                null->{
                    val msg = replace(
                        messageModel.drawStr.random(),challenger,target)
                    msgBuilder.add(msg)
                    msgBuilder.add("\n积分-1")
                    challenger.fraction -= 1
                }
            }
        }
        return msgBuilder.build()
    }

    /**@return true-胜利, false-失败, null-平局*/
    private fun jijianCompare2(challenger: Member,target:Member):Boolean?{
        if (!challenger.gender && target.gender){return true}
        if (challenger.gender && !target.gender){return false}
        val a = target.defence - challenger.attack
        val b = challenger.strength - target.strength
        logger.info("$a $b")
        if (a.absoluteValue < 0.01 && b.absoluteValue < 0.01) return null
        return if (a<0 && b >= 0){
            true
        } else if (a < 0 && b < 0) {
            val c = b / 10
            c <= a
        }else if (a > 0 && b <= 0) {
            false
        }else{
            val c = b / 10
            c > a
        }
    }

    /**击剑十连*/
    private fun jijianAction10(db: Database,challenger: Member):MessageChain{
        val msg = MessageChainBuilder()
        transaction {
            val jijian = getTodayJijian(db,challenger)
            if (jijian.counter>=10){
                msg.add("您今天已经打过十个了，请明天再来，叶师傅")
            }else{
                val left = 10 - jijian.counter
                val randomMemberList:ArrayList<Member?> = arrayListOf()
                for (i in (1..left)){
                    val rm = getRandomMemberExcept(db,challenger.member.userNum)
                    randomMemberList.add(rm)
                }
                var frac = 0
                var winC = 0
                var loseC = 0
                var drawC = 0
                for (rm in randomMemberList){
                    if (rm != null){
                        when(jijianCompare2(challenger,rm)){
                            true -> {
                                frac += 2
                                winC += 1
                            }
                            false -> {
                                frac-=3
                                loseC+=1
                            }
                            null -> {
                                frac-=1
                                drawC+=1
                            }
                        }
                    }
                }
                jijian.counter = 10
                challenger.fraction += frac
                val giftNum = 0.01 * left
                val giftName = when((0..2).random()){
                    0-> {
                        challenger.attack += giftNum
                        "攻击力"
                    }
                    1-> {
                        challenger.defence += giftNum
                        "防御力"
                    }
                    2-> {
                        challenger.strength += giftNum
                        "体力"
                    }
                    else -> {""}
                }
                msg.add("你一共进行了${winC+loseC+drawC}场比试，战绩${winC}胜${loseC}负${drawC}平，获得积分${frac}点，获得${giftNum}点${giftName}增长")
            }
        }
        return msg.build()
    }

    /**插入一条新击剑记录并返回*/
    private fun insertJijian(db: Database,member: Member):Jijian{
        return transaction {
            return@transaction Jijian.new {
                this.member = member
                timeStamp = todayStamp()
                counter = 0
            }
        }
    }

    /**获取对象今日击剑数据*/
    private fun getTodayJijian(db: Database,target: Member):Jijian{
        return transaction {
            val jijianList = Jijian.all().filter {
                it.member == target
            }
            return@transaction if (jijianList.isEmpty()){
                insertJijian(db, target)
            }else{
                val row = jijianList.last()
                logger.info { "${row.timeStamp}" }
                if (row.timeStamp> todayLine()){
                    row
                }else{
                    insertJijian(db, target)
                }
            }
        }
    }

    /** 是否在健康打卡时间 */
    private fun isHealthTime():Boolean{
        val dateTime = LocalDateTime.now()
        return dateTime.hour in (5..22)
    }
    /***/
    fun getImageResStream(path:String):InputStream?{
        return getResourceAsStream(path)
    }

}