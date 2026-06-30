package de.tectoast.emolga.domain.league.teamgraphic.service

import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.league.draft.repository.LeaguePickRepository
import de.tectoast.emolga.domain.league.member.repository.LeagueMemberRepository
import de.tectoast.emolga.domain.league.signup.model.SignupEntry
import de.tectoast.emolga.domain.league.signup.model.SignupInput
import de.tectoast.emolga.domain.league.signup.repository.SignupRepository
import de.tectoast.emolga.domain.league.signup.service.logo.LogoCloud
import de.tectoast.emolga.domain.league.teamgraphic.model.TeamData
import de.tectoast.emolga.domain.league.tierlist.repository.TierlistRepository
import de.tectoast.emolga.domain.league.tierlist.service.action.dispatcher.TierlistActionDispatcher
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.userdata.service.DiscordUserService
import de.tectoast.emolga.utils.newThreadSafeCache
import mu.KotlinLogging
import org.koin.core.annotation.Single
import java.awt.image.BufferedImage

@Single
class TeamDataCreationService(
    private val discordUserService: DiscordUserService,
    private val leagueMemberRepo: LeagueMemberRepository,
    private val leagueCoreRepo: LeagueCoreRepository,
    private val leaguePickRepo: LeaguePickRepository,
    private val leagueConfigRepo: LeagueConfigRepository,
    private val tierlistRepo: TierlistRepository,
    private val tierlistActionDispatcher: TierlistActionDispatcher,
    private val signupRepo: SignupRepository,
    private val logoCloud: LogoCloud
) {
    private val logger = KotlinLogging.logger {}
    private val docOrderProviderCache = newThreadSafeCache<String, DocOrderProvider>()
    suspend fun allFromLeague(leagueName: String): List<TeamData> {
        val leagueData = leagueCoreRepo.getScalarLeagueData(leagueName)
        val idMap = leagueMemberRepo.getPrimaryIds(leagueName)
        val ids = idMap.flatMap { it.value }
        discordUserService.getNames(leagueData.guild, ids)
        val docOrderProvider = createDocOrderProvider(leagueName)
        return (0..<idMap.size).map {
            logger.info { "Generating TeamData for team index $it" }
            singleFromLeague(leagueName, it, docOrderProvider = docOrderProvider)
        }
    }

    typealias DocOrderProvider = (List<DraftPokemon>) -> Map<Int, DraftPokemon>

    private val defaultDocOrderProvider: DocOrderProvider =
        { it.mapIndexed { index, pokemon -> index to pokemon }.toMap() }

    private suspend fun createDocOrderProvider(leagueName: String): DocOrderProvider =
        docOrderProviderCache.getOrPut(leagueName) {
            val leagueData = leagueCoreRepo.getScalarLeagueData(leagueName)
            val config = leagueConfigRepo.getConfig(leagueName)
            val tlMeta = tierlistRepo.getMeta(leagueData.guild, config.tlIdentifier) ?: return defaultDocOrderProvider
            val tierlistConfig = tlMeta.config
            return {
                tierlistActionDispatcher.getSortedPicks(tierlistConfig, it)
                    .mapIndexed { index, pokemon -> index to pokemon }.toMap()
            }
        }

    suspend fun singleFromLeague(
        leagueName: String,
        idx: Int,
        takePickCount: Int? = null,
        docOrderProvider: DocOrderProvider? = null
    ): TeamData {

        val picks =
            leaguePickRepo.getPicksForUser(leagueName, idx)
                .let { if (takePickCount != null) it.take(takePickCount) else it }
        return singleFromLeagueUnordered(leagueName, idx, picks, docOrderProvider)
    }

    suspend fun singleFromLeagueUnordered(
        leagueName: String,
        idx: Int,
        picks: List<DraftPokemon>,
        docOrderProvider: DocOrderProvider? = null
    ): TeamData {
        val docOrder: (List<DraftPokemon>) -> Map<Int, DraftPokemon> =
            docOrderProvider ?: createDocOrderProvider(leagueName)
        val ordered = docOrder(picks).mapValues { it.value.showdownId }
        return singleFromLeague(leagueName, idx, ordered)
    }

    private suspend fun singleFromLeague(
        leagueName: String,
        idx: Int,
        picks: Map<Int, ShowdownID>
    ): TeamData {
        val uids = leagueMemberRepo.getPrimaryIds(leagueName, idx)
        val leagueData = leagueCoreRepo.getScalarLeagueData(leagueName)
        val userId = uids.first()
        val signup = signupRepo.getLeagueSignupOfUser(leagueData.guild, userId)!!
        val (_, entry) = signupRepo.getSignupEntryByUserId(signup.id, userId)!!
        val names = discordUserService.getNames(leagueData.guild, uids)
        val teamOwner = uids.joinToString { names[it] ?: "N/A" }
        val teamName = entry.data[SignupInput.TEAMNAME_ID]
        val logo = entry.downloadLogo()
        return TeamData(
            teamOwner = teamOwner,
            teamName = teamName,
            logo = logo,
            picks = picks,
            leaguename = leagueName,
            idx = idx,
            users = uids
        )
    }

    private suspend fun SignupEntry.downloadLogo(): BufferedImage? {
        val checksum = logoIdentifier ?: return null
        return logoCloud.downloadImage(checksum)
    }
}