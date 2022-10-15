package org.hezistudio.klaxonModels

import com.beust.klaxon.Json

/** 消息模板
 *
 * 【发起人】-击剑发起人
 *
 * 【击剑目标】-击剑被挑战者 */
class MessageModel(
    @Json("击剑胜利")
    val winStr:List<String> = listOf(
        "你击败了ta, 获得2点积分"
    ),
    @Json("击剑失败")
    val lossStr:List<String> = listOf(
        "好一对卧龙凤雏，你俩斗得难解难分，消耗一点积分"
    ),
    @Json("击剑平局")
    val drawStr:List<String> = listOf(
        "ta的战力在你之上！, 你被【击剑目标】击败，扣除3点积分"
    )
)