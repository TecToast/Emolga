package de.tectoast.emolga.features.league.result

import de.tectoast.emolga.domain.league.result.service.ResultStartService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.league.K18n_EnterResult
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.k18n
import de.tectoast.emolga.utils.msg
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class ResultWithGuildCommand(private val resultStartService: ResultStartService) :
    CommandFeature<ResultWithGuildCommand.Args>(
        ::Args, CommandSpec("reswithguild", K18n_EnterResult.ResultHelp)
    ) {
    class Args : Arguments() {
        var guild by long("guild", "guild".k18n)
        var user by long("user", "user".k18n)
        var opponent by long("opponent", "opponent".k18n)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.deferReply(true)
        val result = resultStartService.handleStart(opponent = e.opponent, user = e.user, guild = e.guild)
        iData.reply(result.msg(), ephemeral = true)
    }
}