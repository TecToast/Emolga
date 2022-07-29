package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class DexNumberCommand :
    Command("dexnumber", "Zeigt das Pokemon, dass zur Dex-Nummer gehört", CommandCategory.Pokemon) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("num", "Dex-Nummer", "Die Dex-Nummer", ArgumentManagerTemplate.Number.range(1, 898))
            .setExample("!dexnumber 730")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        val data = dataJSON
        val num = e.arguments.getInt("num")
        data.keySet().asSequence().map { data.getJSONObject(it) }.firstOrNull { it.getInt(num) == num }
            ?.let { e.reply(getGerNameNoCheck(it.getString("name"))) } ?: e.reply("huch")
    }
}