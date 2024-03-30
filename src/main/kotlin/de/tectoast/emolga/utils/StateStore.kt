package de.tectoast.emolga.utils

import com.mongodb.client.model.Filters
import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.database.exposed.SpoilerTagsDB
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.draft.EnterResult
import de.tectoast.emolga.features.draft.Nominate
import de.tectoast.emolga.features.draft.during.QueuePicks
import de.tectoast.emolga.features.draft.during.QueuePicks.ControlButton.ControlMode
import de.tectoast.emolga.features.draft.during.QueuePicks.ControlButton.ControlMode.*
import de.tectoast.emolga.features.draft.during.QueuePicks.checkIfTeamCantBeFinished
import de.tectoast.emolga.features.intoMultipleRows
import de.tectoast.emolga.utils.draft.DraftPlayer
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.Nominations
import de.tectoast.emolga.utils.json.emolga.draft.GamedayData
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.NDS
import de.tectoast.emolga.utils.json.emolga.draft.QueuePicksData
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into
import kotlinx.serialization.*
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import org.litote.kmongo.*

@Serializable
sealed class StateStore {
    var uid: Long = -1

    constructor(uid: Long) {
        this.uid = uid
    }

    @Transient
    private var forDeletion = false

    suspend fun afterOperation() {
        if (forDeletion) deleteFromDB()
        else save()
    }

    private suspend fun save() {
        val filter = createFilter()
        if (db.statestore.findOne(filter) == null) db.statestore.insertOne(this)
        else db.statestore.updateOne(filter, this)
    }

    private suspend fun deleteFromDB() {
        db.statestore.deleteOne(createFilter())
    }

    fun delete() {
        forDeletion = true
    }

    private fun createFilter() = and(Filters.eq("type", this::class.simpleName!!), StateStore::uid eq uid)

    companion object {
        context(InteractionData)
        suspend inline fun <reified T : StateStore> process(block: T.() -> Unit) {
            processIgnore<T>(block) ?: reply(
                "Diese Interaktion ist nicht mehr gültig! Starte sie wenn nötig erneut!", ephemeral = true
            )
        }

        context(InteractionData)
        suspend inline fun <reified T : StateStore> processIgnore(block: T.() -> Unit) =
            (db.statestore.findOne(StateStore::uid eq user, Filters.eq<String?>("type", T::class.simpleName)) as T?)
                ?.process(block)

    }
}

suspend inline fun <T : StateStore> T.process(block: T.() -> Unit) {
    block()
    afterOperation()
}

@Serializable
@SerialName("ResultEntry")
class ResultEntry : StateStore {

    var leaguename: String = ""

    val data: List<MutableList<MonData>> = listOf(mutableListOf(), mutableListOf())
    private val uids = mutableListOf<Long>()

    @Transient
    val league = OneTimeCache { db.league(leaguename) }

    constructor(uid: Long, league: League) : super(uid) {
        this.leaguename = league.leaguename
    }


    private suspend fun getPicksByUid(uid: Long) = league().providePicksForGameday(gamedayData.gameday)[uid]!!
    private suspend fun getMonsByUid(uid: Long) = getPicksByUid(uid).sortedWith(league().tierorderingComparator).map {
        (it.name to NameConventionsDB.convertOfficialToTL(
            it.name, league().guild
        )!!).let { (official, tl) -> SelectOption(tl, "$official#$tl") }
    }

    @Transient
    val picks: Cache<Map<Long, List<SelectOption>>> = OneTimeCache { uids.associateWith { getMonsByUid(it) } }
    private lateinit var gamedayData: GamedayData


    private val wifiPlayers = (0..1).map { DraftPlayer(0, false) }

    @Transient
    private val defaultComponents: Cache<List<ActionRow>> = OneTimeCache {
        uids.mapIndexed { index, uid ->
            ActionRow.of(EnterResult.ResultMenu(
                "${if (index == 0) "Deine" else "Gegnerische"} Pokemon",
                options = picks()[uid]!!,
            ) { this.userindex = index })
        } + listOf(ActionRow.of(EnterResult.ResultFinish("Ergebnis bestätigen", ButtonStyle.PRIMARY) {
            mode = EnterResult.ResultFinish.Mode.CHECK
        }))
    }

