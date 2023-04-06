package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.Translation

class ResistedCommand :
    Command("resisted", "Zeigt alle Typen an, die der angegebene Typ resistiert", CommandCategory.Pokemon) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .addEngl("type", "Typ", "Typ, bei dem geschaut werden soll, was er resistiert", Translation.Type.TYPE)
            .setExample("!resistance Feuer")
            .build()
        disable()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val type = e.arguments.getTranslation("type")
        val json = typeJSON
        val b = StringBuilder()
        json.entries.forEach { (it, typeData) ->
            val damageTaken = typeData.damageTaken[type.translation] ?: return@forEach
            if (damageTaken > 1) {
                val gername = getTypeGerNameOrNull(it) ?: return@forEach
                if (damageTaken == 3) b.append("**")
                b.append(gername)
                if (damageTaken == 3) b.append("**")
                b.append("\n")
            }
        }
        if (b.isEmpty()) {
            e.reply("Es wurden keine Typen gefunden, die diesen Typ resistieren! (Das ist für normal ein Fehler, Flo wurde kontaktiert)")
            sendToMe("ResistanceCommand ListSize 0")
            return
        }
        e.reply(
            """
    Folgende Typen resistieren ${type.otherLang}:
    $b
    """.trimIndent()
        )
    }
}
