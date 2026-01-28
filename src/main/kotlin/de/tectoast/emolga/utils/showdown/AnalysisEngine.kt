package de.tectoast.emolga.utils.showdown

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.database.exposed.SwitchType
import de.tectoast.emolga.utils.showdown.PokemonSaveKey.*
import de.tectoast.emolga.utils.toUsername
import mu.KotlinLogging
import kotlin.properties.Delegates

private val otherThanNumbers = Regex("[^0-9]")
private val logger = KotlinLogging.logger {}

context(context: BattleContext)
fun SDPokemon.withZoroCheck(): SDPokemon =
    this.zoroLines.toList().firstOrNull { context.currentLineIndex in it.first }?.second ?: this

data class SDPokemon(
    var pokemon: String, val player: Int, val pokemonSaves: MutableMap<PokemonSaveKey, SDPokemon> = mutableMapOf()
) {
    private val effects: MutableMap<SDEffect, SDPokemon> = mutableMapOf()
    val volatileEffects: MutableMap<String, SDPokemon> = mutableMapOf()
    var kills = 0
    var activeKills = 0
    var hp by Delegates.observable(100) { _, _, new ->
        logger.debug { "$pokemon has $new" }
    }
    var healed = 0
    var damageDealt = 0
    private var revivedAmount = 0
    var isDead = false
    val zoroLines = mutableMapOf<IntRange, SDPokemon>()
    val otherNames = mutableSetOf<String>()
    var nickname: String? = null
    lateinit var draftname: DraftName

    fun hasName(name: String) = pokemon == name || otherNames.contains(name)

    val deadCount get() = revivedAmount + (if (isDead) 1 else 0)
    val isDummy get() = player == -1

    context(context: BattleContext)
    fun addEffect(type: SDEffect, pokemon: SDPokemon) {
        effects[type] = pokemon.withZoroCheck()
    }

    context(context: BattleContext)
    fun addVolatileEffect(name: String, pokemon: SDPokemon) {
        volatileEffects[name] = pokemon.withZoroCheck()
    }

    fun getEffectSource(type: SDEffect): SDPokemon? {
        return effects[type]
    }

    private fun addKill(active: Boolean) {
        kills++
        if (active) activeKills++
    }

    context(context: BattleContext)
    fun claimDamage(
        damagedMonArg: SDPokemon, fainted: Boolean, amount: Int, by: String, activeDamage: Boolean = false
    ) {
        val claimer = this.withZoroCheck()
        val damagedMon = damagedMonArg.withZoroCheck()
        if (fainted) {
            val idx = context.monsOnField[damagedMon.player].indexOf(damagedMon)
            val killGetter = if (damagedMon.player == claimer.player) {
                (damagedMon[LAST_DAMAGE_BY] ?: context.monsOnField.getOrNull(1 - damagedMon.player)?.let { field ->
                    field.getOrElse(1 - idx) { field[idx] }
                })
            } else claimer
            killGetter?.addKill(activeDamage)
        }
        if (damagedMon.player != claimer.player) damagedMon[LAST_DAMAGE_BY] = claimer
        damagedMon.hp -= amount
        claimer.damageDealt += amount
        context.totalDmgAmount += amount
        context.events.damage += AnalysisDamage(
            row = context.currentLineIndex,
            source = claimer,
            target = damagedMon,
            by = by.substringAfter(":").trim(),
            percent = amount,
            faint = fainted,
            active = activeDamage
        )
    }

    context(context: BattleContext)
    fun setNewHPAndHeal(newhp: Int, by: String, healer: SDPokemon? = null) {
        val mon = withZoroCheck()
        val currentHp = mon.hp
        mon.hp = newhp
        if (newhp > currentHp) {
            val healed = newhp - currentHp
            val realHealer = (healer ?: mon).withZoroCheck()
            realHealer.healed += healed
            context.events.heal += AnalysisHeal(context.currentLineIndex, realHealer, mon, percent = healed, by = by)
        }
    }

    context(context: BattleContext)
    fun setNickname(nickname: String) {
        val mon = withZoroCheck()
        mon.nickname = nickname
    }

    fun revive() {
        isDead = false
        revivedAmount++
    }

    operator fun get(key: PokemonSaveKey) = pokemonSaves[key]
    operator fun set(key: PokemonSaveKey, value: SDPokemon) {
        pokemonSaves[key] = value
    }

    override fun toString(): String {
        return "SDPokemon(pokemon='$pokemon', player=$player, hp=$hp)"
    }
}

