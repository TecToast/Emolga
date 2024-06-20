package de.tectoast.emolga.utils.showdown

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.utils.draft.DraftPlayer
import de.tectoast.emolga.utils.showdown.PokemonSaveKey.*
import mu.KotlinLogging
import kotlin.properties.Delegates
import kotlin.reflect.KClass

private val otherThanNumbers = Regex("[^0-9]")
private val logger = KotlinLogging.logger {}

data class SDPokemon(
    var pokemon: String, val player: Int, val pokemonSaves: MutableMap<PokemonSaveKey, SDPokemon> = mutableMapOf()
) : MutableMap<PokemonSaveKey, SDPokemon> by pokemonSaves {
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

    val passiveKills get() = kills - activeKills
    val deadCount get() = revivedAmount + (if (isDead) 1 else 0)

    companion object {
        context(BattleContext)
        private fun SDPokemon.withZoroCheck(): SDPokemon =
            this.zoroLines.toList().firstOrNull { currentLineIndex in it.first }?.second ?: this
    }

    context(BattleContext)
    fun addEffect(type: SDEffect, pokemon: SDPokemon) {
        effects[type] = pokemon.withZoroCheck()
    }

    context(BattleContext)
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

    context(BattleContext)
    fun claimDamage(
        damagedMon: SDPokemon, fainted: Boolean, amount: Int, activeKill: Boolean = false
    ) {
        val claimer = this.withZoroCheck()
        if (fainted) {
            val idx = monsOnField[damagedMon.player].indexOf(damagedMon)
            if (damagedMon.player == claimer.player) (damagedMon[LAST_DAMAGE_BY]
                ?: monsOnField.getOrNull(1 - damagedMon.player)?.let { field ->
                    field.getOrElse(1 - idx) { field[idx] }
                })?.addKill(activeKill) else claimer.addKill(activeKill)
        }
        if (damagedMon.player != claimer.player) damagedMon[LAST_DAMAGE_BY] = claimer
        damagedMon.hp -= amount
        claimer.damageDealt += amount
        totalDmgAmount += amount
    }

    context(BattleContext)
    fun setNewHPAndHeal(newhp: Int, healer: SDPokemon? = null) {
        val mon = withZoroCheck()
        val currentHp = mon.hp
        mon.hp = newhp
        if (newhp > currentHp) {
            (healer ?: mon).healed += newhp - currentHp
        }
    }

    context(BattleContext)
    fun setNickname(nickname: String) {
        val mon = withZoroCheck()
        mon.nickname = nickname
    }

    fun revive() {
        isDead = false
        revivedAmount++
    }
}

enum class PokemonSaveKey {
    LAST_DAMAGE_BY, ITEM_OBTAINED_FROM, TARGET_FOR_TRICK
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

    context(BattleContext)
    abstract fun execute(split: List<String>)

    context(BattleContext)
    fun List<String>.getSource(): SDPokemon? {
        val inlined = this.dropWhile { !it.startsWith("[from]") }
        if (inlined.isNotEmpty()) return inlined.getOrNull(1)?.parsePokemon() ?: this[1].parsePokemon().let {
            it[ITEM_OBTAINED_FROM].takeIf { "item:" in inlined[0] } ?: it
        }
        this.firstOrNull { it.startsWith("[of] p") }?.let { return it.parsePokemon() }
        return lastLine.value.cleanSplit().takeIf { it.getOrNull(0) == "-activate" }?.getOrNull(1)?.parsePokemon()
            ?: run {
                for (i in currentLineIndex downTo lastMove.index) {
                    val line = game[i]
                    if (line.startsWith("|switch") || line.startsWith("|upkeep")) return@run null
                    if (line.startsWith("|move")) return@run line.cleanSplit().getOrNull(1)?.parsePokemon()
                }
                null
            }
    }

    data object Turn : SDEffect("turn") {
        context(BattleContext)
        override fun execute(split: List<String>) {
            turn = split[1].toInt()
            sdPlayers.forEach { it.hittingFutureMoves.clear() }
        }
    }

    data object Replace : SDEffect("replace") {
        context(BattleContext)
        override fun execute(split: List<String>) {
            val (pl, i) = split[1].parsePokemonLocation()
            val oldMon = monsOnField[pl][i]
            val newMon = sdPlayers[pl].pokemon.first { it.hasName(split[2].substringBefore(",")) && !it.isDead }
            newMon.volatileEffects.putAll(oldMon.volatileEffects)
            oldMon.volatileEffects.clear()
            monsOnField[pl][i] = newMon
        }
    }

