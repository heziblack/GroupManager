package org.hezistudio.itemSys

import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.message.data.findIsInstance
import org.hezistudio.GroupmanagerHz
import org.hezistudio.dataBase.DBTools
import org.hezistudio.dataBase.MemberItem
import org.hezistudio.dataBase.MemberItems
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.name
import org.jetbrains.exposed.sql.transactions.transaction

object ListenerOfItemSys:ListenerHost {
    /**道具系统的相关功能实现：
     *
     * 道具商店
     *
     * 使用道具
     *
     * 出售道具
     *
     * 购买道具*/
    @EventHandler
    suspend fun onEvent(e:GroupMessageEvent){
        val func = cmdParser(e.message)?:return
        when(func){
            Functions.Store -> {
                GroupmanagerHz.logger.info("显示道具商店")
                e.group.sendMessage(itemStore())
            }
            Functions.Use -> {
                GroupmanagerHz.logger.info("使用道具")
                val msgContent = e.message.content
                val p = msgContent.indexOf("使用")
                val itemName = msgContent.substring(p+2).trim()
                val db = GroupmanagerHz.getDBC(e.bot.id)
                val dbItem = DBTools.getItem(db,itemName)
                if (dbItem==null){
                    e.group.sendMessage("我们没有这种东西哦")
                    return
                }
                val dbUser = DBTools.getMember(db,e.sender.id,e.group.id)?:return
                if (DBTools.memberHasItem(db, dbUser, dbItem)){
                    val item = BuildInItemList.get(itemName)?:return
                    if (item.target == ItemEntity.ItemTarget.Self){
                        val useResult = item.active(GroupmanagerHz.getDBC(e.bot.id),e.sender)
                        if (useResult && item.canBeConsumed){
                            val memberItem = DBTools.getMemberItem(db,dbUser,dbItem)!!
                            DBTools.deleteMemberItem(db,memberItem)
                        }
                    }else{
                        e.group.sendMessage("${item.name}只能对自己使用哦")
                    }
                }else{
                    e.group.sendMessage("你没有道具${itemName}")
                }
            }
            Functions.UseTo -> {
                GroupmanagerHz.logger.info("向对象使用道具")
                val target = atTarget(e.message)?:return
                val msgContent = e.message.content
                val p = msgContent.indexOf("使用")
                val itemName = msgContent.substring(p+2).trim()
                val db = GroupmanagerHz.getDBC(e.bot.id)
                val dbItem = DBTools.getItem(db,itemName)
                if (dbItem==null){
                    e.group.sendMessage("我们没有这种东西哦")
                    return
                }
                val dbUser = DBTools.getMember(db,e.sender.id,e.group.id)?:return
                if (DBTools.memberHasItem(db,dbUser,dbItem)){
                    val item = BuildInItemList.get(itemName)?:return
                    if (item.target == ItemEntity.ItemTarget.Someone){
                        val useResult = item.active(GroupmanagerHz.getDBC(e.bot.id),e.sender,target.target.toString())
                        if (useResult){
                            val memberItem = DBTools.getMemberItem(db,dbUser,dbItem)!!
                            DBTools.deleteMemberItem(db,memberItem)
                        }
                    }else{
                        e.group.sendMessage("${item.name}只能对他人使用哦")
                    }
                }else{
                    e.group.sendMessage("你没有道具${itemName}")
                }
            }
            Functions.Buy -> {
                GroupmanagerHz.logger.info("购买道具")
                val msgContent = e.message.content
                val p = msgContent.indexOf("购买")
                val itemName = msgContent.substring(p+2).trim()
                val db = GroupmanagerHz.getDBC(e.bot.id)
                val dbItem = DBTools.getItem(db,itemName)
                if (dbItem==null){
                    e.group.sendMessage("我们没有这种东西哦")
                    return
                }
                val dbUser = DBTools.getMember(db,e.sender.id,e.group.id)?:return
                val itemEntity = BuildInItemList.get(itemName)?:return
                if (!itemEntity.canBuy){
                    e.group.sendMessage("${itemEntity.name}不可以购买")
                    return
                }
                val itemPrice = itemEntity.inPrice
                if (itemPrice == 0){
                    DBTools.insertMemberItem(db, dbItem, dbUser)
                    e.group.sendMessage("购买成功，花费${itemPrice}")
                } else if (dbUser.fraction >= itemPrice){
                    DBTools.insertMemberItem(db, dbItem, dbUser)
                    DBTools.memberCost(db,itemPrice,dbUser)
                    e.group.sendMessage("购买成功，花费${itemPrice}")
                }else{
                    e.group.sendMessage("购买失败，你没有足够的积分\n(需要${itemPrice}，持有${dbUser.fraction})")
                }
            }
            Functions.Sell -> {
                GroupmanagerHz.logger.info("出售道具")
                val msgContent = e.message.content
                val p = msgContent.indexOf("出售")
                val itemName = msgContent.substring(p+2).trim()
                val db = GroupmanagerHz.getDBC(e.bot.id)
                val dbItem = DBTools.getItem(db,itemName)
                if (dbItem==null){
                    e.group.sendMessage("我们没有这种东西哦")
                    return
                }
                val dbUser = DBTools.getMember(db,e.sender.id,e.group.id)?:return
                val itemEntity = BuildInItemList.get(itemName)?:return
                if (!itemEntity.canSell){
                    e.group.sendMessage("${itemEntity.name}不可以出售")
                    return
                }
                val memberItem = DBTools.getMemberItem(db,dbUser,dbItem)
                if (memberItem == null){
                    e.group.sendMessage("未持有${itemEntity.name}")
                    return
                }
                val itemPrice = itemEntity.outPrice
                DBTools.deleteMemberItem(db, memberItem)
                DBTools.memberAdd(db,itemPrice,dbUser)
                e.group.sendMessage("出售${itemEntity.name}，获得${itemPrice}")
            }
            Functions.Storage -> {
                e.group.sendMessage(storage(GroupmanagerHz.getDBC(e.bot.id),e.sender.id,e.group.id))
            }
        }
    }

