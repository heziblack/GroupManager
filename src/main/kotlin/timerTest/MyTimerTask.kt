package org.hezistudio.timerTest

import net.mamoe.mirai.event.broadcast
import org.hezistudio.myEvent.CustomEvent
import java.util.TimerTask

class MyTimerTask():TimerTask() {
    /**
     * The action to be performed by this timer task.
     */
    override fun run() {
//        CustomEvent().broadcast()
        println("taskTest!")
    }
}