    data object VGC : SDEffect("tier") {
        context(BattleContext)
        override fun execute(split: List<String>) {
            format = split[1]
        }
    }

    data object Switch : SDEffect("switch", "drag") {
        context(BattleContext)
        override fun execute(split: List<String>) {
            val (pl, idx) = split[1].parsePokemonLocation()
            val nickname = split[1].substringAfter(": ")
            val playerSide = sdPlayers[pl]
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
            if (split.getOrNull(4) == "[from] Baton Pass") {
                switchIn.volatileEffects.clear()
                switchIn.volatileEffects.putAll(monsOnField[pl][idx].volatileEffects)
            }
            monsOnField[pl][idx] = switchIn
            switchIn.setNewHPAndHeal(split[3].substringBefore("/").replace(otherThanNumbers, "").toInt())
        }
    }

    data object CourtChange : SDEffect("-activate") {
        context(BattleContext)
        override fun execute(split: List<String>) {
            if ("move: Court Change" !in split) return
            val players = sdPlayers
            val (usingPlayer, loc) = split[1].parsePokemonLocation()
            val pkmn = monsOnField[usingPlayer][loc]
            val hazards = (0..1).map { num ->
                players[num].fieldConditions.filter { h -> h.key is Hazards }
                    .mapValues { if (num == usingPlayer) pkmn else it.value }
            }
            for (i in 0..1) {
                removeConditionOfType<Hazards>(i)
                players[i].fieldConditions.putAll(hazards[1 - i])
            }
        }
    }

    data object Heal : SDEffect("-heal") {
        context(BattleContext)
        override fun execute(split: List<String>) {
            val healedTo = split[2].substringBefore("/").replace(otherThanNumbers, "").toInt()
            val (side, num) = split[1].substringAfter('p').substringBefore(':').let {
                val p = it[0].digitToInt() - 1
                p to if (it.length == 1) -1 else if (p > 1) 0 else it[1].cToI()
            }
            val nickname = split[1].substringAfter(": ")
            val healedMon = if (num == -1) sdPlayers[side].pokemon.first { it.nickname == nickname }
                .also { it.revive() } else monsOnField[side][num]
            val healer = split.getOrNull(3)?.substringAfter(": ")?.let { move ->
                findResponsiblePokemon<RemoteHeal>(move, side = side)
            }
            healedMon.setNewHPAndHeal(healedTo, healer)
        }
    }

    data object Damage : SDEffect("-damage") {
        context(BattleContext)
        override fun execute(split: List<String>) {
            val damagedMon = split[1].parsePokemon()
            val fainted = "fnt" in split[2]
            val amount = damagedMon.hp - split[2].substringBefore("/").replace(otherThanNumbers, "").toInt()
            if (split.size > 4 && "[of]" in split[4] && split[3].substringAfter("[from] ") !in damagedMon.volatileEffects) {
                split[4].parsePokemon().claimDamage(damagedMon, fainted, amount)
                return
            }
            split.getOrNull(3)?.substringAfter("[from] ")?.let {
                when (it) {
                    "psn", "tox", "brn" -> {
                        damagedMon.getEffectSource(Status)?.claimDamage(damagedMon, fainted, amount)
                    }

                    "Recoil", "recoil", "mindblown", "steelbeam", "highjumpkick", "supercellslam" -> {
                        val (pl, idx) = split[1].parsePokemonLocation()
                        (damagedMon[LAST_DAMAGE_BY] ?: monsOnField.getOrNull(1 - pl)?.let { field ->
                            field.getOrElse(1 - idx) { field[idx] }
                        })?.claimDamage(
                            damagedMon, fainted, amount
                        )
                    }

                    else -> {
                        if (it.startsWith("item: ")) {
                            (damagedMon[ITEM_OBTAINED_FROM] ?: damagedMon).claimDamage(
                                damagedMon, fainted, amount
                            )
                        }
                    }
                }
                findResponsiblePokemon<Hazards>(it, side = damagedMon.player)?.claimDamage(
                    damagedMon, fainted, amount
                )
                findGlobalResponsiblePokemon<Weather>(it)?.claimDamage(damagedMon, fainted, amount)

                damagedMon.volatileEffects.entries.firstOrNull { h ->
                    h.key == it || h.key == it.substringAfter(":").trim()
                }?.value?.claimDamage(
                    damagedMon, fainted, amount
                )
                Unit
            } ?: run {
                sdPlayers.getOrNull(1 - damagedMon.player)?.let { p ->
                    p.hittingFutureMoves.removeFirstOrNull()?.let {
                        p.fieldConditions[it]?.claimDamage(damagedMon, fainted, amount)
                        return@run
                    }
                }
                // TODO: Future Moves work in FFA
                lastMove.value.parsePokemon().claimDamage(damagedMon, fainted, amount, activeKill = true)
            }
        }
    }

