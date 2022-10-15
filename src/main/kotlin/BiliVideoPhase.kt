package org.hezistudio

import com.beust.klaxon.Klaxon
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.hezistudio.GroupmanagerHz.logger
import org.hezistudio.klaxonModels.MiniAppModel
import java.awt.image.BufferedImage
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.imageio.ImageIO

object BiliVideoPhase:SimpleListenerHost() {
    val Plugin = GroupmanagerHz
    @EventHandler
    suspend fun onEvent(e: GroupMessageEvent){
        if(e.message.size != 2) return
        val content = e.message[1].content
        if (!content.startsWith("{\"app\":\"com.tencent.miniapp_01\",")) return
        try {
            val p = Klaxon().parse<MiniAppModel>(content) ?: return
            val a = p.meta["detail_1"] ?: return
            val mb = MessageChainBuilder()
            val tittle = a["desc"]?:"无标题"
            val previewURL = a["preview"]
            val videoURL = a["qqdocurl"]

            var img: BufferedImage? = null
            if (previewURL!=null){
                // 根据URL 实例， 获取HttpURLConnection 实例
                val url = URL( "http://" + previewURL )
                println(url)
                val httpURLConnection: HttpURLConnection = url.openConnection() as HttpURLConnection
                // 设置读取 和 连接 time out 时间
                httpURLConnection.readTimeout = 2000
                httpURLConnection.connectTimeout = 2000
                // 获取图片输入流
                val inputStream = httpURLConnection.inputStream
                // 获取网络响应结果
                val responseCode = httpURLConnection.responseCode
                // 获取正常
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // 解析图片
                    img = ImageIO.read(inputStream)
                    inputStream.close()
                }
            }
            mb.add(tittle)
            if (img != null){
                val folder = Plugin.dataFolder
                val fileName = "cache"
                val file =  File(folder,fileName)
                if(!file.exists()) {
                    val pf = file.parentFile
                    if (!pf.exists()){
                        pf.mkdirs()
                    }
                    file.createNewFile()
                }
                ImageIO.write(img, "png", file)
                val exf = file.toExternalResource("png")
                val ii = e.group.uploadImage(exf)
                mb.add(ii)
            }
            val link = (videoURL?:"??").substringBefore("?")
            mb.add("链接：${link}")
            e.group.sendMessage(mb.build())
        }catch (e:Exception){
            logger.error("解析出错")
            logger.error(e.message)
        }
    }
}