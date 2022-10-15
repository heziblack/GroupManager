package org.hezistudio.apexSearch

import com.google.gson.Gson
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.content
import org.hezistudio.klaxonModels.ApexInfo
import org.hezistudio.klaxonModels.ApexInfoGetter
import java.lang.Exception

object ListenerOfApexSearch:ListenerHost {
    @EventHandler
    suspend fun onEvent(e:GroupMessageEvent){
        val result = normalSearch(e.message.content.substring(5))
        if (result==null){
            e.group.sendMessage("未查询到账号或出现内部错误，请检查您的输入是否正确或询问BOT管理员")
        }else{
            val rankNameOfDts = rankNameTransform(result.global.rank.rankName)
            val rankNameOfArena = rankNameTransform(result.global.arena.rankName)
            val seasonName = seasonTranslate(result.global.rank.rankedSeason)
            val rankDivOfDts = if (result.global.rank.rankDiv==0) "" else (result.global.rank.rankDiv).toString()
            val rankDivOfArena = if (result.global.arena.rankDiv==0) "" else (result.global.arena.rankDiv).toString()
            val re = """
                玩家昵称：${result.global.name} Lv.${result.global.level}
                赛季：${seasonName}
                大逃杀段位：${rankNameOfDts}${rankDivOfDts}
                大逃杀积分：${result.global.rank.rankScore}
                竞技场段位：${rankNameOfArena}${rankDivOfArena}
                竞技场积分：${result.global.arena.rankScore}
            """.trimIndent()
            e.group.sendMessage(re)
        }
    }

    private fun normalSearch(name:String):ApexInfo? {
        val getter = ApexInfoGetter(name)
        try {
            getter.run()
        }catch (e:Exception){
            println(e.message)
            return null
        }
        if (getter.hasResult){
            return try {
                println("T")
                val gson = Gson()
                gson.fromJson(getter.result, ApexInfo::class.java)
            } catch (e: Exception) {
                println("E")
                null
            }
        }
        println("N")
        return null
    }

    private fun rankNameTransform(rankName:String):String{
        return when(rankName){
            "Rookie"->"菜鸟"
            "Bronze"-> "青铜"
            "Silver"->"白银"
            "Gold"->"黄金"
            "Platinum"->"白金"
            "Diamond"->"钻石"
            "Master"->"大师"
            "ApexPredator"->"顶级猎杀者"
            else -> "未定级"
        }
    }
    private fun seasonTranslate(season:String):String{
        val list = season.replace("season","").split("_split_")
        val seasonNum = list[0]
        val breakNum = list[1]
        return "${seasonNum}赛季${breakNum}阶段"
    }
}