    data object PerishSong : SDEffect("-start", "move") {
        context(BattleContext)
        override fun execute(split: List<String>) {
            val pkmn = split[1].parsePokemon()
            if (split[0] == "move") {
                if (split[2] == "Perish Song") {
                    globalConditions[PerishSong] = pkmn
                }
            } else {
                if (split.getOrNull(2) == "perish0") {
                    globalConditions[PerishSong]?.claimDamage(pkmn, true, pkmn.hp)
                }
            }
        }

    }

    data object Faint : SDEffect("faint") {
        context(BattleContext)
        override fun execute(split: List<String>) {
            val fainted = split[1].parsePokemon()
            fainted.isDead = true
            sdPlayers[fainted.player].putIfAbsent(PlayerSaveKey.FIRST_FAINTED, fainted)
            val lastLine = lastLine.value.cleanSplit()
            if (lastLine.getOrNull(0) == "-activate" && lastLine.getOrNull(2) == "move: Destiny Bond") {
                lastLine.getOrNull(1)?.parsePokemon()?.claimDamage(fainted, true, fainted.hp)
            }
        }
    }

    data object Swap : SDEffect("swap") {
        context(BattleContext)
        override fun execute(split: List<String>) {
            val (pl, idx) = split[1].parsePokemonLocation()
            val newlocation = split[2].toInt()
            monsOnField[pl].let {
                val tmp = it[idx]
                it[idx] = it[newlocation]
                it[newlocation] = tmp
            }
        }
    }

    data object DetailsChanged : SDEffect("detailschange") {
        context(BattleContext)
        override fun execute(split: List<String>) {
            val mon = split[1].parsePokemon()
            mon.otherNames += mon.pokemon
            mon.pokemon = split[2].substringBefore(",")
        }
    }

    data object Explosion : SDEffect("move") {
        context(BattleContext)
        override fun execute(split: List<String>) {
            if (split[2] in explosionMoves) {
                val (pl, idx) = split[1].parsePokemonLocation()
                run {
                    var i = currentLineIndex + 1
                    while (i < game.size) {
                        val line = game[i]
                        val faintCheck = line.cleanSplit()
                        if (faintCheck.getOrNull(0) == "faint") {
                            val fainted = faintCheck[1].parsePokemonLocation()
                            if (fainted.first == pl && fainted.second == idx) {
                                return@run
                            }
                        }
                        if ("|turn" in line || "|move" in line) break
                        i++
                    }
                    return
                }
                val boomed = monsOnField[pl][idx]
                (boomed[LAST_DAMAGE_BY] ?: monsOnField.getOrNull(1 - pl)?.let {
                    it.getOrNull(1 - idx) ?: it[idx]
                })?.claimDamage(boomed, true, boomed.hp)
            }
        }

        private val explosionMoves = setOf(
            "Explosion", "Self-Destruct", "Memento", "Final Gambit", "Misty Explosion", "Healing Wish", "Lunar Dance"
        )
    }

    data object Trickeroo : SDEffect("-item", "-activate") {

        private val moves = listOf("Trick", "Switcheroo")
        context(BattleContext)
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
        context(BattleContext)
        override fun execute(split: List<String>) {
            if (split[1] != name) return
            if (split.getOrNull(2) != "[upkeep]") globalConditions[this] =
                (split.getOrNull(3)?.parsePokemon() ?: lastMove.value.parsePokemon())
        }

        data object SunnyDay : Weather("SunnyDay")
        data object RainDance : Weather("RainDance")
        data object Sandstorm : Weather("Sandstorm")
        data object Hail : Weather("Hail")
        data object DesolateLand : Weather("DesolateLand")
        data object PrimordialSea : Weather("PrimordialSea")
    }


    data object Status : SDEffect("-status") {
        context(BattleContext)
        override fun execute(split: List<String>) {
            split[1].parsePokemon().run {
                val tspikes = sdPlayers[player].fieldConditions[Hazards.ToxicSpikes]
                (split.getSource() ?: tspikes)?.let {
                    addEffect(Status, it)
                }
            }
        }
    }

    data object Volatile : SDEffect("-start", "-activate") {
        context(BattleContext)
        override fun execute(split: List<String>) {
            split[1].parsePokemon().run {
                split.getSource()?.let {
                    addVolatileEffect(split[2].substringAfter(": "), it)
                }
            }
        }
    }

