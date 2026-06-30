package de.tectoast.emolga.domain.league.transaction.service

import de.tectoast.emolga.discord.ChannelInterface
import de.tectoast.emolga.discord.sendMessage
import de.tectoast.emolga.domain.league.core.model.ScalarLeagueData
import de.tectoast.emolga.domain.league.gamedata.repository.LeagueEventRepository
import de.tectoast.emolga.domain.league.member.repository.LeagueMemberRepository
import de.tectoast.emolga.domain.league.transaction.model.TransactionRequestData
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.pokemon.service.PokemonDisplayService
import de.tectoast.emolga.features.league.K18n_Transaction
import de.tectoast.emolga.utils.Language
import de.tectoast.emolga.utils.joinToTeammates
import de.tectoast.k18n.generated.K18nLanguage
import org.koin.core.annotation.Single

@Single
class SuccessfulTransactionHandler(
    private val channelInterface: ChannelInterface,
    private val leagueMemberRepo: LeagueMemberRepository,
    private val pokemonDisplayService: PokemonDisplayService,
    private val leagueEventRepo: LeagueEventRepository,
    private val transactionExecutionService: TransactionExecutionService
) {
    suspend fun handleSuccessfulTransaction(
        idx: Int,
        leagueData: ScalarLeagueData,
        request: TransactionRequestData,
        week: Int,
        remainingPoints: Int,
        teraUserChanges: List<Pair<ShowdownID, ShowdownID>>
    ) {
        val leagueName = leagueData.leagueName
        val primaryIds = leagueMemberRepo.getPrimaryIds(leagueName, idx)
        val displayNames = pokemonDisplayService.getDisplayNames(
            buildSet {
                addAll(request.picks)
                addAll(request.drops)
                addAll(teraUserChanges.flatMap { listOf(it.first, it.second) })
            },
            leagueData.guild,
            Language.ENGLISH
        )
        leagueData.draftChannel?.let { draftChannel ->
            val message = with(displayNames) {
                K18n_Transaction.Done(
                    primaryIds.joinToTeammates(),
                    request.drops.joinToString("\n") { it.toDisplayName() },
                    request.picks.joinToString("\n") { it.toDisplayName() },
                    teraUserChanges.joinToString("\n") { (old, new) ->
                        "${old.toDisplayName()} -> ${new.toDisplayName()}"
                    },
                    remainingPoints,
                    week + 1
                )
            }
            channelInterface.sendMessage(
                draftChannel, message.translateTo(K18nLanguage.EN)
            )
        }
        if (leagueEventRepo.hasPlayedGame(leagueName, week, idx)) {
            transactionExecutionService.registerTransactions(leagueName, week, listOf(idx))
        }
    }

    context(displayNames: Map<ShowdownID, String>)
    private fun ShowdownID.toDisplayName() = displayNames[this] ?: this.value
}