package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import java.util.*
import java.util.function.Consumer

class TypeInfoCommand : Command("typeinfo", "Zeigt dir Informationen über einen Typen an", CommandCategory.Pokemon) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .addEngl("type", "Typ", "Der Typ", Translation.Type.TYPE)
            .setExample("!typeinfo Wasser")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        val tt = e.arguments!!.getTranslation("type")
        val type = tt.translation
        val json = typeJSON
        val effectiveAgainst: MutableList<String> = LinkedList()
        val resistedBy: MutableList<String> = LinkedList()
        val weakAgainst: MutableList<String> = LinkedList()
        val resisted: MutableList<String> = LinkedList()
        json.keySet().forEach(Consumer { str: String ->
            val damageTaken = json.getJSONObject(str).getJSONObject("damageTaken").getInt(type)
            if (damageTaken > 0) {
                val t = (Translation.Type.TYPE.validate(
                    str,
                    Translation.Language.GERMAN,
                    "default"
                ) as Translation?)!!.translation
                if (damageTaken > 1) {
                    if (damageTaken == 3) resistedBy.add("$t **(immun)**") else resistedBy.add(t)
                } else {
                    effectiveAgainst.add(t)
                }
            }
        })
        val typejson = json.getJSONObject(type).getJSONObject("damageTaken")
        typejson.keySet().forEach(Consumer { str: String ->
            val damageTaken = typejson.getInt(str)
            if (damageTaken > 0) {
                val t = Translation.Type.TYPE.validate(str, Translation.Language.GERMAN, "default") as Translation?
                if (t != null) {
                    if (damageTaken > 1) {
                        if (damageTaken == 3) resisted.add(t.translation + " **(immun)**") else resisted.add(t.translation)
                    } else {
                        weakAgainst.add(t.translation)
                    }
                }
            }
        })
        e.reply(
            """
    **${tt.otherLang}**
    
    - effektiv gegen
    ${java.lang.String.join("\n", effectiveAgainst)}
    
    - wird resistiert von
    ${java.lang.String.join("\n", resistedBy)}
    
    - ist schwach gegen
    ${java.lang.String.join("\n", weakAgainst)}
    
    - resistiert
    ${java.lang.String.join("\n", resisted)}
    """.trimIndent()
        )
    }
}