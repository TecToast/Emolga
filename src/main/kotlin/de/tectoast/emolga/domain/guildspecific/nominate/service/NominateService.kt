package de.tectoast.emolga.domain.guildspecific.nominate.service

import de.tectoast.emolga.domain.guildspecific.nominate.model.NominationCurrentWeek
import de.tectoast.emolga.domain.guildspecific.nominate.repository.NominateRepository
import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.core.repository.LeagueAttributesRepository
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.draft.repository.LeaguePickRepository
import de.tectoast.emolga.domain.league.tierlist.repository.TierlistRepository
import de.tectoast.emolga.domain.league.tierlist.service.action.dispatcher.TierlistActionDispatcher
import de.tectoast.emolga.domain.statestore.model.NominateState
import de.tectoast.emolga.domain.statestore.service.NominateStateStoreHandler
import de.tectoast.emolga.domain.statestore.service.StateStoreDispatcher
import de.tectoast.emolga.features.league.K18n_Nominate
import de.tectoast.emolga.features.league.NominateButton
import de.tectoast.emolga.features.league.draft.generic.K18n_NoTierlist
import de.tectoast.emolga.utils.*
import de.tectoast.k18n.generated.K18N_DEFAULT_LANGUAGE
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.into
import net.dv8tion.jda.api.components.actionrow.ActionRow.of
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


@Single
class NominateService(
    private val leagueCoreRepo: LeagueCoreRepository,
    private val leaguePickRepo: LeaguePickRepository,
    private val leagueConfigRepo: LeagueConfigRepository,
    private val tierlistRepo: TierlistRepository,
    private val tierlistActionDispatcher: TierlistActionDispatcher,
    private val nominateRepo: NominateRepository,
    private val leagueAttributesRepo: LeagueAttributesRepository,
    private val botConstants: BotConstants
) : KoinComponent {
    private val stateStore: StateStoreDispatcher by inject()
    private suspend fun findCurrentNDSLeague() = leagueCoreRepo.getLeagueNameByPrefix("NDS").firstOrNull()
    suspend fun handleStart(
        userId: Long,
        potentialNomUser: Int?,
        nominateBtn: NominateButton
    ): CalcResult<MessageCreateData> {
        val language = K18N_DEFAULT_LANGUAGE
        val leagueName = findCurrentNDSLeague() ?: return K18n_Nominate.NoActiveLeague.error()
        val league = leagueCoreRepo.getLeagueWithParticipants(leagueName) ?: return K18n_Nominate.NoActiveLeague.error()
        if (userId != botConstants.botOwnerId && userId !in league.users) return K18n_Nominate.NoActiveLeague.error()
        val idx =
            if (userId == botConstants.botOwnerId) potentialNomUser!! else league.users.indexOf(
                userId
            )
        val currentWeek = leagueAttributesRepo.getOrDefault(leagueName, NominationCurrentWeek)
        if (nominateRepo.hasNominated(leagueName, currentWeek, idx)) return K18n_Nominate.AlreadyNominated(currentWeek)
            .error()
        val config = leagueConfigRepo.getConfig(leagueName)
        val meta = tierlistRepo.getMeta(guildId = league.guild, config.tlIdentifier) ?: return K18n_NoTierlist.error()
        val comparator = tierlistActionDispatcher.getTierOrderingComparatorWithoutName(meta.config)
        val picks = leaguePickRepo.getPicksForUser(leagueName, idx)
        val sortedPicks = picks.sortedWith(comparator)
        return stateStore.process<_, NominateStateStoreHandler, _>(NominateState(idx, picks, sortedPicks), userId) {
            MessageCreate(
                embeds = Embed(
                    title = K18n_Nominate.EmbedTitle.translateTo(language),
                    color = Constants.EMBED_COLOR,
                    description = generateDescription()
                ).into(), components = sortedPicks.map {
                    nominateBtn.withoutIData(
                        language = language,
                        label = it.showdownId.value.k18n, buttonStyle = ButtonStyle.PRIMARY
                    ) { data = it.showdownId; mode = NominateButton.Mode.UNNOMINATE }
                }.chunked(5).map { of(it) }.toMutableList().apply {
                    add(
                        of(
                            nominateBtn.withoutIData(
                                language = language,
                                buttonStyle = ButtonStyle.SUCCESS,
                                emoji = Emoji.fromUnicode("✅"),
                                disabled = true
                            ) { mode = NominateButton.Mode.FINISH })
                    )
                })
        }.success()
    }

    suspend fun handleFinish(idx: Int, nominated: List<Int>): K18nMessageOrError {
        val leagueName = findCurrentNDSLeague() ?: return K18n_Nominate.NoActiveLeague.error()
        val currentWeek = leagueAttributesRepo.getOrDefault(leagueName, NominationCurrentWeek)
        if (nominateRepo.hasNominated(leagueName, currentWeek, idx)) return K18n_Nominate.AlreadyNominated(currentWeek)
            .error()
        nominateRepo.setNominations(leagueName, currentWeek, idx, nominated)
        return K18n_Nominate.NominationSaved(currentWeek).success()
    }
}


