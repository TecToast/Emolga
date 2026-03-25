package de.tectoast.emolga.utils.draft

import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.league.*
import de.tectoast.emolga.utils.b
import de.tectoast.emolga.utils.draft.DraftMessageType.*
import de.tectoast.emolga.utils.ifTrueOrEmpty
import de.tectoast.emolga.utils.invoke
import de.tectoast.emolga.utils.json.onFailureReply
import de.tectoast.emolga.utils.translateToGuildLanguage
import dev.minn.jda.ktx.coroutines.await
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

object DraftUtils {
    context(league: League, iData: InteractionData)
    suspend fun executeWithinLock(
        input: DraftInput, type: DraftMessageType
    ) {
        league.checkLegalDraftInput(input, type)?.let { return iData.reply(it) }
        val isInBetween = league.potentialBetweenPick && league.currentOverride != null
        val success = input.execute(type)
        if (success) league.afterPickOfficial(if (isInBetween) NextPlayerData.InBetween else NextPlayerData.Normal)
    }

}


interface DraftInput {
    context(iData: InteractionData, league: League)
    suspend fun execute(type: DraftMessageType): Boolean

    context(data: DraftData, interactionData: InteractionData)
    fun checkEmolga(happy: Boolean = true) {
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
    context(iData: InteractionData, league: League)
    override suspend fun execute(type: DraftMessageType): Boolean {
        with(league) {
            if (isSwitchDraft && !config.triggers.allowPickDuringSwitch) {
                return iData.replyFalse(K18n_DraftUtils.NoPickDuringSwitch)
            }
            val (tlName, official, _, _, _) = pokemon
            if (isPicked(official)) return iData.replyFalse(K18n_DraftUtils.PokemonAlreadyPicked)
            val idx = current
            val teraConfig = config.teraPick
            val picks = league.picks(idx)
            if (tera && teraConfig != null && picks.count { it.tera } >= teraConfig.amount) {
                return iData.replyFalse(K18n_DraftUtils.TeraUserAlreadyPicked)
            }
            val tl = if (teraConfig != null && tera) (Tierlist[league.guild, teraConfig.tlIdentifier]
                ?: error("No TERA tierlist found for guild ${league.guild}")) else tierlist
            logger.info("tlName: $tlName, official: $official")
            val tierData = tl.getTierOfCommand(pokemon, tier).onFailureReply() ?: return false
            val context = DraftActionContext()
            tl.withTL {
                it.handleDraftActionWithGeneralChecks(
                    DraftAction(
                        tier = tierData,
                        official = official,
                        free = free,
                        tera = tera,
                        switch = null
                    ), context
                )
            }?.let { return iData.replyFalse(it) }
            val saveTier = context.saveTier ?: tierData.specified
            lastPickedMon = pokemon
            val pickData = PickData(
                league = league,
                pokemon = pokemon.tlName, // TODO refactor this
                pokemonofficial = official,
                tier = saveTier,
                idx = idx,
                round = getPickRoundOfficial(),
                freePick = context.freePick,
                updrafted = saveTier != tierData.official,
                tera = tera
            )
            pickData.savePick(noCost)
            invalidatePicksCache()
            pickData.reply(type)
            builder().apply { pickDoc(pickData) }.execute()
            return true
        }
    }

    context(iData: InteractionData, league: League)
    suspend fun PickData.reply(type: DraftMessageType) {
        val pokemonName = displayName()
        when (type) {
            REGULAR -> {
                league.replyGeneral(b {
                    val infoTier = (league.config.triggers.alwaysSendTierOnPick || updrafted).ifTrueOrEmpty {
                        K18n_DraftUtils.InfoTier(tier)()
                    }
                    val infoTeraUser = tera.ifTrueOrEmpty { K18n_DraftUtils.InfoTeraUser() }
                    val infoUpdrafted = updrafted.ifTrueOrEmpty { K18n_DraftUtils.InfoUpdrafted() }
                    K18n_DraftUtils.PickRegular(pokemon, infoTier, infoTeraUser, infoUpdrafted)()
                })
                checkEmolga()
            }

            QUEUE -> {
                league.tc.sendMessage(
                    K18n_DraftUtils.PickQueue(league[league.current], pokemonName)
                        .translateToGuildLanguage(league.guild)
                ).await()
                checkEmolga()
            }

            RANDOM -> league.replyGeneral(
                K18n_DraftUtils.PickRandom(tier, pokemonName), ifTestUseTc = league.tc
            )

            ACCEPT -> iData.replyAwait(K18n_DraftUtils.PickAccept(pokemonName, tier))
            REROLL -> iData.replyAwait(K18n_DraftUtils.PickReroll(pokemonName, tier))
        }
    }
}

data class SwitchInput(val oldmon: DraftName, val newmon: DraftName) : DraftInput {
    context(iData: InteractionData, league: League)
    override suspend fun execute(type: DraftMessageType): Boolean {
        with(league) {
            if (!isSwitchDraft) {
                return iData.replyFalse(K18n_DraftUtils.NoSwitchAvailable)
            }
            if (isPicked(newmon.official)) {
                return iData.replyFalse(K18n_DraftUtils.PokemonAlreadyPicked)
            }
            val mem = current
            logger.info("Switching $oldmon to $newmon")
            val draftPokemons = picks[mem]!!
            val oldDraftMon = draftPokemons.firstOrNull { it.name == oldmon.official } ?: return iData.reply(
                K18n_DraftUtils.PokemonNotInYourTeam(oldmon.tlName)
            ).let { false }
            val newTier =
                tierlist.getTierOfCommand(newmon, null).onFailureReply() ?: return false
            tierlist.withTL {
                it.handleDraftActionWithGeneralChecks(
                    DraftAction(
                        tier = newTier,
                        official = newmon.official,
                        free = false,
                        tera = false,
                        switch = oldDraftMon
                    )
                )
            }?.let { return iData.replyFalse(it) }
            lastPickedMon = newmon
            val oldIndex = draftPokemons.indexOfFirst { it.name == oldmon.official }
            val switchData = SwitchData(
                league = league,
                pokemon = if (tierlist.isEnglish) newmon.otherTl!! else newmon.tlName, // TODO refactor this
                pokemonofficial = newmon.official,
                tier = newTier.specified,
                mem = mem,
                round = round,
                oldmon = oldmon,
                oldIndex = oldIndex
            )
            switchData.saveSwitch()
            invalidatePicksCache()
            switchData.reply(type)
            builder().apply { switchDoc(switchData) }.execute()
            return true
        }
    }

