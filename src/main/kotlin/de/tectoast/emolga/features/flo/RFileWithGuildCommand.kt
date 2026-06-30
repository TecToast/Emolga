package de.tectoast.emolga.features.flo

import de.tectoast.emolga.domain.game.model.analysis.ShowdownLogProvider
import de.tectoast.emolga.domain.game.service.process.FullInputGameBuilder
import de.tectoast.emolga.domain.game.service.process.GameProcessService
import de.tectoast.emolga.features.*
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.interaction.toK18nMessageSender
import de.tectoast.emolga.features.interaction.toMessageSender
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.getOrReturn
import de.tectoast.emolga.utils.k18n
import kotlinx.coroutines.future.await
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class RFileWithGuildCommand(
    private val gameProcessService: GameProcessService,
    private val fullInputGameBuilder: FullInputGameBuilder
) :
    CommandFeature<RFileWithGuildCommand.Args>(::Args, CommandSpec("rfilewithguild", "Replay-File with guild".k18n)) {
    init {
        restrict(flo)
    }

    class Args : Arguments() {
        var guild by long("gid", "gid".k18n)
        var file by attachment("file", "file".k18n)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.deferReply()
        val replayData =
            e.file.proxy.download().await().bufferedReader().use { it.readText() }
                .substringAfter("class=\"battle-log-data\">").substringBefore("</script>").split("\n")
        val infoSender = iData.toK18nMessageSender(true)
        val fullInputGame = fullInputGameBuilder.fromShowdown(
            e.guild,
            listOf(ShowdownLogProvider.ReplayLog(replayData)),
            infoSender
        ).getOrReturn<_, Unit> { iData.reply(it.message); return }
        gameProcessService.analyseGame(
            fullInputGame = fullInputGame,
            infoSender = infoSender,
            replaySender = iData.toMessageSender(false),
            resultchannelParam = iData.tc,
            guildOfChannel = iData.gid,
            customGuild = e.guild,
        )
    }
}
