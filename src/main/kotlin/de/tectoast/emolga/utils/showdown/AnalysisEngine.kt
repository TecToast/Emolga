package de.tectoast.emolga.utils.showdown

import de.tectoast.emolga.bot.EmolgaMain

data class SDPokemon(var pokemon: String, val player: Int) {
    private val effects: MutableMap<SDEffect, SDPokemon> = mutableMapOf()
    val volatileEffects: MutableMap<String, SDPokemon> = mutableMapOf()
    var kills = 0
    var activeKills = 0
    var isDead = false
    var lastDamageBy: SDPokemon? = null
    var itemObtainedFrom: SDPokemon? = null
    var targetForTrick: SDPokemon? = null
    var perishedBy: SDPokemon? = null
    val zoroLines = mutableMapOf<IntRange, SDPokemon>()
    val otherNames = mutableSetOf<String>()

    fun hasName(name: String) = pokemon == name || otherNames.contains(name)

    val passiveKills get() = kills - activeKills

    companion object {
        private fun SDPokemon.withZoroCheck(ctx: BattleContext): SDPokemon =
            this.zoroLines.toList().firstOrNull { ctx.currentLineIndex in it.first }?.second ?: this
    }

    fun addEffect(type: SDEffect, pokemon: SDPokemon, ctx: BattleContext) {
        effects[type] = pokemon.withZoroCheck(ctx)
    }

    fun addVolatileEffect(name: String, pokemon: SDPokemon, ctx: BattleContext) {
        volatileEffects[name] = pokemon.withZoroCheck(ctx)
    }

    fun getEffectSource(type: SDEffect): SDPokemon? {
        return effects[type]
    }

    private fun addKill(active: Boolean) {
        kills++
        if (active) activeKills++
    }

    fun claimDamage(damagedMon: SDPokemon, fainted: Boolean, ctx: BattleContext, activeKill: Boolean = false) {
        val claimer = this.withZoroCheck(ctx)
        if (fainted) {
            val idx = ctx.monsOnField[damagedMon.player].indexOf(damagedMon)
            if (damagedMon.player == claimer.player) (damagedMon.lastDamageBy
                ?: ctx.monsOnField.getOrNull(1 - damagedMon.player)?.let { field ->
                    field.getOrElse(1 - idx) { field[idx] }
                })?.addKill(activeKill) else claimer.addKill(activeKill)
        }
        if (damagedMon.player != claimer.player) damagedMon.lastDamageBy = claimer
    }
}

@Suppress("unused")
sealed class SDEffect(vararg val types: String) {

    companion object {
        val effects: Map<String, List<SDEffect>> by lazy {
            SDEffect::class.sealedSubclasses.flatMap {
                it.objectInstance?.let { o -> listOf(o) } ?: it.sealedSubclasses.map { c -> c.objectInstance!! }
            }.flatMap { e -> e.types.map { t -> t to e } }.groupBy({ it.first }, { it.second })
        }

    }

    fun BattleContext.reportUsage() = EmolgaMain.emolgajda.getTextChannelById(1099651412742389820)!!
        .sendMessage("Effect ${this@SDEffect::class.simpleName} was used! $url").queue()

    abstract fun execute(split: List<String>, ctx: BattleContext)

    fun List<String>.getSource(ctx: BattleContext): SDPokemon? {
        val inlined = this.dropWhile { !it.startsWith("[from]") }
        if (inlined.isNotEmpty()) return inlined.getOrNull(1)?.parsePokemon(ctx) ?: this[1].parsePokemon(ctx).let {
            it.itemObtainedFrom.takeIf { "item:" in inlined[0] } ?: it
        }
        this.firstOrNull { it.startsWith("[of] p") }?.let { return it.parsePokemon(ctx) }
        return ctx.takeIf { it.lastLine.startsWith("|-damage") || it.lastLine.startsWith("|move") }?.lastMove?.cleanSplit()
            ?.getOrNull(1)?.parsePokemon(ctx) ?: ctx.lastLine.cleanSplit().takeIf { it.getOrNull(0) == "-activate" }
            ?.getOrNull(1)?.parsePokemon(ctx)
    }

