package de.tectoast.emolga.utils

import com.mongodb.client.model.Filters
import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.database.exposed.SpoilerTagsDB
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.draft.K18n_Nominate
import de.tectoast.emolga.features.draft.Nominate
import de.tectoast.emolga.features.draft.during.K18n_QueuePicks
import de.tectoast.emolga.features.draft.during.QueuePicks
import de.tectoast.emolga.features.draft.during.QueuePicks.ControlButton.ControlMode.*
import de.tectoast.emolga.features.draft.during.QueuePicks.isIllegal
import de.tectoast.emolga.features.intoMultipleRows
import de.tectoast.emolga.ktor.generateFinalMessage
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.NDS
import de.tectoast.emolga.league.config.QueuePicksUserData
import de.tectoast.emolga.utils.draft.DraftInput
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.PickInput
import de.tectoast.emolga.utils.draft.SwitchInput
import de.tectoast.emolga.utils.json.mdb
import de.tectoast.generic.*
import de.tectoast.k18n.generated.K18N_DEFAULT_LANGUAGE
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import kotlinx.serialization.*
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
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
        if (mdb.statestore.findOne(filter) == null) mdb.statestore.insertOne(this)
        else mdb.statestore.updateOne(filter, this)
    }

    private suspend fun deleteFromDB() {
        mdb.statestore.deleteOne(createFilter())
    }

    fun delete() {
        forDeletion = true
    }

    private fun createFilter() = and(Filters.eq("type", this::class.simpleName!!), StateStore::uid eq uid)

    companion object {
        context(iData: InteractionData)
        suspend inline fun <reified T : StateStore> process(block: T.() -> Unit) {
            processIgnoreMissing<T>(block) ?: iData.reply(
                K18n_StateStore.InteractionNotValid, ephemeral = true
            )
        }

        context(iData: InteractionData)
        suspend inline fun <reified T : StateStore> processIgnoreMissing(block: T.() -> Unit) =
            (mdb.statestore.findOne(
                StateStore::uid eq iData.user, Filters.eq<String?>("type", T::class.simpleName)
            ) as T?)?.process(block)

    }
}

suspend inline fun <T : StateStore> T.process(block: T.() -> Unit) {
    block()
    afterOperation()
}