enum class PokemonSaveKey {
    LAST_DAMAGE_BY, ITEM_OBTAINED_FROM, TARGET_FOR_TRICK, FUTURE_MOVE_SOURCE
}

enum class PlayerSaveKey {
    FIRST_FAINTED
}

@Suppress("unused")
sealed class SDEffect(vararg val types: String) {
    open val name: String? = null

    companion object {
        val effects: Map<String, List<SDEffect>> by lazy {
            SDEffect::class.sealedSubclasses.flatMap {
                it.objectInstance?.let { o -> listOf(o) } ?: it.sealedSubclasses.map { c -> c.objectInstance!! }
            }.flatMap { e -> e.types.map { t -> t to e } }.groupBy({ it.first }, { it.second })
        }

    }

    fun BattleContext.reportUsage() = jda.getTextChannelById(1099651412742389820)!!
        .sendMessage("Effect ${this@SDEffect::class.simpleName} was used! $url").queue()

    context(context: BattleContext)
    abstract fun execute(split: List<String>)

    context(context: BattleContext)
    fun List<String>.getSource(): SDPokemon? {
        val inlined = this.dropWhile { !it.startsWith("[from]") }
        if (inlined.isNotEmpty()) return inlined.getOrNull(1)?.parsePokemon() ?: this[1].parsePokemon().let {
            it[ITEM_OBTAINED_FROM].takeIf { "item:" in inlined[0] } ?: it
        }
        this.firstOrNull { it.startsWith("[of] p") }?.let { return it.parsePokemon() }
        return context.lastLine.value.cleanSplit().takeIf { it.getOrNull(0) == "-activate" }?.getOrNull(1)
            ?.parsePokemon()
            ?: run {
                for (i in context.currentLineIndex downTo context.lastMoveUser.index) {
                    val line = context.game[i]
                    if (line.startsWith("|switch") || line.startsWith("|upkeep")) return@run null
                    if (line.startsWith("|move")) return@run line.cleanSplit().getOrNull(1)?.parsePokemon()
                }
                null
            }
    }

    data object Turn : SDEffect("turn") {
        context(context: BattleContext)
        override fun execute(split: List<String>) {
            context.turn = split[1].toInt()
            context.events.turn += AnalysisTurn(context.currentLineIndex, context.turn)
            context.sdPlayers.forEach { it.hittingFutureMove = null }
        }
    }

    data object Replace : SDEffect("replace") {
        context(context: BattleContext)
        override fun execute(split: List<String>) {
            val (pl, i) = split[1].parsePokemonLocation()
            val oldMon = context.monsOnField[pl][i]
            val newMon = context.sdPlayers[pl].pokemon.first { it.hasName(split[2].substringBefore(",")) && !it.isDead }
            newMon.volatileEffects.putAll(oldMon.volatileEffects)
            oldMon.volatileEffects.clear()
            context.monsOnField[pl][i] = newMon
        }
    }

    data object Format : SDEffect("tier") {
        context(context: BattleContext)
        override fun execute(split: List<String>) {
            context.format = split[1]
        }
    }