    object Turn : SDEffect("turn") {
        override fun execute(split: List<String>, ctx: BattleContext) {
            ctx.turn = split[1].toInt()
            ctx.sdPlayers.forEach { it.hittingFutureMoves.clear() }
        }
    }

    object Replace : SDEffect("replace") {
        override fun execute(split: List<String>, ctx: BattleContext) {
            val (pl, i) = split[1].parsePokemonLocation()
            val oldMon = ctx.monsOnField[pl][i]
            val newMon = ctx.sdPlayers[pl].pokemon.first { it.hasName(split[2].substringBefore(",")) }
            newMon.volatileEffects.putAll(oldMon.volatileEffects)
            oldMon.volatileEffects.clear()
            ctx.monsOnField[pl][i] = newMon
        }
    }

    object VGC : SDEffect("tier") {
        override fun execute(split: List<String>, ctx: BattleContext) {
            if ("VGC" in split[1]) ctx.vgc = true
        }
    }

    object Switch : SDEffect("switch", "drag") {
        override fun execute(split: List<String>, ctx: BattleContext) {
            val (pl, idx) = split[1].parsePokemonLocation()
            val playerSide = ctx.sdPlayers[pl]
            val monName = split[2].substringBefore(",")
            playerSide.pokemon.firstOrNull { it.pokemon.endsWith("-*") && monName.split("-")[0] == it.pokemon.split("-")[0] }
                ?.let {
                    it.pokemon = monName
                }
            val switchIn = playerSide.pokemon.first { it.hasName(monName) }
            if (split.getOrNull(4) == "[from] Baton Pass") {
                switchIn.volatileEffects.clear()
                switchIn.volatileEffects.putAll(ctx.monsOnField[pl][idx].volatileEffects)
            }
            ctx.monsOnField[pl][idx] = switchIn
        }
    }

    object CourtChange : SDEffect("-activate") {
        override fun execute(split: List<String>, ctx: BattleContext) {
            if ("move: Court Change" !in split) return
            ctx.reportUsage()
            val players = ctx.sdPlayers
            val (usingPlayer, loc) = split[1].parsePokemonLocation()
            val pkmn = ctx.monsOnField[usingPlayer][loc]
            val hazards = (0..1).map { num ->
                players[num].fieldConditions.filter { h -> h.key is Hazards }
                    .mapValues { if (num == usingPlayer) pkmn else it.value }
            }
            for (i in 0..1) {
                Hazards.allHazards.forEach { h -> players[i].fieldConditions.remove(h) }
                players[i].fieldConditions.putAll(hazards[1 - i])
            }
        }
    }

    object Damage : SDEffect("-damage") {
        override fun execute(split: List<String>, ctx: BattleContext) {
            with(ctx) {
                //println(split)
                val damagedMon = split[1].parsePokemon(this)
                val playerSide = sdPlayers[damagedMon.player]
                val fainted = "fnt" in split[2]
                val hazards = playerSide.fieldConditions
                if (split.size > 4 && split[3].substringAfter("[from] ") !in damagedMon.volatileEffects) {
                    split[4].parsePokemon(ctx).claimDamage(damagedMon, fainted, ctx)
                    return
                }
                split.getOrNull(3)?.substringAfter("[from] ")?.let {
                    when (it) {
                        "psn", "tox", "brn" -> {
                            damagedMon.getEffectSource(Status)?.claimDamage(damagedMon, fainted, ctx)
                        }

                        "Recoil", "recoil", "mindblown", "steelbeam" -> {
                            val (pl, idx) = split[1].parsePokemonLocation()
                            (damagedMon.lastDamageBy ?: monsOnField.getOrNull(1 - pl)?.let { field ->
                                field.getOrElse(1 - idx) { field[idx] }
                            })?.claimDamage(
                                damagedMon, fainted, ctx
                            )
                        }

                        else -> {
                            if (it.startsWith("item: ")) {
                                (damagedMon.itemObtainedFrom ?: damagedMon).claimDamage(damagedMon, fainted, ctx)
                            }
                        }
                    }
                    hazards.keys.filterIsInstance<Hazards>().firstOrNull { h -> h.name == it }
                        ?.let { h -> hazards[h]!!.claimDamage(damagedMon, fainted, ctx) }
                    activeWeather?.let { w -> if (w.first == it) w.second.claimDamage(damagedMon, fainted, ctx) }
                    damagedMon.volatileEffects.entries.firstOrNull { h -> h.key == it }?.value?.claimDamage(
                        damagedMon, fainted, ctx
                    )
                    Unit
                } ?: run {
                    ctx.sdPlayers.getOrNull(1 - damagedMon.player)?.let { p ->
                        p.hittingFutureMoves.removeFirstOrNull()?.let {
                            p.fieldConditions[it]?.claimDamage(damagedMon, fainted, ctx)
                            return@run
                        }
                    }
                    // TODO: Future Moves work in FFA
                    lastMove.parsePokemon(this).claimDamage(damagedMon, fainted, ctx, activeKill = true)
                }
            }
        }
    }

