package org.hezistudio.stockSys

import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.content
import org.hezistudio.GroupmanagerHz
import org.hezistudio.MyTools
import org.hezistudio.dataBase.DBTools
import org.hezistudio.dataBase.MemberStocks
import org.hezistudio.dataBase.Stock
import org.hezistudio.dataBase.Stocks
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.LocalDateTime
import kotlin.math.roundToInt

object StockListener:ListenerHost {
    private val initialStock:List<Pair<String,Double>> = listOf(
    Pair("田所药业",10.0),
    Pair("博丽神社",10.0),
    Pair("邪神重工",10.0),
    Pair("圣地鸭歌",10.0),
    Pair("往生堂殡葬",10.0),
    )
    private val exampleStockName:List<String> = listOf(
        "王小美乳业","刻记牛杂","冬至机造"
    )
    private const val addHelp = "帮助股票添加"

    init {
        val botid = GroupmanagerHz.pluginConfig.bot
        if(botid != -1L){
            GroupmanagerHz.getDBC(botid)
            val allStock = transaction { Stock.all().toList() }
            for (buildInStock in initialStock){
                var isInDB = false
                for (dbs in allStock){
                    if (dbs.stName == buildInStock.first){
                        isInDB = true
                    }
                }
                if (!isInDB){
                    transaction {
                        Stock.new {
                            stName = buildInStock.first
                            price = buildInStock.second
                            hold = 0L
                            updateTime = MyTools.fromDateTimeToLong(LocalDateTime.now())
                        }
                    }
                }
            }

        }
        update()
    }
    @EventHandler
    suspend fun onEvent(e:GroupMessageEvent){
        parseCmd(e.sender, e.message.content)
    }
    private suspend fun buyStock(member: Member,num: Int,stock: Stock):Boolean{
        val db = GroupmanagerHz.getDBC(member.bot.id)
        val dbMember = DBTools.getMember(db,member.id,member.group.id)?:return false
        if (num%100!=0){
            member.group.sendMessage("购买股票数必须是100的整数倍")
            return false
        }

        val cost = (stock.price * num).roundToInt()

        if (dbMember.fraction < cost){
            member.group.sendMessage("您的积分不够")
            return false
        }

        return if(DBTools.memberBuyStock(db,member,stock.id.value,num)){
            transaction {
                dbMember.fraction -= cost
                stock.hold += num
            }
            member.group.sendMessage("成功购买${stock.stName}${num}股，花费${cost}积分")
            true
        }else{
            member.group.sendMessage("因为未知原因，购买失败")
            false
        }
    }
    private suspend fun sellStock(member: Member,num: Int,stock: Stock):Boolean{
        val db = GroupmanagerHz.getDBC(member.bot.id)
        val dbMember = DBTools.getMember(db,member.id,member.group.id)?:return false
        if (num%100!=0){
            member.group.sendMessage("出售股票数必须是100的整数倍")
            return false
        }
        val cost = (stock.price * num).roundToInt()
        val canSellStock = DBTools.canSellStock(db,stock.id.value,dbMember)
        if (canSellStock<num){
            member.group.sendMessage("你没有持有足够的可出售股票")
            return false
        }
        return if(DBTools.memberSellStock(db,member,stock.id.value,num)){
            transaction {
                dbMember.fraction += cost
                stock.hold -= num
            }
            member.group.sendMessage("成功出售${stock.stName}${num}股，获得${cost}积分")
            true
        }else{
            member.group.sendMessage("因为未知原因，出售失败")
            false
        }
    }
    private suspend fun parseCmd(member: Member, cmd:String):Boolean?{
        val buy = Regex( """股票购买 \S+ [0-9]+""")
        val sell = Regex("""股票出售 \S+ [0-9]+""")
        val add = Regex("""股票添加 \S{4,5} \d{1,5}(.\d{1,2})?""")
        val db = GroupmanagerHz.getDBC(member.bot.id)
        if (buy.matches(cmd)){
            val spitedCmd = cmd.split(" ")
            val stock = getStockByName(db,spitedCmd[1])
            if (stock==null){
                member.group.sendMessage("没有叫做${spitedCmd[1]}的股票")
                return null
            }
            val sNum = spitedCmd[2].toInt()
            if (sNum<=0) return null
            return buyStock(member,sNum, stock)
        }else if (sell.matches(cmd)){
            val spitedCmd = cmd.split(" ")
            val sNum = spitedCmd[2].toInt()
            val stock = getStockByName(db,spitedCmd[1])
            if (stock==null){
                member.group.sendMessage("没有叫做${spitedCmd[1]}的股票")
                return null
            }
            return sellStock(member,sNum, stock)
        }else if (cmd == "行情"){
            update()
            member.group.sendMessage(showStocks())
            return true
        }else if (cmd == "股票持有"){
            member.group.sendMessage(showHold(member))
            return true
        }else if (cmd == "股票测试刷新" && member.id==GroupmanagerHz.pluginConfig.owner){
            updateActive(member)
            return true
        } else if(cmd.startsWith("股票添加")){
            if (member.permission>MemberPermission.MEMBER){
                if (add.matches(cmd)){
                    val split = cmd.split(" ")
                    val name = split[1]
                    val price = split[2].toDouble()
                    administratorAddStock(name,price,member)
                }else{
                    member.group.sendMessage("添加错误，请检查您的输入，发送'$addHelp'了解详细说明")
                }
            }else{
                member.group.sendMessage("您的权限不够, 只有管理员与群主可以添加股票")
            }
            return null
        }else if (cmd == addHelp){
            val em = exampleStockName.random()
            val reply = """
                股票添加 股票名 上市价格
                e.g. 股票添加 $em 5.5
                [股票名] 限定4-5个字符
                [上市价格] (0,10w)内的两位小数
            """.trimIndent()
            member.group.sendMessage(reply)
            return null
        }else{
            return null
        }
    }
    private fun update(){
        val tdl = GroupmanagerHz.todayLine()
        GroupmanagerHz.getDBC(GroupmanagerHz.pluginConfig.bot)
        transaction {
            Stock.all().forEach{
                if (it.updateTime<tdl){
                    val oldPrice = it.price
                    val trendTurn = (1..100).random()
                    if (trendTurn>80){
                        it.trend = !it.trend
                    }
                    val range =
                    if (it.trend){
                        generateDouble() / 100
                    }else{
                        -generateDouble() / 100
                    }
                    val newPrice = oldPrice * (range + 1)
                    if (newPrice > 0.1){
                        it.price = oldPrice * (range + 1)
                    }
                    it.updateTime = GroupmanagerHz.todayStamp()
                }
            }
        }
    }