    context(InteractionData)
    suspend fun init(opponent: Long, user: Long) {
        uids += user
        uids += opponent
        gamedayData = league().getGameplayData(uids[0], uids[1], wifiPlayers)
        reply(embeds = buildEmbed(), components = defaultComponents(), ephemeral = true)
    }

    private fun buildEmbed() = Embed {
        title = "Interaktive Ergebnis-Eingabe"
        description = "Wähle im Menü unten ein Pokemon aus!"
        data.forEachIndexed { i, map ->
            field {
                name = "${if (i == 0) "Deine" else "Gegnerische"} Pokemon"
                value = map.joinToString("\n")
                inline = false
            }
        }
    }.into()

    context(InteractionData)
    fun handleSelect(e: EnterResult.ResultMenu.Args) {
        val selected = e.selected
        val userindex = e.userindex
        replyModal(EnterResult.ResultModal(
            "Ergebnis für ${selected.substringAfterLast("#")}",
            mapOf(EnterResult.Remove to data[userindex].any { it.official == selected.substringBefore("#") })
        ) {
            this.userindex = userindex
            this.selected = selected
        })
    }

    context(InteractionData)
    fun handleModal(e: EnterResult.ResultModal.Args) {
        val userindex = e.userindex
        val (official, tl) = e.selected.split("#")
        val list = data[userindex]
        list.indexOfFirst { it.official == official }.let {
            if (e.remove && it != -1) {
                list.removeAt(it)
                return@let
            }
            val monData = MonData(tl, official, e.kills, e.dead)
            if (it == -1) list.add(monData)
            else list[it] = monData
        }
        edit(embeds = buildEmbed())
    }

    context(InteractionData)
    suspend fun handleFinish(e: EnterResult.ResultFinish.Args) {
        if (checkConditionsForFinish()) return
        val league = league()
        when (e.mode) {
            EnterResult.ResultFinish.Mode.CHECK -> {
                val originalComponents = defaultComponents()
                val buttons = ActionRow.of(EnterResult.ResultFinish("Wirklich bestätigen", ButtonStyle.SUCCESS) {
                    mode = EnterResult.ResultFinish.Mode.YES
                }, EnterResult.ResultFinish("Abbrechen", ButtonStyle.DANGER) {
                    mode = EnterResult.ResultFinish.Mode.NO
                })
                val newComponents = originalComponents + listOf(buttons)
                edit(components = newComponents)
            }

            EnterResult.ResultFinish.Mode.YES -> {
                if (league.replayDataStore != null) reply(
                    "Das Ergebnis des Kampfes wurde gespeichert! Du kannst nun die Eingabe-Nachricht verwerfen.",
                    ephemeral = true
                )
                else {
                    reply(generateFinalMessage())
                }
                delete()
                val game = data.mapIndexed { index, d ->
                    wifiPlayers[index].apply {
                        alivePokemon = d.size - d.dead
                        winner = d.size != d.dead
                    }
                }
                league.docEntry?.analyse(
                    ReplayData(game = game,
                        uids = uids,
                        kd = data.map { it.associate { p -> p.official to (p.kills to if (p.dead) 1 else 0) } },
                        mons = data.map { l -> l.map { it.official } },
                        url = "WIFI",
                        gamedayData = gamedayData.apply {
                            numbers = game.map { it.alivePokemon }
                                .let { l -> if (gamedayData.u1IsSecond) l.reversed() else l }
                        })
                )
            }

            EnterResult.ResultFinish.Mode.NO -> edit(components = defaultComponents())
        }
    }

    context(InteractionData)
    private fun checkConditionsForFinish(): Boolean {
        if (data[0].isEmpty() || data[1].isEmpty()) return reply(
            "Du hast noch keine Daten eingeben!", ephemeral = true
        ).let { true }
        if ((0..1).any { data[it].kills != data[1 - it].dead }) return reply(
            "Die Kills und Tode müssen übereinstimmen!", ephemeral = true
        ).let { true }
        return false
    }

