package de.tectoast.emolga.features.draft

import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.features.*
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.config.LeagueConfig
import de.tectoast.emolga.league.config.Triggers
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.draft.isEnglish
import de.tectoast.emolga.utils.filterContainsIgnoreCase
import de.tectoast.emolga.utils.json.db
import net.dv8tion.jda.api.interactions.commands.OptionType
import org.litote.kmongo.contains
import org.litote.kmongo.div
import org.litote.kmongo.eq


object AddMonCommand : CommandFeature<AddMonCommand.Args>(
    ::Args, CommandSpec("addmon", "Fügt ein Pokemon zu deinem Team hinzu").apply { inDM = true }) {
    class Args : Arguments() {
        var pokemon by createArg(
            "pokemon", "Das Pokemon, das zum Team hinzugefügt werden soll", OptionType.STRING
        ) {
            validate {
                val user = PrivateCommands.teamSubmitOverride.takeIf { user == Constants.FLOID } ?: user
                val league = db.league.findOne(
                    League::config / LeagueConfig::triggers / Triggers::teamSubmit eq true, League::table contains user
                )
                    ?: throw InvalidArgumentException("Du nimmst an keiner Liga teil, bei der man ein Team einreichen muss!")
                val guildId = league.guild
                NameConventionsDB.getDiscordTranslation(
                    it, guildId, english = Tierlist[guildId].isEnglish
                ) ?: throw InvalidArgumentException("Pokemon `$it` nicht gefunden!")
            }
            slashCommand(autocomplete = lambda@{ s, event ->
                val user = PrivateCommands.teamSubmitOverride.takeIf { event.user.idLong == Constants.FLOID }
                    ?: event.user.idLong
                val league = db.league.findOne(
                    League::config / LeagueConfig::triggers / Triggers::teamSubmit eq true,
                    League::table contains user
                ) ?: return@lambda listOf("Du nimmst an keiner Liga teil, bei der man ein Team einreichen muss!")
                val tierlist = league.getTierlistFor(league(user))
                val strings = tierlist.autoComplete().filterContainsIgnoreCase(s)
                if (strings.size > 25) return@lambda listOf("Zu viele Ergebnisse, bitte spezifiziere deine Suche!")
                strings.sortedWith(compareBy({ !it.startsWith(s) }, { it }))
            })
        }
    }

    context(iData: InteractionData) override suspend fun exec(e: Args) {
        val user = PrivateCommands.teamSubmitOverride.takeIf { iData.isFlo } ?: iData.user
        League.executeOnFreshLock({
            db.league.findOne(
                League::config / LeagueConfig::triggers / Triggers::teamSubmit eq true,
                League::table contains user
            )
        }, { iData.reply("Du nimmst an keiner Liga teil, bei der man ein Team einreichen muss!") }) l@{
            val idx = this(user)
            val tl = getTierlistFor(idx)
            currentOverride = idx
            tierlistOverride = tl
            val official = e.pokemon.official
            val picks = picks.getOrPut(idx) { mutableListOf() }
            if (picks.any { it.name == official }) return@l iData.reply("Du hast das Pokemon bereits in deinem Team!")
            if (picks.size >= teamsize) {
                return@l iData.reply("Dein Team ist bereits vollständig! Falls du einen Fehler bemerkst, melde dich bitte bei ${Constants.MYTAG}.")
            }
            val (tier, _, _) = (tl.getTierOfCommand(e.pokemon, null)
                ?: return@l iData.reply("Dieses Pokemon ist nicht in der Tierliste!"))
            if (handlePoints(false, tier)) return@l
            picks.add(DraftPokemon(official, tier))
            val picksAsString = convertPicksToString(picks, tl)
            if (picks.size >= teamsize) {
                iData.reply("$picksAsString\n\n**Dein Team ist nun vollständig!** Falls du einen Fehler bemerkst, melde dich bitte bei ${Constants.MYTAG}.")
            } else {
                iData.reply(
                    "${picksAsString}\n\nDu hast **${picks.size}/${teamsize}** Pokemon in deinem Team. ${
                        announceData(
                            withTimerAnnounce = false
                        )
                    }"
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
