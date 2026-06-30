package de.tectoast.emolga.domain.guildspecific.laddertournament.model

data class LadderTournamentUserData(val sdName: String, val formats: List<String>, var verified: Boolean = true)