    // 道具系统行为
    private enum class Functions(cnName:String){
        Store("道具商店"),
        Use("使用道具"),
        UseTo("对他人使用道具"),
        Buy("购买道具"),
        Sell("出售道具"),
        Storage("背包"),
    }

    // 解析消息为行为(check)
    private fun cmdParser(msg: MessageChain):Functions?{
        val useItem = Regex("""使用 ?\S+""")
        val buyItem = Regex("""购买 ?\S+""")
        val sellItem = Regex("""出售 ?\S+""")
        val useToItem = Regex("""对 ?@[0-9]+ ?使用 ?\S+""")
        println(msg.content)
        return if(msg.content == "道具商店"){
            Functions.Store
        }else if(msg.content == "背包"){
            Functions.Storage
        } else if (msg.findIsInstance<At>() != null && useToItem.matches(msg.content)){
            Functions.UseTo
        }else if(useItem.matches(msg.content)){
            Functions.Use
        }else if(buyItem.matches(msg.content)){
            Functions.Buy
        }else if (sellItem.matches(msg.content)) {
            Functions.Sell
        } else{
            null
        }
    }

    // 道具商店
    private fun itemStore():String{
        val strBuilder = StringBuilder("======道具商店======")
        strBuilder.append("\n       道具名          价格")
        val sortedItems = BuildInItemList.sortByPrice()
        for(item in sortedItems){
            if (item.canBuy){
                val lOfItemName = item.name.length * 2
                val lOfItemPrice = when(item.inPrice){
                    0 -> 4
                    in (1..9) -> 1
                    in (10..99) -> 2
                    in (100..999) -> 3
                    in (1000..9999) -> 4
                    else -> 5
                }
                val space = 30 - lOfItemName - lOfItemPrice
                if (space<=0){
                    strBuilder.append("\n${item.name} ${item.inPrice}")
                }else{
                    strBuilder.append("\n")
                    val startSpace = (20 - lOfItemName) / 2
                    val midSpace2 = (10 - lOfItemPrice) / 2 + (10 - lOfItemPrice) % 2
                    val midSpace = startSpace + midSpace2
                    for (i in (1..startSpace)){
                        strBuilder.append(" ")
                    }
                    strBuilder.append(item.name)
                    for (i in (1..midSpace)){
                        strBuilder.append(" ")
                    }
                    if (item.inPrice == 0){
                        strBuilder.append("免费")
                    }else{
                        strBuilder.append(item.inPrice)
                    }
                }
            }
        }
        return strBuilder.toString()
    }

    // 从消息中获取at对象
    private fun atTarget(msg: MessageChain):At?{
        for (sMsg in msg){
            if (sMsg is At){
                return sMsg
            }
        }
        return null
    }

    // 背包
    private fun storage(db:Database,memID:Long, gID:Long):String{
        db.name
        val strBuilder = StringBuilder()
        transaction {
            val dbMember = DBTools.getMember(db,memID,gID)
            if (dbMember==null) {
                strBuilder.append("出现错误！")
                return@transaction
            }
            val itemList = MemberItem.find {
                MemberItems.member eq dbMember.id
            }
            if (itemList.empty()){
                strBuilder.append("无物品")
                return@transaction
            }
            val map:MutableMap<String,Int> = mutableMapOf()
            itemList.forEach {
                val itemName = it.item.name
                if (itemName in map.keys){
                    val vom = map[itemName]?:0
                    map[itemName] = vom+1
                }else{
                    map[itemName] = 1
                }
            }
            for (ii in map){
                strBuilder.append(ii.key+"(${ii.value})")
            }
        }
        return strBuilder.toString()
    }
}