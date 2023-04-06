package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.*
import de.tectoast.emolga.database.exposed.AbiDataDB
import de.tectoast.emolga.database.exposed.AtkDataDB
import de.tectoast.emolga.database.exposed.ItemDataDB
import de.tectoast.emolga.utils.json.showdown.Pokemon
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
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
                    val mon = dataJSON[toSDName(
                        gerName.otherLang + args.getOrDefault(
                            "regform", ""
                        ) + args.getOrDefault("form", "") + (gerName.forme ?: "")
                    )] ?: run {
                        e.reply("$name besitzt diese Form nicht!")
                        return
                    }
                    val builder = EmbedBuilder()
                    builder.addField("Englisch", mon.name, true)
                    builder.addField("Dex", mon.num.toString(), true)
                    builder.addField(
                        "Geschlecht",
                        mon.genderRatio?.let { "${it["M"]!! * 100}% ♂ ${it["F"]!! * 100}% ♀" }
                            ?: mon.gender?.let { if (it == "M") "100% ♂" else "100% ♀" } ?: "50% ♂ 50% ♀",
                        true)
                    //list.forEach(j -> logger.info(j.toString(4)));
                    val monname = mon.name
                    if (monname.equals("silvally", ignoreCase = true) || monname.equals("arceus", ignoreCase = true)) {
                        builder.addField("Typen", "Normal", false)
                    } else {
                        logger.info(mon.toString())
                        builder.addField("Typen", mon.types.joinToString(" ") {
                            if (it == "Psychic") "Psycho"
                            else getGerNameNoCheck(it)
                        }, false)
                    }
                    builder.addField("Größe", "${mon.heightm} m", true)
                    builder.addField("Gewicht", "${mon.weightkg} kg", true)
                    builder.addField("Eigruppe", mon.eggGroups.joinToString {
                        getGerNameNoCheck("E_$it")
                    }, true)
                    if (monname.equals("silvally", ignoreCase = true) || monname.equals("arceus", ignoreCase = true)) {
                        builder.addField(
                            "Fähigkeiten",
                            if (monname.equals("silvally", ignoreCase = true)) "Alpha-System" else "Variabilität",
                            false
                        )
                    } else {
                        builder.addField("Fähigkeiten", mon.abilities.entries.joinToString("\n") {
                            getGerNameNoCheck(it.value).condAppend(it.key == "H", " (VF)")
                        }, false)
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
                        val str = mon.buildStatString()
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
                            ma.setComponents(ActionRow.of(StringSelectMenu.create("mondata").addOptions(list.map {
                                SelectOption.of(
                                    "Form: " + getGerNameWithForm(it.name),
                                    toSDName(it.name)
                                ).withDefault(mon.name == it.name)
                            }).build()))
                        }
                    }, ra = { ra: ReplyCallbackAction ->
                        if (list.size > 1) {
                            ra.setComponents(ActionRow.of(StringSelectMenu.create("mondata").addOptions(list.map {
                                val so = SelectOption.of(
                                    "Form: " + getGerNameWithForm(it.name),
                                    toSDName(it.name)
                                )
                                if (mon.name == it.name) so.withDefault(true) else so
                            }).build()))
                        }
                    })
                }

                Translation.Type.MOVE -> {
                    name = gerName.translation
                    val data = movesJSON[getSDName(name)]!!.jsonObject
                    var type = data["type"].string
                    type = if (type == "Psychic") "Psycho" else getGerNameNoCheck(type)
                    val p: String
                    val maxPower: Int
                    val cat = data["category"].string
                    val isStatus = cat == "Status"
                    if ("ohko" in data) {
                        p = "K.O."
                        maxPower = 130
                    } else {
                        val bp = data["basePower"].int
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
                    val acc = data["accuracy"]!!.jsonPrimitive
                    val accuracy = if (acc.isString) "$acc%" else "-"
                    val category = when (cat) {
                        "Physical" -> "Physisch"
                        "Special" -> "Speziell"
                        "Status" -> "Status"
                        else -> "ERROR"
                    }
                    val ppc = data["pp"].int
                    val pp = ppc.toString() + " (max. " + (ppc shl 3) / 5 + ")"
                    val builder = EmbedBuilder()
                    builder.setTitle(name).addField("English", getEnglName(name), true).addField("Power", p, true)
                        .addField("Dyna-Power", if (maxPower == -1) "-" else maxPower.toString(), true)
                        .addField("Accuracy", accuracy, true).addField("Category", category, true)
                        .addField("AP", pp, true).addField("Type", type, true)
                        .addField("Priority", data["priority"].string, true).setColor(Color.CYAN)
                        .setDescription(AtkDataDB.getData(name))
                    if (isStatus) {
                        val eff = data["zMove"]!!.jsonObject
                        builder.addField("Z-Effect", eff["effect"]?.string?.let {
                            when (it) {
                                "clearnegativeboost" -> "Negative Statusveränderungen werden zurückgesetzt"
                                "crit2" -> "Critchance +2"
                                "heal" -> "Volle Heilung"
                                "curse" -> "Volle Heilung beim Geist Typ, sonst Atk +1"
                                "redirect" -> "Spotlight"
                                "healreplacement" -> "Heilt eingewechseltes Mon voll"
                                else -> {
                                    logger.info(eff.toString())
                                    "Error"
                                }
                            }
                        } ?: run {
                            val boosts = eff["boost"]!!.jsonObject
                            val stat = boosts.keys.first()
                            when (stat) {
                                "atk" -> "Atk"
                                "def" -> "Def"
                                "spa" -> "SpAtk"
                                "spd" -> "SpDef"
                                "spe" -> "Init"
                                "accuracy" -> "Genauigkeit"
                                "evasion" -> "Ausweichwert"
                                else -> "Error"
                            } + " +${boosts[stat].int}"
                        }, true)

                    } else {
                        builder.addField("Z-Power", data["zpower"]?.string ?: run {
                            when (p) {
                                "K.O." -> "180"
                                "variiert" -> "variiert"
                                else -> {
                                    val power = p.toInt()
                                    zMovePowers.entries.first { power in it.key }.value.toString()
                                }
                            }
                        }, true)
                    }
                    tco.sendMessageEmbeds(builder.build()).queue()
                }

                Translation.Type.ABILITY -> {
                    val abiname = gerName.translation
                    tco.sendMessageEmbeds(
                        EmbedBuilder().setTitle(abiname).setDescription(
                            """
    Englisch: ${getEnglName(abiname)}
    ${AbiDataDB.getData(name)}
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
    ${ItemDataDB.getData(name)}
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

        val zMovePowers: Map<IntRange, Int> = mapOf(
            1..55 to 100,
            60..65 to 120,
            70..75 to 140,
            80..85 to 160,
            90..95 to 175,
            100..100 to 180,
            110..110 to 185,
            120..120 to 190,
            130..130 to 195,
            140..200 to 200
        )

        fun getPrevoInfo(obj: Pokemon): String {
            if (obj.forme == "Mega") return "Megaentwicklung von " + getGerNameNoCheck(obj.baseSpecies!!)
            val prev = obj.prevo ?: return ""
            //String prevo = getGerNameNoCheck(obj.getString("prevo"));
            return "Entwickelt sich aus ${
                if (prev.endsWith("-Alola") || prev.endsWith("-Galar")) prev.substring(prev.length - 5) + "-" + getGerNameNoCheck(
                    prev.substring(0, prev.length - 6)
                ) else getGerNameNoCheck(prev)
            } ${
                obj.evoLevel?.let { "auf Level $it" } ?: obj.evoType?.let {
                    when (it) {
                        "useItem" -> "mit dem Item \"${getGerNameNoCheck(obj.evoItem!!)}\""
                        "levelFriendship" -> "durch Freundschaft"
                        "trade" -> "durch Tausch".notNullAppend(obj.evoItem?.let { itm ->
                            " mit dem Item \"${
                                getGerNameNoCheck(
                                    itm
                                )
                            }\""
                        })

                        "levelExtra" -> ""
                        "levelHold" -> "durch ein Level-Up, wenn es das Item \"${getGerNameNoCheck(obj.evoItem!!)}\" trägt"
                        "levelMove" -> "durch ein Level-Up, wenn es die Attacke \"${getGerNameNoCheck(obj.evoMove!!)}\" beherrscht"
                        else -> null
                    }
                } ?: "ERROR (Wenn du das siehst, melde dich bitte bei Flo)"
            }".notNullAppend(obj.evoCondition?.let { "\nBedingung: $it" })
        }
    }
}
