package de.tectoast.emolga.buttons.buttonsaves

import de.tectoast.emolga.commands.Command
import de.tectoast.jsolf.JSONObject
import java.util.function.Consumer

class MonData(val list: List<JSONObject>) {
    val data = LinkedHashMap<String, JSONObject>()

    init {
        list.forEach(Consumer { j: JSONObject -> data[Command.toSDName(j.getString("name"))] = j })
    }

    fun getData(id: String): JSONObject? {
        return data[id]
    }
}