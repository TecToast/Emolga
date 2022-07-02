package de.tectoast.emolga.utils.showdown

import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.util.function.Supplier

class Pokemon(
    var pokemon: String,
    val player: Player,
    private val zoroTurns: List<Int>,
    private val game: List<String?>,
    private val disabledAbi: Supplier<String?>,
    private val zoru: MutableMap<Int, String>,
    private val gender: String?
) {
    val moves: MutableSet<String> = HashSet()
    var kills = 0
        private set
    var statusedBy: Pokemon? = null
    var bindedBy: Pokemon? = null
    var cursedBy: Pokemon? = null
    var seededBy: Pokemon? = null
    var nightmaredBy: Pokemon? = null
    var confusedBy: Pokemon? = null
    var lastDmgBy: Pokemon? = null
    var perishedBy: Pokemon? = null
    var isDead = false
        private set
    var hp = 100
        private set
    var ability = ""
        set(value) {
            logger.debug("setting ability {} to {}...", value, pokemon)
            if (this.ability.isEmpty()) {
                field = value
            }
        }
    private var lastKillTurn = -1
    var nickname: String? = null
        get() = if (field == null) pokemon else field
    var item: String? = null
        set(value) {
            if (this.item == null) field = value
        }

    fun addMove(move: String) {
        moves.add(move)
    }

    fun buildGenderStr(): String {
        return gender?.let { " (${gender.trim()})" } ?: ""
    }

    fun noAbilityTrigger(line: Int): Boolean {
        return ability.isNotEmpty() && disabledAbi.get() != ability && !game[line + 1]!!
            .contains("[from] ability: $ability")
    }

    fun checkHPZoro(hp: Int): Boolean {
        return this.hp != hp
    }

    fun setDead(line: Int) {
        if (game[line + 1]!!.contains("|replace|") && game[line + 1]!!.contains("|Zor")) {
            player.mons.stream().filter { p: Pokemon? -> p!!.pokemon == "Zoroark" || p.pokemon == "Zorua" }
                .findFirst().ifPresent { p: Pokemon? ->
                    p!!.isDead = true
                    zoru.remove(player.number)
                }
        } else {
            isDead = true
        }
    }

    fun setHp(hp: Int, turn: Int) {
        if (zoroTurns.contains(turn)) {
            player.mons.stream().filter { p: Pokemon? -> p!!.pokemon == "Zoroark" || p.pokemon == "Zorua" }
                .findFirst().ifPresent { p: Pokemon? -> p!!.hp = hp }
            logger.info(MarkerFactory.getMarker("important"), "set hp zoroark in turn {} to {}", turn, hp)
        } else this.hp = hp
    }

    fun killsPlus1(turn: Int) {
        if (zoroTurns.contains(turn)) {
            player.mons.stream().filter { p: Pokemon? -> p!!.pokemon == "Zoroark" || p.pokemon == "Zorua" }
                .findFirst().ifPresent { p: Pokemon? ->
                    if (p!!.lastKillTurn == turn) return@ifPresent
                    p.kills++
                    p.lastKillTurn = turn
                }
        } else {
            if (lastKillTurn == turn) return
            kills++
            lastKillTurn = turn
        }
    }

    override fun toString(): String {
        return "Pokemon{player=$player, pokemon='$pokemon', kills=$kills, dead=$isDead}"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Pokemon::class.java)
    }
}