package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

class ResistanceCommand :
    Command("resistance", "Zeigt alle Typen an, die der angegebene Typ resistiert", CommandCategory.Pokemon) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .addEngl(
                "type",
                "Typ",
                "Typ, bei dem geschaut werden soll, was er resistiert",
                Translation.Type.of(Translation.Type.TYPE, Translation.Type.POKEMON)
            )
            .setExample("!resistance Feuer")
            .build()
        disable()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val type = e.arguments.getTranslation("type")
        if (type.isFromType(Translation.Type.TYPE)) {
            val json = typeJSON
            val b = StringBuilder()
            val typejson = json.getJSONObject(type.translation).getJSONObject("damageTaken")
            typejson.keySet().forEach { str: String ->
                val damageTaken = typejson.getInt(str)
                if (damageTaken > 1) {
                    val t = Translation.Type.TYPE.validate(str, Translation.Language.GERMAN, "default") as Translation?
                    if (t != null) {
                        if (damageTaken == 3) b.append("**")
                        b.append(t.translation)
                        if (damageTaken == 3) b.append("**")
                        b.append("\n")
                    }
                }
            }
            if (b.isEmpty()) {
                e.reply("Es wurden keine Typen gefunden, die diesen Typ resistieren! (Das ist für normal ein Fehler, Flo wurde kontaktiert)")
                sendToMe("ResistanceCommand ListSize 0")
                return
            }
            e.reply(
                "Folgende Typen resistiert ${type.otherLang}:\n$b"
            )
        } else {
            e.reply("Macht Flo noch irgendwann :)")
        }
    }
}