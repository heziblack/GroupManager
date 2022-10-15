package org.hezistudio.dataBase

import org.hezistudio.dataBase.GroupMemberBlueMaps.default
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

/**群*/
class Group(id: EntityID<Int>):IntEntity(id){
    companion object:IntEntityClass<Group>(Groups)
    var number by Groups.number
    var isWorking by Groups.isWorking
}

/**用户*/
class User(id: EntityID<Int>):IntEntity(id){
    companion object:IntEntityClass<User>(Users)
    var userNum by Users.userNum
}

/**道具*/
class Item(id: EntityID<Int>):IntEntity(id){
    companion object:IntEntityClass<Item>(Items)
    var name by Items.name
    var description by Items.description
    var inPrice by Items.inPrice
    var outPrice by Items.outPrice
    var usage by Items.usage
    val sellable by Items.sellable
    val equippable by Items.equippable
    val duration by Items.duration
}

/**蓝图*/
class BlueMap(id: EntityID<Int>):IntEntity(id){
    companion object:IntEntityClass<BlueMap>(BlueMaps)
    var name by BlueMaps.name
    var description by BlueMaps.description
}

/**输入道具**/
class BlueMapInputItem(id: EntityID<Int>):IntEntity(id){
    companion object:IntEntityClass<BlueMapInputItem>(BlueMapInputItems)
    var item by Item referencedOn BlueMapInputItems.item
    var mapId by Item referencedOn BlueMapInputItems.mapId
}

/**输出道具*/
class BlueMapOutputItem(id: EntityID<Int>):IntEntity(id){
    companion object:IntEntityClass<BlueMapOutputItem>(BlueMapOutputItems)
    var item by Item referencedOn BlueMapOutputItems.item
    var mapId by Item referencedOn BlueMapOutputItems.mapId
}

/**成员对象*/
class Member(id: EntityID<Int>):IntEntity(id){
    companion object:IntEntityClass<Member>(GroupMembers)
    var member by User referencedOn GroupMembers.member
    var group by Group referencedOn GroupMembers.group
    var nickName by GroupMembers.nickName
    var gender by GroupMembers.gender.default(true)
    var health by GroupMembers.health
    var fraction by GroupMembers.fraction
    var strength by GroupMembers.strength
    var attack by GroupMembers.attack
    var defence by GroupMembers.defence
}

/**成员-蓝图*/
class MemberBlueMap(id: EntityID<Int>):IntEntity(id){
    companion object:IntEntityClass<MemberBlueMap>(GroupMemberBlueMaps)
    var member by Member referencedOn GroupMemberBlueMaps.groupMember
    var blueMap by BlueMap referencedOn GroupMemberBlueMaps.blueMap
}

/**成员-道具*/
class MemberItem(id: EntityID<Long>):LongEntity(id){
    companion object:LongEntityClass<MemberItem>(MemberItems)
    var member by Member referencedOn MemberItems.member
    var item by Item referencedOn MemberItems.item
}

/**签到*/
class MemberSignIn(id: EntityID<Long>): LongEntity(id){
    companion object:LongEntityClass<MemberSignIn>(MemberSignIns)
    var member by Member referencedOn MemberSignIns.member
    var timeStamp by MemberSignIns.timeStamp
}

/**击剑记录*/
class Jijian(id:EntityID<Long>): LongEntity(id){
    companion object:LongEntityClass<Jijian>(JiJians)
    var member by Member referencedOn JiJians.member
    var timeStamp by JiJians.timeStamp
    var counter by JiJians.counter.default(1)
}

/**暂时状态*/
class TempProp(id:EntityID<Long>):LongEntity(id){
    companion object:LongEntityClass<TempProp>(TempProps)
    var mem by Member referencedOn TempProps.mem
    var props by TempProps.props
    var stopTime by TempProps.stopTime
}

/**游戏日程*/
class GameSchedule(id:EntityID<Long>):LongEntity(id){
    companion object:LongEntityClass<GameSchedule>(GameSchedules)
    var gameName by GameSchedules.gameName
    var startTime by GameSchedules.startTime
    var endTime by GameSchedules.endTime
}

/**日程成员*/
class ScheduleMember(id:EntityID<Long>):LongEntity(id){
    companion object:LongEntityClass<ScheduleMember>(ScheduleMembers)
    var schedule by GameSchedule referencedOn ScheduleMembers.schedule
    var member by Member referencedOn ScheduleMembers.member
}