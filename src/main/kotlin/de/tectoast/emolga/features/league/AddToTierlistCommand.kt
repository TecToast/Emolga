package de.tectoast.emolga.features.league

import de.tectoast.emolga.domain.league.draft.repository.DraftAdminRepository
import de.tectoast.emolga.domain.league.tierlist.service.core.TierDataService
import de.tectoast.emolga.domain.pokemon.repository.PokemonNamesRepository
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.filterStartsWithIgnoreCase
import de.tectoast.emolga.utils.msg
import org.koin.core.annotation.Single
import org.koin.core.component.inject

@Single(binds = [ListenerProvider::class])
class AddToTierlistCommand(
    private val draftAdminRepo: DraftAdminRepository,
    private val tierDataService: TierDataService
) :
    CommandFeature<AddToTierlistCommand.Args>(
        ::Args,
        CommandSpec("addtotierlist", K18n_AddToTierlist.Help)
    ) {

    class Args : Arguments() {
        private val pokemonNamesRepo: PokemonNamesRepository by inject()
        private val tierDataService: TierDataService by inject()

        var mon by draftPokemon("Mon", K18n_AddToTierlist.ArgPokemon) { s, _ ->
            pokemonNamesRepo.getOfficialNames(s, limit = 25).sorted()
        }
        var tier by string("Tier", K18n_AddToTierlist.ArgTier) {
            slashCommand { s, event ->
                tierDataService.getTiersOnGuild(event.guild!!.idLong).filterStartsWithIgnoreCase(s).sorted()
            }
        }.nullable()
        var identifier by string("Identifier", K18n_AddToTierlist.ArgIdentifier).default("")
    }

    init {
        restrict { admin(this) || draftAdminRepo.isAdmin(gid, user, data.memberRoles) }
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val result = tierDataService.addPokemon(iData.gid, e.identifier, e.mon, e.tier)
        iData.reply(result.msg())
    }
}
