package de.tectoast.emolga.commands.showdown

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent

object AnalyseCommand :
    Command("analyse", "Schickt das Ergebnis des angegebenen Kampfes in den Channel", CommandCategory.Showdown) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("url", "Replay-Link", "Der Replay-Link", ArgumentManagerTemplate.Text.any())
            .setExample("!analyse https://replay.pokemonshowdown.com/oumonotype-82345404").build()
        slash()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        analyseReplay(
            url = e.arguments.getText("url"),
            resultchannelParam = tco,
            fromAnalyseCommand = e.run { deferReply(); slashCommandEvent?.hook })
    }
}
