package de.tectoast.emolga.domain.league.doc.service.provider.data

import de.tectoast.emolga.domain.game.model.KDWithName
import de.tectoast.emolga.domain.league.doc.model.AdditionalDataProvider
import de.tectoast.emolga.domain.league.doc.model.StatProcessorData
import de.tectoast.emolga.domain.league.doc.model.StatisticSource
import de.tectoast.emolga.domain.league.doc.model.WinLossData
import de.tectoast.emolga.domain.league.gamedata.model.FullGameData
import de.tectoast.emolga.domain.statistics.model.SwitchType
import java.util.*

private fun FullGameData.getStatisticSource(
    data: StatProcessorData, provider: AdditionalDataProvider
): StatisticSource {
    val gameData = games[data.matchNum]
    val events = provider.analysisEventProvider.getEvents(gameData)
    val winnerInSDBattle = events.winLoss.first { it.win }.indexInBattle
    val winnerInGameData = gameData.winnerIndex
    val indexInData = if (winnerInSDBattle == winnerInGameData) {
        data.indexInBattle
    } else {
        1 - data.indexInBattle
    }
    val showdownId = getKDWithName(data).name
    return StatisticSource(events, showdownId, indexInData)
}

fun FullGameData.getActiveTurns(data: StatProcessorData, provider: AdditionalDataProvider): Int {
    val (events, showdownId, indexInData) = getStatisticSource(data, provider)
    val allSwitchOutRowsOfPlayer =
        events.switch.filter { it.pokemon.player == indexInData && it.type == SwitchType.OUT }
            .mapTo(mutableSetOf()) { it.row }
    val allSwitches =
        events.switch.filter { it.pokemon.player == indexInData && it.pokemon.showdownIDInRoster == showdownId }
    val (switchIns, switchOuts) = allSwitches.partition { it.type == SwitchType.IN }
    val faint = events.damage.firstOrNull {
        it.target.player == indexInData && it.target.showdownIDInRoster == showdownId && it.faint
    }?.row ?: Int.MAX_VALUE
    val turns = events.turn.associateTo(TreeMap()) { it.row to it.turn }
    var turnCount = 0
    for (switch in switchIns) {
        var startTurn = turns.floorEntry(switch.row)?.value ?: 0
        if (switch.row !in allSwitchOutRowsOfPlayer) {
            startTurn++
        }
        val matchingSwitchOut = switchOuts.firstOrNull { it.row > switch.row }
        val endRow = matchingSwitchOut?.row ?: faint
        var endTurn = turns.floorEntry(endRow)?.value ?: 1
        if (matchingSwitchOut?.from != "Switch") {
            endTurn++
        }
        turnCount += (endTurn - startTurn).coerceAtLeast(0)
    }
    return turnCount
}

fun FullGameData.getDamageDealt(
    data: StatProcessorData,
    provider: AdditionalDataProvider,
    active: Boolean
): Int {
    val (events, showdownId, indexInData) = getStatisticSource(data, provider)
    return events.damage.filter { it.source.player == indexInData && it.target.player != indexInData && it.source.showdownIDInRoster == showdownId && it.active == active }
        .sumOf { it.percent }
}

fun FullGameData.getDamageTaken(data: StatProcessorData, provider: AdditionalDataProvider): Int {
    val (events, showdownId, indexInData) = getStatisticSource(data, provider)
    return events.damage.filter { it.target.player == indexInData && it.target.showdownIDInRoster == showdownId }
        .sumOf { it.percent }
}

fun FullGameData.getKDWithName(data: StatProcessorData): KDWithName =
    games[data.matchNum].kd[data.indexInBattle][data.monIterationIndex]


fun FullGameData.getWinsLosses(data: StatProcessorData): WinLossData {
    var wins = 0
    var looses = 0
    val game = games[data.matchNum]
    val winnerIndex = game.winnerIndex
    if (data.indexInBattle == winnerIndex) {
        wins++
    } else {
        looses++
    }
    return WinLossData(wins, looses)
}