context(league: League)
suspend fun MessageChannel.sendResultEntryMessage(gameday: Int, input: ResultEntryDescription) {
    val gamedayString = K18n_Gameday.translateToGuildLanguage(league.guild)
    val embeds = if (input is ResultEntryDescription.Bo3) {
        // TODO: Clean this up
        val spoiler = SpoilerTagsDB.contains(league.guild)
        val fullGameData = input.fullGameData
        val descriptions =
            fullGameData.games.map { game -> generateFinalMessage(league, fullGameData.uindices, game.kd) }
        buildList {
            val actualBo3 = fullGameData.games.size > 1
            if (actualBo3) add(
                Embed(
                    title = "$gamedayString $gameday",
                    description = "<@${league[fullGameData.uindices[0]]}> ${
                        (0..1).map { i -> fullGameData.games.count { replayData -> replayData.winnerIndex == i } }
                            .joinToString(":").surroundWithIf("||", spoiler)
                    } <@${league[fullGameData.uindices[1]]}>",
                    color = Color.YELLOW.rgb
                )
            )
            addAll(descriptions.mapIndexed { index, desc ->
                Embed(
                    title = "$gamedayString $gameday".condAppend(
                        actualBo3,
                        " - ${K18n_Battle.translateToGuildLanguage(league.guild)} ${index + 1}"
                    ),
                    description = desc,
                    color = embedColor
                )
            })
        }
    } else Embed(
        title = "$gamedayString $gameday", description = input.provideDescription(), color = embedColor
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

    data class MatchPresent(val uids: List<Long>) : ResultEntryDescription {
        override fun provideDescription() = uids.joinToString(" vs. ") { "<@${it}>" } + " ✅"
    }

    // TODO: clean up this
    data class Bo3(val fullGameData: FullGameData) :
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
        const val MON_AMOUNT = 11
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
        return K18n_Nominate.Description(
            nominated.size,
            nominated.toMessage(),
            notNominated.size,
            notNominated.toMessage()
        ).translateTo(K18N_DEFAULT_LANGUAGE)
    }

    context(iData: InteractionData)
    fun render() {
        iData.edit(
            contentK18n = null,
            embeds = Embed(
                title = K18n_Nominate.EmbedTitle.t(), color = embedColor, description = generateDescription()
            ).into(), components = mons.map {
                val s = it.name
                val isNom = isNominated(s)
                Nominate.NominateButton(
                    s.k18n,
                    if (isNom) ButtonStyle.PRIMARY else ButtonStyle.SECONDARY
                ) {
                    data = s; this.mode =
                    if (isNom) Nominate.NominateButton.Mode.UNNOMINATE else Nominate.NominateButton.Mode.NOMINATE
                }
            }.intoMultipleRows().toMutableList().apply {
                add(
                    ActionRow.of(
                        Nominate.NominateButton(
                            buttonStyle = ButtonStyle.SUCCESS,
                            emoji = Emoji.fromUnicode("✅"),
                            disabled = nominated.size != MON_AMOUNT
                        ) { mode = Nominate.NominateButton.Mode.FINISH; data = "NOTNOW" })
                )
            })
    }

    private fun buildJSONList(): List<Int> {
        return nominated.toJSON() + notNominated.toJSON()
    }

    context(iData: InteractionData)
    suspend fun finish(now: Boolean) {
        if (now) {
            return League.executeOnFreshLock({ mdb.nds() }) l@{
                val nom = (this as NDS).nominations
                val day = nom.current()
                val currentDay = nom.currentDay
                if (nomUser in day) return@l iData.reply(K18n_Nominate.AlreadyNominated(currentDay))
                day[nomUser] = buildJSONList()
                save()
                delete()
                return@l iData.reply(K18n_Nominate.NominationSaved(currentDay))
            }
        }
        if (nominated.size != MON_AMOUNT) {
            iData.reply(content = K18n_Nominate.AmountIncorrect(MON_AMOUNT), ephemeral = true)
        } else {
            iData.edit(
                contentK18n = null,
                embeds = Embed(
                    title = K18n_Nominate.ConfirmationQuestion.t(),
                    color = embedColor,
                    description = generateDescription()
                ).into(), components = listOf(Nominate.NominateButton(K18n_Yes, ButtonStyle.SUCCESS) {
                    mode = Nominate.NominateButton.Mode.FINISH
                    data = "FINISHNOW"
                }, Nominate.NominateButton(K18n_No, ButtonStyle.DANGER) {
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

    context(iData: InteractionData)
    private fun buildStateEmbed(currentMon: String?) = Embed {
        color = embedColor
        title = K18n_QueuePicks.ManageEmbedTitle.t()
        description = K18n_QueuePicks.ManageEmbedDescription.t()
        field(
            K18n_QueuePicks.ManageCurrentOrder.t(),
            currentState.mapIndexed { index, draftName -> "${index + 1}. ".notNullAppend(draftName.y) { it.tlName + " -> " } + draftName.g.tlName }
                .joinToString("\n"),
            false)
        field(K18n_Status.t(), (if (currentlyEnabled) K18n_Active else K18n_Inactive).t(), false)
        currentMon?.let {
            field(K18n_QueuePicks.ManageCurrentPokemon.t(), it, false)
        }
    }.into()

    context(iData: InteractionData)
    private fun buildSelectMenu() = listOf(
        if (currentState.isEmpty()) {
            val noPicks = K18n_QueuePicks.ManageNoPicks.t()
            ActionRow.of(
                QueuePicks.Menu(
                    placeholder = noPicks,
                    disabled = true,
                    options = listOf(SelectOption(noPicks, noPicks))
                )
            )
        } else ActionRow.of(QueuePicks.Menu(options = currentState.map { SelectOption(it.toString(), it.g.tlName) }))
    ) + controlButtons()

    context(iData: InteractionData)
    private fun buildButtons(tlName: String): List<ActionRow> {
        val first = currentState.firstOrNull()?.g?.tlName == tlName
        val last = currentState.lastOrNull()?.g?.tlName == tlName
        return listOf(
            ActionRow.of(
                QueuePicks.ControlButton(
                    K18n_QueuePicks.ManageUp, ButtonStyle.PRIMARY, Emoji.fromUnicode("⬆"), disabled = first
                ) {
                    mon = tlName; controlMode = UP
                },
                QueuePicks.ControlButton(
                    K18n_QueuePicks.ManageDown,
                    ButtonStyle.PRIMARY,
                    Emoji.fromUnicode("⬇"),
                    disabled = last
                ) {
                    mon = tlName; controlMode = DOWN
                },
                QueuePicks.ControlButton(
                    K18n_QueuePicks.ManageNewLocation, ButtonStyle.SECONDARY, Emoji.fromUnicode("1\uFE0F⃣")
                ) {
                    mon = tlName; controlMode = MODAL
                },
                QueuePicks.ControlButton(K18n_QueuePicks.ManageRemove, ButtonStyle.DANGER, Emoji.fromUnicode("❌")) {
                    mon = tlName; controlMode = REMOVE
                }), ActionRow.of(QueuePicks.ControlButton(K18n_QueuePicks.ManageConfirm, ButtonStyle.SUCCESS) {
                controlMode = CANCEL
            })
        )
    }

    context(iData: InteractionData)
    fun init() {
        iData.reply(embeds = buildStateEmbed(null), components = buildSelectMenu(), ephemeral = true)
    }

    context(iData: InteractionData)
    fun handleSelect(tlName: String) {
        iData.edit(contentK18n = null, embeds = buildStateEmbed(tlName), components = buildButtons(tlName))
    }

    context(iData: InteractionData)
    suspend fun handleButton(e: QueuePicks.ControlButton.Args) {
        val index by lazy { currentState.indexOfFirst { it.g.tlName == e.mon } }
        val currentMon = when (e.controlMode) {
            UP -> {
                if (index == 0) return iData.reply(K18n_QueuePicks.ManagePokemonAlreadyAtTop, ephemeral = true)
                val mon = currentState.removeAt(index)
                currentState.add(index - 1, mon)
                mon.g.tlName
            }

            DOWN -> {
                if (index == currentState.lastIndex) return iData.reply(
                    K18n_QueuePicks.ManagePokemonAlreadyAtBottom, ephemeral = true
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
            contentK18n = null,
            embeds = buildStateEmbed(currentMon),
            components = currentMon?.let { buildButtons(it) } ?: buildSelectMenu())
    }

    context(iData: InteractionData)
    fun setLocation(tlName: String, location: Int) {
        val index = currentState.indexOfFirst { it.g.tlName == tlName }
        val range = currentState.indices
        val mon = currentState.removeAt(index)
        currentState.add(location.minus(1).coerceIn(range), mon)
        iData.edit(
            contentK18n = null,
            embeds = buildStateEmbed(tlName), components = buildButtons(tlName)
        )
    }

    context(iData: InteractionData)
    suspend fun finish(enable: Boolean) {
        iData.ephemeralDefault()
        iData.deferEdit()
        League.executeOnFreshLock(leaguename) l@{
            isIllegal(this(uid), currentState)?.let { return@l iData.reply(it) }
            val data = persistentData.queuePicks.queuedPicks.getOrPut(this(iData.user)) { QueuePicksUserData() }
            data.queued = currentState.toMutableList()
            data.enabled = enable
            currentlyEnabled = enable
            iData.edit(contentK18n = null, embeds = buildStateEmbed(null), components = emptyList())
            save()
            delete()
            iData.reply(if (enable) K18n_QueuePicks.ManageFinishEnabled else K18n_QueuePicks.ManageFinishDisabled)
        }
    }

    context(iData: InteractionData)
    fun reload() {
        currentState += addedMeanwhile
        addedMeanwhile.clear()
        iData.edit(contentK18n = null, embeds = buildStateEmbed(null), components = buildSelectMenu())
    }


    companion object {
        context(iData: InteractionData)
        fun controlButtons() = listOf(
            ActionRow.of(QueuePicks.ReloadButton()), ActionRow.of(
                QueuePicks.FinishButton(
                    K18n_QueuePicks.ManageSaveAndDisable, buttonStyle = ButtonStyle.SECONDARY
                ) {
                    enable = false
                }, QueuePicks.FinishButton(K18n_QueuePicks.ManageSaveAndEnable, buttonStyle = ButtonStyle.SUCCESS) {
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
