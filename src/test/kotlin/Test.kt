import org.hezistudio.listenerHosts.NewFishSys

fun main(){
//    val today = LocalDateTime.now()
//    val fmt = DateTimeFormatter.ofPattern("yyMMdd0000")
//    println(today.format(fmt).toLong())
//    val sa = "TDna"
//    println(sa)
//    val ua = stringTranslate(sa)
//    println(ua)
    val drop1 = NewFishSys.Drop("臭鱼","散发出腐败的味道",NewFishSys.Quality.C1,-1)
    val drop1String = drop1.toJson()
    val drop2 = NewFishSys.Drop.fromJson(drop1String)
    println(drop2.name)
    println(drop2.desc)
    println(drop2.qualityClass)
}

fun stringTranslate(str:String):String{
    val saToUa = '\uff41' - 'a'
    val sb = StringBuilder()
    for (c in str){
        if (c in ('a'..'z')){
            sb.append((c+saToUa).toChar())
        }else{
            sb.append(c)
        }
    }
    return sb.toString()
}


