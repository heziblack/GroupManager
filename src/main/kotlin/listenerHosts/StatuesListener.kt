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
import java.text.DecimalFormat
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
        }else if (msg =="功能需求"){
            e.group.sendMessage("https://docs.qq.com/form/page/DRGR1c2djZmpmWGF3")
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
        val rank = DBTools.fractionRank(dbMember)
        return "您当前的积分为：${dbMember.fraction}，排名：${rank}"
    }

    private fun characterCard(user: Member):BufferedImage?{
        val dbc = GroupmanagerHz.getDBC(user.bot.id)
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

        val commonX = 140.0
        /**圆角矩形生成数据
         * 1-垂直位置
         * 2-宽度 */
        val rrtg:List<Pair<Double,Double>> = prepareProgressBar(dbMember)
        g2d.color = Color.LIGHT_GRAY
        val colorsForPB:List<Color> = listOf(Color(255,0,0), Color(0,0,255),Color(0,255,0))
        for ((idx,i) in rrtg.withIndex()){
            if (i.second!=0.0){
                g2d.color = colorsForPB[idx]
                val g = generateRoundRectangle(commonX,i.first,i.second)
                g2d.fill(g)
            }
        }
        // 绘制进度条

        // 写玩家积分等
        g2d.font = Font("华文隶书",Font.PLAIN,36)
        g2d.color = normalFontColor
        var ah = g2d.fontMetrics.ascent
        val dft = DecimalFormat.getInstance()
        dft.maximumFractionDigits = 2
        dft.minimumFractionDigits = 0
        dft.format(dbMember.attack)
        g2d.drawString("${dbMember.fraction} #${DBTools.fractionRank(dbMember)}",140,154+ah)
        g2d.drawString(dft.format(dbMember.attack),144,418+ah)
        g2d.drawString(dft.format(dbMember.defence),144,460+ah)
        g2d.drawString(dft.format(dbMember.strength),144,502+ah)
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


    private fun prepareProgressBar(dbMember: org.hezistudio.dataBase.Member):List<Pair<Double,Double>>{
        val origin:List<Double> = listOf(dbMember.attack,dbMember.defence,dbMember.strength)
        val graphicLen = 400.0
        val offSetHeight = 420.0
        val heights = listOf<Double>(0.0,42.0,83.0)
        val biggestIndex = biggestDoubleAt(origin)
        val biggestValue = origin[biggestIndex]
        val lenClass = if ( biggestValue>0.0 && biggestValue<100.0 ){
            100.0
        }else{
            0.0
        }
        val data:ArrayList<Pair<Double,Double>> = arrayListOf()
        if (lenClass!=0.0){
            val percentOfAll = listOf<Double>(
                origin[0]/lenClass, origin[1]/lenClass, origin[2]/lenClass
            )
            for ((idx,percent) in percentOfAll.withIndex()){
                if (percent>0.05){
                    data.add(Pair(offSetHeight+heights[idx],percent*graphicLen))
                }else{
                    data.add(Pair(offSetHeight+heights[idx],0.0))
                }
            }
            return data
        }else{
            for (idx in origin.indices){
                data.add(Pair(offSetHeight+heights[idx],0.0))
            }
            return data
        }
    }
}