package de.tectoast.emolga.utils

import com.mongodb.client.model.Filters
import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.database.exposed.SpoilerTagsDB
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.draft.EnterResult
import de.tectoast.emolga.features.draft.Nominate
import de.tectoast.emolga.features.draft.during.QueuePicks
import de.tectoast.emolga.features.draft.during.QueuePicks.ControlButton.ControlMode.*
import de.tectoast.emolga.features.draft.during.QueuePicks.isIllegal
import de.tectoast.emolga.features.intoMultipleRows
import de.tectoast.emolga.ktor.KD
import de.tectoast.emolga.ktor.generateFinalMessage
import de.tectoast.emolga.league.GamedayData
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.NDS
import de.tectoast.emolga.league.config.QueuePicksUserData
import de.tectoast.emolga.utils.draft.*
import de.tectoast.emolga.utils.json.db
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import kotlinx.serialization.*
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import org.litote.kmongo.and
import org.litote.kmongo.eq
import java.awt.Color

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
        context(iData: InteractionData) suspend inline fun <reified T : StateStore> process(block: T.() -> Unit) {
            processIgnoreMissing<T>(block) ?: iData.reply(
                "Diese Interaktion ist nicht mehr gültig! Starte sie wenn nötig erneut!", ephemeral = true
            )
        }

        context(iData: InteractionData) suspend inline fun <reified T : StateStore> processIgnoreMissing(block: T.() -> Unit) =
            (db.statestore.findOne(
                StateStore::uid eq iData.user, Filters.eq<String?>("type", T::class.simpleName)
            ) as T?)?.process(block)

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
    var channelToSend = -1L

    val data: List<MutableList<MonData>> = listOf(mutableListOf(), mutableListOf())
    private val uidxs = mutableListOf<Int>()

    @Transient
    val league = OneTimeCache { db.league(leaguename) }

    constructor(uid: Long, league: League, tcid: Long) : super(uid) {
        this.leaguename = league.leaguename
        this.channelToSend = tcid
    }


    private suspend fun getPicksByUid(idx: Int) = league().providePicksForGameday(gamedayData.gameday)[idx]!!
    private suspend fun getMonsByUid(idx: Int) = getPicksByUid(idx).sortedWith(league().tierorderingComparator).map {
        (it.name to NameConventionsDB.convertOfficialToTL(
            it.name, league().guild
        )!!).let { (official, tl) -> SelectOption(tl, "$official#$tl") }
    }

    @Transient
    val picks: Cache<Map<Int, List<SelectOption>>> = OneTimeCache { uidxs.associateWith { getMonsByUid(it) } }
    private lateinit var gamedayData: GamedayData


    private val wifiPlayers = (0..1).map { DraftPlayer(0, false) }

    @Transient
    private val defaultComponents: Cache<List<ActionRow>> = OneTimeCache {
        uidxs.mapIndexed { index, idx ->
            ActionRow.of(
                EnterResult.ResultMenu(
                    "${if (index == 0) "Deine" else "Gegnerische"} Pokemon",
                    options = picks()[idx]!!,
                ) { this.userindex = index })
        } + listOf(ActionRow.of(EnterResult.ResultFinish("Ergebnis bestätigen", ButtonStyle.PRIMARY) {
            mode = EnterResult.ResultFinish.Mode.CHECK
        }))
    }

    context(iData: InteractionData) suspend fun init(opponent: Long, user: Long) {
        val l = league()
        uidxs += l(user)
        uidxs += l(opponent)
        gamedayData = l.getGamedayData(uidxs[0], uidxs[1], wifiPlayers)
        if (gamedayData.gameday == -1) {
            iData.reply(
                "Im Spielplan ist kein Kampf zwischen dir und <@$opponent> geplant!", ephemeral = true
            )
            delete()
            return
        }
        iData.reply(embeds = buildEmbed(), components = defaultComponents(), ephemeral = true)
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

    context(iData: InteractionData) fun handleSelect(e: EnterResult.ResultMenu.Args) {
        val selected = e.selected
        val userindex = e.userindex
        iData.replyModal(
            EnterResult.ResultModal(
                "Ergebnis für ${selected.substringAfterLast("#")}",
                mapOf(EnterResult.Remove to data[userindex].any { it.official == selected.substringBefore("#") })
            ) {
                this.userindex = userindex
                this.selected = selected
            })
    }

    context(iData: InteractionData) fun handleModal(e: EnterResult.ResultModal.Args) {
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
        iData.edit(embeds = buildEmbed())
    }

    context(iData: InteractionData) suspend fun handleFinish(e: EnterResult.ResultFinish.Args) {
        if (invalidConditionsForFinish()) return
        when (e.mode) {
            EnterResult.ResultFinish.Mode.CHECK -> {
                val originalComponents = defaultComponents()
                val buttons = ActionRow.of(EnterResult.ResultFinish("Wirklich bestätigen", ButtonStyle.SUCCESS) {
                    mode = EnterResult.ResultFinish.Mode.YES
                }, EnterResult.ResultFinish("Abbrechen", ButtonStyle.DANGER) {
                    mode = EnterResult.ResultFinish.Mode.NO
                })
                val newComponents = originalComponents + listOf(buttons)
                iData.edit(components = newComponents)
            }

            EnterResult.ResultFinish.Mode.YES -> {
                League.executeOnFreshLock(leaguename) {
                    delete()
                    val channel = iData.jda.getTextChannelById(channelToSend)!!
                    if (config.replayDataStore != null) {
                        iData.reply(
                            "Das Ergebnis des Kampfes wurde gespeichert! Du kannst nun die Eingabe-Nachricht verwerfen.",
                            ephemeral = true
                        )
                        channel.sendResultEntryMessage(
                            gamedayData.gameday,
                            ResultEntryDescription.FromUids(uidxs.let { if (gamedayData.u1IsSecond) it.reversed() else it }
                                .map { league()[it] })
                        )
                    } else {
                        iData.reply(":)", ephemeral = true)
                        channel.sendResultEntryMessage(
                            gamedayData.gameday, ResultEntryDescription.Direct(generateFinalMessage())
                        )
                    }
                    val game = data.mapIndexed { index, d ->
                        wifiPlayers[index].apply {
                            alivePokemon = d.size - d.dead
                            winner = d.size != d.dead
                        }
                    }
                    docEntry?.analyse(
                        listOf(
                            ReplayData(
                                game = game,
                                uindices = uidxs,
                                kd = data.map { it.associate { p -> p.official to (p.kills to if (p.dead) 1 else 0) } },
                                mons = data.map { l -> l.map { it.official } },
                                url = "WIFI",
                                gamedayData = gamedayData.apply {
                                    numbers = game.map { it.alivePokemon }
                                        .let { l -> if (gamedayData.u1IsSecond) l.reversed() else l }
                                })
                        )
                    )
                }
            }

            EnterResult.ResultFinish.Mode.NO -> iData.edit(components = defaultComponents())
        }
    }

    context(iData: InteractionData) private fun invalidConditionsForFinish(): Boolean {
        if (data[0].isEmpty() || data[1].isEmpty()) return iData.reply(
            "Du hast noch keine Daten eingeben!", ephemeral = true
        ).let { true }
        if ((0..1).any { data[it].kills != data[1 - it].dead }) return iData.reply(
            "Die Kills und Tode müssen übereinstimmen!", ephemeral = true
        ).let { true }
        if (data[0].size != data[1].size) return iData.reply(
            "Die Anzahl der Pokemon muss übereinstimmen!", ephemeral = true
        ).let { true }
        return false
    }

    private suspend fun generateFinalMessage(): String {
        val l = league()
        val spoiler = SpoilerTagsDB.contains(l.guild)
        return "${
            data.mapIndexed { index, sdPlayer ->
                mutableListOf<Any>("<@${l[uidxs[index]]}>", sdPlayer.count { !it.dead }).apply {
                    if (spoiler) add(
                        1, "||"
                    )
                }.let { if (index % 2 > 0) it.asReversed() else it }
            }.joinToString(":") { it.joinToString(" ") }
        }\n\n${
            data.mapIndexed { index, monData ->
                "<@${l[uidxs[index]]}>:\n${monData.joinToString("\n").surroundWith(if (spoiler) "||" else "")}"
            }.joinToString("\n\n")
        }"
    }

    private val List<MonData>.kills get() = sumOf { it.kills }
    private val List<MonData>.dead get() = sumOf { (if (it.dead) 1 else 0) }

    @Serializable
    data class MonData(val pokemon: String, val official: String, val kills: Int, val dead: Boolean) {
        override fun toString(): String {
            return "$pokemon $kills".condAppend(dead) { " X" }
        }
    }

}