    data object Switch : SDEffect("switch", "drag") {
        context(context: BattleContext)
        override fun execute(split: List<String>) {
            val (pl, idx) = split[1].parsePokemonLocation()
            val nickname = split[1].substringAfter(": ")
            val playerSide = context.sdPlayers[pl]
            val monName = split[2].substringBefore(",")
            run starcheck@{
                playerSide.pokemon.firstOrNull {
                    if (monName == it.pokemon) return@starcheck
                    !it.isDead && it.pokemon.endsWith("-*") && monName.split("-")[0] == it.pokemon.split("-")[0]
                }?.let {
                    it.pokemon = monName
                }
            }
            val switchIn = playerSide.pokemon.sortedBy { it.isDead }.first { it.hasName(monName) }
            switchIn.setNickname(nickname)
            val switchOut = context.monsOnField[pl][idx]
            if (split.getOrNull(4) == "[from] Baton Pass") {
                switchIn.volatileEffects.clear()
                switchIn.volatileEffects.putAll(switchOut.volatileEffects)
            }
            val from = if (split[0] == "switch") {
                split.getOrNull(4)?.substringAfter("[from] ")?.trim() ?: context.getLastContentSplit()?.let {
                    if (it.getOrNull(0) == "-activate" || it.getOrNull(0) == "-enditem") it.getOrNull(2)
                        ?.substringAfter(":")?.trim()
                    else null
                } ?: "Switch"
            } else if (split[0] == "drag") {
                context.lastLine.value.cleanSplit().getOrNull(2) ?: "Switch"
            } else error("Unknown switch type: ${split[0]}")
            if (!switchOut.isDummy && !switchOut.isDead) {
                context.events.switch += AnalysisSwitch(
                    context.currentLineIndex,
                    switchOut.withZoroCheck(),
                    SwitchType.OUT,
                    from
                )
            }
            context.monsOnField[pl][idx] = switchIn
            context.events.switch += AnalysisSwitch(
                context.currentLineIndex,
                switchIn.withZoroCheck(),
                SwitchType.IN,
                from
            )
            switchIn.setNewHPAndHeal(
                newhp = split[3].parseHPPercentage(), by = "Switch", healer = null
            )
        }
    }

    data object CourtChange : SDEffect("-activate") {
        context(context: BattleContext)
        override fun execute(split: List<String>) {
            if ("move: Court Change" !in split) return
            val players = context.sdPlayers
            val (usingPlayer, loc) = split[1].parsePokemonLocation()
            val pkmn = context.monsOnField[usingPlayer][loc]
            val hazards = (0..1).map { num ->
                players[num].sideConditions.mapValues { if (num == usingPlayer) pkmn else it.value }
            }
            for (i in 0..1) {
                players[i].sideConditions.putAll(hazards[1 - i])
            }
        }
    }

    data object Heal : SDEffect("-heal") {
        context(context: BattleContext)
        override fun execute(split: List<String>) {
            val healedTo = split[2].parseHPPercentage()
            val (side, num) = split[1].substringAfter('p').substringBefore(':').let {
                val p = it[0].digitToInt() - 1
                p to if (it.length == 1) -1 else if (p > 1) 0 else it[1].cToI()
            }
            val nickname = split[1].substringAfter(": ")
            val healedMon = if (num == -1) context.sdPlayers[side].pokemon.first { it.nickname == nickname }
                .also { it.revive() } else context.monsOnField[side][num]
            val source = split.getOrNull(3)?.substringAfter(": ")
            var healer = source?.let { move ->
                context.findResponsiblePokemonSlot<RemoteHeal>(move, side = side, slot = num)
            }
            val actualSource = if (source == "[silent]") {
                val cleanSplitLast = context.lastLine.value.cleanSplit()
                if (cleanSplitLast.getOrNull(0) == "-damage" && cleanSplitLast.getOrNull(3) == "[from] Leech Seed") {
                    healer = cleanSplitLast[1].parsePokemon().withZoroCheck().volatileEffects["Leech Seed"]
                    "Leech Seed"
                } else if (context.lastMoveUsed.value == "Rest") {
                    "Rest"
                } else source
            } else source
            healedMon.setNewHPAndHeal(healedTo, by = actualSource ?: context.lastMoveUsed.value, healer)
        }
    }

