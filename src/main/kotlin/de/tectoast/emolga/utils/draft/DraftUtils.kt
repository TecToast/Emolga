package de.tectoast.emolga.utils.draft

import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.condAppend
import de.tectoast.emolga.utils.draft.DraftMessageType.*
import de.tectoast.emolga.utils.json.emolga.draft.AllowPickDuringSwitch
import de.tectoast.emolga.utils.json.emolga.draft.DraftData
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.PickData
import de.tectoast.emolga.utils.json.emolga.draft.SwitchData
import de.tectoast.emolga.utils.json.emolga.draft.isMega
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

object DraftUtils {
    context(League, InteractionData)
    suspend fun executeWithinLock(
        input: DraftInput, type: DraftMessageType
    ) {
        val success = input.execute(type)
        if (success) afterPickOfficial()
    }

}


interface DraftInput {
    context(InteractionData, League)
    suspend fun execute(type: DraftMessageType): Boolean

    context(DraftData, InteractionData)
    fun checkEmolga() {
        if (pokemonofficial == "Emolga") {
            sendMessage("<:Happy:967390966153609226> ".repeat(5))
        }
    }

}


data class PickInput(val pokemon: DraftName, val tier: String?, val free: Boolean) : DraftInput {
    context(InteractionData, League)
    override suspend fun execute(type: DraftMessageType): Boolean {
        if (isSwitchDraft && !config<AllowPickDuringSwitch>()) {
            return reply("Du kannst während des Switch-Drafts nicht picken!").let { false }
        }
        val mem = current
        val tierlist = tierlist
        val (tlName, official, _) = pokemon
        logger.info("tlName: $tlName, official: $official")
        val (specifiedTier, officialTier) = (getTierOf(tlName, tier)
            ?: return reply("Dieses Pokemon ist nicht in der Tierliste!").let { false })
        checkUpdraft(specifiedTier, officialTier)?.let { return reply(it).let { false } }
        if (isPicked(official, officialTier)) return reply("Dieses Pokemon wurde bereits gepickt!").let { false }
        val tlMode = tierlist.mode
        val freepick =
            free.takeIf { tlMode.isTiersWithFree() && !(tierlist.variableMegaPrice && official.isMega) } == true
        if (!freepick && handleTiers(specifiedTier, officialTier)) return false
        if (handlePoints(
                free = freepick, tier = officialTier, mega = official.isMega
            )
        ) return false
        val saveTier = if (freepick) officialTier else specifiedTier
        lastPickedMon = pokemon
        val pickData = PickData(
            league = this@League,
            pokemon = tlName,
            pokemonofficial = official,
            tier = saveTier,
            idx = mem,
            round = getPickRoundOfficial(),
            freePick = freepick,
            updrafted = saveTier != officialTier
        )
        pickData.savePick()
        pickData.reply(type)
        builder().apply { pickDoc(pickData) }.execute()
        return true
    }

    context(InteractionData, League)
    suspend fun PickData.reply(type: DraftMessageType) {
        when (type) {
            REGULAR -> {
                replyGeneral("${displayName()} ".condAppend(alwaysSendTier || updrafted) { "im $tier " } + "gepickt!".condAppend(
                    updrafted
                ) { " (Hochgedraftet)" }.condAppend(freePick) { " (Free-Pick, neue Punktzahl: ${points[current]})" })
                checkEmolga()
            }

            QUEUE -> {
                this@League.tc.sendMessage("**<@${table[current]}>** hat ${displayName()} gepickt! [Queue]").queue()
                checkEmolga()
            }

            RANDOM -> replyGeneral(
                "einen Random-Pick im $tier gemacht und **${displayName()}** bekommen!"
            )

            ACCEPT -> replyAwait("Akzeptiert: **${displayName()} (${tier})**!")
            REROLL -> replyAwait("Reroll: **${displayName()} (${tier})**!")
        }
    }
}

data class SwitchInput(val oldmon: DraftName, val newmon: DraftName) : DraftInput {
    context(InteractionData, League)
    override suspend fun execute(type: DraftMessageType): Boolean {
        if (!isSwitchDraft) {
            return reply("Dieser Draft ist kein Switch-Draft, daher wird /switch nicht unterstützt!").let { false }
        }
        val mem = current
        logger.info("Switching $oldmon to $newmon")
        val draftPokemons = picks[mem]!!
        val oldDraftMon = draftPokemons.firstOrNull { it.name == oldmon.official }
            ?: return reply("${oldmon.tlName} befindet sich nicht in deinem Kader!").let { false }
        val newtier =
            getTierOf(newmon.tlName, null) ?: return reply("Das neue Pokemon ist nicht in der Tierliste!").let { false }
        val oldtier = getTierOf(oldmon.tlName, null)!!.specified
        checkUpdraft(oldDraftMon.tier, newtier.official)?.let { return reply(it).let { false }; }
        if (isPicked(newmon.official, newtier.official)) {
            return reply("${newmon.tlName} wurde bereits gepickt!").let { false }
        }
        if (handleTiers(newtier.specified, newtier.official, fromSwitch = true)) return false
        if (handlePoints(
                free = false, tier = newtier.official, tierOld = oldtier
            )
        ) return false
        lastPickedMon = newmon
        val oldIndex = draftPokemons.indexOfFirst { it.name == oldmon.official }
        val switchData = SwitchData(
            league = this@League,
            pokemon = newmon.tlName,
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

    context(InteractionData, League)
    suspend fun SwitchData.reply(type: DraftMessageType) {
        when (type) {
            REGULAR -> {
                replyGeneral("${oldDisplayName()} gegen ${displayName()} getauscht!")
                checkEmolga()
            }

            QUEUE -> {
                this@League.tc.sendMessage("**<@${table[current]}>** hat ${oldDisplayName()} gegen ${displayName()} getauscht! [Queue]")
                    .queue()
                checkEmolga()
            }

            RANDOM -> replyGeneral("Random-Switch: ${oldDisplayName()} gegen ${displayName()} getauscht!")
            ACCEPT -> replyAwait("Akzeptiert: ${oldDisplayName()} gegen ${displayName()} getauscht!")
            REROLL -> replyAwait("Reroll: ${oldDisplayName()} gegen ${displayName()} getauscht!")
        }
    }
}

enum class DraftMessageType {
    REGULAR, QUEUE, RANDOM, ACCEPT, REROLL
}
