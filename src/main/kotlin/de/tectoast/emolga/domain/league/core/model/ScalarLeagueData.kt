package de.tectoast.emolga.domain.league.core.model

data class ScalarLeagueData(
    val leagueName: String,
    val prettyName: String?,
    val num: Int,
    val guild: Long,
    val sheetId: String,
    val draftChannel: Long?
) {
    val displayName get() = prettyName ?: leagueName
}