    data object Damage : SDEffect("-damage") {
        context(context: BattleContext)
        override fun execute(split: List<String>) {
            val damagedMonLocation = split[1].parsePokemonLocation()
            val damagedMon = context.monsOnField[damagedMonLocation.first][damagedMonLocation.second]
            val fainted = "fnt" in split[2]
            val amount = damagedMon.withZoroCheck().hp - split[2].parseHPPercentage()
            if (split.size > 4 && "[of]" in split[4] && split[3].substringAfter("[from] ") !in damagedMon.volatileEffects) {
                split[4].parsePokemon()
                    .claimDamage(damagedMon, fainted, amount, by = split[3].substringAfter("[from] "))
                return
            }
            split.getOrNull(3)?.substringAfter("[from] ")?.let {
                when (it) {
                    "psn", "tox", "brn" -> {
                        damagedMon.getEffectSource(Status)?.claimDamage(damagedMon, fainted, amount, by = it)
                    }

                    "Recoil", "recoil", "mindblown", "steelbeam", "highjumpkick", "supercellslam" -> {
                        val (pl, idx) = split[1].parsePokemonLocation()
                        (damagedMon[LAST_DAMAGE_BY] ?: context.monsOnField.getOrNull(1 - pl)?.let { field ->
                            field.getOrElse(1 - idx) { field[idx] }
                        })?.claimDamage(
                            damagedMon, fainted, amount, by = "Self:$it"
                        )
                    }

                    else -> {
                        if (it.startsWith("item: ")) {
                            (damagedMon[ITEM_OBTAINED_FROM] ?: damagedMon).claimDamage(
                                damagedMon, fainted, amount, it.substringAfter("item: ")
                            )
                        }
                    }
                }
                context.findResponsiblePokemonSide(it, side = damagedMon.player)?.claimDamage(
                    damagedMon, fainted, amount, by = it
                )
                context.findGlobalResponsiblePokemon<Weather>(it)?.claimDamage(damagedMon, fainted, amount, by = it)

                damagedMon.volatileEffects.entries.firstOrNull { h ->
                    h.key == it || h.key == it.substringAfter(":").trim()
                }?.let { en ->
                    en.value.claimDamage(
                        damagedMon, fainted, amount, en.key
                    )
                }
                Unit
            } ?: run {
                val p = context.sdPlayers[damagedMonLocation.first]
                p.hittingFutureMove?.let { (idx, move) ->
                    if (damagedMonLocation.second == idx) {
                        p.slotConditions[idx]?.get(move)?.claimDamage(damagedMon, fainted, amount, by = move.name)
                        return@run
                    }
                }
                // TODO: Future Moves work in FFA
                context.lastMoveUser.value.parsePokemon()
                    .claimDamage(damagedMon, fainted, amount, context.lastMoveUsed.value, activeDamage = true)
            }
        }
    }

    data object PerishSong : SDEffect("-start", "-ability", "move") {
        context(context: BattleContext)
        override fun execute(split: List<String>) {
            val pkmn = split[1].parsePokemon()
            if (split[0] == "move") {
                if (split[2] == "Perish Song") {
                    context.globalConditions[PerishSong] = pkmn
                }
            } else if (split[0] == "-ability") {
                if (split[2] == "Perish Body") {
                    context.globalConditions[PerishSong] = pkmn
                }
            } else {
                if (split.getOrNull(2) == "perish0") {
                    context.globalConditions[PerishSong]?.claimDamage(pkmn, true, pkmn.hp, by = "Perish Song")
                }
            }
        }

    }

