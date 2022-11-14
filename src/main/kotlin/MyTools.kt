package org.hezistudio

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object MyTools {
    private val dtFmt = DateTimeFormatter.ofPattern("yyMMddHHmm")
    fun fromDateTimeToLong(dt:LocalDateTime):Long{
        return dt.format(dtFmt).toLong()
    }
    fun fromLongToDateTime(long: Long):LocalDateTime{
        val s = try {
            dtFmt.parse(long.toString())
        }catch (e:Exception){
            return LocalDateTime.now()
        }
        return LocalDateTime.from(s)
    }
}