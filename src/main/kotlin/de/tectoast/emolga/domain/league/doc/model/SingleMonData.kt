package de.tectoast.emolga.domain.league.doc.model

import de.tectoast.emolga.domain.pokemon.model.ShowdownID

data class SingleMonData(
    val showdownId: ShowdownID, val matchNum: Int, val monIndex: Int, val data: Map<DocDataProviderConfig, Any>
)
