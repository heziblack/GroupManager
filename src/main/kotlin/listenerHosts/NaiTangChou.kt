package org.hezistudio.listenerHosts

import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.content
import org.hezistudio.GroupmanagerHz
import org.hezistudio.dataBase.DBTools
import org.hezistudio.dataBase.GroupMembers
import org.hezistudio.dataBase.MemberSignIn
import org.hezistudio.dataBase.MemberSignIns
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

object NaiTangChou:ListenerHost {
    const val NatTangQQ1:Long = 3103889391L
    @EventHandler
    suspend fun onEvent(e:GroupMessageEvent){
        if(e.sender.id == NatTangQQ1) return
        if (e.message.content == "乃糖筹"){
            val mb = MessageChainBuilder()
            val db = GroupmanagerHz.getDBC(GroupmanagerHz.pluginConfig.bot)
            if((1..10).random() <= 3){
                donateOtherPlayer(e.sender,db, mb)
                e.group.sendMessage(mb.build())
                return
            }
//            val db = GroupmanagerHz.getDBC(GroupmanagerHz.pluginConfig.bot)
            val addFra = (5..20).random()
            var ifSend = false
            transaction {
                val NaitangDBEntity = DBTools.getMember(db, NatTangQQ1,e.group.id)?:return@transaction
                ifSend = true
                val sender = DBTools.getMemberOrCreate(db,e.sender)
                val q = MemberSignIn.find {
                    MemberSignIns.member eq sender.id.value
                }
                if (q.empty()){
                    MemberSignIn.new {
                        member = sender
                        timeStamp = GroupmanagerHz.todayStamp().toString()
                    }
                    mb.add("使用你的第一次签到次数给奶糖助力！奶糖积分+${addFra}")
                    NaitangDBEntity.fraction+=addFra
                }else{
                    val last = q.last()
                    if (last.timeStamp.toLong()>GroupmanagerHz.todayLine()){
                        mb.add("今日已经签到，无法助力奶糖哦，请明天再来！")
                    }else{
                        MemberSignIn.new {
                            member = sender
                            timeStamp = GroupmanagerHz.todayStamp().toString()
                        }
                        mb.add("使用签到次数给奶糖助力！奶糖积分+${addFra}")
                        NaitangDBEntity.fraction+=addFra
                    }
                }

            }
            if (ifSend){
                e.group.sendMessage(mb.build())
            }
        }
    }

    private fun donateOtherPlayer(member: Member, db:Database, mb:MessageChainBuilder){
        transaction(db) {
            val memberHasSignIn = DBTools.ifMemberSignIn(member)
            if (memberHasSignIn){
                mb.add("今日已经签到，无法助力任何人了哦，请明天再来！")
                return@transaction
            }
            val underFractions = org.hezistudio.dataBase.Member.find {
                GroupMembers.fraction less 0
            }.toList()
            if (underFractions.isEmpty()){
                mb.add("在大家的帮助下，已经没有需要帮助的群友啦~")
                return@transaction
            }else{
                val donatedPlayer = underFractions.random()
                val addFrac = (5..20).random()
                donatedPlayer.fraction += addFrac
                val sender = DBTools.getMemberOrCreate(db,member)
                MemberSignIn.new {
                    this.member = sender
                    timeStamp = GroupmanagerHz.todayStamp().toString()
                }
                val ta = if (donatedPlayer.gender){
                    "他"
                }else{
                    "她"
                }
                mb.add("使用签到次数给${donatedPlayer.nickName}助力！${ta}的积分+${addFrac}")
            }
        }
    }

}