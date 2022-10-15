

fun main(){
    val testStr = "我要改名孙大圣"
    val regexMatchStr = """我要改名 ?\S+"""
    val r = Regex(regexMatchStr).matches(testStr)

    if (r){
        println("匹配！")
    }else{
        println("不匹配！")
    }
}