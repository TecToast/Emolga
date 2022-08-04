package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.jsolf.JSONObject
import net.dv8tion.jda.api.EmbedBuilder
import java.awt.Color
import java.util.*

class WhatLearnCommand : Command(
    "whatlearn",
    "Zeigt an, welche Attacken ein Pokemon auf eine bestimme Art lernen kann",
    CommandCategory.Pokemon
) {
    val map = HashMap<String, String>()

    init {
        map["Level"] = "L"
        map["TM"] = "M"
        map["Tutor"] = "T"
        map["Zucht"] = "E"
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "form", "Form", "Optionale alternative Form", ArgumentManagerTemplate.Text.of(
                    SubCommand.of("Alola"), SubCommand.of("Galar")
                ), true
            )
            .addEngl("mon", "Pokemon", "Das Pokemon", Translation.Type.POKEMON)
            .add("type", "Art", "Die Lernart", ArgumentManagerTemplate.Text.of(
                SubCommand.of("Level"),
                SubCommand.of("TM"),
                SubCommand.of("Tutor"),
                SubCommand.of("Zucht")
            ).setMapper { map.getValue(it) })
            .add(
                "gen",
                "Generation",
                "Die Generation, sonst Gen 8",
                ArgumentManagerTemplate.Number.range(1, 8),
                true
            )
            .setExample("!howlearn Emolga Level 6")
            .build()
        aliases.add("howlearn")
    }

    override suspend fun process(e: GuildCommandEvent) {
        val args = e.arguments
        val mon = toSDName(args.getTranslation("mon").translation + args.getOrDefault("form", ""))
        val type = args.getText("type")
        var gen = args.getOrDefault("gen", 8)
        val learnset = learnsetJSON.getJSONObject(mon).getJSONObject("learnset")
        val list = LinkedList<String>()
        val levels = HashMap<Int, MutableList<String>>()
        val b = banane(learnset, gen, type, levels, list)
        if (!b) banane(learnset, --gen, type, levels, list)
        val send: String = if (type == "L") {
            val str = StringBuilder()
            for (i in 0..100) {
                if (levels.containsKey(i)) {
                    levels[i]!!.sort()
                    str.append("L").append(i).append(": ").append(java.lang.String.join(", ", levels[i])).append("\n")
                }
            }
            str.toString()
        } else {
            if (list.isEmpty()) {
                e.reply("Dieses Pokemon kann in Generation $gen auf diese Art keine Attacken lernen!")
                return
            }
            val set = ArrayList(HashSet(list))
            set.sort()
            java.lang.String.join("\n", set)
        }
        e.reply(EmbedBuilder().setColor(Color.CYAN).setDescription(send).setTitle("Attacken").build())
        if (e.member.idLong == 598199247124299776L) e.reply("Hier noch ein Keks für dich :3")
    }

    companion object {
        fun banane(
            learnset: JSONObject,
            gen: Int,
            type: String,
            levels: HashMap<Int, MutableList<String>>,
            list: LinkedList<String>
        ): Boolean {
            for (s in learnset.keySet()) {
                val arr = learnset.getJSONArray(s).toStringList()
                if (arr.any { t: String -> t.startsWith(gen.toString()) && t.contains(type) }) {
                    val name = getGerNameNoCheck(s)
                    if (type == "L") {
                        arr.asSequence().filter { str: String -> str.startsWith(gen.toString() + "L") }
                            .map { str: String -> str.substring(str.indexOf('L') + 1) }
                            .map { it.toInt() }.forEach { i: Int ->
                                if (!levels.containsKey(i)) levels[i] = ArrayList()
                                val l = levels[i]!!
                                l.add(name)
                            }
                    } else list.add(name)
                }
            }
            return levels.size > 0
        }
    }
}