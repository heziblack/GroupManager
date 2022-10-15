package org.hezistudio.itemSys

import org.hezistudio.itemSys.buildInItems.MuteDrag
import org.hezistudio.itemSys.buildInItems.RenameCard
import org.hezistudio.itemSys.buildInItems.SexualConversionDrug
import org.hezistudio.itemSys.buildInItems.StrengthDrug

object BuildInItemList: ArrayList<ItemEntity>() {
    init {
        this.add(RenameCard)
        this.add(MuteDrag)
        this.add(StrengthDrug)
        this.add(SexualConversionDrug)
    }

    fun get(itemName:String):ItemEntity?{
        for (it in this){
            if (it.name==itemName){
                return it
            }
        }
        return null
    }

    val nameSet:List<String>
        get() {
            val list:ArrayList<String> = arrayListOf()
            for (i in this){
                list.add(i.name)
            }
            return list
        }

}