    private suspend fun generateFinalMessage(): String {
        val spoiler = SpoilerTagsDB.contains(league().guild)
        return "${
            data.mapIndexed { index, sdPlayer ->
                mutableListOf<Any>("<@${uids[index]}>", sdPlayer.count { !it.dead }).apply {
                    if (spoiler) add(
                        1, "||"
                    )
                }.let { if (index % 2 > 0) it.asReversed() else it }
            }.joinToString(":") { it.joinToString(" ") }
        }\n\n${
            data.mapIndexed { index, monData ->
                "<@${uids[index]}>:\n${monData.joinToString("\n").surroundWith(if (spoiler) "||" else "")}"
            }.joinToString("\n\n")
        }"
    }

    private val List<MonData>.kills get() = sumOf { it.kills }
    private val List<MonData>.dead get() = sumOf { (if (it.dead) 1 else 0).toInt() }

    @Serializable
    data class MonData(val pokemon: String, val official: String, val kills: Int, val dead: Boolean) {
        override fun toString(): String {
            return "$pokemon $kills".condAppend(dead) { " X" }
        }
    }

}

@Serializable
@SerialName("NominateState")
class NominateState : StateStore {

    private val originalMons: List<DraftPokemon>
    private val mons: List<DraftPokemon>
    private val nominated: MutableList<DraftPokemon>
    private val notNominated: MutableList<DraftPokemon>
    private val nomUser: Long

    constructor(uid: Long, nomUser: Long, originalMons: List<DraftPokemon>, mons: List<DraftPokemon>) : super(uid) {
        this.nomUser = nomUser
        this.originalMons = originalMons
        this.mons = mons
        this.nominated = ArrayList(mons)
        this.notNominated = ArrayList(mons.size)
    }


    fun unnominate(name: String) = mons.first { it.name == name }.let {
        nominated.remove(it)
        notNominated.add(it)
    }


    fun nominate(name: String) = mons.first { it.name == name }.let {
        notNominated.remove(it)
        nominated.add(it)
    }

    private fun isNominated(s: String) = nominated.any { it.name == s }

    companion object {
        private val tiers = listOf("S", "A", "B")
        val comparator = compareBy<DraftPokemon>({ it.tier.indexedBy(tiers) }, { it.name })
    }

    private fun List<DraftPokemon>.toMessage(): String {
        return this.sortedWith(comparator).joinToString("\n") {
            "${it.tier}: ${it.name}"
        }
    }

    private fun List<DraftPokemon>.toJSON() = this.sortedWith(comparator).joinToString(";") {
        it.indexedBy(originalMons).toString()
    }

    fun generateDescription(): String {
        return buildString {
            append("**Nominiert: (${nominated.size})**\n")
            append(nominated.toMessage())
            append("\n**Nicht nominiert: (").append(notNominated.size).append(")**\n")
            append(notNominated.toMessage())
        }
    }

    context(InteractionData)
    fun render() {
        edit(embeds = Embed(
            title = "Nominierungen", color = embedColor, description = generateDescription()
        ).into(), components = mons.map {
            val s = it.name
            val isNom = isNominated(s)
            Nominate.NominateButton(s, if (isNom) ButtonStyle.PRIMARY else ButtonStyle.SECONDARY) {
                data = s; this.mode =
                if (isNom) Nominate.NominateButton.Mode.UNNOMINATE else Nominate.NominateButton.Mode.NOMINATE
            }
        }.intoMultipleRows().toMutableList().apply {
            add(
                ActionRow.of(Nominate.NominateButton(
                    buttonStyle = ButtonStyle.SUCCESS,
                    emoji = Emoji.fromUnicode("✅"),
                    disabled = nominated.size != 11
                ) { mode = Nominate.NominateButton.Mode.FINISH; data = "NOTNOW" })
            )
        })
    }

    private fun buildJSONString(): String {
        return buildString {
            append(nominated.toJSON())
            append(";")
            append(notNominated.toJSON())
        }
    }

