
import com.google.gson.Gson
import kotlinx.coroutines.delay
import okhttp3.*
import org.hezistudio.klaxonModels.ApexInfo
import org.hezistudio.klaxonModels.ApexInfoGetter
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.TimerTask

suspend fun main() {
    val testUserNames:ArrayList<String> = arrayListOf("HeziBlack", "Mr_Yohko", "steamnbnb", "black_hezi")
    val apexInfoGetter = ApexInfoGetter("hezi_black")
    apexInfoGetter.run()
//    println(apexInfoGetter.hasResult)
//    println(apexInfoGetter.result)
    if(apexInfoGetter.hasResult){
        val gson = Gson()
        println(apexInfoGetter.result)
        val obj = gson.fromJson<ApexInfo>(apexInfoGetter.result, ApexInfo::class.java)
        println(obj.global.name)
//        apexInfoGetter.getMatchHistory(obj.global.uid)
    }
}

