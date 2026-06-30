package de.tectoast.emolga.domain.game.service.process.analysis

import de.tectoast.emolga.domain.game.model.analysis.PokemonSaveKey
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.utils.toShowdownID

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
    var hp = 100
    var healed = 0
    var damageDealt = 0
    private var revivedAmount = 0
    var isDead = false
    val zoroLines = mutableMapOf<IntRange, SDPokemon>()
    val otherNames = mutableSetOf<String>()
    var nickname: String? = null
    var showdownIDOverride: ShowdownID? = null
    val showdownIDInRoster: ShowdownID get() = showdownIDOverride ?: pokemon.toShowdownID()

    fun hasName(name: String) = pokemon == name || otherNames.contains(name)

    val deadCount get() = revivedAmount + (if (isDead) 1 else 0)
    val isDummy get() = player == -1

    context(context: BattleContext)
    fun addEffect(type: SDEffect, pokemon: SDPokemon) {
        effects[type] = pokemon.withZoroCheck()
    }

    context(context: BattleContext)
    fun addVolatileEffect(name: String, pokemon: SDPokemon, fromActivate: Boolean) {
        if (fromActivate && name in volatileEffects) return
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