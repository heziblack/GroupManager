package org.hezistudio.klaxonModels

import com.beust.klaxon.Json




class MiniAppModel(
    @Json
    val app:String,
    @Json
    val desc:String,
    @Json
    val extra:Map<String,Long>,
    @Json
    val meta:Map<String,Map<String,String>>
) {


}

class Meta(
    @Json
    val detail_1:AppDetail
){

}

class AppDetail(
    @Json
    val appType:Int,
    @Json
    val appid:Long,
    @Json
    val desc: String,
    @Json
    val icon:String,
    @Json
    val qqdocurl:String
)