package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants

class MovesetCommand : Command("moveset", "Zeigt das Moveset von nem Mon in ner Gen an", CommandCategory.Pokemon) {

    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            addEngl("pokemon", "Pokemon", "Das Pokemon", Translation.Type.POKEMON)
            add("level", "Level", "Das Level", ArgumentManagerTemplate.Number.range(1..100))
            add("gen", "Generation", "Die Generation", ArgumentManagerTemplate.Number.range(1..8))
        }
        slash(true, Constants.G.PEPE)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val args = e.arguments
        val learnset =
            learnsetJSON.getJSONObject(toSDName(args.getTranslation("pokemon").translation)).getJSONObject("learnset")
        val level = args.getInt("level")
        val gen = args.getInt("gen")
        learnset
            .keySet()
            .asSequence()
            .map { it to learnset.getStringList(it) }
            .filter { it.second.any { s -> s.startsWith("${gen}L") && s.substring(2).toInt() <= level } }
            .map { it.first to it.second.first { s -> s.startsWith("${gen}L") }.substring(2).toInt() }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(4).joinToString("\n") {
                movesJSON.getJSONObject(it).run { "- ${getGerNameNoCheck(it)} (${getInt("pp")})" }
            }
            .let { e.reply(it) }
    }

}