    data object Faint : SDEffect("faint") {
        context(context: BattleContext)
        override fun execute(split: List<String>) {
            val fainted = split[1].parsePokemon()
            fainted.isDead = true
            context.sdPlayers[fainted.player].putIfAbsent(PlayerSaveKey.FIRST_FAINTED, fainted)
            var cLineIndex = context.currentLineIndex - 1
            while (cLineIndex >= 0) {
                val line = context.game[cLineIndex].cleanSplit()
                val first = line.getOrNull(0)
                if (first == "-activate" && line.getOrNull(2) == "move: Destiny Bond") {
                    line.getOrNull(1)?.parsePokemon()?.claimDamage(fainted, true, fainted.hp, "Destiny Bond")
                    break
                }
                if (first == "move" || first == "turn") break
                cLineIndex--
            }
        }
    }

    data object Swap : SDEffect("swap") {
        context(context: BattleContext)
        override fun execute(split: List<String>) {
            val (pl, idx) = split[1].parsePokemonLocation()
            val newlocation = split[2].toInt()
            context.monsOnField[pl].let {
                val tmp = it[idx]
                it[idx] = it[newlocation]
                it[newlocation] = tmp
            }
        }
    }

    data object DetailsChanged : SDEffect("detailschange") {
        context(context: BattleContext)
        override fun execute(split: List<String>) {
            val mon = runCatching { split[1].parsePokemon() }.getOrNull() ?: return
            mon.otherNames += mon.pokemon
            mon.pokemon = split[2].substringBefore(",")
        }
    }

    data object Explosion : SDEffect("move") {
        context(context: BattleContext)
        override fun execute(split: List<String>) {
            if (split[2] in explosionMoves) {
                val (pl, idx) = split[1].parsePokemonLocation()
                run {
                    var i = context.currentLineIndex + 1
                    while (i < context.game.size) {
                        val line = context.game[i]
                        val faintCheck = line.cleanSplit()
                        if (faintCheck.getOrNull(0) == "faint") {
                            val fainted = faintCheck[1].parsePokemonLocation()
                            if (fainted.first == pl && fainted.second == idx) {
                                return@run
                            }
                        }
                        if (line.startsWith("|turn") || line.startsWith("|move")) break
                        i++
                    }
                    return
                }
                val boomed = context.monsOnField[pl][idx]
                (boomed[LAST_DAMAGE_BY] ?: split.getOrNull(3)?.takeIf { it.startsWith("p") }?.parsePokemon()
                ?: context.monsOnField.getOrNull(1 - pl)?.let {
                    it.getOrNull(1 - idx) ?: it[idx]
                })?.claimDamage(boomed, true, boomed.hp, "Self:${split[2]}")
            }
        }

        private val explosionMoves = setOf(
            "Explosion", "Self-Destruct", "Memento", "Final Gambit", "Misty Explosion", "Healing Wish", "Lunar Dance"
        )
    }

    data object Trickeroo : SDEffect("-item", "-activate") {

        private val moves = listOf("Trick", "Switcheroo")

        context(context: BattleContext)
        override fun execute(split: List<String>) {
            when (split[0]) {
                "-activate" -> if (moves.any { it == split.getOrNull(2)?.substringAfter(": ") }) {
                    val p1 = split[1].parsePokemon()
                    val p2 = split[3].parsePokemon()
                    p1[TARGET_FOR_TRICK] = p2
                    p2[TARGET_FOR_TRICK] = p1
                }

                "-item" -> if (split.getOrNull(3)?.substringAfter("move:")?.trim()?.let { it in moves } == true) {
                    split[1].parsePokemon().let {
                        it[TARGET_FOR_TRICK]?.let { target ->
                            it[ITEM_OBTAINED_FROM] = target
                        } ?: logger.warn { "No target for Trickeroo found!" }
                    }
                }
            }
        }
    }

    sealed class Weather(override val name: String) : SDEffect("-weather") {
        context(context: BattleContext)
        override fun execute(split: List<String>) {
            if (split[1] != name) return
            if (split.getOrNull(2) != "[upkeep]") context.globalConditions[this] =
                (split.getOrNull(3)?.parsePokemon() ?: context.lastMoveUser.value.parsePokemon())
        }

