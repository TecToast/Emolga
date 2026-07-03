package de.tectoast.emolga.domain.league.archive.service

import de.tectoast.emolga.domain.league.archive.model.LeagueArchiveData
import de.tectoast.emolga.domain.league.archive.repository.LeagueArchiveRepository
import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.draft.repository.LeaguePickRepository
import de.tectoast.emolga.domain.league.gamedata.repository.GameDataRepository
import de.tectoast.emolga.domain.league.gamedata.repository.LeagueEventRepository
import de.tectoast.emolga.domain.league.member.repository.LeagueMemberRepository
import de.tectoast.emolga.domain.league.prediction.repository.PredictionGameVoteRepository
import org.koin.core.annotation.Single

@Single
class LeagueArchiveService(
    private val repo: LeagueArchiveRepository,
    private val coreRepo: LeagueCoreRepository,
    private val configRepo: LeagueConfigRepository,
    private val pickRepo: LeaguePickRepository,
    private val eventRepo: LeagueEventRepository,
    private val memberRepo: LeagueMemberRepository,
    private val gameDataRepo: GameDataRepository,
    private val predictionRepo: PredictionGameVoteRepository
) {
    suspend fun archive(leagueName: String) {
        val draftRelevantData = coreRepo.getDraftRelevantData(leagueName, locking = false, checkRunning = false)!!
        val archiveData = LeagueArchiveData(
            draftRelevantData = draftRelevantData,
            config = configRepo.getConfig(leagueName),
            picks = pickRepo.getAllPicks(leagueName),
            eventData = eventRepo.getAllEvents(leagueName),
            participants = memberRepo.getAllParticipants(leagueName),
            games = gameDataRepo.getAllFullGameDatas(leagueName),
            predictionGame = predictionRepo.getAllPredictionGameVotes(leagueName)
        )
        repo.save(leagueName, archiveData)
        coreRepo.delete(leagueName)
    }

    suspend fun archiveGuild(guild: Long) {
        for (leagueName in coreRepo.getLeagueNamesByGuild(guild)) {
            archive(leagueName)
        }
    }

}