    sealed class FutureMoves(private val moveName: String) : SDEffect("-start", "-end") {


        data object FutureSight : FutureMoves("Future Sight")
        data object DoomDesire : FutureMoves("Doom Desire")

        context(BattleContext)
        override fun execute(split: List<String>) {
            if (split[0] == "-start") {
                if (split.getOrNull(2) == "move: $moveName") {
                    sdPlayers[split[1].parsePokemonLocation().first].fieldConditions[this] = split[1].parsePokemon()
                }
            } else if (split[0] == "-end") {
                if (split.getOrNull(2) == "move: $moveName") {
                    sdPlayers.getOrNull(1 - split[1].parsePokemonLocation().first)?.hittingFutureMoves?.add(this)
                }
            }
        }
    }

    sealed class RemoteHeal(override val name: String) : SDEffect("move") {
        context(BattleContext)
        override fun execute(split: List<String>) {
            val usedMove = split[2]
            if (usedMove == name) {
                val (side, num) = split[1].parsePokemonLocation()
                sdPlayers[side].fieldConditions[this] = monsOnField[side][num]
            }
        }

        data object Wish : RemoteHeal("Wish")
        data object HealingWish : RemoteHeal("Healing Wish")
        data object LunarDance : RemoteHeal("Lunar Dance")
        data object RevivalBlessing : RemoteHeal("Revival Blessing")
    }

    sealed class Hazards(override val name: String) : SDEffect("-sidestart") {
        context(BattleContext)
        override fun execute(split: List<String>) {
            val pokemon = lastMove.value.parsePokemon()
            val type = split[2].substringAfter(": ")
            if (type == name) {
                sdPlayers[split[1][1].digitToInt() - 1].fieldConditions[this] = pokemon
            }
        }

        data object StealthRock : Hazards("Stealth Rock")
        data object Spikes : Hazards("Spikes")
        data object ToxicSpikes : Hazards("Toxic Spikes")
        data object SteelSurge : Hazards("G-Max Steelsurge")
    }

    data object Teamsize : SDEffect("teamsize") {
        context(BattleContext)
        override fun execute(split: List<String>) {
            sdPlayers[split[1][1].digitToInt() - 1].teamSize = split[2].toInt()
        }

    }

    data object Win : SDEffect("win") {
        context(BattleContext)
        override fun execute(split: List<String>) {
            sdPlayers.first { it.nickname == split[1] }.winnerOfGame = true
        }
    }

}

private fun <T : Any> KClass<T>.dataobjects() = this.sealedSubclasses.mapNotNull { it.objectInstance }


data class BattleContext(
    val url: String,
    val monsOnField: List<MutableList<SDPokemon>>,
    var lastMove: IndexedValue<String> = IndexedValue(0, ""),
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
    val debugMode: Boolean
) {
    inline fun <reified T : SDEffect> findResponsiblePokemon(name: String, side: Int) =
        sdPlayers[side].fieldConditions.entries.firstOrNull { (it.key as? T)?.name == name }?.value
    inline fun <reified T : SDEffect> findGlobalResponsiblePokemon(name: String) =
        globalConditions.entries.firstOrNull { (it.key as? T)?.name == name }?.value

    inline fun <reified T : SDEffect> removeConditionOfType(side: Int) =
        sdPlayers[side].fieldConditions.keys.removeIf { it is T }

    val vgc by lazy { "VGC" in format }
}

class SDPlayer(
    val nickname: String,
    val pokemon: MutableList<SDPokemon>,
    val fieldConditions: MutableMap<SDEffect, SDPokemon> = mutableMapOf(),
    val hittingFutureMoves: MutableList<SDEffect.FutureMoves> = mutableListOf(),
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

    fun toDraftPlayer() = DraftPlayer(pokemon.count { !it.isDead }, winnerOfGame, this)

    fun containsZoro() = pokemon.any { "Zoroark" in it.pokemon || "Zorua" in it.pokemon }
}

fun String.cleanSplit() = this.split("|").drop(1)
context(BattleContext)
fun String.parsePokemon() = parsePokemonLocation().let { monsOnField[it.first][it.second] }

fun String.parsePokemonLocation() = substringAfter('p').substringBefore(':').let {
    val p = it[0].digitToInt() - 1
    p to if (p > 1) 0 else it[1].cToI()
}

private fun Char.cToI(): Int {
    return this.code - 97 //ich mag ganz viele leckere Kekse nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom Bounjour!
}
