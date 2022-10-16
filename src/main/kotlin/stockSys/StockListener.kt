package org.hezistudio.stockSys

import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.content
import org.hezistudio.GroupmanagerHz
import org.hezistudio.dataBase.DBTools
import org.hezistudio.dataBase.MemberStocks
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.text.DecimalFormat
import java.text.NumberFormat
import kotlin.math.roundToInt

object StockListener:ListenerHost {
    private val stocks:ArrayList<Pair<String,Double>> = arrayListOf(
        Pair("田所药业",10.0),
        Pair("博丽神社",10.0),
        Pair("邪神重工",10.0),
        Pair("圣地鸭歌",10.0),
        Pair("往生堂殡葬",10.0),
    )

    var timeStamp = 0L

    init{
        update()
    }

    @EventHandler
    suspend fun onEvent(e:GroupMessageEvent){
        parseCmd(e.sender, e.message.content)
    }

    private suspend fun buyStock(member:Member, num:Int, stockIndex:Int):Boolean{
        val db = GroupmanagerHz.getDBC(member.bot.id)
        val dbMember = DBTools.getMember(db,member.id,member.group.id)?:return false
        if (num%100!=0){
            member.group.sendMessage("购买股票数必须是100的整数倍")
            return false
        }
        val targetStock = stocks[stockIndex]
        val cost = (targetStock.second * num).roundToInt()
        if (dbMember.fraction < cost){
            member.group.sendMessage("您的积分不够")
            return false
        }
        return if(DBTools.memberBuyStock(db,member,stockIndex,num)){
            transaction {
                dbMember.fraction -= cost
            }
            member.group.sendMessage("成功购买${targetStock.first}${num}股，花费${cost}积分")
            true
        }else{
            member.group.sendMessage("因为未知原因，购买失败")
            false
        }
    }

    private suspend fun sellStock(member: Member, num:Int, stockIndex: Int):Boolean{
        val db = GroupmanagerHz.getDBC(member.bot.id)
        val dbMember = DBTools.getMember(db,member.id,member.group.id)?:return false
        if (num%100!=0){
            member.group.sendMessage("出售股票数必须是100的整数倍")
            return false
        }
        val targetStock = stocks[stockIndex]
        val cost = (targetStock.second * num).roundToInt()
        val canSellStock = DBTools.canSellStock(db,stockIndex,dbMember)
        if (canSellStock<num){
            member.group.sendMessage("你没有持有足够的可出售股票")
            return false
        }
        return if(DBTools.memberSellStock(db,member,stockIndex,num)){
            transaction {
                dbMember.fraction += cost
            }
            member.group.sendMessage("成功出售${targetStock.first}${num}股，获得${cost}积分")
            true
        }else{
            member.group.sendMessage("因为未知原因，出售失败")
            false
        }
    }

    private suspend fun parseCmd(member: Member, cmd:String):Boolean?{
        val buy = Regex( """股票购买 \S+ [0-9]+""")
        val sell = Regex("""股票出售 \S+ [0-9]+""")
        if (buy.matches(cmd)){
            val spitedCmd = cmd.split(" ")
            val stockIndex = indexOfStock(spitedCmd[1])
            val sNum = spitedCmd[2].toInt()
            if (sNum<=0) return null
            if (stockIndex == -1) return null
            return buyStock(member,sNum, stockIndex)
        }else if (sell.matches(cmd)){
            val spitedCmd = cmd.split(" ")
            val stockIndex = indexOfStock(spitedCmd[1])
            val sNum = spitedCmd[2].toInt()
            if (sNum<=0) return null
            if (stockIndex == -1) return null
            return sellStock(member,sNum, stockIndex)
        }else if (cmd == "行情"){
            update()
            member.group.sendMessage(showStocks())
            return true
        }else if (cmd == "股票持有"){
            member.group.sendMessage(showHold(member))
            return true
        } else{
            return null
        }
    }

    private fun indexOfStock(stockName:String):Int{
        for ((i,s) in stocks.withIndex()){
            if (stockName == s.first){
                return i
            }
        }
        return -1
    }

    private fun update(){
        val tdl = GroupmanagerHz.todayLine()
        if (timeStamp<tdl){
            timeStamp = GroupmanagerHz.todayStamp()
        }else{
            return
        }
        for (i in stocks.indices){
            val name = stocks[i].first
            val price = stocks[i].second
            val range = generateDouble() / 100
            val newPrice = price * (range + 1)
            stocks[i] = Pair(name,newPrice)
        }
    }

    private fun generateDouble():Double{
        val integer = (0..4).random().toDouble()
        val float = (0..99).random().toDouble()
        val i = if (Math.random() > 0) -1 else 1
        return (integer + float / 100.0) * i
    }

    private fun showStocks():String{
        val sb = StringBuilder("机构名称     每股价格\n")
        val nf = NumberFormat.getInstance()
        nf.maximumFractionDigits = 2
        nf.minimumFractionDigits = 1
        for (s in stocks){
            sb.append(s.first)
            if (s.first.length==4){
                sb.append("     ")
            }else{
                sb.append("  ")
            }

            sb.append(nf.format(s.second))
            if(s != stocks.last()){
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
                MemberStocks.memId eq dbMember.id
            }.toList()
            q.size
        }
        if (n==0) return "没有持有任何股票"
        val sb = StringBuilder("机构名称====持有==可出售==不可出售\n")
        val rows = arrayListOf<ArrayList<Int>>()
        val names = arrayListOf<String>()
        for ((si,s) in stocks.withIndex()){
            val vs = DBTools.memberStockHold(db,si,dbMember)
            if (vs[0]!=0){
                rows.add(vs)
                names.add(s.first)
            }
        }
        for ((i,r) in rows.withIndex()){
//            val df = DecimalFormat.getInstance()
            val df = DecimalFormat("0000")
            val name = if (names[i].length == 4){
                names[i]+"____"
            }else{
                names[i]+"__"
            }
            val d0 = df.format(r[0])
            val d1 = df.format(r[1])
            val d2 = df.format(r[2])
            sb.append(name,"__",d0,"____",d1,"____",d2)
            if (r != rows.last()){
                sb.append("\n")
            }
        }
        return sb.toString()
    }

}