    object PerishSong : SDEffect("-start", "move") {
        override fun execute(split: List<String>, ctx: BattleContext) {
            val pkmn = split[1].parsePokemon(ctx)
            if (split[0] == "move") {
                if (split[2] == "Perish Song") {
                    ctx.monsOnField.flatten().forEach { it.perishedBy = pkmn }
                }
            } else {
                if (split.getOrNull(2) == "perish0") {
                    pkmn.perishedBy?.claimDamage(pkmn, true, ctx)
                }
            }
        }

    }

    object Faint : SDEffect("faint") {
        override fun execute(split: List<String>, ctx: BattleContext) {
            val fainted = split[1].parsePokemon(ctx)
            fainted.isDead = true
            val lastLine = ctx.lastLine.cleanSplit()
            if (lastLine.getOrNull(0) == "-activate" && lastLine.getOrNull(2) == "move: Destiny Bond") {
                lastLine.getOrNull(1)?.parsePokemon(ctx)?.claimDamage(fainted, true, ctx, activeKill = false)
            }
        }
    }

    object Swap : SDEffect("swap") {
        override fun execute(split: List<String>, ctx: BattleContext) {
            val (pl, idx) = split[1].parsePokemonLocation()
            val newlocation = split[2].toInt()
            ctx.monsOnField[pl].let {
                val tmp = it[idx]
                it[idx] = it[newlocation]
                it[newlocation] = tmp
            }
        }
    }

    object DetailsChanged : SDEffect("detailschange") {
        override fun execute(split: List<String>, ctx: BattleContext) {
            val mon = split[1].parsePokemon(ctx)
            mon.otherNames += mon.pokemon
            mon.pokemon = split[2].substringBefore(",")
        }
    }

    object Explosion : SDEffect("move") {

        override fun execute(split: List<String>, ctx: BattleContext) {
            if (split[2] in explosionMoves) {
                if (ctx.nextLine.contains("-fail")) return
                val (pl, idx) = split[1].parsePokemonLocation()
                val boomed = ctx.monsOnField[pl][idx]
                (boomed.lastDamageBy ?: ctx.monsOnField.getOrNull(1 - pl)?.let {
                    it.getOrNull(1 - idx) ?: it[idx]
                })?.claimDamage(boomed, true, ctx)
            }
        }

        private val explosionMoves = listOf(
            "Explosion", "Self-Destruct", "Memento", "Final Gambit", "Misty Explosion", "Healing Wish", "Lunar Dance"
        )
    }

    object Trickeroo : SDEffect("-item", "-activate") {

        private val moves = listOf("Trick", "Switcheroo")

        override fun execute(split: List<String>, ctx: BattleContext) {
            when (split[0]) {
                "-activate" -> if (moves.any { it == split.getOrNull(2)?.substringAfter(": ") }) {
                    val p1 = split[1].parsePokemon(ctx)
                    val p2 = split[3].parsePokemon(ctx)
                    p1.targetForTrick = p2
                    p2.targetForTrick = p1
                }

                "-item" -> if (split.getOrNull(3)?.substringAfter("move:")?.trim()?.let { it in moves } == true) {
                    split[1].parsePokemon(ctx).let { it.itemObtainedFrom = it.targetForTrick }
                }
            }
        }
    }

    object Weather : SDEffect("-weather") {

        override fun execute(split: List<String>, ctx: BattleContext) {
            if (split.getOrNull(2) != "[upkeep]") ctx.activeWeather =
                split[1] to (split.getOrNull(3)?.parsePokemon(ctx) ?: ctx.lastMove.parsePokemon(ctx))
        }
    }


