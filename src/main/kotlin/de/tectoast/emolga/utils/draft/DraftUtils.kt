package de.tectoast.emolga.utils.draft

import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.league.*
import de.tectoast.emolga.utils.condAppend
import de.tectoast.emolga.utils.draft.DraftMessageType.*
import de.tectoast.emolga.utils.isMega
import dev.minn.jda.ktx.coroutines.await
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

object DraftUtils {
    context(league: League, iData: InteractionData) suspend fun executeWithinLock(
        input: DraftInput, type: DraftMessageType
    ) {
        league.checkLegalDraftInput(input, type)?.let { return iData.reply(it) }
        val isInBetween =
            league.potentialBetweenPick && league.currentOverride != null
        val success = input.execute(type)
        if (success) league.afterPickOfficial(if (isInBetween) NextPlayerData.InBetween else NextPlayerData.Normal)
    }

}


interface DraftInput {
    context(iData: InteractionData, league: League) suspend fun execute(type: DraftMessageType): Boolean

    context(data: DraftData, interactionData: InteractionData) fun checkEmolga(happy: Boolean = true) {
        if (data.pokemonofficial == "Emolga") {
            if (happy) {
                interactionData.sendMessage("<:Happy:967390966153609226> ".repeat(5))
            } else {
                interactionData.sendMessage("<a:mademolga:1317190059337973770> ".repeat(5))
            }
        }
    }

}


data class PickInput(
    val pokemon: DraftName, val tier: String?, val free: Boolean, val tera: Boolean = false, val noCost: Boolean = false
) : DraftInput {
    context(iData: InteractionData, league: League) override suspend fun execute(type: DraftMessageType): Boolean {
        with(league) {
            if (isSwitchDraft && !config.triggers.allowPickDuringSwitch) {
                return iData.reply("Du kannst während des Switch-Drafts nicht picken!").let { false }
            }
            val idx = current
            val teraConfig = config.teraPick
            if (tera && draftData.teraPick.alreadyHasTeraUser.contains(idx)) {
                return iData.reply("Du hast bereits einen Tera-User gepickt!").let { false }
            }
            val tl = if (teraConfig != null && tera) (Tierlist[league.guild, teraConfig.tlIdentifier]
                ?: error("No TERA tierlist found for guild ${league.guild}")) else tierlist
            val (tlName, official, _, _, _) = pokemon
            logger.info("tlName: $tlName, official: $official")
            val (specifiedTier, officialTier, points) = (tl.getTierOfCommand(pokemon, tier)
                ?: return iData.reply("Dieses Pokemon ist nicht in der Tierliste!").let { false })
            checkUpdraft(specifiedTier, officialTier)?.let { return iData.reply(it).let { false } }
            if (isPicked(official, officialTier)) return iData.reply("Dieses Pokemon wurde bereits gepickt!")
                .let { false }
            val tlMode = tl.mode
            val freepick =
                free.takeIf { tlMode.isTiersWithFree() && !(tl.variableMegaPrice && official.isMega) } == true
            if (!freepick && handleTiers(specifiedTier, officialTier)) return false
            if (!noCost && handlePoints(
                    free = freepick, tier = officialTier, mega = official.isMega, extraCosts = points.takeIf { tera })
            ) return false
            val saveTier = if (freepick) officialTier else specifiedTier
            if (tera) draftData.teraPick.alreadyHasTeraUser.add(idx)
            lastPickedMon = pokemon
            val pickData = PickData(
                league = league,
                pokemon = pokemon.tlName, // TODO refactor this
                pokemonofficial = official,
                tier = saveTier,
                idx = idx,
                round = getPickRoundOfficial(),
                freePick = freepick,
                updrafted = saveTier != officialTier,
                tera = tera
            )
            pickData.savePick(noCost)
            pickData.reply(type)
            builder().apply { pickDoc(pickData) }.execute()
            return true
        }
    }

    context(iData: InteractionData, league: League) suspend fun PickData.reply(type: DraftMessageType) {
        when (type) {
            REGULAR -> {
                league.replyGeneral("${displayName()} ".condAppend(league.config.triggers.alwaysSendTierOnPick || updrafted) { "im $tier " } + (if (tera) "als Tera-User " else "") + "gepickt!".condAppend(
                    updrafted
                ) { " (Hochgedraftet)" }.condAppend(freePick) { " (Free-Pick)" }
                    .condAppend(freePick) { " [Neue Punktzahl: ${league.points[league.current]}]" })
                checkEmolga()
            }

            QUEUE -> {
                league.tc.sendMessage("**<@${league.table[league.current]}>** hat ${displayName()} gepickt! [Queue]")
                    .await()
                checkEmolga()
            }

            RANDOM -> league.replyGeneral(
                "einen Random-Pick im $tier gemacht und **${displayName()}** bekommen!", ifTestUseTc = league.tc
            )

            ACCEPT -> iData.replyAwait("Akzeptiert: **${displayName()} (${tier})**!")
            REROLL -> iData.replyAwait("Reroll: **${displayName()} (${tier})**!")
        }
    }
}