    /**更新不检查时间戳*/
    private fun updateWithoutCheckingTimeStamp(){
        GroupmanagerHz.getDBC(GroupmanagerHz.pluginConfig.bot)
        transaction {
            Stock.all().forEach{
                val oldPrice = it.price
                val trendTurn = (1..100).random()
                if (trendTurn>80){
                    it.trend = !it.trend
                }
                val range =
                    if (it.trend){
                        generateDouble() / 100
                    }else{
                        -generateDouble() / 100
                    }
                val newPrice = oldPrice * (range + 1)
                if (newPrice > 0.1){
                    it.price = oldPrice * (range + 1)
                }
                it.updateTime = GroupmanagerHz.todayStamp()
            }
        }
    }
    /**手动更新股票*/
    private suspend fun updateActive(sender:Member){
        val mb = MessageChainBuilder()
        val priceFormatter = DecimalFormat.getInstance()
        priceFormatter.maximumFractionDigits = 2
        GroupmanagerHz.getDBC(sender.bot.id)
        transaction {
            val oldStockPrice:ArrayList<Double> = arrayListOf()
            val newStockPrice:ArrayList<Double> = arrayListOf()
            val stockNames = arrayListOf<String>()
            Stock.all().forEach{
                oldStockPrice.add(it.price)
                stockNames.add(it.stName)
            }
            updateWithoutCheckingTimeStamp()
            Stock.all().forEach{
                newStockPrice.add(it.price)
            }
            val updatePercent:List<Double> = List(oldStockPrice.size){
                (newStockPrice[it]-oldStockPrice[it])/oldStockPrice[it]*100.0
            }
            for ((idx,name) in stockNames.withIndex()){
                mb.add("$name ${priceFormatter.format(oldStockPrice[idx])}->${priceFormatter.format(newStockPrice[idx])}(${priceFormatter.format(updatePercent[idx])}%)")
                if (name != stockNames.last()){
                    mb.add("\n")
                }
            }
        }
        sender.group.sendMessage(mb.build())
    }