    object Status : SDEffect("-status") {
        override fun execute(split: List<String>, ctx: BattleContext) {
            split[1].parsePokemon(ctx).run {
                val tspikes = ctx.sdPlayers[player].fieldConditions[Hazards.ToxicSpikes]
                (split.getSource(ctx) ?: tspikes)?.let {
                    addEffect(Status, it, ctx)
                }
            }
        }
    }

    object Volatile : SDEffect("-start", "-activate") {
        override fun execute(split: List<String>, ctx: BattleContext) {
            split[1].parsePokemon(ctx).run {
                split.getSource(ctx)?.let {
                    addVolatileEffect(split[2].substringAfter(": "), it, ctx)
                }
            }
        }
    }

    sealed class FutureMoves(private val moveName: String) : SDEffect("-start", "-end") {


        object FutureSight : FutureMoves("Future Sight")
        object DoomDesire : FutureMoves("Doom Desire")

        override fun execute(split: List<String>, ctx: BattleContext) {
            if (split[0] == "-start") {
                if (split.getOrNull(2) == "move: $moveName") {
                    ctx.sdPlayers[split[1].parsePokemonLocation().first].fieldConditions[this] =
                        split[1].parsePokemon(ctx)
                }
            } else if (split[0] == "-end") {
                if (split.getOrNull(2) == "move: $moveName") {
                    ctx.sdPlayers.getOrNull(1 - split[1].parsePokemonLocation().first)?.hittingFutureMoves?.add(this)
                }
            }
        }
    }

    sealed class Hazards(val name: String) : SDEffect("-sidestart") {

        override fun execute(split: List<String>, ctx: BattleContext) {
            val pokemon = ctx.lastMove.parsePokemon(ctx)
            val type = split[2].substringAfter(": ")
            allHazards.firstOrNull { it.name == type }?.let {
                ctx.sdPlayers[split[1][1].digitToInt() - 1].fieldConditions[it] = pokemon
            }
        }

        object StealthRock : Hazards("Stealth Rock")
        object Spikes : Hazards("Spikes")
        object ToxicSpikes : Hazards("Toxic Spikes")
        object SteelSurge : Hazards("G-Max Steelsurge")
        companion object {

            val allHazards by lazy {
                Hazards::class.sealedSubclasses.map {
                    it.objectInstance!!
                }
            }
        }
    }

    object Teamsize : SDEffect("teamsize") {
        override fun execute(split: List<String>, ctx: BattleContext) {
            ctx.sdPlayers[split[1][1].digitToInt() - 1].teamSize = split[2].toInt()
        }

    }

    object Win : SDEffect("win") {
        override fun execute(split: List<String>, ctx: BattleContext) {
            ctx.sdPlayers.first { it.nickname == split[1] }.winner = true
        }
    }

}


data class BattleContext(
    val url: String,
    val monsOnField: List<MutableList<SDPokemon>>,
    var lastMove: String,
    val sdPlayers: List<SDPlayer>,
    var activeWeather: Pair<String, SDPokemon>? = null,
    var randomBattle: Boolean = false,
    var lastLine: String = "",
    var nextLine: String = "",
    var currentLineIndex: Int = -1,
    var turn: Int = 0,
    var vgc: Boolean = false
)

data class SDPlayer(
    val nickname: String,
    val pokemon: MutableList<SDPokemon>,
    val fieldConditions: MutableMap<SDEffect, SDPokemon> = mutableMapOf(),
    val hittingFutureMoves: MutableList<SDEffect.FutureMoves> = mutableListOf(),
    var winner: Boolean = false,
    var teamSize: Int = 6
) {
    val allMonsDead: Boolean
        get() = pokemon.all { it.isDead }
}

fun String.cleanSplit() = this.split("|").drop(1)

fun String.parsePokemon(ctx: BattleContext) = parsePokemonLocation().let { ctx.monsOnField[it.first][it.second] }

fun String.parsePokemonLocation() = substringAfter('p').substringBefore(':').let {
    val p = it[0].digitToInt() - 1
    p to if (p > 1) 0 else it[1].cToI()
}

private fun Char.cToI(): Int {
    return this.code - 97 //ich mag ganz viele leckere Kekse nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom nom Bounjour!
}