data class SwitchInput(val oldmon: DraftName, val newmon: DraftName) : DraftInput {
    context(iData: InteractionData, league: League) override suspend fun execute(type: DraftMessageType): Boolean {
        with(league) {
            if (!isSwitchDraft) {
                return iData.reply("Dieser Draft ist kein Switch-Draft, daher wird /switch nicht unterstützt!")
                    .let { false }
            }
            val mem = current
            logger.info("Switching $oldmon to $newmon")
            val draftPokemons = picks[mem]!!
            val oldDraftMon = draftPokemons.firstOrNull { it.name == oldmon.official }
                ?: return iData.reply("${oldmon.tlName} befindet sich nicht in deinem Kader!").let { false }
            val newtier = tierlist.getTierOfCommand(newmon, null)
                ?: return iData.reply("Das neue Pokemon ist nicht in der Tierliste!").let { false }
            val oldtier = tierlist.getTierOfCommand(oldmon, null)!!.specified
            checkUpdraft(oldDraftMon.tier, newtier.official)?.let { return iData.reply(it).let { false }; }
            if (isPicked(newmon.official, newtier.official)) {
                return iData.reply("${newmon.tlName} wurde bereits gepickt!").let { false }
            }
            if (handleTiers(oldtier, newtier.official, fromSwitch = true)) return false
            if (handlePoints(
                    free = false, tier = newtier.official, tierOld = oldtier
                )
            ) return false
            lastPickedMon = newmon
            val oldIndex = draftPokemons.indexOfFirst { it.name == oldmon.official }
            val switchData = SwitchData(
                league = league,
                pokemon = if (tierlist.isEnglish) newmon.otherTl!! else newmon.tlName, // TODO refactor this
                pokemonofficial = newmon.official,
                tier = newtier.specified,
                mem = mem,
                round = round,
                oldmon = oldmon,
                oldIndex = oldIndex
            )
            switchData.saveSwitch()
            switchData.reply(type)
            builder().apply { switchDoc(switchData) }.execute()
            return true
        }
    }

    context(iData: InteractionData, league: League) suspend fun SwitchData.reply(type: DraftMessageType) {
        when (type) {
            REGULAR -> {
                league.replyGeneral("${oldDisplayName()} gegen ${displayName()} getauscht!")
                checkEmolga()
            }

            QUEUE -> {
                league.tc.sendMessage("**<@${league.table[league.current]}>** hat ${oldDisplayName()} gegen ${displayName()} getauscht! [Queue]")
                    .queue()
                checkEmolga()
            }

            RANDOM -> league.replyGeneral(
                "Random-Switch: ${oldDisplayName()} gegen ${displayName()} getauscht!", ifTestUseTc = league.tc
            )

            ACCEPT -> iData.replyAwait("Akzeptiert: ${oldDisplayName()} gegen ${displayName()} getauscht!")
            REROLL -> iData.replyAwait("Reroll: ${oldDisplayName()} gegen ${displayName()} getauscht!")
        }
    }
}

data class BanInput(val pokemon: DraftName) : DraftInput {
    context(iData: InteractionData, league: League) override suspend fun execute(type: DraftMessageType): Boolean {
        with(league) {
            val config =
                config.draftBan ?: return iData.reply("In diesem Draft sind keine Bans vorgesehen!").let { false }
            if (pokemon.official in config.notBannable) return iData.reply("`${pokemon.tlName}` kann nicht gebannt werden!")
                .let { false }
            val banRoundConfig =
                config.banRounds[round] ?: return iData.reply("Runde **$round** ist keine Ban-Runde!").let { false }
            val tier = (tierlist.getTierOfCommand(pokemon, insertedTier = null)
                ?: return iData.reply("Dieses Pokemon ist nicht in der Tierliste!").let { false }).official
            banRoundConfig.checkBan(tier, getAlreadyBannedMonsInThisRound())?.let { reason ->
                return iData.reply(reason).let { false }
            }
            if (isPicked(
                    pokemon.official, tier
                )
            ) return iData.reply("Dieses Pokemon wurde bereits gebannt/gepickt!").let { false }
            val banData = BanData(
                league,
                if (tierlist.isEnglish) pokemon.otherTl!! else pokemon.tlName,
                pokemon.official,
                tier,
                current,
                round
            )
            lastPickedMon = pokemon
            banData.saveBan()
            banData.reply(type)
            builder().apply { banDoc(banData) }.execute()
            return true
        }
    }

    context(data: InteractionData, league: League) suspend fun BanData.reply(type: DraftMessageType) {
        when (type) {
            REGULAR -> {
                league.replyGeneral("${displayName()} ($tier) gebannt!")
                checkEmolga(happy = false)
            }

            QUEUE -> {
                league.tc.sendMessage("<@${league.table[league.current]}> hat ${displayName()} gebannt! [Queue]")
                    .queue()
                checkEmolga(happy = false)
            }

            RANDOM -> league.replyWithTestInteractionCheck(
                "Timer ausgelaufen: Ich habe ${displayName()} ($tier) für <@${league.table[league.current]}> gebannt!",
                ifTestUseTc = league.tc
            )

            ACCEPT, REROLL -> {}
        }
    }

}

enum class DraftMessageType {
    REGULAR, QUEUE, RANDOM, ACCEPT, REROLL
}