    context(InteractionData)
    suspend fun finish(now: Boolean) {
        if (now) {
            val nom = db.nds().nominations
            val day = nom.current()
            if (nomUser in day) return reply("Du hast dein Team bereits für Spieltag ${nom.currentDay} nominiert!")
            db.drafts.updateOne(
                League::leaguename eq "NDS", set(
                    (NDS::nominations / Nominations::nominated).keyProjection(nom.currentDay)
                        .keyProjection(nomUser) setTo buildJSONString()
                )
            )
            delete()
            return reply("Deine Nominierung wurde gespeichert!")
        }
        if (nominated.size != 11) {
            reply(content = "Du musst exakt 11 Pokemon nominieren!", ephemeral = true)
        } else {
            edit(
                embeds = Embed(
                    title = "Bist du dir wirklich sicher? Die Nominierung kann nicht rückgängig gemacht werden!",
                    color = embedColor,
                    description = generateDescription()
                ).into(), components = listOf(Nominate.NominateButton("Ja", ButtonStyle.SUCCESS) {
                    mode = Nominate.NominateButton.Mode.FINISH
                    data = "FINISHNOW"
                }, Nominate.NominateButton("Nein", ButtonStyle.DANGER) {
                    mode = Nominate.NominateButton.Mode.CANCEL
                }).into()
            )
        }
    }
}

@Serializable
@SerialName("QueuePicks")
@OptIn(ExperimentalSerializationApi::class)
class QueuePicks : StateStore {

    var leaguename = ""
    var currentlyEnabled = false

    @EncodeDefault
    private val currentState: MutableList<DraftName>

    @EncodeDefault
    private val addedMeanwhile: MutableList<DraftName> = mutableListOf()

    constructor(uid: Long, leaguename: String, currentData: QueuePicksData) : super(uid) {
        this.leaguename = leaguename
        this.currentState = currentData.queued.toMutableList()
        this.currentlyEnabled = currentData.enabled
    }

    fun addNewMon(mon: DraftName) {
        addedMeanwhile.add(mon)
    }

    private fun buildStateEmbed(currentMon: String?) = Embed {
        color = embedColor
        title = "Deine gequeueten Picks"
        description =
            "Hier kannst du die Pokemon verschieben oder entfernen.\n" +
                    "Allgemein ist die hier aufgezeigte Liste eine Kopie deiner echten Liste, die erst nach dem Klick auf `Speichern` oder `Speichern und aktivieren` aktualisiert wird.\n" +
                    "Da über den `/queuepicks add` Befehl das Pokemon direkt in die echte Liste hinzugefügt wird, muss man danach hier `Neue Pokemon laden` klicken, um die Liste zu aktualisieren.\n" +
                    "Alternativ kann man auch ein neues Verwaltungsfenster über `/queuepicks manage` öffnen."
        field(
            "Aktuelle Reihenfolge",
            currentState.mapIndexed { index, draftName -> "${index + 1}. ${draftName.tlName}" }.joinToString("\n"),
            false
        )
        field("Status", if (currentlyEnabled) "Aktiv" else "Inaktiv", false)
        currentMon?.let {
            field("Aktuelles Pokemon", it, false)
        }
    }.into()

    private fun buildSelectMenu() = listOf(
        if (currentState.isEmpty()) ActionRow.of(
            QueuePicks.Menu(
                placeholder = "Keine Picks mehr",
                disabled = true,
                options = listOf(SelectOption("Keine Picks", "Keine Picks"))
            )
        ) else ActionRow.of(QueuePicks.Menu(options = currentState.map { SelectOption(it.tlName, it.tlName) }))
    ) + controlButtons

