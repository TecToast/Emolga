package de.tectoast.emolga.commands.flo

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.showdown.Analysis
import dev.minn.jda.ktx.coroutines.await

object RFileWithGuildCommand : Command("rfilewithguild", "Replay-File mit Guild", CommandCategory.Flo) {

    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add("id", "ID", "Die ID", ArgumentManagerTemplate.DiscordType.ID)
            add("file", "Replay-File", "Das Replay-File", ArgumentManagerTemplate.DiscordFile.of("html"))
        }
        slash(true)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val replayData =
            e.arguments.getAttachment("file").proxy.download().await().bufferedReader().use { it.readText() }
                .substringAfter("class=\"battle-log-data\">").substringBefore("</script>").split("\n")
        analyseReplay(
            "FILE",
            resultchannelParam = e.textChannel,
            customGuild = e.arguments.getID("id"),
            fromAnalyseCommand = e.run { deferReply(); slashCommandEvent?.hook },
            analysisData = Analysis.analyseFromString(replayData, "FILE"),
            useReplayResultChannelAnyways = true
        )
    }

}
