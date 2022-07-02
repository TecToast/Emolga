package de.tectoast.emolga.utils.showdown

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.pokemon.WeaknessCommand.Companion.getEffectiveness
import de.tectoast.emolga.utils.sql.managers.ReplayCheckManager
import net.dv8tion.jda.api.entities.Message
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.IntFunction
import java.util.function.Supplier

class Analysis(private val link: String, m: Message?) {
    private val pl: MutableMap<Int, Player> = mutableMapOf()
    private val zoroTurns: MutableMap<Int, MutableList<Int>> = mutableMapOf()
    private val zoru: MutableMap<Int, String> = mutableMapOf()
    private val activeP: MutableMap<Int, Pokemon> = mutableMapOf()
    private val futureSightBy: MutableMap<Int, Pokemon> = mutableMapOf()
    private val futureSight: MutableMap<Int, Boolean> = mutableMapOf()
    private val doomDesireBy: MutableMap<Int, Pokemon> = mutableMapOf()
    private val doomDesire: MutableMap<Int, Boolean> = mutableMapOf()
    private val actMons: MutableMap<Int, MutableList<String>> = mutableMapOf()
    private val actMon: MutableMap<Int, String> = mutableMapOf()
    private var randomBattle = false
    private var lastMove: Pokemon? = null
    private var weatherBy: Pokemon? = null
    private var s: String = ""
    private lateinit var split: List<String>
    private var line = -1
    private var turn = 0
    private var disabledAbility: String? = null
    private val abiSupplier = Supplier { disabledAbility }
    private val dummyPlayer: Player = Player(-1)
    private val dummyPokemon: Pokemon =
        Pokemon("DUMMY", dummyPlayer, emptyList(), emptyList(), abiSupplier, mutableMapOf(), null)

    init {
        if (m != null) ReplayCheckManager[m.channel.idLong] = m.idLong
        for (i in 1..2) {
            pl[i] = Player(i)
            zoroTurns[i] = mutableListOf()
            actMons[i] = mutableListOf()
        }
    }

    private fun getZoro(i: Int, reason: String): Pokemon {
        logger.warn("Requested Zoro from player {} in turn {} Reason: {}", i, turn, reason)
        return pl.getValue(i).indexOfName(zoru.getOrDefault(i, "")).let { pl.getValue(i).mons[it] }
    }

