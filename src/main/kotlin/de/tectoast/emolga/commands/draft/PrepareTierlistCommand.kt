package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.PrivateCommands
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
        try {
            TierlistBuilderConfigurator(
                userId = e.author.idLong,
                channelId = e.channel.idLong,
                guildId = PrivateCommands.guildForTLSetup?.takeUnless { e.isNotFlo } ?: e.guild.idLong,
                mons =
                Google.batchGet(
                    sid,
                    (0 until 10).mapNotNull { args.getNullable<String>("range$it")?.let { a -> "$tierlistsheet!$a" } },
                    false
                )
                    .map { col -> col.mapNotNull { it.getOrNull(0)?.toString()?.prepareForTL() } }
                    .also { tierlistcols += it }
                    .flatten().ensureNoDuplicates(),
                tierlistcols = tierlistcols
            )
        } catch (ex: DuplicatesFoundException) {
            e.hook.sendMessage(
                "Es wurden Pokemon doppelt in der Tierliste gefunden! Bitte überprüfe die folgenden Pokemon: ${
                    ex.duplicates.joinToString(
                        ", "
                    )
                }"
            ).queue()
        }
        /*e.deferReply()
        TierlistBuilderConfigurator(
            Constants.FLOID,
            447357526997073932,
            651152835425075218,
            MDLTierlist.get.values.flatMap { map -> map.values.flatten() }, emptyList()
        )*/
    }
}

private val complexSigns = setOf("*", "^", "(")
private fun String.prepareForTL(): String? {
    if (toIntOrNull() != null) return null
    var x = this
    complexSigns.forEach { x = x.substringBefore(it) }
    return x.trim().takeUnless { it.isBlank() }
}

fun List<String>.ensureNoDuplicates(): List<String> {
    return groupingBy { it }.eachCount().filter { it.value > 1 }.keys.toList().takeIf { it.isNotEmpty() }?.let {
        throw DuplicatesFoundException(it)
    } ?: this
}

class DuplicatesFoundException(val duplicates: List<String>) : Exception()