        data object SunnyDay : Weather("SunnyDay")
        data object RainDance : Weather("RainDance")
        data object Sandstorm : Weather("Sandstorm")
        data object Hail : Weather("Hail")
        data object DesolateLand : Weather("DesolateLand")
        data object PrimordialSea : Weather("PrimordialSea")
    }


    data object Status : SDEffect("-status") {
        context(context: BattleContext)
        override fun execute(split: List<String>) {
            split[1].parsePokemon().run {
                val tspikes = context.sdPlayers[player].sideConditions["Toxic Spikes"]
                (split.getSource() ?: tspikes)?.let {
                    addEffect(Status, it)
                    context.events.status += AnalysisStatus(
                        context.currentLineIndex,
                        it.withZoroCheck(),
                        this.withZoroCheck(),
                        split[2]
                    )
                }
            }
        }
    }

    data object Volatile : SDEffect("-start", "-activate") {
        context(context: BattleContext)
        override fun execute(split: List<String>) {
            split[1].parsePokemon().run {
                split.getSource()?.let {
                    addVolatileEffect(split[2].substringAfter(": "), it)
                }
            }
        }
    }

    sealed class FutureMoves(override val name: String) : SDEffect("-start", "-end") {


        data object FutureSight : FutureMoves("Future Sight")
        data object DoomDesire : FutureMoves("Doom Desire")

        context(context: BattleContext)
        override fun execute(split: List<String>) {
            if (split[0] == "-start") {
                if (split.getOrNull(2)?.contains(name) == true) {
                    val (pl, idx) = context.lastLine.value.cleanSplit()[3].parsePokemonLocation()
                    context.sdPlayers[pl].slotConditions.getOrPut(idx) { mutableMapOf() }[this] =
                        split[1].parsePokemon()
                }
            } else if (split[0] == "-end") {
                if (split.getOrNull(2)?.contains(name) == true) {
                    val (p, idx) = split[1].parsePokemonLocation()
                    context.sdPlayers.getOrNull(p)?.hittingFutureMove = idx to this
                }
                // TODO: Future Moves shoot on one slot on the opponent, save it there (prepared)
            }
        }
    }

    sealed class RemoteHeal(override val name: String) : SDEffect("move") {
        context(context: BattleContext)
        override fun execute(split: List<String>) {
            val usedMove = split[2]
            if (usedMove == name) {
                val (side, num) = split[1].parsePokemonLocation()
                context.sdPlayers[side].slotConditions.getOrPut(num) { mutableMapOf() }[this] =
                    context.monsOnField[side][num]
            }
        }

        data object Wish : RemoteHeal("Wish")
        data object HealingWish : RemoteHeal("Healing Wish")
        data object LunarDance : RemoteHeal("Lunar Dance")
        data object RevivalBlessing : RemoteHeal("Revival Blessing")
    }

    data object SideCondition : SDEffect("-sidestart") {
        context(context: BattleContext)
        override fun execute(split: List<String>) {
            val pokemon = context.lastMoveUser.value.parsePokemon()
            val type = split[2].substringAfter(": ")
            context.sdPlayers[split[1][1].digitToInt() - 1].sideConditions[type] = pokemon
        }
    }

    data object Teamsize : SDEffect("teamsize") {
        context(context: BattleContext)
        override fun execute(split: List<String>) {
            context.sdPlayers[split[1][1].digitToInt() - 1].teamSize = split[2].toInt()
        }

    }

    data object SetHP : SDEffect("-sethp") {
        context(context: BattleContext)
        override fun execute(split: List<String>) {
            val (side, num) = split[1].parsePokemonLocation()
            val hp = split[2].parseHPPercentage()
            val isPainSplit = split.getOrNull(3)?.contains("Pain Split") == true
            val target = context.monsOnField[side][num]
            val lastMoveUser = context.lastMoveUser.value.parsePokemon()
            val source = if (isPainSplit) {
                lastMoveUser
            } else target.also { logger.warn("SetHP called without Pain Split: $split") }
            val by = if (isPainSplit) "Pain Split" else "SetHP"
            if (hp < target.hp) {
                lastMoveUser.claimDamage(target, fainted = false, amount = target.hp - hp, by = by)
            } else {
                target.setNewHPAndHeal(hp, by = by, healer = source)
            }
        }
    }

