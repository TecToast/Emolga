package de.tectoast.emolga.domain.league.doc.service.provider.monname

import de.tectoast.emolga.domain.pokemon.model.ShowdownID

interface MonNameProvider {
    suspend fun getDisplayName(showdownId: ShowdownID): String
}