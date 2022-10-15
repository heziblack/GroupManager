package org.hezistudio.listenerHosts

import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import org.hezistudio.GroupmanagerHz
import org.hezistudio.dataBase.DBTools
import org.hezistudio.itemSys.buildInItems.RenameCard

object ListenerForTest:ListenerHost {
    @EventHandler
    suspend fun onEvent(e:GroupMessageEvent){
        val img = GroupmanagerHz.getImageResStream("images/CharacterCardBase.jpg",)?:return

        e.group.sendImage(img,"jpg")
    }
}