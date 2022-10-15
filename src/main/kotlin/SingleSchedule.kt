package org.hezistudio

import java.time.LocalTime

class SingleSchedule(
    val game:Game,
    startUser:Long,
    val time:LocalTime,
) {
    val players:ArrayList<Long> = arrayListOf(startUser)

    fun addPlayer(player:Long):Boolean{
        if (players.size < game.maxSize && player !in players) {
            players.add(player)
            return true
        }
        return false
    }
}