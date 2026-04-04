package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.intoMultipleRows
import de.tectoast.emolga.features.league.K18n_Nominate
import de.tectoast.emolga.features.league.Nominate
import de.tectoast.emolga.features.league.draft.K18n_QueuePicks
import de.tectoast.emolga.features.league.draft.QueuePicks
import de.tectoast.emolga.features.league.draft.QueuePicks.ControlButton.ControlMode.*
import de.tectoast.emolga.features.league.draft.QueuePicksComponents
import de.tectoast.emolga.features.league.draft.isIllegal
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.NDS
import de.tectoast.emolga.league.config.QueuePicksUserData
import de.tectoast.emolga.utils.*
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.emoji.Emoji
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.upsert
import org.koin.core.annotation.Single
import kotlin.reflect.KClass

object StateStoreTable : Table("state_store") {
    val uid = long("user")
    val type = varchar("type", 100)
    val data = jsonb<StateStore>("data")

    override val primaryKey = PrimaryKey(uid, type)
}

class StateStoreRepository(val db: R2dbcDatabase) {
    suspend fun save(uid: Long, state: StateStore) {
        StateStoreTable.upsert {
            it[StateStoreTable.uid] = uid
            it[StateStoreTable.type] = state::class.simpleName!!
            it[StateStoreTable.data] = state
        }
    }

    suspend fun delete(uid: Long, state: StateStore) {
        StateStoreTable.deleteWhere {
            (StateStoreTable.uid eq uid) and (StateStoreTable.type eq state::class.simpleName!!)
        }
    }

    suspend fun get(uid: Long, type: String): StateStore? {
        return StateStoreTable.selectAll().where { (StateStoreTable.uid eq uid) and (StateStoreTable.type eq type) }
            .map { it[StateStoreTable.data] }.firstOrNull()
    }
}

@Serializable
sealed class StateStore {

    @Transient
    var forDeletion = false

    fun delete() {
        forDeletion = true
    }
}

class StateStoreDispatcher(handlers: List<StateStoreHandler<out StateStore>>, val repository: StateStoreRepository) {
    val handlerMap = handlers.associateBy { it.targetClass }

    suspend inline fun <T : StateStore, H : StateStoreHandler<T>> process(
        state: T,
        user: Long,
        block: context(T) H.() -> Unit
    ) {
        val handler = handlerMap[state::class] ?: error("No handler for state store of type ${state::class}")
        with(state) {
            @Suppress("UNCHECKED_CAST") (handler as H).block()
            afterOperation(user)
        }
    }

    context(iData: InteractionData)
    suspend inline fun <reified T : StateStore, H : StateStoreHandler<T>> processIgnoreMissing(
        user: Long,
        block: context(T) H.() -> Unit
    ) =
        (repository.get(iData.user, T::class.simpleName!!) as? T)?.let { process(it, user, block) }

    context(iData: InteractionData)
    suspend inline fun <reified T : StateStore, H : StateStoreHandler<T>> process(
        user: Long,
        block: context(T) H.() -> Unit
    ) {
        processIgnoreMissing(user, block) ?: iData.reply(
            K18n_StateStore.InteractionNotValid, ephemeral = true
        )
    }

    suspend fun StateStore.afterOperation(user: Long) {
        if (forDeletion) {
            repository.delete(user, this)
        } else {
            repository.save(user, this)
        }
    }
}

interface StateStoreHandler<T : StateStore> {
    val targetClass: KClass<T>
}

@Single
class NominateStateHandler : StateStoreHandler<NominateState> {
    override val targetClass = NominateState::class

    context(state: NominateState)
    fun unnominate(name: String) = state.mons.first { it.name == name }.let {
        state.nominated.remove(it)
        state.notNominated.add(it)
    }

    context(state: NominateState)
    fun nominate(name: String) = state.mons.first { it.name == name }.let {
        state.notNominated.remove(it)
        state.nominated.add(it)
    }

    context(state: NominateState)
    private fun isNominated(s: String) = state.nominated.any { it.name == s }

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

    context(state: NominateState)
    private fun List<DraftPokemon>.toJSON() = this.sortedWith(comparator).map {
        it.indexedBy(state.originalMons)
    }

    context(state: NominateState)
    fun generateDescription(): String {
        return K18n_Nominate.Description(
            state.nominated.size, state.nominated.toMessage(), state.notNominated.size, state.notNominated.toMessage()
        ).translateTo(K18N_DEFAULT_LANGUAGE)
    }

    context(iData: InteractionData, state: NominateState, btn: Nominate.NominateButton)
    fun render() {
        iData.edit(
            contentK18n = null, embeds = Embed(
                title = K18n_Nominate.EmbedTitle.t(), color = embedColor, description = generateDescription()
            ).into(), components = state.mons.map {
                val s = it.name
                val isNom = isNominated(s)
                btn(
                    s.k18n, if (isNom) ButtonStyle.PRIMARY else ButtonStyle.SECONDARY
                ) {
                    data = s; this.mode =
                    if (isNom) Nominate.NominateButton.Mode.UNNOMINATE else Nominate.NominateButton.Mode.NOMINATE
                }
            }.intoMultipleRows().toMutableList().apply {
                add(
                    ActionRow.of(
                        btn(
                            buttonStyle = ButtonStyle.SUCCESS,
                            emoji = Emoji.fromUnicode("✅"),
                            disabled = state.nominated.size != MON_AMOUNT
                        ) { mode = Nominate.NominateButton.Mode.FINISH; data = "NOTNOW" })
                )
            })
    }

