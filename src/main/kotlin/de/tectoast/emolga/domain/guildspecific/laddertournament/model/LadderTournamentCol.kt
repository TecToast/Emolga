package de.tectoast.emolga.domain.guildspecific.laddertournament.model

import kotlinx.serialization.Serializable
import kotlin.reflect.KProperty1

@Serializable
enum class LadderTournamentCol(private val property: KProperty1<LadderRankData, Number>) {
    WINS(LadderRankData::wins), LOSSES(LadderRankData::losses), TIES(LadderRankData::ties), GXE(LadderRankData::gxe), ELO(
        LadderRankData::elo
    );

    operator fun get(data: LadderRankData?) = data?.let { property.get(it) } ?: 0
}