    @Throws(IOException::class)
    fun analyse(): Array<Player> {
        logger.info("Reading URL... {}", link)
        val game = BufferedReader(
            InputStreamReader(
                URL(
                    "$link.log"
                ).openConnection().getInputStream()
            )
        ).lines().toList()
        logger.info("Starting analyse!")
        val time = System.currentTimeMillis()
        for (currentLine in game) {
            s = currentLine
            split = s.split("\\|".toRegex())
            checkPlayer({ i: Int -> s.contains("|player|p$i") && s.length > 11 }) { p: Player ->
                logger.info("setting nickname ${split[3]} for ${p.number}")
                p.nickname = split[3]
            }
            check({ i: Int -> s.contains("|poke|p$i") }) { i: Int ->
                val spl = split[3].split(",".toRegex())
                val poke = spl[0]
                pl.getValue(i).mons.add(
                    Pokemon(
                        poke,
                        pl.getValue(i),
                        zoroTurns.getValue(i),
                        game,
                        abiSupplier,
                        zoru,
                        if (spl.size == 1) null else spl[1]
                    )
                )
                if (poke == "Zoroark" || poke == "Zorua") zoru[i] = poke
            }
            checkPlayer({ i: Int -> s.contains("|teamsize|p$i") }) { p: Player -> p.teamsize = split[3].toInt() }
            check({ i: Int -> s.contains("|switch|p$i") || s.contains("|drag|p$i") }) { i: Int ->
                val spl = split[3].split(",".toRegex())
                val pokemon = spl[0]
                val p = pl.getValue(i)
                if (p.mons.size == 0 && !randomBattle) randomBattle = true
                if (randomBattle) {
                    if (p.indexOfName(pokemon) == -1) {
                        logger.info("Adding {} to {}...", pokemon, p.nickname)
                        p.mons.add(
                            Pokemon(
                                pokemon,
                                p,
                                zoroTurns.getValue(i),
                                game,
                                abiSupplier,
                                zoru,
                                spl.asSequence().map { obj: String -> obj.trim() }
                                    .filter { str: String ->
                                        if (str == "F") return@filter true
                                        str == "M"
                                    }.firstOrNull()
                            )
                        )
                    }
                } else {
                    unknownFormes.asSequence()
                        .filter { pokemon.contains(it) }
                        .filter { p.indexOfName("$it-*") != -1 }
                        .forEach { p.mons[p.indexOfName("$it-*")].pokemon = pokemon }
                    /*if (pokemon.contains("Silvally") && p.indexOfName("Silvally-*") != -1) {//Silvally-Problem
                        p.getMons().get(p.indexOfName("Silvally-*")).setPokemon(pokemon);
                    }
                    if (pokemon.contains("Arceus") && p.indexOfName("Arceus-*") != -1) {//Arceus-Problem
                        p.getMons().get(p.indexOfName("Arceus-*")).setPokemon(pokemon);
                    }
                    if (pokemon.contains("Genesect") && p.indexOfName("Genesect-*") != -1) {//Genesect-Problem
                        p.getMons().get(p.indexOfName("Genesect-*")).setPokemon(pokemon);
                    }
                    if (pokemon.contains("Gourgeist") && p.indexOfName("Gourgeist-*") != -1) {//Gourgeist-Problem
                        p.getMons().get(p.indexOfName("Gourgeist-*")).setPokemon(pokemon);
                    }
                    if (pokemon.contains("Urshifu") && p.indexOfName("Urshifu-*") != -1) {//Urshifu-Problem
                        p.getMons().get(p.indexOfName("Urshifu-*")).setPokemon(pokemon);
                    }
                    if (pokemon.contains("Zacian") && p.indexOfName("Zacian-*") != -1) {//Zacian-Problem
                        p.getMons().get(p.indexOfName("Zacian-*")).setPokemon(pokemon);
                    }
                    if (pokemon.contains("Zamazenta") && p.indexOfName("Zamazenta-*") != -1) {//Zacian-Problem
                        p.getMons().get(p.indexOfName("Zamazenta-*")).setPokemon(pokemon);
                    }
                    if (pokemon.contains("Xerneas") && p.indexOfName("Xerneas-*") != -1) {//Xerneas-Problem
                        p.getMons().get(p.indexOfName("Xerneas-*")).setPokemon(pokemon);
                    }*/
                }
            }
            check({ i: Int ->
                s.contains("|switch|p$i") || s.contains("|drag|p$i") || s.contains(
                    "|replace|p$i"
                )
            }) { i: Int -> actMon[i] = split[3].split(",".toRegex())[0] }
            for (i in 1..2) {
                actMons.computeIfAbsent(i) { mutableListOf() }.add(actMon.getOrDefault(i, ""))
            }
            checkPlayer({ i: Int -> s.contains("|win|" + pl.getValue(i).checkNickname()) }) { p: Player ->
                p.isWinner = true
            }
        }
        actMons.getValue(1).reverse()
        actMons.getValue(2).reverse()
        val reversedGame: List<String> = game.reversed()
        check({ key: Int -> zoru.containsKey(key) }) { i: Int ->
            var t: Int
            var isZ = false
            val learnset = Command.learnsetJSON
            val dex = Command.dataJSON
            for ((x, s) in reversedGame.withIndex()) {
                if (s.contains("|turn|")) {
                    t = s.substring(6).toInt()
                    if (isZ) zoroTurns.getValue(i).add(t)
                }
                if (s.contains("|replace|p$i") && s.contains("|Zor")) {
                    isZ = true
                    logger.info(MarkerFactory.getMarker("important"), "isZ REPLACE")
                }
                if (s.contains("|move|p$i")) {
                    val o = dex.getJSONObject(Command.toSDName(actMons.getValue(i)[x]))
                    if (!learnset.getJSONObject(Command.toSDName(o.optString("baseSpecies", o.getString("name"))))
                            .getJSONObject("learnset").keySet().contains(
                                Command.toSDName(
                                    s.split("\\|".toRegex())[3]
                                )
                            )
                    ) {
                        isZ = true
                        logger.info(MarkerFactory.getMarker("important"), "isZ MOVE")
                    }
                }
                if (s.contains("|switch|p$i") || s.contains("|drag|p$i")) isZ = false
            }
        }
        line = -1
        logger.info(MarkerFactory.getMarker("important"), "zoroTurns.get(1) = {}", zoroTurns[1])
        for (currentLine in game) {
            line++
            s = currentLine
            split = s.split("\\|".toRegex())
            check({ i: Int ->
                s.contains("|switch|p$i") || s.contains("|drag|p$i") || s.contains(
                    "|replace|p$i"
                )
            }) { i: Int ->
                val mon =
                    pl.getValue(i).mons[pl.getValue(i)
                        .indexOfName(split[3].split(",".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[0])]
                mon.nickname =
                    split[2].split(":".toRegex())[1].trim()
                lastMove = null
                if (!s.contains("|replace|") && zoru.containsKey(i)) {
                    val noabi = mon.noAbilityTrigger(line)
                    if (noabi || mon.checkHPZoro(split[4].split("/".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[0].toInt())) {
                        activeP[i] = getZoro(i, if (noabi) "NoAbilityTrigger" else "HPZoro")
                    } else {
                        activeP[i] = mon
                    }
                } else {
                    activeP[i] = mon
                }
            }
            checkPokemon({ i: Int -> s.contains("|move|p$i") }) { p: Pokemon ->
                lastMove = p
                p.addMove(split[3])
            }
            checkPokemon({ i: Int ->
                s.contains("|-activate|p$i") && (s.contains("|ability: Synchronize") || s.contains(
                    "|move: Protect"
                ))
            }) { p: Pokemon -> lastMove = p }
            if (s.contains("|turn|")) {
                lastMove = null
                for (i in 1..2) {
                    futureSight[i] = false
                    doomDesire[i] = false
                }
                turn = s.substring(6).toInt()
            }
            checkPokemon({ i: Int -> s.contains("|detailschange|p$i") }) { p: Pokemon ->
                p.pokemon = split[3].split(",".toRegex())[0]
            }
            check({ i: Int -> s.contains("|-activate|p$i") && s.contains("move: Court Change") }) { i: Int ->
                val p1 = pl.getValue(i)
                val p2 = pl.getValue(3 - i)
                val activeP1 = activeP.getValue(i)
                val activeP2 = activeP.getValue(3 - i)
                if (p1.getSpikesBy(activeP2) != null) p2.setSpikesBy(activeP1)
                if (p1.getRocksBy(activeP2) != null) p2.setRocksBy(activeP1)
                if (p1.getSpikesBy(activeP2) != null) p2.settSpikesBy(activeP1)
                if (p2.getSpikesBy(activeP1) != null) p1.setSpikesBy(activeP1) else p1.setSpikesBy(null)
                if (p2.getRocksBy(activeP1) != null) p1.setRocksBy(activeP1) else p1.setRocksBy(null)
                if (p2.gettSpikesBy(activeP1) != null) p1.settSpikesBy(activeP1) else p1.settSpikesBy(null)
            }
            check({ key: Int -> zoru.containsKey(key) }) { i: Int ->
                var activeP1 = activeP.getValue(i)
                if (s.contains("|-damage|p$i")) {
                    val oldHP = activeP1.hp
                    val lifes = split[3].split("/".toRegex())[0]
                    val newHp: Int = if (lifes.contains("fnt")) 0 else lifes.toInt()
                    if (s.contains("[from] Stealth Rock")) {
                        val dif = oldHP - newHp
                        if (getEffectiveness(
                                "Rock",
                                *Command.dataJSON.getJSONObject(Command.toSDName(activeP1.pokemon))
                                    .getStringList("types")
                                    .toTypedArray()
                            ) != 0 && dif > 10 && dif < 14
                        ) activeP1 = getZoro(i, "Stealth Rock")
                    } else if (s.contains("[from] Spikes")) {
                        val mon = Command.dataJSON.getJSONObject(Command.toSDName(activeP1.pokemon))
                        if (mon.getStringList("types").contains("Flying") || mon.getJSONObject("abilities").toMap()
                                .containsValue("Levitate")
                        ) activeP1 = getZoro(i, "Spikes")
                    }
                    activeP1.setHp(newHp, turn)
                    activeP[i] = activeP1
                } else if (s.contains("|-heal|p$i")) {
                    activeP1.setHp(split[3].split("/".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[0].toInt(), turn)
                } else if (s.contains("|-activate|p$i") && s.contains("|move: Sticky Web")) {
                    val mon = Command.dataJSON.getJSONObject(Command.toSDName(activeP1.pokemon))
                    if (mon.getStringList("types").contains("Flying") || mon.getJSONObject("abilities").toMap()
                            .containsValue("Levitate")
                    ) activeP[i] = getZoro(i, "Sticky Web")
                } else if (s.contains("[from] ability:") && s.contains("[of] p$i")) {
                    split.asSequence().filter { str: String -> str.contains("[from] ability:") }
                        .map { str: String ->
                            str.split(":".toRegex())[1].trim()
                        }.forEach { str: String -> activeP.getValue(i).ability = str }
                } else if (s.contains("|-ability|p$i")) {
                    activeP.getValue(i).ability = split[3].trim()
                }
            }
            checkPokemon({ i: Int -> s.contains("[from] ability:") && s.contains("[of] p$i") }) { p: Pokemon ->
                split.asSequence().filter { str: String -> str.contains("[from] ability:") }
                    .map { str: String ->
                        str.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].trim()
                    }.forEach { ability: String -> p.ability = ability }
            }
            checkPokemon({ i: Int -> s.contains("|-ability|p$i") }) { p: Pokemon ->
                p.ability = split[3].trim()
            }
            check({ i: Int -> s.contains("|-damage|p$i") && split.size == 4 }) { i: Int ->
                val activeP1 = activeP.getValue(i)
                val activeP2 = activeP.getValue(3 - i)
                if (futureSight.getValue(3 - i)) {
                    if (s.contains("0 fnt")) {
                        activeP.getValue(i).setDead(line)
                        futureSightBy.getValue(3 - i).killsPlus1(turn)
                    } else {
                        activeP1.lastDmgBy = futureSightBy.getValue(3 - i)
                    }
                } else if (doomDesire.getValue(3 - i)) {
                    if (s.contains("0 fnt")) {
                        activeP1.setDead(line)
                        doomDesireBy.getValue(3 - i).killsPlus1(turn)
                    } else {
                        activeP1.lastDmgBy = doomDesireBy.getValue(3 - i)
                    }
                } else {
                    if (s.contains("0 fnt")) {
                        //Wenn CurseSD
                        val move = lastMove ?: dummyPokemon
                        if (move === activeP1) {
                            move.lastDmgBy?.also {
                                it.killsPlus1(turn)
                                move.setDead(line)
                            } ?: run {
                                activeP1.setDead(line)
                                activeP2.killsPlus1(turn)
                            }
                        } else {
                            activeP1.setDead(line)
                            activeP2.killsPlus1(turn)
                        }
                    } else {
                        activeP1.lastDmgBy = activeP2
                    }
                }
            }
            checkPokemonBoth({ i: Int -> s.contains("|-activate|p$i") && split.size == 5 }) { obj: Pokemon, bindedBy: Pokemon ->
                obj.bindedBy = bindedBy
            }
            checkPokemon({ i: Int -> s.contains("partiallytrapped") && s.contains("|-damage|p$i") && split.size == 6 }) { p: Pokemon ->
                if (s.contains("0 fnt")) {
                    p.setDead(line)
                    p.bindedBy?.run { killsPlus1(turn) }
                } else {
                    p.lastDmgBy = p.bindedBy
                }
            }
            checkPokemonBoth({ i: Int -> s.contains("|-activate|p$i") && s.contains("move: Destiny Bond") }) { p1: Pokemon, p2: Pokemon ->
                p1.killsPlus1(turn)
                p2.setDead(line)
            }
            checkPokemonBoth({ i: Int -> s.contains("|-start|p$i") && s.contains("|Curse|") }) { obj: Pokemon, cursedBy: Pokemon ->
                obj.cursedBy = cursedBy
            }
            checkPokemon({ i: Int -> s.contains("|[from] Curse") && s.contains("|-damage|p$i") }) { activeP1: Pokemon ->
                if (s.contains("0 fnt")) {
                    activeP1.setDead(line)
                    activeP1.cursedBy?.run { killsPlus1(turn) }
                } else {
                    activeP1.lastDmgBy = activeP1.cursedBy
                }
            }
            checkPokemonBoth({ i: Int -> s.contains("|move|p$i") && s.contains("|Perish Song|") }) { activeP1: Pokemon, activeP2: Pokemon ->
                activeP1.perishedBy = activeP1
                activeP2.perishedBy = activeP1
            }
            checkPokemon({ i: Int -> s.contains("|-start|p$i") && s.contains("|perish0") }) { activeP1: Pokemon ->
                activeP1.setDead(line)
                if (activeP1 === activeP1.perishedBy) activeP1.lastDmgBy?.run { killsPlus1(turn) } else activeP1.perishedBy?.run {
                    killsPlus1(
                        turn
                    )
                }
            }
            checkPokemonBoth({ i: Int -> s.contains("|-start|p$i") && s.contains("|move: Leech Seed") }) { obj: Pokemon, seededBy: Pokemon ->
                obj.seededBy = seededBy

            }
            checkPokemon({ i: Int -> s.contains("|[from] Leech Seed") && s.contains("|-damage|p$i") }) { activeP1: Pokemon ->
                if (s.contains("0 fnt")) {
                    activeP1.setDead(line)
                    activeP1.seededBy?.run { killsPlus1(turn) }
                } else {
                    activeP1.lastDmgBy = activeP1.seededBy
                }
            }
            checkPokemonBoth({ i: Int -> s.contains("|-start|p$i") && s.contains("|Nightmare") }) { obj: Pokemon, nightmaredBy: Pokemon ->
                obj.nightmaredBy = nightmaredBy
            }
            checkPokemon({ i: Int -> s.contains("|[from] Nightmare") && s.contains("|-damage|p$i") }) { activeP1: Pokemon ->
                if (s.contains("0 fnt")) {
                    activeP1.setDead(line)
                    activeP1.nightmaredBy?.run { killsPlus1(turn) }
                } else {
                    activeP1.lastDmgBy = activeP1.nightmaredBy
                }
            }
            checkPokemonBoth({ i: Int -> s.contains("|-start|p$i") && s.contains("|confusion") }) { obj: Pokemon, confusedBy: Pokemon ->
                obj.confusedBy = confusedBy
            }
            checkPokemon({ i: Int -> s.contains("|[from] confusion") && s.contains("|-damage|p$i") }) { activeP1: Pokemon ->
                if (s.contains("0 fnt")) {
                    activeP1.setDead(line)
                    activeP1.confusedBy?.run { killsPlus1(turn) }
                } else {
                    activeP1.lastDmgBy = activeP1.confusedBy
                }
            }
            check({ i: Int -> s.contains("|-start|p$i") && s.contains("Future Sight") }) { i: Int ->
                futureSightBy[i] = activeP.getValue(i)
            }
            check({ i: Int -> s.contains("|-start|p$i") && s.contains("Doom Desire") }) { i: Int ->
                doomDesireBy[i] = activeP.getValue(i)
            }
            check({ i: Int -> s.contains("|-end|p$i") && s.contains("Future Sight") }) { i: Int ->
                lastMove = futureSightBy.getValue(3 - i)
                futureSight[3 - i] = true
            }
            check({ i: Int -> s.contains("|-end|p$i") && s.contains("Doom Desire") }) { i: Int ->
                lastMove = doomDesireBy.getValue(3 - i)
                doomDesire[3 - i] = true
            }
            check({ i: Int -> s.contains("|-status|p$i") }) { i: Int ->
                val activeP1 = activeP.getValue(i)
                val activeP2 = activeP.getValue(3 - i)
                if (s.contains("|[of] p")) {
                    activeP1.statusedBy = activeP2
                } else if (s.contains("|[from] item:")) {
                    activeP1.statusedBy = null
                } else if (lastMove != null) {
                    activeP1.statusedBy = lastMove
                } else {
                    activeP1.statusedBy = (pl.getValue(i).gettSpikesBy(activeP2))
                }
            }
            checkPokemonBoth({ i: Int -> (s.contains("|[from] psn") || s.contains("|[from] brn")) && s.contains("|-damage|p$i") }) { activeP1: Pokemon, activeP2: Pokemon ->
                activeP1.statusedBy?.also {
                    if (s.contains("0 fnt")) {
                        activeP1.setDead(line)
                        it.killsPlus1(turn)
                    } else {
                        activeP1.lastDmgBy = it
                    }
                } ?: run {
                    if (s.contains("0 fnt")) {
                        activeP1.setDead(line)
                        if (activeP1.lastDmgBy != null) {
                            activeP1.lastDmgBy?.run { killsPlus1(turn) }
                        } else {
                            activeP2.killsPlus1(turn)
                        }
                    }
                }
            }
            check({ i: Int -> s.contains("[from] item:") && s.contains("|p$i") }) { i: Int ->
                (if (s.contains("[of] p" + (3 - i))) activeP.getValue(3 - i) else activeP.getValue(i)).item = (
                        split.asSequence().filter { str: String -> str.contains("[from] item") }
                            .map { str: String ->
                                str.split(":".toRegex())[1].trim()
                            }.firstOrNull()
                        )
            }
            checkPokemonBoth({ i: Int ->
                sequenceOf(
                    "|[from] High Jump Kick",
                    "|[from] Jump Kick",
                    "|[from] item: Life Orb",
                    "|[from] Recoil",
                    "|[from] recoil",
                    "|[from] item: Black Sludge",
                    "|[from] item: Sticky Barb",
                    "|[from] ability: Solar Power",
                    "|[from] ability: Dry Skin",
                    "|[from] mindblown"
                ).any { s.contains(it) } && s.contains("|-damage|p$i")
            }) { activeP1: Pokemon, activeP2: Pokemon ->
                if (s.contains("0 fnt")) {
                    activeP1.setDead(line)
                    if (activeP1.lastDmgBy != null) {
                        activeP1.lastDmgBy?.run { killsPlus1(turn) }
                    } else {
                        activeP2.killsPlus1(turn)
                    }
                }
            }
            checkPokemonBoth({ i: Int ->
                sequenceOf(
                    "|[from] jumpkick",
                    "|[from] highjumpkick",
                    "|[from] ability: Liquid Ooze",
                    "|[from] ability: Aftermath",
                    "|[from] ability: Rough Skin",
                    "|[from] ability: Iron Barbs",
                    "|[from] ability: Bad Dreams",
                    "|[from] item: Rocky Helmet",
                    "|[from] Spiky Shield",
                    "|[from] item: Rowap Berry",
                    "|[from] item: Jaboca Berry"
                ).any { s.contains(it) } && s.contains(
                    "|-damage|p$i"
                )
            }) { activeP1: Pokemon, activeP2: Pokemon ->
                if (s.contains("0 fnt")) {
                    activeP1.setDead(line)
                    activeP2.killsPlus1(turn)
                } else {
                    activeP1.lastDmgBy = activeP2
                }
            }
            checkPokemonBoth({ i: Int -> s.contains("|-damage|p$i") && s.contains("[silent]") }) { activeP1: Pokemon, activeP2: Pokemon ->
                if (s.contains("0 fnt")) {
                    activeP1.setDead(line)
                    activeP2.killsPlus1(turn)
                } else {
                    activeP1.lastDmgBy = activeP2
                }
            }
            checkPokemon({ i: Int -> s.contains("|-weather|") && s.contains("|[of] p$i") }) { p: Pokemon ->
                weatherBy = p
            }
            if (s.contains("|-weather|") && !s.contains("|[upkeep]") && !s.contains("|none") && !s.contains("|[of] p")) weatherBy =
                lastMove
            if (s.contains("|-weather|")) {
                disabledAbility = when (split[2]) {
                    "SunnyDay" -> "Drought"
                    "RainDance" -> "Drizzle"
                    "Sandstorm" -> "Sand Stream"
                    "Hail" -> "Snow Warning"
                    "none" -> "None"
                    else -> throw IllegalStateException("Unexpected value: " + split[2])
                }
            }
            check({ i: Int ->
                (s.contains("|[from] Sandstorm") || s.contains("|[from] Hail")) && s.contains(
                    "|-damage|p$i"
                )
            }) { i: Int ->
                val activeP1 = activeP.getValue(i)
                if (weatherBy in pl.getValue(3 - i).mons) { //Abfrage, ob das Weather von einem gegnerischem Mon kommt
                    if (s.contains("0 fnt")) {
                        activeP1.setDead(line)
                        weatherBy?.run { killsPlus1(turn) }
                    } else {
                        activeP1.lastDmgBy = (weatherBy)
                    }
                } else {
                    if (s.contains("0 fnt")) {
                        activeP1.setDead(line)
                        activeP1.lastDmgBy?.run { killsPlus1(turn) }
                    }
                }
            }
            check({ i: Int -> s.contains("|-sidestart|p$i") && s.contains("|move: Stealth Rock") }) { i: Int ->
                pl.getValue(i)
                    .setRocksBy(activeP.getValue(3 - i))
            }
            check({ i: Int -> s.contains("|[from] Stealth Rock") && s.contains("|-damage|p$i") }) { i: Int ->
                if (s.contains("0 fnt")) {
                    activeP.getValue(i).setDead(line)
                    pl.getValue(i).getRocksBy(activeP.getValue(3 - i))?.run { killsPlus1(turn) }
                } else {
                    activeP.getValue(i).lastDmgBy = (pl.getValue(i).getRocksBy(activeP.getValue(3 - i)))
                }
            }
            check({ i: Int -> s.contains("|-sidestart|p$i") && s.contains("|Spikes") }) { i: Int ->
                pl.getValue(i)
                    .setSpikesBy(activeP.getValue(3 - i))
            }
            check({ i: Int -> s.contains("|[from] Spikes") && s.contains("|-damage|p$i") }) { i: Int ->
                if (s.contains("0 fnt")) {
                    activeP.getValue(i).setDead(line)
                    pl.getValue(i).getSpikesBy(activeP.getValue(3 - i))?.run { killsPlus1(turn) }
                } else {
                    activeP.getValue(i).lastDmgBy = (pl.getValue(i).getSpikesBy(activeP.getValue(3 - i)))
                }
            }
            check({ i: Int -> s.contains("|-sidestart|p$i") && s.contains("|move: Toxic Spikes") }) { i: Int ->
                pl.getValue(i)
                    .settSpikesBy(activeP.getValue(3 - i))
            }
            checkPokemonBoth({ i: Int ->
                (s.contains("|Lunar Dance|") || s.contains("|Healing Wish|")) && s.contains(
                    "|move|p$i"
                )
            }) { activeP1: Pokemon, activeP2: Pokemon ->
                if (!s.contains("|[still]")) {
                    activeP1.setDead(line)
                    if (activeP1.lastDmgBy != null) {
                        activeP1.lastDmgBy?.run { killsPlus1(turn) }
                    } else {
                        activeP2.killsPlus1(turn)
                    }
                }
            }
            checkPokemonBoth({ i: Int -> (s.contains("|Final Gambit|") || s.contains("|Memento|")) && s.contains("|move|p$i") }) { activeP1: Pokemon, activeP2: Pokemon ->
                if (!s.contains("|[notarget]") && !s.contains("|[still]")) {
                    activeP1.setDead(line)
                    if (activeP1.lastDmgBy != null) {
                        activeP1.lastDmgBy?.run { killsPlus1(turn) }
                    } else {
                        activeP2.killsPlus1(turn)
                    }
                }
            }
            checkPokemonBoth({ i: Int ->
                (s.contains("|Explosion|") || s.contains("|Self-Destruct|") || s.contains("|Misty Explosion|")) && s.contains(
                    "|move|p$i"
                )
            }) { activeP1: Pokemon, activeP2: Pokemon ->
                activeP1.setDead(line)
                if (activeP1.lastDmgBy != null) {
                    activeP1.lastDmgBy?.run { killsPlus1(turn) }
                } else {
                    activeP2.killsPlus1(turn)
                }
            }
        }
        logger.info("TIME: " + (System.currentTimeMillis() - time) + " ==========================================================")
        return pl.values.toTypedArray()
    }

    private fun checkPokemon(ch: IntFunction<Boolean>, active: Consumer<Pokemon>) {
        check(ch) { i: Int -> active.accept(activeP.getValue(i)) }
    }

    private fun checkPokemonBoth(ch: IntFunction<Boolean>, active: BiConsumer<Pokemon, Pokemon>) {
        check(ch) { i: Int -> active.accept(activeP.getValue(i), activeP.getValue(3 - i)) }
    }

    private fun checkPlayer(ch: IntFunction<Boolean>, player: Consumer<Player>) {
        check(ch) { i: Int -> player.accept(pl.getValue(i)) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Analysis::class.java)
        private val unknownFormes: Collection<String> = listOf(
            "Silvally",
            "Arceus",
            "Genesect",
            "Gourgeist",
            "Urshifu",
            "Zacian",
            "Zamazenta",
            "Xerneas"
        )

        private fun check(ch: IntFunction<Boolean>, c: Consumer<Int>) {
            for (i in 1..2) {
                if (ch.apply(i)) c.accept(i)
            }
        }
    }
}