package de.tectoast.emolga.domain.league.draft.service.core.ban

import de.tectoast.emolga.domain.league.draft.model.ban.BanRoundConfig
import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.utils.handler.BaseHandler
import de.tectoast.k18n.generated.K18nMessage

interface BanRoundConfigOperations<C : BanRoundConfig> {
    fun checkBan(config: C, tier: String, alreadyBanned: Set<DraftPokemon>): K18nMessage?
    fun getPossibleBanTiers(config: C, alreadyBanned: Set<DraftPokemon>): List<String>
}

interface BanRoundConfigHandler<C : BanRoundConfig> : BaseHandler<C>, BanRoundConfigOperations<C>