package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.records.NGData
import de.tectoast.emolga.utils.sql.managers.NaturalGiftManager
import net.dv8tion.jda.api.EmbedBuilder
import java.awt.Color
import java.util.stream.Collectors

class NaturalGiftCommand : Command(
    "naturalgift",
    "Entweder alle Beeren für einen Typ, oder den Typen von einer Beere",
    CommandCategory.Pokemon
) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "stuff",
                "Sache",
                "Entweder ein Typ oder ein Beerenname",
                Translation.Type.of(Translation.Type.ITEM, Translation.Type.TYPE)
            )
            .setExample("!naturalgift Water")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        val t = e.arguments!!.getTranslation("stuff")
        val translation = t.translation
        if (t.isFromType(Translation.Type.ITEM)) {
            val ngData = NaturalGiftManager.fromName(translation)
            e.reply(
                EmbedBuilder()
                    .setTitle(translation)
                    .addField("Typ", ngData.type, false)
                    .addField("Basepower", ngData.bp.toString(), false)
                    .setColor(Color.CYAN)
                    .build()
            )
        } else {
            val ngData = NaturalGiftManager.fromType(translation)
            e.reply(EmbedBuilder()
                .setTitle(translation)
                .setDescription(
                    ngData.stream().sorted(Comparator.comparing { obj: NGData -> obj.bp })
                        .map { d: NGData -> d.name + "/" + getEnglName(d.name) + ": " + d.bp }
                        .collect(Collectors.joining("\n"))
                )
                .setColor(Color.CYAN)
                .build()
            )
        }
    }
}