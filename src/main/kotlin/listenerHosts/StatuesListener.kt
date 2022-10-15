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
//        g2d.fontMetrics
        g2d.font = Font("楷体",Font.PLAIN,36)
        g2d.color = Color.BLACK
//        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        g2d.drawString(dbMember.nickName,200,98)
        val gender = if(dbMember.gender) "男" else "女"
        g2d.drawString(gender,200,170)
        g2d.drawString("${dbMember.fraction}",200,250)
        g2d.drawString("${dbMember.attack}",200,330)
        g2d.drawString("${dbMember.defence}",200,400)
        g2d.drawString("${dbMember.strength}",200,480)
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
        g2d.drawString("$jijianCounter",750,100)
        if (signIn==null){
            g2d.drawString("今日未签到",600,500)
        }else{
            g2d.drawString("$signIn",600,500)
        }
        g2d.dispose()
        return img
    }



}