    context(iData: InteractionData, league: League)
    suspend fun SwitchData.reply(type: DraftMessageType) {
        when (type) {
            REGULAR -> {
                league.replyGeneral(K18n_DraftUtils.SwitchRegular(oldDisplayName(), displayName()))
                checkEmolga()
            }

            QUEUE -> {
                league.tc.sendMessage(
                    K18n_DraftUtils.SwitchQueue(
                        league[league.current], oldDisplayName(), displayName()
                    ).translateToGuildLanguage(league.guild)
                ).queue()
                checkEmolga()
            }

            RANDOM -> league.replyGeneral(
                K18n_DraftUtils.SwitchRandom(
                    oldDisplayName(), displayName()
                ), ifTestUseTc = league.tc
            )

            ACCEPT -> iData.replyAwait(K18n_DraftUtils.SwitchAccept(oldDisplayName(), displayName()))
            REROLL -> iData.replyAwait(K18n_DraftUtils.SwitchReroll(oldDisplayName(), displayName()))
        }
    }
}

data class BanInput(val pokemon: DraftName) : DraftInput {
    context(iData: InteractionData, league: League)
    override suspend fun execute(type: DraftMessageType): Boolean {
        with(league) {
            val config =
                config.draftBan ?: return iData.replyFalse(K18n_DraftUtils.BanNotEnabled)
            if (pokemon.official in config.notBannable) return iData.replyFalse(
                K18n_DraftUtils.BanNotPossibleForMon(
                    pokemon.tlName
                )
            )
            val banRoundConfig =
                config.banRounds[round] ?: return iData.replyFalse(K18n_DraftUtils.NoBanRound(round))
            val tier =
                tierlist.getTierOfCommand(pokemon, requestedTier = null).onFailureReply()?.official ?: return false
            banRoundConfig.checkBan(tier, getAlreadyBannedMonsInThisRound())?.let { reason ->
                return iData.replyFalse(reason)
            }
            if (isPicked(pokemon.official)) return iData.reply("Dieses Pokemon wurde bereits gebannt/gepickt!")
                .let { false }
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

    context(data: InteractionData, league: League)
    suspend fun BanData.reply(type: DraftMessageType) {
        when (type) {
            REGULAR -> {
                league.replyGeneral(K18n_DraftUtils.BanRegular(displayName(), tier))
                checkEmolga(happy = false)
            }

            QUEUE -> {
                league.tc.sendMessage(
                    K18n_DraftUtils.BanQueue(league[league.current], displayName(), tier)
                        .translateToGuildLanguage(league.guild)
                ).queue()
                checkEmolga(happy = false)
            }

            RANDOM -> league.replyWithTestInteractionCheck(
                K18n_DraftUtils.BanRandom(displayName(), tier, league[league.current]), ifTestUseTc = league.tc
            )

            ACCEPT, REROLL -> {}
        }
    }

}

enum class DraftMessageType {
    REGULAR, QUEUE, RANDOM, ACCEPT, REROLL
}
