package org.hezistudio.listenerHosts

import com.google.gson.Gson
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import org.hezistudio.GroupmanagerHz
import org.hezistudio.dataBase.DBTools
import org.hezistudio.dataBase.ExProps
import org.hezistudio.dataBase.FishingDrops
import org.hezistudio.dataBase.Member
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.absoluteValue

/**需要实现的功能:
 * 抛竿 收杆
 * 算法：计算收杆时间与预定时间的时间差。距离预定时间越近，幸运值越高。
 * 然后根据玩家钓鱼技能补正幸运值，其次根据玩家道具补正幸运值
 * 幸运值决定奖励或惩罚概率等级
 *
 *
 * 1/1000稀有度 绝品
 * 1/100 稀有度 史诗
 * 1/10  稀有度 传奇
 * 3/10  稀有度 稀有
 * 其余   稀有度 普通
 *
 *
 * 补正算法：
 * 升阶概率：10% 技能
 * 留出道具奖励接口
 * @property defaultDrops 默认掉落*/
object NewFishSys:ListenerHost {
    private const val fishingSkillName = "钓鱼"
    private const val fishingSkillDefault = 0.0
    private val defaultDrops:List<Pair<String,Int>> = listOf()
    private val defaultDrop:Drop = Drop("普通的鱼","一条普通的鱼",Quality.C1,1)
    @EventHandler
    suspend fun onEvent(e:GroupMessageEvent){

    }
    init {
        if (GroupmanagerHz.pluginConfig.bot == -1L) throw Exception("未配置参数，拒绝初始化")
        initSysDatabase()
    }
    /**获取钓鱼技能值*/
    private fun getMemberFishingSkill(member:Member):Double{
        val prop = DBTools.getNumericProp(member, fishingSkillName, fishingSkillDefault)
        return prop.exPropValue
    }

    /**增长钓鱼技能*/
    private fun addFishingSkill(member:Member,addValue:Double):Boolean{
        member.db
        return try{
            transaction {
                val prop = DBTools.getNumericProp(member, fishingSkillName, fishingSkillDefault)
                prop.exPropValue += addValue
            }
            true
        }catch (e:Exception){
            GroupmanagerHz.logger.error("数据库额外属性写入失败: dbMember:${member.id.value}-${fishingSkillName}")
            false
        }
    }

    /**减少钓鱼技能*/
    private fun reduceFishingSkill(member: Member,reduceValue:Double):Boolean{
        return addFishingSkill(member,-reduceValue)
    }

    /**初始化数据库相关部分*/
    private fun initSysDatabase(){
        val db = GroupmanagerHz.getDBC(GroupmanagerHz.pluginConfig.bot)
        transaction(db) {
            SchemaUtils.create(
                ExProps, FishingDrops
            )
            SchemaUtils.addMissingColumnsStatements(
                ExProps, FishingDrops
            )
            FishingDrops.insert{

            }
        }
    }

    /**钓鱼掉落*/
    class Drop(
        val name:String,
        val desc:String,
        val qualityClass:Quality,
        val fraction:Int
    ){
        init {
            if (name.length > 10) throw DropInitError("名字不能超过10个字符")
            if (desc.length>128) throw DropInitError("介绍不能超过128个字符")
            if (fraction == 0) throw DropInitError("积分变化不能为0")
            if (fraction.absoluteValue > 100) throw DropInitError("积分变化绝对值在100以内")
        }
        fun toJson():String{
            val gson = Gson()
            return gson.toJson(this)
        }
        companion object{
            /**从Json创建Drop(类似于Factory方法)*/
            fun fromJson(jsonStr: String): Drop {
                return Gson().fromJson(jsonStr, Drop::class.java)
            }
        }
        class DropInitError(error:String):Exception(error)
    }

    /**钓鱼品质*/
    enum class Quality(
        val zhName:String,
        val rarity:Double,
    ){
        C1("普通",0.589),
        C2("稀有",0.3),
        C3("传奇",0.1),
        C4("史诗",0.01),
        C5("绝品",0.001)
    }

    /**获取品质*/
    fun Quality.findByName(name:String):Quality?{
        for (c in Quality.values()){
            if (c.zhName == name){
                return c
            }
        }
        return null
    }

    /**管理员添加钓鱼凋落物*/
    fun administratorCreateDrop():Boolean{
        TODO("检查指令合法性")
//        TODO("检查添加物品是否重名")
//        TODO("检查是否符合更多规范")
    }

    /**管理员编辑凋落物*/
    fun administratorEditDrop():Boolean{
        TODO("检查指令合法性")
//        TODO("检查物品是否存在")
//        TODO("检查是否符合更多规范")
    }

    class DropEditor(){

    }

}