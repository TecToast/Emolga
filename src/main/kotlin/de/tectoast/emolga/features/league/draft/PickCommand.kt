package de.tectoast.emolga.features.league.draft

import de.tectoast.emolga.features.*
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.config.LeagueConfig
import de.tectoast.emolga.league.config.Triggers
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.draft.*
import de.tectoast.emolga.utils.filterStartsWithIgnoreCase
import de.tectoast.emolga.utils.json.mdb
import org.litote.kmongo.div
import org.litote.kmongo.eq

object PickCommand :
    CommandFeature<PickCommand.Args>(PickCommand::Args, CommandSpec("pick", K18n_Pick.Help)) {


    class Args : Arguments() {
        var pokemon by draftPokemon("pokemon", K18n_Pick.ArgPokemon)
        var tier by string("tier", K18n_Pick.ArgTier) {
            slashCommand(autocomplete = { s, event ->
                val league = League.onlyChannel(event.channel.idLong) ?: return@slashCommand null
                val current = league.currentOrFromID(event.user.idLong) ?: return@slashCommand null
                league.tierlist.withTierBasedPriceManager(league) { it.getCurrentAvailableTiers() }
                    ?.filterStartsWithIgnoreCase(s) ?: listOf("Keine Tiers verfügbar")
            }, guildChecker = {
                if (gid == Constants.G.MY) ArgumentPresence.OPTIONAL
                else
                    if (mdb.league.findOne(
                            League::guild eq gid,
                            League::config / LeagueConfig::triggers / Triggers::updraftDisabled eq true
                        ) != null
                    ) ArgumentPresence.NOT_PRESENT
                    else ArgumentPresence.OPTIONAL
            })
        }.nullable()
        var free by boolean("free", K18n_Pick.ArgFree) {
            default = false
            slashCommand(guildChecker = {
                if (gid == Constants.G.MY) ArgumentPresence.OPTIONAL
                else
                    when (Tierlist[gid]?.has<FreePickPriceManager>()) {
                        true -> ArgumentPresence.OPTIONAL
                        else -> ArgumentPresence.NOT_PRESENT
                    }
            })
        }
        var tera by boolean("tera", K18n_Pick.ArgTera) {
            default = false
            slashCommand(guildChecker = {
                if (gid == Constants.G.MY || league()?.config?.teraPick != null) ArgumentPresence.OPTIONAL
                else ArgumentPresence.NOT_PRESENT
            })
        }
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        League.executePickLike {
            DraftUtils.executeWithinLock(
                PickInput(e.pokemon, e.tier, e.free, e.tera), DraftMessageType.REGULAR
            )
        }
    }

}