    private fun buildButtons(tlName: String): List<ActionRow> {
        val first = currentState.firstOrNull()?.tlName == tlName
        val last = currentState.lastOrNull()?.tlName == tlName
        return listOf(ActionRow.of(QueuePicks.ControlButton(
            "Hoch", ButtonStyle.PRIMARY, Emoji.fromUnicode("⬆"), disabled = first
        ) {
            mon = tlName; controlMode = UP
        },
            QueuePicks.ControlButton("Runter", ButtonStyle.PRIMARY, Emoji.fromUnicode("⬇"), disabled = last) {
                mon = tlName; controlMode = DOWN
            },
            QueuePicks.ControlButton(
                "Neuen Platz auswählen",
                ButtonStyle.SECONDARY,
                Emoji.fromUnicode("1\uFE0F⃣")
            ) {
                mon = tlName; controlMode = MODAL
            },
            QueuePicks.ControlButton("Entfernen", ButtonStyle.DANGER, Emoji.fromUnicode("❌")) {
                mon = tlName; controlMode = REMOVE
            }), ActionRow.of(QueuePicks.ControlButton("Bestätigen", ButtonStyle.SUCCESS) {
            controlMode = CANCEL
        })
        )
    }

    context(InteractionData)
    fun init() {
        reply(embeds = buildStateEmbed(null), components = buildSelectMenu(), ephemeral = true)
    }

    context(InteractionData)
    fun handleSelect(tlName: String) {
        edit(embeds = buildStateEmbed(tlName), components = buildButtons(tlName))
    }

    context(InteractionData)
    fun handleButton(tlName: String, controlMode: ControlMode) {
        val index = currentState.indexOfFirst { it.tlName == tlName }
        val currentMon = when (controlMode) {
            UP -> {
                if (index == 0) return reply("Das Pokemon ist bereits an erster Stelle!", ephemeral = true)
                val mon = currentState.removeAt(index)
                currentState.add(index - 1, mon)
                mon.tlName
            }

            DOWN -> {
                if (index == currentState.lastIndex) return reply(
                    "Das Pokemon ist bereits an letzter Stelle!", ephemeral = true
                )
                val mon = currentState.removeAt(index)
                currentState.add(index + 1, mon)
                mon.tlName
            }

            REMOVE -> {
                currentState.removeAt(index)
                null
            }

            CANCEL -> {
                null
            }

            MODAL -> return replyModal(QueuePicks.SetLocationModal {
                this.mon = tlName
            })
        }
        edit(embeds = buildStateEmbed(currentMon),
            components = currentMon?.let { buildButtons(it) } ?: buildSelectMenu())
    }

    context(InteractionData)
    fun setLocation(tlName: String, location: Int) {
        val index = currentState.indexOfFirst { it.tlName == tlName }
        val range = currentState.indices
        val mon = currentState.removeAt(index)
        currentState.add(location.minus(1).coerceIn(range), mon)
        edit(
            embeds = buildStateEmbed(tlName), components = buildButtons(tlName)
        )
    }

    context(InteractionData)
    suspend fun finish(enable: Boolean) {
        ephemeralDefault()
        deferEdit()
        League.executeOnFreshLock(leaguename) {
            if (checkIfTeamCantBeFinished(
                    uid,
                    currentState
                )
            ) return reply("Mit dieser Queue hast du nicht genug Punkte für ein vollständiges Team!")
            val data = queuedPicks.getOrPut(user) { QueuePicksData() }
            data.queued = currentState.toMutableList()
            data.enabled = enable
            currentlyEnabled = enable
            edit(embeds = buildStateEmbed(null), components = emptyList())
            save()
            delete()
            reply("Deine neue Queue-Pick-Reihenfolge wurde gespeichert!\nDas System ist für dich zurzeit **${if (enable) "" else "de"}aktiviert**.")
        }
    }

    context(InteractionData)
    fun reload() {
        currentState += addedMeanwhile
        addedMeanwhile.clear()
        edit(embeds = buildStateEmbed(null), components = buildSelectMenu())
    }


    companion object {
        val controlButtons =
            listOf(
                ActionRow.of(QueuePicks.ReloadButton()),
                ActionRow.of(
                    QueuePicks.FinishButton(
                        "Speichern und deaktivieren",
                        buttonStyle = ButtonStyle.SECONDARY
                    ) {
                        enable = false
                    }, QueuePicks.FinishButton("Speichern und aktivieren", buttonStyle = ButtonStyle.SUCCESS) {
                        enable = true
                    })
            )
    }
}
