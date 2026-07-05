package de.tectoast.emolga.features.league.result

import de.tectoast.emolga.domain.config.repository.GuildConfigRepository
import de.tectoast.emolga.domain.league.core.model.LeagueWithParticipants
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.result.service.ResultCacheService
import de.tectoast.emolga.domain.league.result.service.ResultStartService
import de.tectoast.emolga.features.K18n_Arguments
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.league.K18n_EnterResult
import de.tectoast.emolga.features.league.draft.generic.K18n_NoLeagueForGuildFound
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.msg
import org.koin.core.annotation.Single
import org.koin.core.component.inject

@Single(binds = [ListenerProvider::class])
class ResultCommand(
    private val resultCacheService: ResultCacheService,
    private val resultStartService: ResultStartService
) :
    CommandFeature<ResultCommand.Args>(
        ::Args, CommandSpec(
            "result", K18n_EnterResult.ResultHelp
        )
    ) {

    class Args : Arguments() {

        val leagueCoreRepo: LeagueCoreRepository by inject()
        private val resultCacheService: ResultCacheService by inject()
        val languageRepo: GuildConfigRepository by inject()

        var opponent by fromListCommand("Opponent", K18n_EnterResult.ResultArgOpponent, {
            val gid = it.guild!!.idLong
            leagueCoreRepo.getLeagueWithParticipants(gid, it.user.idLong).handle(it.user.idLong, gid)
        })

        private suspend fun LeagueWithParticipants?.handle(user: Long, gid: Long?): Collection<String> {
            this ?: return listOf(K18n_NoLeagueForGuildFound.translateTo(languageRepo.getLanguage(gid)))
            resultCacheService.setLeague(user, leagueName)
            return resultCacheService.getNamesOrFetch(this)
        }
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val opponent = resultCacheService.resolveOpponent(iData.user, e.opponent) ?: return iData.reply(
            K18n_Arguments.NotAutocompleteConform, ephemeral = true
        )
        iData.deferReply(true)
        val result = resultStartService.handleStart(opponent = opponent, user = iData.user, guild = iData.gid)
        iData.reply(result.msg(), ephemeral = true)
    }
}