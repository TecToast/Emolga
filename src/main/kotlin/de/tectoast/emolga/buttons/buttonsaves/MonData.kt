package de.tectoast.emolga.buttons.buttonsaves

import de.tectoast.emolga.commands.Command
import de.tectoast.jsolf.JSONObject

class MonData(val list: List<JSONObject>) {
    val data: MutableMap<String, JSONObject> = mutableMapOf()

    init {
        list.forEach { data[Command.toSDName(it.getString("name"))] = it }
    }

    fun getData(id: String): JSONObject {
        return data.getValue(id)
    }
}