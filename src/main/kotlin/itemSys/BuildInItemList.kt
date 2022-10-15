package org.hezistudio.itemSys

import org.hezistudio.itemSys.buildInItems.*

object BuildInItemList: ArrayList<ItemEntity>() {
    init {
        this.add(RenameCard)
        this.add(MuteDrag)
        this.add(StrengthDrug)
        this.add(SexualConversionDrug)
        this.add(FallenPotion)
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

    /**道具按价格升序*/
    fun sortByPrice():ArrayList<ItemEntity>{
        val out:ArrayList<ItemEntity> = arrayListOf()
        val tempList:ArrayList<ItemEntity> = arrayListOf()
        tempList.addAll(this)
        while (tempList.isNotEmpty()){
            var temp = tempList.first()
            for (tl in tempList){
                if (tl == tempList.first()){
                    continue
                }
                if (tl.inPrice < temp.inPrice){
                    temp = tl
                }
            }
            out.add(temp)
            tempList.remove(temp)
        }
        return out
    }
}