package de.tectoast.emolga.domain.league.tierlist.service.core

import de.tectoast.emolga.domain.league.teamgraphic.repository.PokemonCropRepository
import de.tectoast.emolga.domain.league.tierlist.model.TierData
import de.tectoast.emolga.domain.league.tierlist.model.TierlistMeta
import de.tectoast.emolga.domain.league.tierlist.model.config.TierBasedTierlistConfig
import de.tectoast.emolga.domain.league.tierlist.repository.TierlistRepository
import de.tectoast.emolga.domain.league.tierlist.service.action.dispatcher.TierlistActionDispatcher
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.features.league.K18n_AddToTierlist
import de.tectoast.emolga.features.league.draft.generic.K18n_NoTierlist
import de.tectoast.emolga.features.league.draft.generic.K18n_TierNotFound
import de.tectoast.emolga.utils.CalcResult
import de.tectoast.emolga.utils.K18nMessageOrError
import de.tectoast.emolga.utils.draft.K18n_DraftUtils
import de.tectoast.emolga.utils.error
import de.tectoast.emolga.utils.success
import io.r2dbc.spi.R2dbcDataIntegrityViolationException
import org.jetbrains.exposed.v1.r2dbc.ExposedR2dbcException
import org.koin.core.annotation.Single

@Single
class TierDataService(
    private val repo: TierlistRepository,
    private val dispatcher: TierlistActionDispatcher,
    private val cropRepo: PokemonCropRepository
) {
    suspend fun getTierData(
        meta: TierlistMeta,
        showdownId: ShowdownID,
        requestedTier: String?,
        identifier: String = meta.identifier
    ): CalcResult<TierData> {
        val real =
            repo.getTier(meta.guild, identifier, showdownId) ?: return K18n_DraftUtils.PokemonNotInTierlist.error()
        if (requestedTier != null && meta.config is TierBasedTierlistConfig) {
            val existingTiers = dispatcher.getTiers(meta.config)
            val specifiedTier =
                existingTiers.firstOrNull { it.equals(requestedTier, ignoreCase = true) } ?: return K18n_TierNotFound(
                    requestedTier
                ).error()
            return TierData(specified = specifiedTier, official = real, isTierSpecified = true).success()
        }
        return TierData(specified = real, official = real, isTierSpecified = false).success()
    }

    suspend fun getTiersOnGuild(guild: Long): Set<String> {
        return repo.getAllMetasForGuild(guild).flatMap { meta ->
            dispatcher.getTiers(meta.config)
        }.toSet()
    }

    suspend fun addPokemon(guild: Long, identifier: String, showdownId: ShowdownID, tier: String?): K18nMessageOrError {
        val tierlist = repo.getMeta(guild, identifier) ?: return K18n_NoTierlist.error()
        val allTiers = dispatcher.getTiers(tierlist.config)
        val tier = tier ?: allTiers.last()
        if (tier !in allTiers) {
            return K18n_TierNotFound(tier).error()
        }
        try {
            repo.addPokemon(guild, identifier, showdownId, tier)
        } catch (ex: ExposedR2dbcException) {
            if (ex.cause is R2dbcDataIntegrityViolationException) {
                return K18n_AddToTierlist.PokemonAlreadyInTierlist(showdownId.value).error()
            }
            throw ex
        }
        return K18n_AddToTierlist.Success(showdownId.value, tier).success()
    }


}
