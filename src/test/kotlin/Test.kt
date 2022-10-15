import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun main(){
//    val today = LocalDateTime.now()
//    val fmt = DateTimeFormatter.ofPattern("yyMMdd0000")
//    println(today.format(fmt).toLong())
    val str = "对fdslkajgdsflkjhg使用afhsdaoiufha"
    val pos = str.indexOf("使用")
    val sub = str.substring(pos+2)
    println(sub)
}


