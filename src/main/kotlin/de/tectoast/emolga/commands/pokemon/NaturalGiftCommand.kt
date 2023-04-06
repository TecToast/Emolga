package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.*
import de.tectoast.emolga.database.exposed.NaturalGift
import dev.minn.jda.ktx.messages.Embed

class NaturalGiftCommand : Command(
    "naturalgift", "Entweder alle Beeren für einen Typ, oder den Typen von einer Beere", CommandCategory.Pokemon
) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder().add(
            "stuff",
            "Sache",
            "Entweder ein Typ oder ein Beerenname",
            Translation.Type.of(Translation.Type.ITEM, Translation.Type.TYPE)
        ).setExample("!naturalgift Water").build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val t = e.arguments.getTranslation("stuff")
        val translation = t.translation
        if (t.isFromType(Translation.Type.ITEM)) {
            val ngData =
                NaturalGift.byName(translation) ?: return e.reply("Dieses Item hat keinen Effekt auf Natural Gift!")
            e.reply(
                Embed {
                    title = translation
                    field {
                        name = "Typ"
                        value = ngData.type
                        inline = false
                    }
                    field {
                        name = "Basepower"
                        value = ngData.bp.toString()
                        inline = false
                    }
                    color = embedColor
                })
        } else {
            e.reply(Embed(title = translation,
                color = embedColor,
                description = NaturalGift.byType(translation)
                    .sortedBy { it.bp }.joinToString("\n") {
                        "${it.name}/${getEnglName(it.name)}: ${it.bp}"
                    })
            )
        }
    }
}