context(league: League)
suspend fun MessageChannel.sendResultEntryMessage(gameday: Int, input: ResultEntryDescription) {
    val embeds = if (input is ResultEntryDescription.Bo3) {
        val spoiler = SpoilerTagsDB.contains(league.guild)
        val descriptions = input.games.map { game -> generateFinalMessage(league, input.idxs, game) }
        listOf(
            Embed(
                title = "Spieltag $gameday",
                description = "<@${league[input.idxs[0]]}> ${
                    input.numbers.joinToString(":").surroundWithIf("||", spoiler)
                } <@${league[input.idxs[1]]}>",
                color = Color.YELLOW.rgb
            )
        ) + descriptions.mapIndexed { index, desc ->
            Embed(
                title = "Spieltag $gameday - Kampf ${index + 1}", description = desc, color = embedColor
            )
        }
    } else Embed(
        title = "Spieltag $gameday", description = input.provideDescription(), color = embedColor
    ).into()
    send(
        embeds = embeds
    ).queue()
}

sealed interface ResultEntryDescription {
    fun provideDescription(): String
    data class Direct(val description: String) : ResultEntryDescription {
        override fun provideDescription() = description
    }

    data class FromUids(val uids: List<Long>) : ResultEntryDescription {
        override fun provideDescription() = uids.joinToString(" vs. ") { "<@${it}>" } + " ✅"
    }

