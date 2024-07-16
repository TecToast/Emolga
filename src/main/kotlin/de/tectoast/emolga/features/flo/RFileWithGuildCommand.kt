package de.tectoast.emolga.features.flo

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.showdown.Analysis
import dev.minn.jda.ktx.coroutines.await

object RFileWithGuildCommand :
    CommandFeature<RFileWithGuildCommand.Args>(::Args, CommandSpec("rfilewithguild", "Replay-File with guild")) {
    init {
        restrict(flo)
    }

    class Args : Arguments() {
        var guild by long("gid", "gid")
        var file by attachment("file", "file")
    }

    context(InteractionData) override suspend fun exec(e: Args) {
        deferReply()
        val replayData =
            e.file.proxy.download().await().bufferedReader().use { it.readText() }
                .substringAfter("class=\"battle-log-data\">").substringBefore("</script>").split("\n")
        Analysis.analyseReplay(
            "FILE",
            resultchannelParam = textChannel,
            customGuild = e.guild,
            fromReplayCommand = self,
            analysisData = Analysis.analyseFromLog(replayData, "FILE"),
            useReplayResultChannelAnyways = true
        )
    }
}
