package de.tectoast.emolga.features.draft

import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.features.*
import de.tectoast.emolga.features.draft.during.generic.K18n_NotInTierlist
import de.tectoast.emolga.features.draft.during.generic.K18n_PokemonNotFound
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.config.LeagueConfig
import de.tectoast.emolga.league.config.Triggers
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.draft.DraftAction
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.draft.isEnglish
import de.tectoast.emolga.utils.filterContainsIgnoreCase
import de.tectoast.emolga.utils.json.mdb
import de.tectoast.emolga.utils.translateToGuildLanguage
import de.tectoast.generic.K18n_TooManyResults
import net.dv8tion.jda.api.interactions.commands.OptionType
import org.litote.kmongo.contains
import org.litote.kmongo.div
import org.litote.kmongo.eq


object AddMonCommand : CommandFeature<AddMonCommand.Args>(
    ::Args, CommandSpec("addmon", K18n_AddMon.Help).apply { inDM = true }) {
    class Args : Arguments() {
        var pokemon by createArg(
            "pokemon", K18n_AddMon.ArgPokemon, OptionType.STRING
        ) {
            validate {
                val user = PrivateCommands.teamSubmitOverride.takeIf { user == Constants.FLOID } ?: user
                val league = mdb.league.findOne(
                    League::config / LeagueConfig::triggers / Triggers::teamSubmit eq true, League::table contains user
                )
                    ?: throw InvalidArgumentException(K18n_AddMon.NotAvailable)
                val guildId = league.guild
                NameConventionsDB.getDiscordTranslation(
                    it, guildId, english = Tierlist[guildId].isEnglish
                ) ?: throw InvalidArgumentException(K18n_PokemonNotFound(it))
            }
            slashCommand(autocomplete = lambda@{ s, event ->
                val user = PrivateCommands.teamSubmitOverride.takeIf { event.user.idLong == Constants.FLOID }
                    ?: event.user.idLong
                val league = mdb.league.findOne(
                    League::config / LeagueConfig::triggers / Triggers::teamSubmit eq true, League::table contains user
                ) ?: return@lambda listOf(K18n_AddMon.NotAvailable.translateToGuildLanguage(event.guild?.idLong))
                val tierlist = league.getTierlistFor(league(user))
                val strings = tierlist.autoComplete().filterContainsIgnoreCase(s)
                if (strings.size > 25) return@lambda listOf(K18n_TooManyResults.translateToGuildLanguage(event.guild?.idLong))
                strings.sortedWith(compareBy({ !it.startsWith(s) }, { it }))
            })
        }
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val user = PrivateCommands.teamSubmitOverride.takeIf { iData.isFlo } ?: iData.user
        League.executeOnFreshLock({
            mdb.league.findOne(
                League::config / LeagueConfig::triggers / Triggers::teamSubmit eq true, League::table contains user
            )
        }, { iData.reply(K18n_AddMon.NotAvailable) }) l@{
            val idx = this(user)
            val tl = getTierlistFor(idx)
            currentOverride = idx
            tierlistOverride = tl
            val official = e.pokemon.official
            val picks = picks.getOrPut(idx) { mutableListOf() }
            if (picks.any { it.name == official }) return@l iData.reply(K18n_AddMon.PokemonAlreadyInTeam)
            if (picks.size >= teamsize) {
                return@l iData.reply(K18n_AddMon.TeamAlreadyComplete(Constants.MYTAG))
            }
            val (tier, _, _) = (tl.getTierOfCommand(e.pokemon, null)
                ?: return@l iData.reply(K18n_NotInTierlist(e.pokemon.tlName)))
            tl.withTL {
                it.handleDraftActionWithGeneralChecks(
                    DraftAction(
                        officialTier = tier,
                        official = official,
                    )
                )
            }?.let { return@l iData.reply(it) }
            picks.add(DraftPokemon(official, tier))
            val picksAsString = convertPicksToString(picks, tl)
            if (picks.size >= teamsize) {
                iData.reply(K18n_AddMon.TeamComplete(picksAsString, Constants.MYTAG))
            } else {
                iData.reply(
                    K18n_AddMon.TeamInProgress(
                        picksAsString,
                        picks.size,
                        teamsize,
                        announceData(withTimerAnnounce = false)
                    )
                )
            }
            save()
        }
    }

    suspend fun convertPicksToString(picks: List<DraftPokemon>, tierlist: Tierlist): String {
        return picks.sortedWith(tierlist.tierorderingComparator)
            .map { "${it.tier}: ${NameConventionsDB.convertOfficialToTL(it.name, tierlist.guildid)}" }
            .joinToString("\n")
    }
}
