package org.hezistudio

import java.time.LocalTime

object Schedule{
    val table = ArrayList<SingleSchedule>()

    fun add(player:Long, game:Game, time:LocalTime):Boolean{
        return if (!playerHasScheduled(player)){
            table.add(SingleSchedule(game,player,time))
            true
        }else{
            false
        }
    }

    fun cancel(player: Long):Boolean{
        if (playerHasScheduled(player)){
            for ((index,schedule) in table.withIndex()){
                if (player in schedule.players){
                    table.removeAt(index)
                    return true
                }
            }
        }
        return false
    }

    fun join(player:Long, scheduleID:Int):Boolean{
        if (playerHasScheduled(player)){
            return false
        }else{
            return table[scheduleID].addPlayer(player)
        }
    }

    fun playerHasScheduled(player: Long):Boolean{
        for (schedule in table){
            if (player in schedule.players){
                return true
            }
        }
        return false
    }

    fun clear(){
        table.clear()
    }
}