    data object Win : SDEffect("win") {
        context(context: BattleContext)
        override fun execute(split: List<String>) {
            val indexOfWinner = context.sdPlayers.indexOfFirst { it.nickname.toUsername() == split[1].toUsername() }
            context.sdPlayers[indexOfWinner].winnerOfGame = true
            for (i in context.sdPlayers.indices) {
                context.events.winLoss += AnalysisWinLoss(i, i == indexOfWinner, context.sdPlayers[i].pokemon)
            }
        }
    }

    data object Time : SDEffect("t:") {
        context(context: BattleContext)
        override fun execute(split: List<String>) {
            val timestamp = split[1].toLong() * 1000
            if (context.events.start.isEmpty())
                context.events.start += AnalysisStart(context.currentLineIndex, timestamp)
            context.events.time += AnalysisTime(context.currentLineIndex, timestamp)
        }
    }

    data object Move : SDEffect("move") {
        context(context: BattleContext)
        override fun execute(split: List<String>) {
            val target = if (split[3].isEmpty()) {
                context.game.getOrNull(context.currentLineIndex + 2)?.cleanSplit()?.takeIf { it[0] == "-anim" }
                    ?.getOrNull(3)
                    ?.parsePokemon()
            } else if (split[1] == split[3]) null else if (split.getOrNull(4) == "[notarget]") null else runCatching { split[3].parsePokemon() }.getOrNull()
            context.events.move += AnalysisMove(
                context.currentLineIndex, split[1].parsePokemon().withZoroCheck(), target?.withZoroCheck(), split[2]
            )
        }
    }

    data object CustomRules : SDEffect("raw") {
        context(context: BattleContext)
        override fun execute(split: List<String>) {
            val raw = split[1]
            if ("custom rule" in raw) context.customRules = raw
        }
    }

}


data class BattleContext(
    val url: String,
    val monsOnField: List<MutableList<SDPokemon>>,
    var lastMoveUser: IndexedValue<String> = IndexedValue(0, ""),
    var lastMoveUsed: IndexedValue<String> = IndexedValue(0, ""),
    val sdPlayers: List<SDPlayer>,
    val globalConditions: MutableMap<SDEffect, SDPokemon> = mutableMapOf(),
    var randomBattle: Boolean = false,
    var lastLine: IndexedValue<String> = IndexedValue(0, ""),
    var nextLine: IndexedValue<String> = IndexedValue(0, ""),
    var currentLineIndex: Int = -1,
    var turn: Int = 0,
    var format: String = "unknown",
    val game: List<String>,
    var totalDmgAmount: Int = 0,
    val events: AnalysisEvents = AnalysisEvents(),
    val debugMode: Boolean,
    var customRules: String = ""
) {
    inline fun <reified T : SDEffect> findResponsiblePokemonSlot(name: String, side: Int, slot: Int) =
        sdPlayers[side].slotConditions[slot]?.entries?.firstOrNull { (it.key as? T)?.name == name }?.value

    fun findResponsiblePokemonSide(name: String, side: Int) = sdPlayers[side].sideConditions[name]
    inline fun <reified T : SDEffect> findGlobalResponsiblePokemon(name: String) =
        globalConditions.entries.firstOrNull { (it.key as? T)?.name == name }?.value


    val is4v4 by lazy { "VGC" in format || "4v4" in format || "Picked Team Size = 4" in customRules }

    fun getLastContentSplit(): List<String>? {
        for (i in currentLineIndex - 1 downTo 0) {
            val line = game[i].cleanSplit()
            if (line.getOrNull(0).isNullOrBlank()) continue
            if (line.getOrNull(0) == "t:") continue
            return line
        }
        return null
    }
}

