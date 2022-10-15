package org.hezistudio.itemSys

import org.hezistudio.dataBase.Member
import org.jetbrains.exposed.sql.Database
import java.time.Duration

/**存储的道具实体
 * @property name 道具名
 * @property description 道具描述
 * @property useStr 使用文本
 * @property useToStr 对目标使用文本
 * @property equipStr 装备文本
 * @property inPrice 购入价格
 * @property outPrice 出售价格
 * @property canSell 是否可出售
 * @property canEquip 是否可装备
 * @property canBuy 是否可购买
 * @property canBeConsumed 使用后是否会被消耗
 * @property isSpecial 是否是特殊道具
 * @property duration 效果持续时间
 * @property target 使用对象
 * */
abstract class ItemEntity(
    val name:String,
    val description:String = "Master是个懒狗，没有写相关介绍",
    val useStr:String = "【使用者】使用了【道具名】",
    val useToStr:String = "【使用者】对【使用对象】使用了【道具名】",
    val equipStr:String = "【使用者】装备了【道具名】",
    val inPrice:Int=0,
    val outPrice:Int=0,
    val canBuy:Boolean = false,
    val canSell:Boolean = false,
    val canEquip:Boolean = false,
    val canBeConsumed:Boolean = true,
    val isSpecial:Boolean = false,
    val duration: Duration = Duration.ZERO,
    val target: ItemTarget = ItemTarget.Self
) {
    /**道具使用对象*/
    enum class ItemTarget(code:Int){
        None(0),
        Self(1),
        Someone(2),
        Others(3),
        All(4)
    }

    /**道具激活
     * @param db 数据库
     *
     * */
    abstract suspend fun active(db: Database?=null, user:net.mamoe.mirai.contact.Member, vararg exArg:String):Boolean
}