    context(state: NominateState)
    private fun buildJSONList(): List<Int> {
        return state.nominated.toJSON() + state.notNominated.toJSON()
    }

    context(iData: InteractionData, state: NominateState, btn: Nominate.NominateButton)
    suspend fun finish(now: Boolean) {
        if (now) {
            return League.executeOnFreshLock({ mdb.nds() }) l@{
                val nom = (this as NDS).nominations
                val day = nom.current()
                val currentDay = nom.currentDay
                if (state.nomUser in day) return@l iData.reply(K18n_Nominate.AlreadyNominated(currentDay))
                day[state.nomUser] = buildJSONList()
                save()
                state.delete()
                return@l iData.reply(K18n_Nominate.NominationSaved(currentDay))
            }
        }
        if (state.nominated.size != MON_AMOUNT) {
            iData.reply(content = K18n_Nominate.AmountIncorrect(MON_AMOUNT), ephemeral = true)
        } else {
            iData.edit(
                contentK18n = null, embeds = Embed(
                    title = K18n_Nominate.ConfirmationQuestion.t(),
                    color = embedColor,
                    description = generateDescription()
                ).into(), components = listOf(btn(K18n_Yes, ButtonStyle.SUCCESS) {
                    mode = Nominate.NominateButton.Mode.FINISH
                    data = "FINISHNOW"
                }, btn(K18n_No, ButtonStyle.DANGER) {
                    mode = Nominate.NominateButton.Mode.CANCEL
                }).into()
            )
        }
    }
}


@Serializable
@SerialName("NominateState")
class NominateState(
    val nomUser: Int, val originalMons: List<DraftPokemon>, val mons: List<DraftPokemon>
) : StateStore() {
    val nominated: MutableList<DraftPokemon> = ArrayList(mons)
    val notNominated: MutableList<DraftPokemon> = ArrayList(mons.size)
}

@Serializable
@SerialName("QueuePicks")
@OptIn(ExperimentalSerializationApi::class)
class QueuePicksState : StateStore {

    var leaguename = ""
    var currentlyEnabled = false
    val currentState: MutableList<QueuedAction>
    val addedMeanwhile: MutableList<QueuedAction> = mutableListOf()

    constructor(leaguename: String, currentData: QueuePicksUserData) {
        this.leaguename = leaguename
        this.currentState = currentData.queued.toMutableList()
        this.currentlyEnabled = currentData.enabled
    }
}

class QueuePicksStateHandler : StateStoreHandler<QueuePicksState> {
    override val targetClass = QueuePicksState::class

    context(state: QueuePicksState)
    fun addNewMon(mon: QueuedAction) {
        state.addedMeanwhile.add(mon)
    }

    context(iData: InteractionData, state: QueuePicksState)
    private fun buildStateEmbed(currentMon: String?) = Embed {
        color = embedColor
        title = K18n_QueuePicks.ManageEmbedTitle.t()
        description = K18n_QueuePicks.ManageEmbedDescription.t()
        field(
            K18n_QueuePicks.ManageCurrentOrder.t(),
            state.currentState.mapIndexed { index, draftName -> "${index + 1}. ".notNullAppend(draftName.y) { it.tlName + " -> " } + draftName.g.tlName }
                .joinToString("\n"),
            false)
        field(K18n_Status.t(), (if (state.currentlyEnabled) K18n_Active else K18n_Inactive).t(), false)
        currentMon?.let {
            field(K18n_QueuePicks.ManageCurrentPokemon.t(), it, false)
        }
    }.into()

    context(iData: InteractionData, state: QueuePicksState, components: QueuePicksComponents)
    private fun buildSelectMenu() = listOf(
        if (state.currentState.isEmpty()) {
            val noPicks = K18n_QueuePicks.ManageNoPicks.t()
            ActionRow.of(
                components.menu(
                    placeholder = noPicks, disabled = true, options = listOf(SelectOption(noPicks, noPicks))
                )
            )
        } else ActionRow.of(components.menu(options = state.currentState.map {
            SelectOption(
                it.toString(),
                it.g.tlName
            )
        }))
    ) + controlButtons()

