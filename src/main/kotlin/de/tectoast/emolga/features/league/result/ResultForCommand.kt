package de.tectoast.emolga.features.league.result

import de.tectoast.emolga.domain.league.result.service.ResultStartService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.league.K18n_EnterResult
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.msg
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class ResultForCommand(private val resultStartService: ResultStartService) : CommandFeature<ResultForCommand.Args>(
    ::Args, CommandSpec(
        "resultfor", K18n_EnterResult.ResultForHelp,
    )
) {
    init {
        restrict(admin)
    }

    class Args : Arguments() {
        var user by member("User 1", K18n_EnterResult.ResultForArgUser)
        var opponent by member("User 2", K18n_EnterResult.ResultForArgOpponent)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.deferReply(true)
        val result = resultStartService.handleStart(
            opponent = e.opponent.idLong,
            user = e.user.idLong,
            guild = iData.gid
        )
        iData.reply(result.msg(), ephemeral = true)
    }
}