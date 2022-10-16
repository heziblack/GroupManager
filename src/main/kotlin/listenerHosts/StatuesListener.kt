package org.hezistudio.listenerHosts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.content
import org.hezistudio.GroupmanagerHz
import org.hezistudio.dataBase.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object StatuesListener:ListenerHost {
    @EventHandler
    suspend fun onEvent(e:GroupMessageEvent){
        val msg = e.message.content
        if (msg == "角色卡"){
            val img = characterCard(e.sender)
            if (img!=null){
                val outStream = ByteArrayOutputStream()
                withContext(Dispatchers.IO) {
                    ImageIO.write(img, "jpg", outStream)
                }
                val inStream = outStream.toByteArray()
                val ii = ByteArrayInputStream(inStream)
                e.group.sendImage(ii)
            }else{
                val outMsg = userStatues(e.sender)
                if (outMsg != "") {
                    e.group.sendMessage(outMsg)
                }
            }
        }else if (msg == "查询积分" || msg == "积分查询"){
            val outMsg = userFraction(e.sender)
            if (outMsg != "") {
                e.group.sendMessage(outMsg)
            }
        }
    }

    private fun userStatues(user:Member):String{
        val dbc = GroupmanagerHz.getDBC(user.bot.id)
        val sb = StringBuilder()
        val dbMember = DBTools.getMember(dbc,user.id,user.group.id)?:return ""
        val gender = if (dbMember.gender) {"男"} else {"女"}
        val outString = """
            ${dbMember.nickName} 性别：$gender
            积分：${dbMember.fraction}
            攻\防\力：${dbMember.attack}\${dbMember.defence}\${dbMember.strength}
        """.trimIndent()
        sb.append(outString)
        return sb.toString()
    }

    private fun userFraction(user: Member):String{
        val dbc = GroupmanagerHz.getDBC(user.bot.id)
        val dbMember = DBTools.getMember(dbc,user.id,user.group.id)?:return ""
        return "您当前的积分为：${dbMember.fraction}"
    }

    private fun characterCard(user: Member):BufferedImage?{
        val dbc = GroupmanagerHz.getDBC(user.bot.id)
        val sb = StringBuilder()
        val dbMember = DBTools.getMember(dbc,user.id,user.group.id)?:return null
        val imgStream = GroupmanagerHz.getImageResStream("images/CharacterCardBase.jpg")?:return null
        val img = ImageIO.read(imgStream)
        imgStream.close()
        val g2d = img.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON)

        // 写玩家昵称
        val fontForNick = Font("华文新魏", Font.PLAIN,72)
        val normalFontColor = Color(55,30,30)
        g2d.font = fontForNick
        g2d.color= Color.WHITE
        g2d.drawString(dbMember.nickName,41,72+28)

        val ppt:List<Double> = listOf(dbMember.attack,dbMember.defence,dbMember.strength)
        val biggest = biggestDoubleAt(ppt)
        val commonX = 140.0
        val startY = 420.0
        val testW = 200.0

        val rrtg:List<Pair<Double,Double>> = listOf(
            Pair(startY,testW),
            Pair(startY+42.0,testW),
            Pair(startY+83.0,testW),
        )

        g2d.color = Color.LIGHT_GRAY
        for (i in rrtg){
            val g = generateRoundRectangle(commonX,i.first,i.second)
            g2d.fill(g)
        }
        // 绘制进度条
        if(biggest<100.0 && biggest > 10.0){

        }else{

        }
//        val roundRectangle = generateRoundRectangle()
//        g2d.draw(roundRectangle)
//        g2d.fill(roundRectangle)

        // 写玩家积分等
        g2d.font = Font("华文隶书",Font.PLAIN,36)
        g2d.color = normalFontColor
        var ah = g2d.fontMetrics.ascent
        g2d.drawString("${dbMember.fraction}",140,154+ah)
        g2d.drawString("${dbMember.attack}",144,418+ah)
        g2d.drawString("${dbMember.defence}",144,460+ah)
        g2d.drawString("${dbMember.strength}",144,502+ah)
//        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON)
        // 签到信息
        val signIn = transaction {
            val q1 = MemberSignIn.find { MemberSignIns.member eq dbMember.id }
            if (q1.empty()){
                null
            }else{
                val stp = q1.last().timeStamp.toLong()
                val td = GroupmanagerHz.todayLine()
                if (stp>=td){
                    stp
                }else{
                    null
                }
            }
        }

        // 击剑信息
        val jijianCounter = transaction {
            val q = Jijian.find(JiJians.member eq dbMember.id)
            if (q.empty()){
                0
            }else{
                val stp = q.last().timeStamp
                val td = GroupmanagerHz.todayLine()
                if (stp>=td){
                    q.last().counter
                }else{
                    0
                }
            }
        }

        // 写击剑信息
        g2d.drawString("$jijianCounter",770,330+ah)

        // 写签到信息
        g2d.color = Color(201,39,39)
        if (signIn==null){
            g2d.font = Font("华文行楷",Font.PLAIN,60)
            ah = g2d.fontMetrics.ascent
            g2d.drawString("今日未签到",621,452+ah)
        }else{
            g2d.font = Font("Bradley Hand ITC",Font.PLAIN,60)
            ah = g2d.fontMetrics.ascent
            g2d.drawString("$signIn",595,446+ah)
        }

        // 写玩家性别
        val gender = if(dbMember.gender) "♂" else "♀"
        g2d.font = Font("华文彩云",Font.PLAIN,32)
        g2d.color = if(dbMember.gender)
            Color(0,255,255)
        else
            Color(255,91,130)
        ah = g2d.fontMetrics.ascent
        g2d.drawString(gender,48,105+ah)
        g2d.dispose()
        return img
    }
    /**生成指定宽度的圆角矩形*/
    private fun generateRoundRectangle(x:Double, y:Double, w:Double):RoundRectangle2D.Double{
        return RoundRectangle2D.Double(x,y,w,35.0,35.0,35.0)
    }

    private fun biggestDoubleAt(list: List<Double>):Int{
        var idx = 0
        for (index in list.indices){
            if (list[index]>list[idx]){
                idx = index
            }
        }
        return idx
    }

}