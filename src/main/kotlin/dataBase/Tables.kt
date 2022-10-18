package org.hezistudio.dataBase

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.dao.id.LongIdTable

/**群表*/
object Groups:IntIdTable(){
    val number = long("number")
    val isWorking = bool("is_working").default(false)
}

/**用户表*/
object Users:IntIdTable(){
    val userNum = long("user_num")
}

/**道具表*/
object Items:IntIdTable(){
    val name = varchar("name",128)
    val description = varchar("description",512)
    val inPrice = integer("in_price")
    val outPrice = integer("out_price")
    val usage = varchar("usage",512)
    val sellable = bool("sellable").default(true)
    val equippable = bool("equippable").default(false)
    /**效果持续时间*/
    val duration = long("duration").default(0)
}

/**蓝图表*/
object BlueMaps:IntIdTable(){
    val name = varchar("name",128)
    val description = varchar("description",512)
}

/**蓝图输入表*/
object BlueMapInputItems:IntIdTable(){
    val item = reference("item",Items)
    val mapId = reference("map_id",BlueMaps)
}

/**蓝图输出表*/
object BlueMapOutputItems:IntIdTable(){
    val item = reference("item",Items)
    val mapId = reference("map_id",BlueMaps)
}

/**群成员表-核心表*/
object GroupMembers:IntIdTable(){
    val member = reference("user_id",Users)
    val group = reference("group_id",Groups)
    val nickName = varchar("nick_name",64)
    /**性别-false(女)|true(男,默认)*/
    val gender = bool("gender").default(true)
    val health = double("health").default(100.0)
    /**积分-玩家积分*/
    val fraction = integer("fraction").default(0)
    val strength = double("strength").default(10.0)
    val attack = double("attack").default(10.0)
    val defence = double("defence").default(10.0)
}

/**成员-蓝图表*/
object GroupMemberBlueMaps:IntIdTable(){
    val groupMember = reference("group_member",GroupMembers)
    val blueMap = reference("blue_map",BlueMaps)
}

/**成员-道具表*/
object MemberItems:LongIdTable(){
    val member = reference("member_id",GroupMembers)
    val item = reference("item_id",Items)
}

/**签到表*/
object MemberSignIns:LongIdTable(){
    val member = reference("member",GroupMembers)
    /**时间戳-格式: yyMMddhhmm*/
    val timeStamp = varchar("time_stamp",16)
}
/**游戏日程*/
object GameSchedules:LongIdTable(){
    val gameName = varchar("game_name",64)
    val startTime = long("start_time")
    val endTime = long("end_time")
}

/**日程成员*/
object ScheduleMembers:LongIdTable(){
    val schedule = reference("schedule",GameSchedules)
    val member = reference("member",GroupMembers)
}

/**击剑表*/
object JiJians:LongIdTable(){
    val member = reference("member", GroupMembers)
    val timeStamp = long("time_stamp")
    val counter = integer("counter")
}

/**临时属性*/
object TempProps:LongIdTable(){
    val mem = reference("mem", GroupMembers)
    val props = varchar("props",128)
    val stopTime = long("stop_time")
}

object MemberMutes:IntIdTable(){
    val member = reference("member_id",GroupMembers)
    val endTime = long("end_time")
}

object MemberStocks:LongIdTable(){
    val stId = integer("st_id")
    val memId = reference("mem_id",GroupMembers)
    val hold = integer("hold_value")
    val ts = long("time_stamp")
}

/**@property targetTime 目标起竿时间*/
object MemberFishings:IntIdTable(){
    val memId = integer("mem_id")
    val rodState = bool("rod_state")
    val targetTime = long("target_time")
}