    context(iData: InteractionData, state: QueuePicksState, components: QueuePicksComponents)
    private fun buildButtons(tlName: String): List<ActionRow> {
        val first = state.currentState.firstOrNull()?.g?.tlName == tlName
        val last = state.currentState.lastOrNull()?.g?.tlName == tlName
        return listOf(
            ActionRow.of(
                components.btn(
                    K18n_QueuePicks.ManageUp, ButtonStyle.PRIMARY, Emoji.fromUnicode("⬆"), disabled = first
                ) {
                    mon = tlName; controlMode = UP
                }, components.btn(
                    K18n_QueuePicks.ManageDown, ButtonStyle.PRIMARY, Emoji.fromUnicode("⬇"), disabled = last
                ) {
                    mon = tlName; controlMode = DOWN
                }, components.btn(
                    K18n_QueuePicks.ManageNewLocation, ButtonStyle.SECONDARY, Emoji.fromUnicode("1\uFE0F⃣")
                ) {
                    mon = tlName; controlMode = MODAL
                }, components.btn(K18n_QueuePicks.ManageRemove, ButtonStyle.DANGER, Emoji.fromUnicode("❌")) {
                    mon = tlName; controlMode = REMOVE
                }), ActionRow.of(components.btn(K18n_QueuePicks.ManageConfirm, ButtonStyle.SUCCESS) {
                controlMode = CANCEL
            })
        )
    }

    context(iData: InteractionData, state: QueuePicksState, components: QueuePicksComponents)
    fun init() {
        iData.reply(embeds = buildStateEmbed(null), components = buildSelectMenu(), ephemeral = true)
    }

    context(iData: InteractionData, state: QueuePicksState, components: QueuePicksComponents)
    fun handleSelect(tlName: String) {
        iData.edit(contentK18n = null, embeds = buildStateEmbed(tlName), components = buildButtons(tlName))
    }

    context(iData: InteractionData, state: QueuePicksState, components: QueuePicksComponents)
    suspend fun handleButton(e: QueuePicks.ControlButton.Args) {
        val index by lazy { state.currentState.indexOfFirst { it.g.tlName == e.mon } }
        val currentMon = when (e.controlMode) {
            UP -> {
                if (index == 0) return iData.reply(K18n_QueuePicks.ManagePokemonAlreadyAtTop, ephemeral = true)
                val mon = state.currentState.removeAt(index)
                state.currentState.add(index - 1, mon)
                mon.g.tlName
            }

            DOWN -> {
                if (index == state.currentState.lastIndex) return iData.reply(
                    K18n_QueuePicks.ManagePokemonAlreadyAtBottom, ephemeral = true
                )
                val mon = state.currentState.removeAt(index)
                state.currentState.add(index + 1, mon)
                mon.g.tlName
            }

            REMOVE -> {
                state.currentState.removeAt(index)
                null
            }

            CANCEL -> {
                null
            }

            MODAL -> return iData.replyModal(components.locationModal {
                this.mon = e.mon
            })
        }
        iData.edit(
            contentK18n = null,
            embeds = buildStateEmbed(currentMon),
            components = currentMon?.let { buildButtons(it) } ?: buildSelectMenu())
    }

    context(iData: InteractionData, state: QueuePicksState, components: QueuePicksComponents)
    fun setLocation(tlName: String, location: Int) {
        val index = state.currentState.indexOfFirst { it.g.tlName == tlName }
        val range = state.currentState.indices
        val mon = state.currentState.removeAt(index)
        state.currentState.add(location.minus(1).coerceIn(range), mon)
        iData.edit(
            contentK18n = null, embeds = buildStateEmbed(tlName), components = buildButtons(tlName)
        )
    }

    context(iData: InteractionData, state: QueuePicksState, components: QueuePicksComponents)
    suspend fun finish(enable: Boolean) {
        iData.ephemeralDefault()
        iData.deferEdit()
        League.executeOnFreshLock(state.leaguename) l@{
            isIllegal(this(iData.user), state.currentState)?.let { return@l iData.reply(it) }
            val data = persistentData.queuePicks.queuedPicks.getOrPut(this(iData.user)) { QueuePicksUserData() }
            data.queued = state.currentState.toMutableList()
            data.enabled = enable
            state.currentlyEnabled = enable
            iData.edit(contentK18n = null, embeds = buildStateEmbed(null), components = emptyList())
            save()
            state.delete()
            iData.reply(if (enable) K18n_QueuePicks.ManageFinishEnabled else K18n_QueuePicks.ManageFinishDisabled)
        }
    }


    context(iData: InteractionData, state: QueuePicksState, components: QueuePicksComponents)
    fun reload() {
        state.currentState += state.addedMeanwhile
        state.addedMeanwhile.clear()
        iData.edit(contentK18n = null, embeds = buildStateEmbed(null), components = buildSelectMenu())
    }


    companion object {
        context(iData: InteractionData, components: QueuePicksComponents)
        fun controlButtons() = listOf(
            ActionRow.of(components.reloadBtn()), ActionRow.of(
                components.finishBtn(
                    K18n_QueuePicks.ManageSaveAndDisable, buttonStyle = ButtonStyle.SECONDARY
                ) {
                    enable = false
                },
                components.finishBtn(K18n_QueuePicks.ManageSaveAndEnable, buttonStyle = ButtonStyle.SUCCESS) {
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
