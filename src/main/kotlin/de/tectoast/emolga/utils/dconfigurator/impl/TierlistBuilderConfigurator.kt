package de.tectoast.emolga.utils.dconfigurator.impl

import de.tectoast.emolga.commands.condAppend
import de.tectoast.emolga.commands.embedColor
import de.tectoast.emolga.commands.file
import de.tectoast.emolga.commands.myJSON
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.utils.dconfigurator.*
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.draft.TierlistMode
import de.tectoast.emolga.utils.json.NameConventions
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.get
import dev.minn.jda.ktx.interactions.components.*
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import kotlinx.serialization.encodeToString
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IDeferrableCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.litote.kmongo.eq
import org.litote.kmongo.keyProjection
import org.litote.kmongo.set
import org.litote.kmongo.setTo

class TierlistBuilderConfigurator(
    userId: Long, channelId: Long, guildId: Long, val mons: List<String>, val tierlistcols: List<List<String>>
) : DGuildConfigurator("tierlistbuilder", userId, channelId, guildId) {
    @OptIn(ExperimentalStdlibApi::class)
    override val steps: List<Step<*>> = listOf(step<SlashCommandInteractionEvent> {
        val nc = db.nameconventions.findOne(NameConventions::guild eq guildId)?.data ?: run {
            val tmp = NameConventions(guildId)
            db.nameconventions.insertOne(tmp)
            tmp.data
        }

        hook.sendMessageEmbeds(
            Embed(
                title = "Gibt es in der Tierliste Alola/Galar/Hisui/Mega-Formen?",
                description = "Wenn ja, dann gib im gleich erscheinenden Formular an den jeweiligen Stellen das Format so ein, dass statt dem Namen des Pokemons `POKEMON` (nur Großbuchstaben) steht.\n" + "Wenn z.B. Alola-Formen so aussehen: `A-Sandamer`, dann gib `A-POKEMON` ein.\n" + "Selbiges geht natürlich auch dahinter, also z.B. für `Sandamer-Alola` wäre es `POKEMON-Alola`.\n\n" + "Falls es generell Probleme oder Fragen gibt, komme auf den in meinem Profil verlinkten Support-Server :)".condAppend(
                    nc.isNotEmpty()
                ) {
                    "\n\nDerzeitige Einstellungen (drücke Nein wenn diese nicht geändert werden sollten):"
                },
            ) {
                color = embedColor
            nc.forEach {
                field {
                    name = it.key.replaceFirstChar { c -> c.uppercase() }
                    value = it.value.pattern.replace("(\\S+)", "POKEMON")
                    inline = true
                }
            }
        }).yesNoButtons().queue()
    }, step<ButtonInteractionEvent> {
        if (isYes) {
            replyModal(nextModal).queue()
            return@step
        }
        deferReply().queue()
        it.skipNextSteps = 1
        if (test(this, it)) it.skipNextSteps++
    }, ModalStep({
        Modal("${it}tierlistbuilder;regionals", "Regionalformen/Megaentwicklungen") {
            short(
                "mega", "Wie sollen Megaentwicklungen aussehen?", required = false
            )
            short(
                "alola", "Wie sollen Alola-Formen aussehen?", required = false
            )
            short(
                "galar", "Wie sollen Galar-Formen aussehen?", required = false
            )
            short("hisui", "Wie sollen Hisui-Formen aussehen?", required = false)
        }
    }, { options ->
        for (form in listOf("Mega", "Alola", "Galar", "Hisui")) {
            getValue(form.replaceFirstChar { c -> c.lowercase() })?.asString?.takeUnless { it.isBlank() }?.also {
                if ("POKEMON" !in it) throw InvalidArgumentException("Das Format muss `POKEMON` enthalten!")
            }?.let {
                db.nameconventions.updateOne(
                    NameConventions::guild eq guildId,
                    set(NameConventions::data.keyProjection(form) setTo it.replace("POKEMON", "(\\S+)").toRegex())
                )
            }
        }
        deferReply().queue()
        if (test(this, options)) options.skipNextSteps = 1
    }), step<SlashCommandInteractionEvent> { options ->
        val name = getOption<String>("name")!!
        newSuspendedTransaction {
            NameConventionsDB.run {
                select(GERMAN eq name).firstOrNull()
            }
        } ?: throw InvalidArgumentException("Dieser Name entspricht nicht meinen Konventionen!")
        NameConventionsDB.addName(mons[index], name, guildId)
        deferReply().queue()
        test(this, options)
    }, step<StringSelectInteractionEvent> {
        tierlistMode = when (val s = values[0]) {
            "points" -> TierlistMode.POINTS
            "tiers" -> TierlistMode.TIERS
            "mix" -> TierlistMode.TIERS_WITH_FREE
            else -> error("Unknown tierlist mode $s")
        }
        replyModal(nextModal).queue()
        hook.editOriginalComponents(
            ActionRow.of(
                StringSelectMenu(
                    componentId,
                    options = component.options.map { it.withDefault(false) })
            )
        ).queue()
    }, ModalStep({
        Modal("${nextIndex}tierlistbuilder;tierlisttiers", "Tierlist-Tiers") {
            short(
                "tiers", "Tiers im Draft [,-separiert]", required = true, placeholder = "S,A,B,C,D"
            )
            when (tierlistMode) {
                TierlistMode.POINTS -> short(
                    "points", "Preise der Tiers [,-separiert]", required = true, placeholder = "160,120,80,40,20"
                )

                TierlistMode.TIERS, TierlistMode.TIERS_WITH_FREE -> short(
                    "monspertier",
                    "Anzahl an Pokemon pro Tier [,-separiert]",
                    required = true,
                    placeholder = "2,2,3,3,1"
                )

                else -> error("Unknown tierlist mode $tierlistMode")
            }
            if (tierlistMode == TierlistMode.TIERS_WITH_FREE) {
                short(
                    "freeprices",
                    "Preise der Tiers als Free-Pick [,-separiert]",
                    required = true,
                    placeholder = "160,120,80,40,20"
                )
                short("freeamount", "Anzahl an Free-Picks", required = true, placeholder = "4")
            }
            when (tierlistMode) {
                TierlistMode.POINTS, TierlistMode.TIERS_WITH_FREE -> short(
                    "budget", "Anzahl an Punkten pro Spieler", required = true, placeholder = "1000"
                )

                else -> {}
            }
        }
    }, {
        val tiers = this["tiers"].split(",").mapNotNull { it.trim().takeUnless { s -> s.isEmpty() } }
        val points = when (tierlistMode) {
            TierlistMode.POINTS -> this["points"].split(",")
                .map { it.trim().toIntOrNull() ?: throw InvalidArgumentException("Ungültige Punkte (`$it`)!") }

            TierlistMode.TIERS, TierlistMode.TIERS_WITH_FREE -> this["monspertier"].split(",")
                .map { it.trim().toIntOrNull() ?: throw InvalidArgumentException("Ungültige Tier-Anzahl (`$it`)!") }

            else -> error("Unknown tierlist mode $tierlistMode")
        }
        val freePrices = when (tierlistMode) {
            TierlistMode.TIERS_WITH_FREE -> this["freeprices"].split(",").map {
                it.trim().toIntOrNull() ?: throw InvalidArgumentException("Ungültige Free-Pick-Punkte (`$it`)!")
            }

            else -> null
        }
        val freeAmount = when (tierlistMode) {
            TierlistMode.TIERS_WITH_FREE -> this["freeamount"].toIntOrNull()
                ?: throw InvalidArgumentException("Ungültige Free-Pick-Anzahl (`${this["freeamount"]}`)!")

            else -> null
        }
        val budget = when (tierlistMode) {
            TierlistMode.POINTS, TierlistMode.TIERS_WITH_FREE -> this["budget"].toIntOrNull()
                ?: throw InvalidArgumentException("Ungültige Anzahl an Punkten (`${this["budget"]}`)!")

            else -> null
        }
        if (tiers.size != points.size) throw InvalidArgumentException("Die Anzahl der Tiers und der Punkte/Mon-Anzahl stimmt nicht überein!")
        if (freePrices != null && tiers.size != freePrices.size) throw InvalidArgumentException("Die Anzahl der Tiers und der Free-Preise stimmt nicht überein!")

        this@TierlistBuilderConfigurator.prices = tiers.zip(points).toMap()
        this@TierlistBuilderConfigurator.freepicks =
            freePrices?.let { tiers.zip(it).toMap().toMutableMap().apply { put("#AMOUNT#", freeAmount!!) } }
        this@TierlistBuilderConfigurator.points = budget
        reply(
            "Alles klar!\n" + "Nächster Schritt: Welcher der am Anfang genannten Bereiche gehört zu welchem Tier? (Ich zeige jeweils das erste Mon aus der Spalte)"
        ).setComponents(tiermappingComponents(false)).queue()
    }), step<GenericComponentInteractionCreateEvent> {
        if (this is ButtonInteractionEvent) {
            val data = componentId.split(":")[1]
            if (data == "default") {
                val tiers = prices!!.keys.toList()
                val lastTier = tiers.lastIndex
                for (i in 0..<lastTier) {
                    tiermapping[i] = tiers.elementAt(i)
                }
                for (i in lastTier..tierlistcols.lastIndex) {
                    tiermapping[i] = tiers.elementAt(lastTier)
                }
                saveToFile()
                reply("Die Einrichtung der Tierliste wurde abgeschlossen!").queue()
                return@step
            }
            currCol = data.toInt()
            editComponents(tiermappingComponents(true)).queue()
            it.dontSkip()
        } else if (this is StringSelectInteractionEvent) {
            val tier = values[0]
            tiermapping[currCol] = tier
            editComponents(tiermappingComponents(false)).queue()
            if (tiermapping.size != tierlistcols.size) {
                it.dontSkip()
                return@step
            }
            saveToFile()
            hook.send("Die Einrichtung der Tierliste wurde abgeschlossen!").queue()
        }
    })


    private var index: Int = 0
    private var sendRegionalInfo = true
    private var tierlistMode: TierlistMode? = null
    private var prices: Map<String, Int>? = null
    private var freepicks: Map<String, Int>? = null
    private var points: Int? = null
    private val tiermapping = mutableMapOf<Int, String>()

    private var currCol = -1


    private suspend fun test(e: IDeferrableCallback, options: StepOptions): Boolean {
        val regional = db.nameconventions.get(guildId)
        while (index < mons.size) {
            val mon = mons[index]
            val regForm = regional.entries.firstNotNullOfOrNull {
                it.value.find(mon)?.let { mr -> mr to it.key }
            }?.let { (mr, key) ->
                mr.groupValues[1] + "-" + key
            }
            println("Testing $mon <=> $regForm")
            if (!NameConventionsDB.checkIfExists(
                    regForm ?: mon, guildId
                ) && (regForm == null || !NameConventionsDB.checkIfExists(mon, guildId))
            ) {
                e.hook.send("`$mon` wurde nicht gefunden, bitte gib den Namen in meinem Format über /addconvention an.".condAppend(
                    sendRegionalInfo
                ) {
                    sendRegionalInfo = false
                    " (Falls es sich um ein normales Alola/Galar/Mega-Pokemon handeln sollte [Glurak-Mega-X/Y sind davon ausgenommen], musst du das am Anfang bei /preparetierlist angeben, damit ich das richtig erkenne. Um das jetzt noch zu ändern, gib einfach nochmal /preparetierlist mit den richtigen Namen ein)"
                }).queue()
                if (e is SlashCommandInteractionEvent) options.dontSkip()
                return false
            }
            index++
        }
        // all names accepted
        e.hook.send(
            "Ich verstehe alle Namen, die du angegeben hast!\n" + "Nächster Schritt: Konfiguration des Drafts!\n" + "Zuerst: Welche Art von Draft ist das hier?"
        ).setComponents(
            modeSelectMenu
        ).queue()
        return true
    }

    private fun tiermappingComponents(withSelect: Boolean) = tierlistcols.mapIndexed { i, list ->
        primary(
            "6tierlistbuilder;tiermapping:$i", "Spalte ${i + 1}: ${list[0]}", disabled = i in tiermapping
        )
    }.chunked(5).map {
        ActionRow.of(it)
    } + (if (withSelect) listOf(
        ActionRow.of(
            StringSelectMenu(
                "6tierlistbuilder;tiermapping",
                options = prices!!.keys.map { SelectOption(it, it) },
                placeholder = "Konfiguration von Spalte ${currCol + 1}"
            )
        )
    ) else emptyList()) + listOf(
        ActionRow.of(
            success("6tierlistbuilder;tiermapping:default", "Standard-Tierzuordnung")
        )
    )

    private fun saveToFile() {
        "Tierlists/$guildId.json".file().writeText(myJSON.encodeToString(Tierlist(guildId).apply {
            this.prices += this@TierlistBuilderConfigurator.prices!!
            this@TierlistBuilderConfigurator.freepicks?.let { this.freepicks += it }
            this@TierlistBuilderConfigurator.points?.let { this.points = it }
            this.mode = this@TierlistBuilderConfigurator.tierlistMode!!
        }))
        transaction {
            Tierlist.batchInsert(tierlistcols.flatMapIndexed { index, strings ->
                strings.map {
                    DraftPokemon(
                        it,
                        tiermapping[index]!!
                    )
                }
            }, shouldReturnGeneratedValues = false) {
                this[Tierlist.guild] = guildId
                this[Tierlist.pokemon] = it.name
                this[Tierlist.tier] = it.tier
            }
        }
    }

    companion object {
        val modeSelectMenu = StringSelectMenu(
            "4tierlistbuilder;tierlistmode", options = listOf(
                SelectOption("Punkte", "points", "Ein Punktedraft"),
                SelectOption("Tiers", "tiers", "Ein Draft mit festgelegten Tiers"),
                SelectOption("Tiers mit Free-Picks", "mix", "Ein Draft mit festgelegten Tiers und Free-Picks")
            )
        ).into()


        val enabledGuilds = setOf(1054161634895069215, 651152835425075218, 736555250118295622)
    }
}