    // TODO: clean up this
    data class Bo3(val games: List<List<Map<String, KD>>>, val idxs: List<Int>, val numbers: List<Int>) :
        ResultEntryDescription {
        override fun provideDescription() = error("Implemented directly")
    }
}

@Serializable
@SerialName("NominateState")
class NominateState : StateStore {

    private val originalMons: List<DraftPokemon>
    private val mons: List<DraftPokemon>
    private val nominated: MutableList<DraftPokemon>
    private val notNominated: MutableList<DraftPokemon>
    private val nomUser: Int

    constructor(uid: Long, nomUser: Int, originalMons: List<DraftPokemon>, mons: List<DraftPokemon>) : super(uid) {
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

    private fun List<DraftPokemon>.toJSON() = this.sortedWith(comparator).map {
        it.indexedBy(originalMons)
    }

    fun generateDescription(): String {
        return buildString {
            append("**Nominiert: (${nominated.size})**\n")
            append(nominated.toMessage())
            append("\n**Nicht nominiert: (").append(notNominated.size).append(")**\n")
            append(notNominated.toMessage())
        }
    }

    context(iData: InteractionData) fun render() {
        iData.edit(
            embeds = Embed(
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
                    ActionRow.of(
                        Nominate.NominateButton(
                            buttonStyle = ButtonStyle.SUCCESS,
                            emoji = Emoji.fromUnicode("✅"),
                            disabled = nominated.size != 11
                        ) { mode = Nominate.NominateButton.Mode.FINISH; data = "NOTNOW" })
                )
            })
    }

    private fun buildJSONList(): List<Int> {
        return nominated.toJSON() + notNominated.toJSON()
    }

