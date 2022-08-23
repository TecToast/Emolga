package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import java.util.*

class TypeInfoCommand : Command("typeinfo", "Zeigt dir Informationen über einen Typen an", CommandCategory.Pokemon) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .addEngl("type", "Typ", "Der Typ", Translation.Type.TYPE)
            .setExample("!typeinfo Wasser")
            .build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val tt = e.arguments.getTranslation("type")
        val type = tt.translation
        val json = typeJSON
        val effectiveAgainst: MutableList<String> = LinkedList()
        val resistedBy: MutableList<String> = LinkedList()
        val weakAgainst: MutableList<String> = LinkedList()
        val resisted: MutableList<String> = LinkedList()
        json.keySet().forEach {
            val damageTaken = json.getJSONObject(it).getJSONObject("damageTaken").getInt(type)
            if (damageTaken > 0) {
                val t = getTypeGerName(it)
                if (damageTaken > 1) {
                    if (damageTaken == 3) resistedBy.add("$t **(immun)**") else resistedBy.add(t)
                } else {
                    effectiveAgainst.add(t)
                }
            }
        }
        val typejson = json.getJSONObject(type).getJSONObject("damageTaken")
        typejson.keySet().forEach {
            val damageTaken = typejson.getInt(it)
            if (damageTaken > 0) {
                val t = getTypeGerName(it)
                    if (damageTaken > 1) {
                        if (damageTaken == 3) resisted.add("$t **(immun)**") else resisted.add(t)
                    } else {
                        weakAgainst.add(t)
                    }

            }
        }
        e.reply(
            "**${tt.otherLang}**\n\n- effektiv gegen\n${
                effectiveAgainst.joinToString("\n")
            }\n\n- wird resistiert von\n${
                resistedBy.joinToString("\n")
            }\n\n- ist schwach gegen\n${
                weakAgainst.joinToString("\n")
            }\n\n- resistiert\n${resisted.joinToString("\n")}"
        )
    }
}