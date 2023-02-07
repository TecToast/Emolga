package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Google
import de.tectoast.emolga.utils.dconfigurator.impl.TierlistBuilderConfigurator

class PrepareTierlistCommand : Command("preparetierlist", "Richtet die Tierliste ein", CommandCategory.Draft) {

    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add("sid", "Doc-URL", "Die URL des Dokuments, in dem die Namen stehen", ArgumentManagerTemplate.Text.any())
            add("tierlistsheet", "Tierlist-Sheet", "Der Name des Tierlist-Sheets", ArgumentManagerTemplate.Text.any())
            for (i in 0 until 10) {
                add(
                    "range$i",
                    "Bereich ${i + 1}",
                    "Der ${i + 1}. Bereich",
                    ArgumentManagerTemplate.Text.any(),
                    optional = i > 0
                )
            }
        }
        slash(false, *TierlistBuilderConfigurator.enabledGuilds.toLongArray())
    }

    override suspend fun process(e: GuildCommandEvent) {
        val args = e.arguments
        val sid = args.getText("sid").substringAfter("d/").substringBefore("/")
        val tierlistsheet = args.getText("tierlistsheet")
        e.deferReply()
        val tierlistcols = mutableListOf<List<String>>()
        TierlistBuilderConfigurator(
            userId = e.author.idLong,
            channelId = e.channel.idLong,
            guildId = e.guild.idLong,
            mons = buildList {
                for (i in 0 until 10) {
                    val range = args.getNullable<String>("range$i") ?: continue
                    addAll(Google[sid, "$tierlistsheet!$range", false].map { it[0].toString().replace("*", "") }
                        .also { tierlistcols += it })
                }
            },
            tierlistcols = tierlistcols
        )
    }
}
