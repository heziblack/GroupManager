package org.hezistudio.dataBase

import org.jetbrains.exposed.sql.transactions.transaction
import org.hezistudio.GroupmanagerHz
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

object DBTools {

    private val defaultGroupID:Long
        get() = GroupmanagerHz.pluginConfig.group

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
}