data class AnalysisEvents(
    val start: MutableList<AnalysisStart> = mutableListOf(),
    val move: MutableList<AnalysisMove> = mutableListOf(),
    val damage: MutableList<AnalysisDamage> = mutableListOf(),
    val heal: MutableList<AnalysisHeal> = mutableListOf(),
    val switch: MutableList<AnalysisSwitch> = mutableListOf(),
    val status: MutableList<AnalysisStatus> = mutableListOf(),
    val turn: MutableList<AnalysisTurn> = mutableListOf(),
    val time: MutableList<AnalysisTime> = mutableListOf(),
    val winLoss: MutableList<AnalysisWinLoss> = mutableListOf()
)

interface AnalysisStatistic {
    val row: Int
}

data class AnalysisStart(override val row: Int, val timestamp: Long) : AnalysisStatistic
data class AnalysisMove(override val row: Int, val source: SDPokemon, val target: SDPokemon?, val move: String) :
    AnalysisStatistic

data class AnalysisDamage(
    override val row: Int,
    val source: SDPokemon,
    val target: SDPokemon,
    val by: String,
    val percent: Int,
    val faint: Boolean,
    val active: Boolean,
) : AnalysisStatistic

data class AnalysisHeal(
    override val row: Int,
    val source: SDPokemon,
    val target: SDPokemon,
    val by: String,
    val percent: Int,
) : AnalysisStatistic

data class AnalysisStatus(
    override val row: Int, val source: SDPokemon, val target: SDPokemon, val status: String
) : AnalysisStatistic

data class AnalysisSwitch(override val row: Int, val pokemon: SDPokemon, val type: SwitchType, val from: String) :
    AnalysisStatistic

data class AnalysisTurn(override val row: Int, val turn: Int) : AnalysisStatistic
data class AnalysisTime(override val row: Int, val timestamp: Long) : AnalysisStatistic
data class AnalysisWinLoss(val indexInBattle: Int, val win: Boolean, val mons: List<SDPokemon>)

class SDPlayer(
    val nickname: String,
    val pokemon: MutableList<SDPokemon>,
    val slotConditions: MutableMap<Int, MutableMap<SDEffect, SDPokemon>> = mutableMapOf(),
    val sideConditions: MutableMap<String, SDPokemon> = mutableMapOf(),
    var hittingFutureMove: Pair<Int, SDEffect.FutureMoves>? = null,
    var winnerOfGame: Boolean = false,
    var teamSize: Int = 6,
    private val playerSaves: MutableMap<PlayerSaveKey, SDPokemon> = mutableMapOf()
) : MutableMap<PlayerSaveKey, SDPokemon> by playerSaves {
    val allMonsDead: Boolean
        get() = pokemon.all { it.isDead }

    val totalKDCount: Pair<Int, Int>
        get() {
            var kills = 0
            var deaths = 0
            pokemon.forEach {
                kills += it.kills
                deaths += it.deadCount
            }
            return kills to deaths
        }

    fun containsZoro() = pokemon.any { "Zoroark" in it.pokemon || "Zorua" in it.pokemon }
}

fun String.cleanSplit() = this.split("|").drop(1)

context(context: BattleContext)
fun String.parsePokemon() =
    parsePokemonLocation().let { context.monsOnField[it.first][it.second] }

fun String.parsePokemonLocation() = substringAfter('p').substringBefore(':').let {
    val p = it[0].digitToInt() - 1
    p to if (p > 1) 0 else it[1].cToI()
}

fun String.parsePlayer() = substringAfter('p').substringBefore(':').let {
    it[0].digitToInt() - 1
}

fun String.parseHPPercentage() = split("/").let {
    val current = it[0].replace(otherThanNumbers, "").toInt()
    val max = it.getOrNull(1)?.replace(otherThanNumbers, "")?.toInt() ?: return@let 0
    current * 100 / max
}

private fun Char.cToI(): Int {
    return this.code - 97 //ich mag ganz viele leckere Kekse nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom Bounjour!
}