    private fun generateDouble():Double{
        val integer = (0..4).random().toDouble()
        val float = (0..99).random().toDouble()
        return integer + float / 100.0
    }
    private fun showStocks():String{
        val sb = StringBuilder("机构名称     每股价格\n")
        val nf = NumberFormat.getInstance()
        nf.maximumFractionDigits = 2
        nf.minimumFractionDigits = 1
        GroupmanagerHz.getDBC(GroupmanagerHz.pluginConfig.bot)
        val allDBStock = transaction {
            Stock.all().toList()
        }
        for(s in allDBStock){
            sb.append(s.stName)
            if(s.stName.length==4){
                sb.append("     ")
            }else{
                sb.append("  ")
            }
            sb.append(nf.format(s.price))
            if(s != allDBStock.last()){
                sb.append("\n")
            }
        }
        return sb.toString()
    }

    private fun showHold(member: Member):String{
        val db = GroupmanagerHz.getDBC(member.bot.id)
        val dbMember = DBTools.getMember(db,member.id,member.group.id)?:return ""
        val n = transaction {
            val q = MemberStocks.select {
                MemberStocks.memId eq dbMember.id.value
            }.toList()
            q.size
        }
        val allStock = transaction {
            Stock.all().toList()
        }
        if (n==0) return "没有持有任何股票"
        val sb = StringBuilder("机构名称=可出售/持有=市值\n")
        val rows = arrayListOf<ArrayList<Int>>()
        val names = arrayListOf<String>()
        for ((si,s) in allStock.withIndex()){
            val vs = DBTools.memberStockHold(db,s.id.value,dbMember)
            if (vs[0]!=0){
                rows.add(vs)
                names.add(s.stName)
            }
        }
        for ((i,r) in rows.withIndex()){
            val df = DecimalFormat("0000")
            val df2 = DecimalFormat("0000.00")
            val name = names[i]
            val d0 = df.format(r[0])
            val d1 = df.format(r[1])
//            val d2 = df.format(r[2])
            val price = getStockPrice(name)
            val holdValue = df2.format(price * r[0])
            sb.append(name,"=",d1,"/",d0,"=",holdValue)
            sb.append("\n")
        }
        return sb.toString().trimEnd()
    }

    private fun getStockPrice(stockName: String):Double{
        val db = GroupmanagerHz.getDBC(GroupmanagerHz.pluginConfig.bot)
        return transaction(db) {
            val q = Stock.find {
                Stocks.stName eq stockName
            }
            if (q.empty()){
                -1.0
            }else{
                q.first().price
            }
        }
    }

    private fun getStockByName(db:Database,name:String):Stock?{
        return transaction(db) {
            val q = Stock.find {
                Stocks.stName eq name
            }
            if (q.empty()){
                null
            }else{
                q.first()
            }
        }
    }

    /**在数据库中添加股票*/
    private fun addStock(stockName:String, initValue:Double):Boolean{
        GroupmanagerHz.getDBC(GroupmanagerHz.pluginConfig.bot)
        return try{
            transaction {
                Stock.new {
                    this.stName = stockName
                    this.price = initValue
                    hold = 0
                    updateTime = GroupmanagerHz.todayStamp()
                }
            }
            true
        }catch (e:Exception){
            false
        }
    }
    /**判断数据库种是否存在股票*/
    private fun testStockExists(stockName: String):Boolean{
        GroupmanagerHz.getDBC(GroupmanagerHz.pluginConfig.bot)
        return !transaction {
            val q = Stock.find {
                Stocks.stName eq stockName
            }
            return@transaction q.empty()
        }
    }

    /**管理员添加股票*/
    private suspend fun administratorAddStock(stockName: String, initValue: Double, sender:Member){
        GroupmanagerHz.getDBC(GroupmanagerHz.pluginConfig.bot)
        val mb = MessageChainBuilder()
        transaction {
            try {
                if (!testStockExists(stockName)){
                    addStock(stockName,initValue)
                    mb.add("成功添加股票：${stockName}, 上市价格${initValue}")
                }else{
                    mb.add("已经存在名为${stockName}的股票了！")
                }
            }catch (e:Exception){
                mb.add("出错了，请联系管理员解决此问题")
            }
        }
        sender.group.sendMessage(mb.build())
    }
}