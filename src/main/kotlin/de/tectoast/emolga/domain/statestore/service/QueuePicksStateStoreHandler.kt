package de.tectoast.emolga.domain.statestore.service

import de.tectoast.emolga.domain.league.queue.model.QueuedAction
import de.tectoast.emolga.domain.league.queue.service.QueuePicksService
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.pokemon.service.PokemonDisplayService
import de.tectoast.emolga.domain.statestore.model.QueuePicksComponents
import de.tectoast.emolga.domain.statestore.model.QueuePicksState
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.league.draft.K18n_QueuePicks
import de.tectoast.emolga.features.league.draft.queue.QueuePicksControlButton
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.t
import de.tectoast.generic.K18n_Active
import de.tectoast.generic.K18n_Inactive
import de.tectoast.generic.K18n_Status
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.emoji.Emoji
import org.koin.core.annotation.Single

@Single
class QueuePicksStateStoreHandler(
    private val displayService: PokemonDisplayService,
    private val queuePicksService: QueuePicksService
) : StateStoreHandler<QueuePicksState> {
    override val targetClass = QueuePicksState::class

    context(state: QueuePicksState)
    private suspend fun display(showdownId: ShowdownID) =
        displayService.getDisplayName(showdownId, state.guild, state.tlLanguage)

    context(state: QueuePicksState)
    private suspend fun QueuedAction.toContent() = buildString {
        if (y != null) {
            append(display(y.id))
            append(" -> ")
        }
        append(display(g.id))
    }

    context(state: QueuePicksState)
    fun addNewMon(mon: QueuedAction) {
        state.addedMeanwhile.add(mon)
    }

    context(iData: InteractionData, state: QueuePicksState)
    private suspend fun buildStateEmbed(currentMonId: ShowdownID?) = Embed {
        color = Constants.EMBED_COLOR
        title = K18n_QueuePicks.ManageEmbedTitle.t()
        description = K18n_QueuePicks.ManageEmbedDescription.t()
        field(
            K18n_QueuePicks.ManageCurrentOrder.t(),
            state.currentState.mapIndexed { index, action ->
                buildString {
                    append(index + 1)
                    append(". ")
                    if (action.y != null) {
                        append(display(action.y.id))
                        append(" -> ")
                    }
                    append(display(action.g.id))
                }
            }
                .joinToString("\n"),
            false)
        field(K18n_Status.t(), (if (state.currentlyEnabled) K18n_Active else K18n_Inactive).t(), false)
        currentMonId?.let {
            field(K18n_QueuePicks.ManageCurrentPokemon.t(), display(it), false)
        }
    }.into()

    context(iData: InteractionData, state: QueuePicksState, components: QueuePicksComponents)
    private suspend fun buildSelectMenu() = listOf(
        if (state.currentState.isEmpty()) {
            val noPicks = K18n_QueuePicks.ManageNoPicks.t()
            ActionRow.of(
                components.menu(
                    placeholder = noPicks, disabled = true, options = listOf(SelectOption(noPicks, noPicks))
                )
            )
        } else ActionRow.of(components.menu(options = state.currentState.map {
            SelectOption(
                it.toContent(),
                it.g.id.value
            )
        }))
    ) + controlButtons()

    context(iData: InteractionData, state: QueuePicksState, components: QueuePicksComponents)
    private fun buildButtons(id: ShowdownID): List<ActionRow> {
        val first = state.currentState.firstOrNull()?.g?.id == id
        val last = state.currentState.lastOrNull()?.g?.id == id
        return listOf(
            ActionRow.of(
                components.btn(
                    K18n_QueuePicks.ManageUp, ButtonStyle.PRIMARY, Emoji.fromUnicode("⬆"), disabled = first
                ) {
                    mon = id; controlMode = UP
                }, components.btn(
                    K18n_QueuePicks.ManageDown, ButtonStyle.PRIMARY, Emoji.fromUnicode("⬇"), disabled = last
                ) {
                    mon = id; controlMode = DOWN
                }, components.btn(
                    K18n_QueuePicks.ManageNewLocation, ButtonStyle.SECONDARY, Emoji.fromUnicode("1\uFE0F⃣")
                ) {
                    mon = id; controlMode = MODAL
                }, components.btn(K18n_QueuePicks.ManageRemove, ButtonStyle.DANGER, Emoji.fromUnicode("❌")) {
                    mon = id; controlMode = REMOVE
                }), ActionRow.of(components.btn(K18n_QueuePicks.ManageConfirm, ButtonStyle.SUCCESS) {
                controlMode = CANCEL
            })
        )
    }

    context(iData: InteractionData, state: QueuePicksState, components: QueuePicksComponents)
    suspend fun init() {
        iData.replyRaw(embeds = buildStateEmbed(null), components = buildSelectMenu(), ephemeral = true)
    }

    context(iData: InteractionData, state: QueuePicksState, components: QueuePicksComponents)
    suspend fun handleSelect(id: ShowdownID) {
        iData.edit(embeds = buildStateEmbed(id), components = buildButtons(id))
    }

    context(iData: InteractionData, state: QueuePicksState, components: QueuePicksComponents)
    suspend fun handleButton(e: QueuePicksControlButton.Args) {
        val index by lazy { state.currentState.indexOfFirst { it.g.id == e.mon } }
        val currentMonId = when (e.controlMode) {
            UP -> {
                if (index == 0) return iData.reply(K18n_QueuePicks.ManagePokemonAlreadyAtTop, ephemeral = true)
                val mon = state.currentState.removeAt(index)
                state.currentState.add(index - 1, mon)
                mon.g.id
            }

            DOWN -> {
                if (index == state.currentState.lastIndex) return iData.reply(
                    K18n_QueuePicks.ManagePokemonAlreadyAtBottom, ephemeral = true
                )
                val mon = state.currentState.removeAt(index)
                state.currentState.add(index + 1, mon)
                mon.g.id
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
            embeds = buildStateEmbed(currentMonId),
            components = currentMonId?.let { buildButtons(it) } ?: buildSelectMenu())
    }

    context(iData: InteractionData, state: QueuePicksState, components: QueuePicksComponents)
    suspend fun setLocation(id: ShowdownID, location: Int) {
        val index = state.currentState.indexOfFirst { it.g.id == id }
        val range = state.currentState.indices
        val mon = state.currentState.removeAt(index)
        state.currentState.add(location.minus(1).coerceIn(range), mon)
        iData.edit(
            embeds = buildStateEmbed(id), components = buildButtons(id)
        )
    }

    context(iData: InteractionData, state: QueuePicksState)
    suspend fun finish(enable: Boolean) {
        iData.ephemeralDefault()
        iData.deferEdit()
        queuePicksService.setNewState(
            state.leaguename, state.guild, state.idx, state.currentState, enable
        )?.let { return iData.reply(it) }
        state.currentlyEnabled = enable
        iData.edit(embeds = buildStateEmbed(null), components = emptyList())
        state.delete()
        iData.reply(if (enable) K18n_QueuePicks.ManageFinishEnabled else K18n_QueuePicks.ManageFinishDisabled)
    }


    context(iData: InteractionData, state: QueuePicksState, components: QueuePicksComponents)
    suspend fun reload() {
        state.currentState += state.addedMeanwhile
        state.addedMeanwhile.clear()
        iData.edit(embeds = buildStateEmbed(null), components = buildSelectMenu())
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