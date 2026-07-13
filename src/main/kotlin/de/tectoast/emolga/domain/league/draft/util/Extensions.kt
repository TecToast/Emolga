package de.tectoast.emolga.domain.league.draft.util

import de.tectoast.emolga.domain.league.draft.model.core.DraftRunContext
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.pokemon.service.PokemonDisplayService

suspend fun PokemonDisplayService.getDisplayName(showdownId: ShowdownID, draftRunContext: DraftRunContext, withAdditionalEnglish: Boolean = false) =
    getDisplayName(showdownId, draftRunContext.league.guild, draftRunContext.tierlistMeta.language, withAdditionalEnglish)