package org.hezistudio

object ScheduleData {
    val gameList:ArrayList<Game> = arrayListOf(
        Game("CSGO", arrayListOf(
            "gogo","GOGO","喜爱死","csgo","cs:go","CS:GO"
        ),5),
        Game("APEX", arrayListOf(
            "apex","派","派派"
        ),3),
        Game("英雄联盟", arrayListOf(
            "lol","LOL","撸"
        ),5),
        Game("永劫无间", arrayListOf(
            "劫","永劫","打劫"
        ),3),
        Game("任意游戏", arrayListOf(
            "any game","随便","啥都行"
        ),10)
    )

    fun getGameByName(name:String):Game?{
        for (g in gameList){
            if (name == g.name || name in g.otherName){
                return g
            }
        }
        return null
    }
}