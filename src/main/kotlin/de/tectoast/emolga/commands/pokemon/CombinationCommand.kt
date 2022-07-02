package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.jsolf.JSONArray
import de.tectoast.jsolf.JSONObject
import net.dv8tion.jda.api.EmbedBuilder
import java.awt.Color
import java.util.*

class CombinationCommand : Command(
    "combination",
    "Zeigt, welche Pokemon die angegeben Attacken lernen bzw. die Fähigkeiten haben können",
    CommandCategory.Pokemon
) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noSpecifiedArgs(
            "!combination <Typ|Attacke|Eigruppe|Fähigkeit>, <Typ|Attacke|Eigruppe|Fähigkeit> usw.",
            "!combination Water, Donnerblitz"
        )
    }

    override fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val m = e.message
        val msg = m!!.contentDisplay
        if (msg.length <= 13) tco.sendMessage(getHelp(e.guild)).queue()
        val args = msg.substring(13)
        val atks: MutableList<String> = ArrayList()
        val abis: MutableList<String> = ArrayList()
        val types: MutableList<String> = ArrayList()
        val egg: MutableList<String> = ArrayList()
        for (s in args.split(",".toRegex())) {
            val t = getGerName(s.trim())
            if (t.isEmpty || t.isFromType(Translation.Type.POKEMON)) {
                tco.sendMessage("**$s** ist kein valides Argument!").queue()
                return
            }
            val trans = t.translation
            if (t.isFromType(Translation.Type.MOVE)) atks.add(getSDName(trans)) else if (t.isFromType(Translation.Type.ABILITY)) abis.add(
                getEnglName(trans)
            ) else if (t.isFromType(Translation.Type.TYPE)) types.add(getEnglName(trans)) else if (t.isFromType(
                    Translation.Type.EGGGROUP
                )
            ) egg.add(
                getEnglName(trans)
            )
        }
        val data = dataJSON
        val moves = learnsetJSON
        val mons = ArrayList<String>()
        for (s in monList) {
            val mon = data.getJSONObject(s)
            if (abis.size > 0 && containsNotAll(mon.getJSONObject("abilities"), abis)) continue
            if (types.size > 0 && containsNotAll(mon.getJSONArray("types"), types)) continue
            if (egg.size > 0 && containsNotAll(mon.getJSONArray("eggGroups"), egg)) continue
            //logger.info("s = " + s);
            var currentMon: String? = null
            var isRegion = false
            for (form in listOf("alola", "galar", "unova")) {
                if (mon.optString("forme", "").lowercase(Locale.getDefault()).contains(form)) isRegion = true
            }
            if (!isRegion) {
                if (mon.has("baseSpecies")) currentMon = mon.getString("baseSpecies")
            }
            if (currentMon == null) currentMon = s
            if (atks.size > 0) {
                if (!moves.has(toSDName(currentMon))) continue
                if (!moves.getJSONObject(toSDName(currentMon)).has("learnset")) continue
                if (containsNotAll(
                        moves.getJSONObject(toSDName(currentMon)).getJSONObject("learnset").keySet(),
                        atks
                    )
                ) continue
            }
            if (mon.getInt("num") < 0) continue
            val name = data.getJSONObject(s).getString("name")
            val split = name.split("-".toRegex())
            if (split.size > 1) mons.add(getGerNameNoCheck(split[0]) + "-" + split[1]) else mons.add(
                getGerNameNoCheck(
                    name
                )
            )
            /*
            JSONObject mon = data.getJSONObject(s);
            if (!containsAll(mon.getString("abilities"), abis)) continue;
            if (!containsAll(mon.getString("types"), types)) continue;
            if (!containsAll(mon.getString("egggroup"), egg)) continue;
            JSONObject forms = mon.getJSONObject("moves");
            for (String st : forms.keySet()) {
                if (containsAll(forms.getString(st), atks)) mons.add(s + (st.equals("Normal") ? "" : " (" + st + ")"));
            }*/
        }
        if (mons.size == 0) {
            tco.sendMessage("Kein Pokemon hat diese Kombination an Attacken/Fähigkeiten/Typen!").queue()
        } else {
            mons.sort()
            var builder = EmbedBuilder()
            builder.setTitle("Diese Kombination haben:").setColor(Color(0, 255, 255))
            var str = StringBuilder()
            for (o in mons) {
                str.append(o).append("\n")
                if (str.length > 1900) {
                    tco.sendMessageEmbeds(builder.setDescription(str.toString()).build()).queue()
                    builder = EmbedBuilder()
                    builder.setTitle("Diese Kombination haben:").setColor(Color(0, 255, 255))
                    str = StringBuilder()
                }
            }
            builder.setDescription(str.toString())
            //tco.sendMessage(builder.build()).queue();
            mons.sort()
            tco.sendMessageEmbeds(
                EmbedBuilder().setColor(Color.CYAN).setTitle("Diese Kombination haben:")
                    .setDescription(java.lang.String.join("\n", mons)).build()
            ).queue()
        }
    }

    companion object {
        fun containsNotAll(mon: JSONObject, list: List<String>?): Boolean {
            val l = ArrayList(list)
            for (s in mon.keySet()) {
                l.remove(mon.getString(s))
            }
            return l.size != 0
        }

        fun containsNotAll(arr: JSONArray, list: List<String>): Boolean {
            val l = arr.toList().stream().map { o: Any? -> o as String? }.toList()
            for (s in list) {
                if (!l.contains(s)) return true
            }
            return false
        }

        fun containsNotAll(set: Set<String?>, list: List<String>): Boolean {
            for (s in list) {
                if (!set.contains(s)) return true
            }
            return false
        }
    }
}