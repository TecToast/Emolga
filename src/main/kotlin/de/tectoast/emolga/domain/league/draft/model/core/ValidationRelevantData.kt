package de.tectoast.emolga.domain.league.draft.model.core

data class ValidationRelevantData(val picks: List<DraftPokemon>, val idx: Int, val teamSize: Int)