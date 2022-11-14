package org.hezistudio.dataBase

import net.mamoe.mirai.contact.nameCardOrNick
import org.jetbrains.exposed.sql.transactions.transaction
import org.hezistudio.GroupmanagerHz
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object DBTools {

    private val defaultGroupID:Long
        get() = GroupmanagerHz.pluginConfig.group

    val db:Database
        get() = GroupmanagerHz.getDBC(GroupmanagerHz.pluginConfig.bot)

    /**获取群员在数据库中的数据对象
     * @return 数据对象, 若未找到返回空*/
    fun getMember(botID:Long, userID:Long, groupID:Long):Member?{
        val db = GroupmanagerHz.getDBC(botID)
        return getMember(db, userID, groupID)
    }

    /**获取群员的数据库对象
     * @return 数据库对象，若未找到则返回空**/
    fun getMember(db: Database, userID: Long, groupID: Long):Member?{
        return transaction {
            val u = User.find { Users.userNum eq userID }.toList()
            val g = Group.find { Groups.number eq groupID }.toList()
            if (u.isEmpty() || g.isEmpty()) return@transaction null else {
                val user = u[0]
                val group = g[0]
                val mems = Member.find { (GroupMembers.member eq user.id) and (GroupMembers.group eq group.id) }.toList()
                return@transaction if (mems.isEmpty()){
                    null
                }else{
                    mems[0]
                }
            }
        }
    }

    fun getMemberOrCreate(db: Database,mem:net.mamoe.mirai.contact.Member):Member{
        val dbMember = getMember(db,mem.id,mem.group.id)
        if (dbMember!=null){
            return dbMember
        }else{
            return transaction {
                val uq = User.find(Users.userNum eq mem.id)
                val user = if (uq.empty()){
                    User.new {
                        userNum = mem.id
                    }
                }else{
                    uq.first()
                }
                val gq = Group.find(Groups.number eq mem.group.id)
                val group = if (gq.empty()){
                    Group.new {
                        number = mem.group.id
                        isWorking = true
                    }
                }else{
                    gq.first()
                }
                Member.new {
                    nickName = mem.nameCardOrNick
                    member = user
                    this.group = group
                }
            }
        }

    }

    /**获取数据库道具对象*/
    fun getItem(db: Database, itemName:String):Item?{
        return transaction {
            val iList = Item.find(Items.name eq itemName)
            if (iList.empty()) return@transaction null
            else return@transaction iList.first()
        }
    }

    /**某人是否持有道具*/
    fun memberHasItem(db: Database,member: Member, item: Item):Boolean{
        println("${member.id}-${item.id}")
        return transaction {
            val items = MemberItem.all().find {
                it.item.id == item.id && it.member.id == member.id
            }
            return@transaction items != null
        }
    }

    /**获取道具持有记录*/
    fun getMemberItem(db: Database, member: Member, item: Item):MemberItem?{
        return transaction {
            val memItem = MemberItem.find {
                (MemberItems.member eq member.id) and (MemberItems.item eq item.id)
            }
            if (memItem.empty()) null else memItem.first()
        }
    }

    /**删除道具持有记录*/
    fun deleteMemberItem(db: Database, memItem: MemberItem){
        transaction {
            memItem.delete()
        }
    }

    /**添加道具持有记录*/
    fun insertMemberItem(db: Database, item:Item, mem:Member){
        transaction {
            MemberItem.new {
                member = mem
                this.item = item
            }
        }
    }

    fun memberCost(db:Database, fraction:Int, mem: Member){
        transaction {
            mem.fraction -= fraction
        }
    }

    fun memberAdd(db:Database,fraction: Int,mem: Member){
        memberCost(db,-fraction,mem)
    }
/**尝试添加新成员*/
    fun addGroupMember(mem:net.mamoe.mirai.contact.Member){
        val db = GroupmanagerHz.getDBC(GroupmanagerHz.pluginConfig.bot)
        transaction(db) {
            val q0 = Groups.select(Groups.number eq mem.group.id)
            val gid = if (q0.empty()){
                val g = Group.new {
                    number = mem.group.id
                    isWorking = true
                }
                g.id
            }else{
                q0.first()[Groups.id]
            }
            val q1 = Users.select(Users.userNum eq mem.id)
            val uid = if (q1.empty()){
                val u = User.new {
                    userNum = mem.id
                }
                u.id
            }else{
                q1.first()[Users.id]
            }
            val q2 = GroupMembers.innerJoin(Groups).innerJoin(Users).select {
                (Users.userNum eq mem.id) and (Groups.number eq mem.group.id)
            }
            if (q2.empty()){
                Member.new {
                    member = User[uid]
                    group = Group(gid)
                    nickName = mem.nameCardOrNick
                    gender = true
                    health = 100.0
                    fraction = 0
                    attack = 10.0
                    strength = 10.0
                    defence = 10.0
                }
            }
        }
    }


    fun memberBuyStock(db: Database,mem: net.mamoe.mirai.contact.Member,stockId:Int,num:Int):Boolean{
        val dbMember = getMember(db,mem.id,mem.group.id)?:return false
        transaction {
            SchemaUtils.create(MemberStocks)
            MemberStocks.insert {
                it[stId] = stockId
                it[memId] = dbMember.id
                it[hold] = num
                it[ts] = GroupmanagerHz.todayStamp()
            }
        }
        return true
    }

    fun memberSellStock(db: Database,mem: net.mamoe.mirai.contact.Member,stockId:Int,num:Int):Boolean{
        val dbMember = getMember(db,mem.id,mem.group.id)?:return false
        return transaction {
            SchemaUtils.create(MemberStocks)
            val q = MemberStocks.select {
                val a = MemberStocks.stId eq stockId
                val b = MemberStocks.memId eq dbMember.id
                a and b
            }
            if (q.empty()) return@transaction false
            val rowList = q.toList()
            var allCanSell = 0
            val tdl = GroupmanagerHz.todayLine()
            q.forEach {
                if (it[MemberStocks.ts] < tdl){
                    allCanSell += it[MemberStocks.hold]
                }
            }
            if (allCanSell<num) return@transaction false
            val soldID:ArrayList<Long> = arrayListOf()
            var rest = num
            rowList.forEach {
                if (it[MemberStocks.ts] < tdl){
                    if (rest >= it[MemberStocks.hold]){
                        rest -= it[MemberStocks.hold]
                        soldID.add(it[MemberStocks.id].value)
                    }
                }
            }
            for (i in soldID){
                deleteMemberStock(db,i)
            }
            if (rest>0){
                val q2 = MemberStocks.select {
                    val a = MemberStocks.stId eq stockId
                    val b = MemberStocks.memId eq dbMember.id
                    val c = MemberStocks.ts less tdl
                    a and b and c
                }
                q2.forEach{
                    if (rest > 0 && it[MemberStocks.hold] >= rest){
                        val holds = it[MemberStocks.hold]
                        val msId = it[MemberStocks.id]
                        if (holds>rest){
                            updateMemberStock(db,msId.value,holds - rest)
                        }else{
                            deleteMemberStock(db,msId.value)
                        }
                        rest = 0
                    }
                }
            }
            true
        }
    }

    private fun deleteMemberStock(db:Database,id:Long){
        transaction {
            MemberStocks.deleteWhere{
                MemberStocks.id eq id
            }
        }
    }

    private fun updateMemberStock(db:Database,id:Long, nV:Int){
        transaction {
            MemberStocks.update({ MemberStocks.id eq id }){
                it[hold] = nV
            }
        }
    }

    fun canSellStock(db:Database, sId:Int, dbMember:Member):Int{
        var canSell = 0
        transaction {
            MemberStocks.select {
                val a = MemberStocks.stId eq sId
                val b = MemberStocks.memId eq dbMember.id
                val c = MemberStocks.ts less GroupmanagerHz.todayLine()
                a and b and c
            }.forEach {
                canSell += it[MemberStocks.hold]
            }
        }
        return canSell
    }

    fun memberStockHold(db:Database, stockId: Int, dbMember: Member):ArrayList<Int>{
        val list:ArrayList<Int> = arrayListOf()
        //持仓 可出售 不可出售
        transaction {
            val all = MemberStocks.select {
                val a = MemberStocks.stId eq stockId
                val b = MemberStocks.memId eq dbMember.id
                a and b
            }
            var allHold = 0
            var canSell = 0
            var cannotSell = 0
            val tdl = GroupmanagerHz.todayLine()
            all.forEach {
                allHold += it[MemberStocks.hold]
                if (it[MemberStocks.ts]<tdl){
                    canSell += it[MemberStocks.hold]
                }else{
                    cannotSell += it[MemberStocks.hold]
                }
            }
            list.addAll(arrayListOf<Int>(allHold,canSell,cannotSell))
        }
        return list
    }
    fun fractionRank(dbMember:Member):Int{
        dbMember.db
        return transaction {
            val q = Member.all().sortedByDescending { it.fraction }
            var rank = -1
            for ((idx,member) in q.withIndex()){
                GroupmanagerHz.logger.info("$idx ${member.nickName}:${member.fraction}")
                if (member.id == dbMember.id){
                    rank = idx+1
                    break
                }
            }
            rank
        }
    }
    /**获取数字属性*/
    fun getNumericProp(dbMem: Member,propName:String,default:Double):MemberExProp{
        dbMem.db
         return transaction {
            val q = MemberExProp.find {
                (ExProps.mem eq dbMem.id) and (ExProps.exPropName eq propName)
            }
            if (q.empty()){
                MemberExProp.new {
                    member = dbMem
                    exPropName = propName
                    exPropValue = default
                }
            }else{
                q.first()
            }
        }
    }
    /**判断成员是否今日签到*/
    fun ifMemberSignIn(mem: net.mamoe.mirai.contact.Member):Boolean{
        val db = GroupmanagerHz.getDBC(GroupmanagerHz.pluginConfig.bot)
        return transaction {
            val target = getMemberOrCreate(db,mem)
            val q = MemberSignIn.find{
                MemberSignIns.member eq target.id
            }
            if (q.empty()){
                return@transaction false
            }else{
                val lastRow = q.last()
                return@transaction lastRow.timeStamp.toLong()>GroupmanagerHz.todayLine()
            }
        }

    }
    /***/
    fun memberSignIn(mem: net.mamoe.mirai.contact.Member){
        transaction(db) {
            if (!ifMemberSignIn(mem)){
                val target = getMemberOrCreate(db,mem)
                MemberSignIn.new {
                    member = target
                    timeStamp = GroupmanagerHz.todayStamp().toString()
                }
            }
        }
    }
}