package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.*
import de.tectoast.emolga.utils.Constants
import kotlinx.serialization.json.jsonObject

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
            learnsetJSON[toSDName(args.getTranslation("pokemon").translation)]!!()
        val level = args.getInt("level")
        val gen = args.getInt("gen")
        learnset
            .entries
            .asSequence()
            .filter { it.value.any { s -> s.startsWith("${gen}L") && s.substring(2).toInt() <= level } }
            .map { it.key to it.value.first { s -> s.startsWith("${gen}L") }.substring(2).toInt() }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(4).joinToString("\n") {
                movesJSON[it]!!.jsonObject.run { "- ${getGerNameNoCheck(it)} (${this["pp"].int})" }
            }
            .let { e.reply(it) }
    }

}
