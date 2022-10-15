package org.hezistudio.klaxonModels

import okhttp3.OkHttpClient
import okhttp3.Request
import org.hezistudio.apexSearch.HttpClient
import java.util.TimerTask


class ApexInfoGetter(
    private val userName:String,
    private var uid:Long):TimerTask() {
    // apiKey
    private val apiKey = "98fdcb127505903c9af49045d5905b78"

    // 按照玩家名字查询玩家信息
    private val urlNormal:String = "https://api.mozambiquehe.re/bridge?auth=${apiKey}&player=${userName}&platform=PC"
    private val urlMatchL = "https://api.mozambiquehe.re/bridge?auth=${apiKey}&uid=PLAYER_UID&platform=PC&history=1&action=get"
    private var urlMatchHistory = "https://api.mozambiquehe.re/games?auth=${apiKey}&uid=PLAYER_UID"
    private val urlPredator = "https://api.mozambiquehe.re/predator?auth=${apiKey}"
    private val urlMap = "https://api.mozambiquehe.re/maprotation?auth=${apiKey}"
    private val UID:Long
        get() = uid
    // 是否已查询
    var hasResult:Boolean = false

    constructor(userName:String):this(userName,-1)

    private var resultTemp:String = ""

    val result:String
        get() {
            return if (!hasResult){
                ""
            }else{
                resultTemp
            }
        }

    override fun run() {
        val request = Request.Builder()
            .url(urlNormal)
            .header("Accept-Encoding","UTF-8")
            .method("GET",null)
            .build()
        val response = HttpClient.newCall(request).execute()
        if (response.code == 200){
//            println(response.code)
//            println(response.body?.string())
            if (response.body == null) return
            val respBody = response.body
            resultTemp = respBody?.string() ?: ""
            if (resultTemp != ""){
                hasResult = true
            }else return
            if (resultTemp.startsWith("{\"Error\"")){
                hasResult = false
                resultTemp = ""
            }
        }
    }

    // 查询历史
    fun getMatchHistory(id:Long){
        if (UID==-1L) {
            uid = id
        }
        val url = urlMap.replace("PLAYER_UID","$UID")
        val request = Request.Builder()
            .url(url)
            .header("Accept-Encoding","UTF-8")
            .method("GET",null)
            .build()
        val response = HttpClient.newCall(request).execute()
        println(response.code)
        if (response.code==200){
            println(response.body?.string())
        }
    }

}