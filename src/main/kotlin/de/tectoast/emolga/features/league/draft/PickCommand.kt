package de.tectoast.emolga.features.league.draft

import de.tectoast.emolga.database.exposed.FreePickPriceConfig
import de.tectoast.emolga.database.exposed.TierBasedPriceConfig
import de.tectoast.emolga.database.exposed.TierlistRepository
import de.tectoast.emolga.database.exposed.UpdraftConfig
import de.tectoast.emolga.database.league.*
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.isError
import org.koin.core.component.inject

class PickCommand(val draftService: DraftService) :
    CommandFeature<PickCommand.Args>(PickCommand::Args, CommandSpec("pick", K18n_Pick.Help)) {

    // TODO autocomplete for tiers

    class Args : Arguments() {
        val tierlistRepo: TierlistRepository by inject()
        val leagueConfigRepo: LeagueConfigRepository by inject()
        val leagueCoreRepo: LeagueCoreRepository by inject()

        var pokemon by draftPokemon("pokemon", K18n_Pick.ArgPokemon)
        var tier by string("tier", K18n_Pick.ArgTier) {
            slashCommand(guildChecker = {
                if (gid == Constants.G.MY) return@slashCommand ArgumentPresence.OPTIONAL
                if (tierlistRepo.getAllMetasForGuild(gid)
                        .any { meta -> meta.priceConfig is TierBasedPriceConfig && meta.priceConfig.updraftConfig != UpdraftConfig.Disabled }
                ) ArgumentPresence.OPTIONAL
                else ArgumentPresence.NOT_PRESENT
            })
        }.nullable()
        var free by boolean("free", K18n_Pick.ArgFree) {
            default = false
            slashCommand(guildChecker = {
                if (gid == Constants.G.MY) ArgumentPresence.OPTIONAL
                else when (tierlistRepo.getAllMetasForGuild(gid)
                    .any { meta -> meta.priceConfig is FreePickPriceConfig }) {
                    true -> ArgumentPresence.OPTIONAL
                    else -> ArgumentPresence.NOT_PRESENT
                }
            })
        }
        var tera by boolean("tera", K18n_Pick.ArgTera) {
            default = false
            slashCommand(guildChecker = {
                if (gid == Constants.G.MY) return@slashCommand ArgumentPresence.OPTIONAL
                val leagueNames = leagueCoreRepo.getLeagueNamesByGuild(gid)
                if (leagueNames.any { leagueConfigRepo.getConfig(it).teraPick != null }) ArgumentPresence.OPTIONAL
                else ArgumentPresence.NOT_PRESENT
            })
        }
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.deferReply(ephemeral = true)
        val validationCompleteCallback = suspend {
            iData.reply("\uD83D\uDC4D", ephemeral = true)
        }
        val result = draftService.executeNormal(
            PickInput(e.pokemon, e.tier, e.free, e.tera),
            DraftMessageType.REGULAR,
            iData.tc,
            iData.user,
            iData.member().unsortedRoles.mapNotNull { it.idLong }.toSet(),
            validationCompleteCallback
        )
        if (result.isError()) {
            iData.reply(result.message, ephemeral = true)
        }
    }

}
