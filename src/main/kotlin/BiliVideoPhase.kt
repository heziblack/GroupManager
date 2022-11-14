package org.hezistudio

import com.beust.klaxon.Klaxon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import okhttp3.Request
import org.hezistudio.GroupmanagerHz.logger
import org.hezistudio.apexSearch.HttpClient
import org.hezistudio.klaxonModels.MiniAppModel
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.*
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
                val url = URL("http://$previewURL")
                val imgGetter = ImageGetter(url.toString())
                if (imgGetter.isOk){
                    img = imgGetter.img
                }
            }
            mb.add(tittle)
            if (img != null){
                val os = ByteArrayOutputStream()
                withContext(Dispatchers.IO) {
                    ImageIO.write(img, "jpg", os)
                }
                val er = os.toByteArray().toExternalResource()
                val image = er.uploadAsImage(e.group)
                withContext(Dispatchers.IO) {
                    er.close()
                }
                mb.add(image)
            }
            val link = (videoURL?:"??").substringBefore("?")
            mb.add("链接：${link}")
            e.group.sendMessage(mb.build())
        }catch (e:Exception){
            logger.error("解析出错")
            logger.error(e.message)
        }
    }

    class ImageGetter(
        private val url:String
    ):TimerTask(){
        private var ok = false
        val isOk
            get() = ok

        var img:BufferedImage? = null
        init {
            this.run()
        }
        override fun run() {
            val request = Request.Builder().url(url).build()
            val response = HttpClient.newCall(request).execute()
            println(response.code)
            if (response.code==200){
                val body = response.body
                if (body != null){
                    val s = body.byteStream()
                    img = ImageIO.read(s)
                    ok = true
                }
            }
        }
    }
}