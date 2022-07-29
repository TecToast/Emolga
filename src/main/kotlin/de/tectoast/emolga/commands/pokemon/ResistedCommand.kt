package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class ResistedCommand :
    Command("resisted", "Zeigt alle Typen an, die der angegebene Typ resistiert", CommandCategory.Pokemon) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .addEngl("type", "Typ", "Typ, bei dem geschaut werden soll, was er resistiert", Translation.Type.TYPE)
            .setExample("!resistance Feuer")
            .build()
        disable()
    }

    override fun process(e: GuildCommandEvent) {
        val type = e.arguments.getTranslation("type")
        val json = typeJSON
        val b = StringBuilder()
        json.keySet().forEach {
            val damageTaken = json.getJSONObject(it).getJSONObject("damageTaken").getInt(type.translation)
            if (damageTaken > 1) {
                if (damageTaken == 3) b.append("**")
                b.append(
                    (Translation.Type.TYPE.validate(
                        it,
                        Translation.Language.GERMAN,
                        "default"
                    ) as Translation).translation
                )
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