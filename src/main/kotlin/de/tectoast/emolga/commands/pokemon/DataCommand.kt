package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.buttons.buttonsaves.MonData
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.sql.managers.AbiDataManager
import de.tectoast.emolga.utils.sql.managers.AtkDataManager
import de.tectoast.emolga.utils.sql.managers.ItemDataManager
import de.tectoast.jsolf.JSONObject
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import org.slf4j.LoggerFactory
import java.awt.Color

class DataCommand : Command("data", "Zeigt Informationen über diese Sache", CommandCategory.Pokemon) {
    init {
        aliases.add("dt")
        argumentTemplate = ArgumentManagerTemplate.builder().add(
            "shiny", "Shiny", "", ArgumentManagerTemplate.Text.of(
                SubCommand.of(
                    "Shiny", "Wenn der Sprite des Mons als Shiny angezeigt werden soll"
                )
            ), true
        ).add(
            "regform", "Form", "", ArgumentManagerTemplate.Text.of(
                SubCommand.of("Alola"), SubCommand.of("Galar"), SubCommand.of("Mega")
            ), true
        ).add(
            "stuff", "Sache", "Pokemon/Item/Whatever", Translation.Type.of(
                Translation.Type.POKEMON, Translation.Type.MOVE, Translation.Type.ITEM, Translation.Type.ABILITY
            )
        ).add(
            "form", "Sonderform", "Sonderform, bspw. `Heat` bei Rotom", ArgumentManagerTemplate.Text.any(), true
        ).setExample("!dt Shiny Primarene").build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        try {
            val args = e.arguments
            val gerName = args.getTranslation("stuff")
            val objtype = gerName.type
            var name = gerName.translation
            when (objtype) {
                Translation.Type.POKEMON -> {
                    val mon = dataJSON.optJSONObject(
                        toSDName(
                            gerName.otherLang + args.getOrDefault(
                                "regform", ""
                            ) + args.getOrDefault("form", "") + (gerName.forme ?: "")
                        )
                    )
                    if (mon == null) {
                        e.reply("$name besitzt diese Form nicht!")
                        return
                    }
                    val builder = EmbedBuilder()
                    builder.addField("Englisch", mon.getString("name"), true)
                    builder.addField("Dex", mon.getInt("num").toString(), true)
                    val gender: String = if (mon.has("genderRatio")) {
                        val gen = mon.getJSONObject("genderRatio")
                        (gen.getDouble("M") * 100).toString() + "% ♂ " + gen.getDouble("F") * 100 + "% ♀"
                    } else if (mon.has("gender")) {
                        if (mon.getString("gender") == "M") "100% ♂" else if (mon.getString("gender") == "F") "100% ♀" else "Unbekannt"
                    } else "50% ♂ 50% ♀"
                    builder.addField("Geschlecht", gender, true)
                    //list.forEach(j -> logger.info(j.toString(4)));
                    val monname = mon.getString("name")
                    if (monname.equals("silvally", ignoreCase = true) || monname.equals("arceus", ignoreCase = true)) {
                        builder.addField("Typen", "Normal", false)
                    } else {
                        logger.info(mon.toString())
                        builder.addField("Typen", mon.getJSONArray("types").toStringList().joinToString(" ") {
                            if (it == "Psychic") "Psycho"
                            else getGerNameNoCheck(it as String)
                        }, false)
                    }
                    builder.addField("Größe", mon.getDouble("heightm").toString() + " m", true)
                    builder.addField("Gewicht", mon.getDouble("weightkg").toString() + " kg", true)
                    builder.addField("Eigruppe", mon.getJSONArray("eggGroups").toStringList().joinToString { o: Any ->
                        getGerNameNoCheck(
                            "E_$o"
                        )
                    }, true)
                    if (monname.equals("silvally", ignoreCase = true) || monname.equals("arceus", ignoreCase = true)) {
                        builder.addField(
                            "Fähigkeiten",
                            if (monname.equals("silvally", ignoreCase = true)) "Alpha-System" else "Variabilität",
                            false
                        )
                    } else {
                        val o = mon.getJSONObject("abilities")
                        val b = StringBuilder()
                        if (o.has("0")) {
                            b.append(getGerNameNoCheck(o.getString("0"))).append("\n")
                        }
                        if (o.has("1")) {
                            b.append(getGerNameNoCheck(o.getString("1"))).append("\n")
                        }
                        if (o.has("H")) {
                            b.append(getGerNameNoCheck(o.getString("H"))).append(" (VF)")
                        }
                        builder.addField("Fähigkeiten", b.toString(), false)
                    }
                    if (monname.equals("silvally", ignoreCase = true) || monname.equals("arceus", ignoreCase = true)) {
                        builder.addField(
                            if (monname.equals("silvally", ignoreCase = true)) "Amigento" else "Arceus",
                            if (monname.equals("silvally", ignoreCase = true)) """
                    KP: 95
                    Atk: 95
                    Def: 95
                    SpAtk: 95
                    SpDef: 95
                    Init: 95
                    Summe: 570
                """.trimIndent() else """
                    KP: 120
                    Atk: 120
                    Def: 120
                    SpAtk: 120
                    SpDef: 120
                    Init: 120
                    Summe: 720
                """.trimIndent(),
                            false
                        )
                    } else {
                        val stats = mon.getJSONObject("baseStats")
                        val kp = stats.getInt("hp")
                        val atk = stats.getInt("atk")
                        val def = stats.getInt("def")
                        val spa = stats.getInt("spa")
                        val spd = stats.getInt("spd")
                        val spe = stats.getInt("spe")
                        val str = """
                            KP: $kp
                            Atk: $atk
                            Def: $def
                            SpAtk: $spa
                            SpDef: $spd
                            Init: $spe
                            Summe: ${kp + atk + def + spa + spd + spe}
                            """.trimIndent()
                        val prevoInfo = getPrevoInfo(mon)
                        if (prevoInfo.isNotEmpty()) {
                            builder.addField("Erhaltbarkeit", prevoInfo, false)
                        }
                        builder.addField("Basestats", str, false)
                    }
                    val shiny = args.isTextIgnoreCase("shiny", "Shiny")
                    builder.setImage(getGen5SpriteWithoutGoogle(mon, shiny))
                    builder.setTitle(getGerNameWithForm(monname))
                    builder.setColor(Color.CYAN)
                    val list = getAllForms(name)
                    e.reply(builder.build(), ma = { ma ->
                        if (list.size > 1) {
                            ma.setComponents(
                                ActionRow.of(
                                    SelectMenu.create("mondata").addOptions(
                                        list.map {
                                            SelectOption.of(
                                                "Form: " + getGerNameWithForm(it.getString("name")),
                                                toSDName(it.getString("name"))
                                            ).withDefault(mon.getString("name") == it.getString("name"))
                                        }
                                    ).build()
                                )
                            )
                        }
                    }, ra = { ra: ReplyCallbackAction ->
                        if (list.size > 1) {
                            ra.setComponents(
                                ActionRow.of(
                                    SelectMenu.create("mondata").addOptions(
                                        list.map {
                                            val so = SelectOption.of(
                                                "Form: " + getGerNameWithForm(it.getString("name")),
                                                toSDName(it.getString("name"))
                                            )
                                            if (mon.getString("name") == it.getString("name")) so.withDefault(true) else so
                                        }
                                    ).build()
                                )
                            )
                        }
                    }, m = { mes: Message ->
                        monDataButtons[mes.idLong] = MonData(list)
                    }, ih = { ih: InteractionHook -> monDataButtons[ih.interaction.idLong] = MonData(list) })
                }
                Translation.Type.MOVE -> {
                    name = gerName.translation
                    val data = movesJSON.getJSONObject(getSDName(name))
                    var type = data.getString("type")
                    type = if (type == "Psychic") "Psycho" else getGerNameNoCheck(type)
                    val p: String
                    val maxPower: Int
                    val isStatus = data.getString("category") == "Status"
                    if (data.has("ohko")) {
                        p = "K.O."
                        maxPower = 130
                    } else {
                        val bp = data.getInt("basePower")
                        p = bp.toString()
                        maxPower = if (isStatus) {
                            -1
                        } else if (type in listOf("Gift", "Kampf")) {
                            if (bp >= 150) {
                                100
                            } else if (bp >= 110) {
                                95
                            } else if (bp >= 75) {
                                90
                            } else if (bp >= 65) {
                                85
                            } else if (bp >= 55) {
                                80
                            } else if (bp >= 45) {
                                75
                            } else {
                                70
                            }
                        } else {
                            if (bp >= 150) {
                                150
                            } else if (bp >= 110) {
                                140
                            } else if (bp >= 75) {
                                130
                            } else if (bp >= 65) {
                                120
                            } else if (bp >= 55) {
                                110
                            } else if (bp >= 45) {
                                100
                            } else {
                                90
                            }
                        }
                    }
                    val accuracy: String
                    val acc = data["accuracy"]
                    accuracy = if (acc is Boolean) "-" else "$acc%"
                    val category = when (data.getString("category")) {
                        "Physical" -> "Physisch"
                        "Special" -> "Speziell"
                        "Status" -> "Status"
                        else -> "ERROR"
                    }
                    val ppc = data.getInt("pp")
                    val pp = ppc.toString() + " (max. " + (ppc shl 3) / 5 + ")"
                    val builder = EmbedBuilder()
                    builder.setTitle(name).addField("English", getEnglName(name), true).addField("Power", p, true)
                        .addField("Dyna-Power", if (maxPower == -1) "-" else maxPower.toString(), true)
                        .addField("Accuracy", accuracy, true).addField("Category", category, true)
                        .addField("AP", pp, true).addField("Type", type, true)
                        .addField("Priority", data.getInt("priority").toString(), true).setColor(Color.CYAN)
                        .setDescription(AtkDataManager.getData(name))
                    if (isStatus) {
                        val text: String
                        val eff = data.getJSONObject("zMove")
                        if (eff.has("effect")) {
                            when (eff.getString("effect")) {
                                "clearnegativeboost" -> text = "Negative Statusveränderungen werden zurückgesetzt"
                                "crit2" -> text = "Critchance +2"
                                "heal" -> text = "Volle Heilung"
                                "curse" -> text = "Volle Heilung beim Geist Typ, sonst Atk +1"
                                "redirect" -> text = "Spotlight"
                                "healreplacement" -> text = "Heilt eingewechseltes Mon voll"
                                else -> {
                                    text = "Error"
                                    logger.info(eff.toString(4))
                                }
                            }
                        } else {
                            val boosts = eff.getJSONObject("boost")
                            val stat = boosts.keys().next()
                            text = when (stat) {
                                "atk" -> "Atk +" + boosts.getInt(stat)
                                "def" -> "Def +" + boosts.getInt(stat)
                                "spa" -> "SpAtk +" + boosts.getInt(stat)
                                "spd" -> "SpDef +" + boosts.getInt(stat)
                                "spe" -> "Init +" + boosts.getInt(stat)
                                "accuracy" -> "Genauigkeit +" + boosts.getInt(stat)
                                "evasion" -> "Ausweichwert +" + boosts.getInt(stat)
                                else -> "Error"
                            }
                        }
                        builder.addField("Z-Effect", text, true)
                    } else {
                        var zpower: String? = null
                        if (data.has("zpower")) zpower = data.getInt("zpower").toString() else {
                            if (p.equals("K.O.", ignoreCase = true)) zpower = "180"
                            if (p.equals("variiert", ignoreCase = true)) {
                                zpower = "variiert"
                            } else {
                                val power = p.toInt()
                                if (power <= 55) zpower = "100" else if (power in 60..65) zpower =
                                    "120" else if (power in 70..75) zpower = "140" else if (power in 80..85) zpower =
                                    "160" else if (power in 90..95) zpower = "175" else if (power == 100) zpower =
                                    "180" else if (power == 110) zpower = "185" else if (power == 120) zpower =
                                    "190" else if (power == 130) zpower = "195" else if (power >= 140) zpower = "200"
                            }
                        }
                        if (zpower == null) sendToMe("Fehler bei Z-$name!")
                        builder.addField("Z-Power", zpower ?: "ERROR", true)
                    }
                    tco.sendMessageEmbeds(builder.build()).queue()
                }
                Translation.Type.ABILITY -> {
                    val abiname = gerName.translation
                    tco.sendMessageEmbeds(
                        EmbedBuilder().setTitle(abiname).setDescription(
                            """
    Englisch: ${getEnglName(abiname)}
    ${AbiDataManager.getData(name)}
    """.trimIndent()
                        ).setColor(Color.CYAN).build()
                    ).queue()
                }
                Translation.Type.ITEM -> {
                    val itemname = gerName.translation
                    tco.sendMessageEmbeds(
                        EmbedBuilder().setTitle(itemname).setDescription(
                            """
    Englisch: ${getEnglName(itemname)}
    ${ItemDataManager.getData(name)}
    """.trimIndent()
                        ).setColor(Color.CYAN).build()
                    ).queue()
                }
                else -> tco.sendMessage("Es gibt kein(e) Pokemon/Attacke/Fähigkeit/Item mit dem Namen $name!").queue()
            }
        } catch (ex: Exception) {
            tco.sendMessage("Es ist ein Fehler aufgetreten!").queue()
            ex.printStackTrace()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DataCommand::class.java)


        fun getPrevoInfo(obj: JSONObject): String {
            if (obj.optString("forme") == "Mega") return "Megaentwicklung von " + getGerNameNoCheck(obj.getString("baseSpecies"))
            if (!obj.has("prevo")) return ""
            var str = "ERROR (Wenn du das siehst, melde dich bitte bei Flo)"
            val prev = obj.getString("prevo")
            //String prevo = getGerNameNoCheck(obj.getString("prevo"));
            val prevo: String =
                if (prev.endsWith("-Alola") || prev.endsWith("-Galar")) prev.substring(prev.length - 5) + "-" + getGerNameNoCheck(
                    prev.substring(0, prev.length - 6)
                ) else getGerNameNoCheck(prev)
            if (obj.has("evoLevel")) str = "auf Level " + obj.getInt("evoLevel") else if (obj.has("evoType")) {
                str = when (obj.getString("evoType")) {
                    "useItem" -> "mit dem Item \"" + getGerNameNoCheck(obj.getString("evoItem")) + "\""
                    "levelFriendship" -> "durch Freundschaft"
                    "trade" -> "durch Tausch"
                    "levelExtra" -> ""
                    "levelHold" -> "durch ein Level-Up, wenn es das Item \"" + getGerNameNoCheck(obj.getString("evoItem")) + "\" trägt"
                    "levelMove" -> "durch ein Level-Up, wenn es die Attacke \"" + getGerNameNoCheck(obj.getString("evoMove")) + "\" beherrscht"
                    else -> str
                }
            }
            var condition = ""
            if (obj.has("evoCondition")) condition = """
     
     Bedingung: ${obj.getString("evoCondition")}
     """.trimIndent()
            return "Entwickelt sich aus $prevo $str$condition"
        }
    }
}