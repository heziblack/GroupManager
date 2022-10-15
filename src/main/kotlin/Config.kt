package org.hezistudio

import com.beust.klaxon.Json

class Config(
    @Json
    val group:Long = -1L,
    @Json
    var bot:Long = -1L,
    @Json
    val owner:Long = -1L
) {
}