    context(iData: InteractionData) suspend fun finish(now: Boolean) {
        if (now) {
            League.executeOnFreshLock({ db.nds() }) {
                val nom = (this as NDS).nominations
                val day = nom.current()
                if (nomUser in day) return iData.reply("Du hast dein Team bereits für Spieltag ${nom.currentDay} nominiert!")
                day[nomUser] = buildJSONList()
                save()
                delete()
                return iData.reply("Deine Nominierung wurde gespeichert!")
            }
        }
        if (nominated.size != 11) {
            iData.reply(content = "Du musst exakt 11 Pokemon nominieren!", ephemeral = true)
        } else {
            iData.edit(
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
    private val currentState: MutableList<QueuedAction>

    @EncodeDefault
    private val addedMeanwhile: MutableList<QueuedAction> = mutableListOf()

    constructor(uid: Long, leaguename: String, currentData: QueuePicksUserData) : super(uid) {
        this.leaguename = leaguename
        this.currentState = currentData.queued.toMutableList()
        this.currentlyEnabled = currentData.enabled
    }

    fun addNewMon(mon: QueuedAction) {
        addedMeanwhile.add(mon)
    }

    private fun buildStateEmbed(currentMon: String?) = Embed {
        color = embedColor
        title = "Deine gequeueten Picks"
        description =
            "Hier kannst du die Pokemon verschieben oder entfernen.\n" + "Allgemein ist die hier aufgezeigte Liste eine Kopie deiner echten Liste, die erst nach dem Klick auf `Speichern` oder `Speichern und aktivieren` aktualisiert wird.\n" + "Da über den `/queuepicks add` Befehl das Pokemon direkt in die echte Liste hinzugefügt wird, muss man danach hier `Neue Pokemon laden` klicken, um die Liste zu aktualisieren.\n" + "Alternativ kann man auch ein neues Verwaltungsfenster über `/queuepicks manage` öffnen."
        field(
            "Aktuelle Reihenfolge",
            currentState.mapIndexed { index, draftName -> "${index + 1}. ".notNullAppend(draftName.y) { it.tlName + " -> " } + draftName.g.tlName }
                .joinToString("\n"),
            false)
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
        ) else ActionRow.of(QueuePicks.Menu(options = currentState.map { SelectOption(it.toString(), it.g.tlName) }))
    ) + controlButtons

    private fun buildButtons(tlName: String): List<ActionRow> {
        val first = currentState.firstOrNull()?.g?.tlName == tlName
        val last = currentState.lastOrNull()?.g?.tlName == tlName
        return listOf(
            ActionRow.of(
                QueuePicks.ControlButton(
                    "Hoch", ButtonStyle.PRIMARY, Emoji.fromUnicode("⬆"), disabled = first
                ) {
                    mon = tlName; controlMode = UP
                }, QueuePicks.ControlButton("Runter", ButtonStyle.PRIMARY, Emoji.fromUnicode("⬇"), disabled = last) {
                    mon = tlName; controlMode = DOWN
                }, QueuePicks.ControlButton(
                    "Neuen Platz auswählen", ButtonStyle.SECONDARY, Emoji.fromUnicode("1\uFE0F⃣")
                ) {
                    mon = tlName; controlMode = MODAL
                }, QueuePicks.ControlButton("Entfernen", ButtonStyle.DANGER, Emoji.fromUnicode("❌")) {
                    mon = tlName; controlMode = REMOVE
                }), ActionRow.of(QueuePicks.ControlButton("Bestätigen", ButtonStyle.SUCCESS) {
                controlMode = CANCEL
            })
        )
    }

    context(iData: InteractionData) fun init() {
        iData.reply(embeds = buildStateEmbed(null), components = buildSelectMenu(), ephemeral = true)
    }

    context(iData: InteractionData) fun handleSelect(tlName: String) {
        iData.edit(embeds = buildStateEmbed(tlName), components = buildButtons(tlName))
    }

    context(iData: InteractionData) fun handleButton(e: QueuePicks.ControlButton.Args) {
        val index by lazy { currentState.indexOfFirst { it.g.tlName == e.mon } }
        val currentMon = when (e.controlMode) {
            UP -> {
                if (index == 0) return iData.reply("Das Pokemon ist bereits an erster Stelle!", ephemeral = true)
                val mon = currentState.removeAt(index)
                currentState.add(index - 1, mon)
                mon.g.tlName
            }

            DOWN -> {
                if (index == currentState.lastIndex) return iData.reply(
                    "Das Pokemon ist bereits an letzter Stelle!", ephemeral = true
                )
                val mon = currentState.removeAt(index)
                currentState.add(index + 1, mon)
                mon.g.tlName
            }

            REMOVE -> {
                currentState.removeAt(index)
                null
            }

            CANCEL -> {
                null
            }

            MODAL -> return iData.replyModal(QueuePicks.SetLocationModal {
                this.mon = e.mon
            })
        }
        iData.edit(
            embeds = buildStateEmbed(currentMon),
            components = currentMon?.let { buildButtons(it) } ?: buildSelectMenu())
    }

    context(iData: InteractionData) fun setLocation(tlName: String, location: Int) {
        val index = currentState.indexOfFirst { it.g.tlName == tlName }
        val range = currentState.indices
        val mon = currentState.removeAt(index)
        currentState.add(location.minus(1).coerceIn(range), mon)
        iData.edit(
            embeds = buildStateEmbed(tlName), components = buildButtons(tlName)
        )
    }

    context(iData: InteractionData) suspend fun finish(enable: Boolean) {
        iData.ephemeralDefault()
        iData.deferEdit()
        League.executeOnFreshLock(leaguename) {
            if (isIllegal(this(uid), currentState)) return
            val data = persistentData.queuePicks.queuedPicks.getOrPut(this(iData.user)) { QueuePicksUserData() }
            data.queued = currentState.toMutableList()
            data.enabled = enable
            currentlyEnabled = enable
            iData.edit(embeds = buildStateEmbed(null), components = emptyList())
            save("QueuePicksManage")
            delete()
            iData.reply("Deine neue Queue-Pick-Reihenfolge wurde gespeichert!\nDas System ist für dich zurzeit **${if (enable) "" else "de"}aktiviert**.")
        }
    }

    context(iData: InteractionData) fun reload() {
        currentState += addedMeanwhile
        addedMeanwhile.clear()
        iData.edit(embeds = buildStateEmbed(null), components = buildSelectMenu())
    }


    companion object {
        val controlButtons = listOf(
            ActionRow.of(QueuePicks.ReloadButton()), ActionRow.of(
                QueuePicks.FinishButton(
                    "Speichern und deaktivieren", buttonStyle = ButtonStyle.SECONDARY
                ) {
                    enable = false
                }, QueuePicks.FinishButton("Speichern und aktivieren", buttonStyle = ButtonStyle.SUCCESS) {
                    enable = true
                })
        )
    }
}

@Serializable
data class QueuedAction(val g: DraftName, val y: DraftName? = null) {
    fun buildDraftInput(): DraftInput {
        return if (y != null) SwitchInput(y, g) else PickInput(g, null, false)
    }

    override fun toString(): String {
        return g.tlName.notNullPrepend(y) { "${it.tlName} -> " }
    }
}
