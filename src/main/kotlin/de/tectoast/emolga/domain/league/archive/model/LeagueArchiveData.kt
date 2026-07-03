package de.tectoast.emolga.domain.league.archive.model

import de.tectoast.emolga.domain.league.config.model.LeagueConfig
import de.tectoast.emolga.domain.league.core.model.DraftRelevantLeagueData
import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.league.gamedata.model.FullGameData
import de.tectoast.emolga.domain.league.gamedata.model.LeagueEvent
import de.tectoast.emolga.domain.league.member.model.LeagueParticipant
import de.tectoast.emolga.domain.league.prediction.model.PredictionGameVoteData
import kotlinx.serialization.Serializable

@Serializable
data class LeagueArchiveData(
    val draftRelevantData: DraftRelevantLeagueData,
    val picks: Map<Int, List<DraftPokemon>>,
    val config: LeagueConfig,
    val eventData: List<LeagueEvent>,
    val participants: List<LeagueParticipant>,
    val games: List<FullGameData>,
    val predictionGame: List<PredictionGameVoteData>
)