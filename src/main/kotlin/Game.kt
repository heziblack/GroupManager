package org.hezistudio

class Game(
    val name:String,
    val otherName:ArrayList<String> = arrayListOf(),
    val maxSize:Int
    ) {
    fun addOtherName(newName: String){
        if (newName != name && newName !in otherName){
            otherName.add(newName)
        }else return
    }
}