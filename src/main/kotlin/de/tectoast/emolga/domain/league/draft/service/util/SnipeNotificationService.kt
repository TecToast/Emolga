package de.tectoast.emolga.domain.league.draft.service.util

import de.tectoast.emolga.discord.DMSender
import de.tectoast.emolga.domain.league.draft.model.execution.SnipeMeta
import de.tectoast.emolga.domain.league.member.repository.LeagueMemberRepository
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.pokemon.service.PokemonDisplayService
import de.tectoast.emolga.features.league.draft.K18n_QueuePicks
import de.tectoast.emolga.utils.Language
import de.tectoast.k18n.generated.K18nLanguage
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.into
import org.koin.core.annotation.Single

@Single
class SnipeNotificationService(
    private val dmSender: DMSender,
    private val leagueMemberRepository: LeagueMemberRepository,
    private val pokemonDisplayService: PokemonDisplayService
) {

    suspend fun handleDraftSnipes(
        leagueName: String,
        guild: Long,
        displayName: String,
        snipeMap: Map<Int, SnipeMeta>,
        k18nLanguage: K18nLanguage,
        pokemonLanguage: Language,
    ) {
        val idsToPing = leagueMemberRepository.getAllIdsToPing(leagueName)
        val allSnipedMons = snipeMap.flatMap { it.value.list }.map { it.pokemon }.toSet()
        val displayNames = pokemonDisplayService.getDisplayNames(allSnipedMons, guild, pokemonLanguage)
        snipeMap.entries.forEach { (idx, snipes) ->
            idsToPing[idx]?.forEach { notifySnipes(it, displayName, snipes, k18nLanguage, displayNames) }
        }
    }

    private fun notifySnipes(
        userId: Long,
        leagueDisplayName: String,
        snipes: SnipeMeta,
        language: K18nLanguage,
        displayNames: Map<ShowdownID, String>
    ) {

        val snipeMessage = snipes.list.joinToString("\n") {
            displayNames[it.pokemon] ?: it.pokemon.value
        }
        val description = (if (snipes.disableIfSniped) {
            K18n_QueuePicks.SnipeWarningDisabled(
                snipeMessage
            )
        } else K18n_QueuePicks.SnipeWarningEnabled(
            snipeMessage
        )).translateTo(language)
        val message = MessageCreate(
            embeds = Embed(
                title = "Queue - $leagueDisplayName", color = 0xFF0000, description = description
            ).into()
        )
        dmSender.sendDM(userId, message)
    }

}
