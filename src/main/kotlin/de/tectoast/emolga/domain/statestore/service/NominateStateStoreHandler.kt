package de.tectoast.emolga.domain.statestore.service

import de.tectoast.emolga.domain.guildspecific.nominate.service.NominateService
import de.tectoast.emolga.domain.league.draft.model.core.DraftPokemon
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.statestore.model.NominateState
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.league.K18n_Nominate
import de.tectoast.emolga.features.league.NominateButton
import de.tectoast.emolga.utils.*
import de.tectoast.generic.K18n_No
import de.tectoast.generic.K18n_Yes
import de.tectoast.k18n.generated.K18N_DEFAULT_LANGUAGE
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.emoji.Emoji
import org.koin.core.annotation.Single

@Single
class NominateStateStoreHandler(private val nominateService: NominateService) :
    StateStoreHandler<NominateState> {
    override val targetClass = NominateState::class

    context(state: NominateState)
    fun unnominate(name: ShowdownID) = state.mons.first { it.showdownId == name }.let {
        state.nominated.remove(it)
        state.notNominated.add(it)
    }

    context(state: NominateState)
    fun nominate(name: ShowdownID) = state.mons.first { it.showdownId == name }.let {
        state.notNominated.remove(it)
        state.nominated.add(it)
    }

    context(state: NominateState)
    private fun isNominated(s: ShowdownID) = state.nominated.any { it.showdownId == s }

    companion object {
        private val tiers = listOf("S", "A", "B")
        val comparator = compareBy<DraftPokemon>({ tiers.indexOf(it.tier) }, { it.showdownId })
        const val MON_AMOUNT = 11
    }

    private fun List<DraftPokemon>.toMessage(): String {
        return this.sortedWith(comparator).joinToString("\n") {
            "${it.tier}: ${it.showdownId}"
        }
    }

    context(state: NominateState)
    private fun List<DraftPokemon>.toJSON() = this.sortedWith(comparator).map {
        state.originalMons.indexOf(it)
    }

    context(state: NominateState)
    fun generateDescription(): String {
        return K18n_Nominate.Description(
            state.nominated.size, state.nominated.toMessage(), state.notNominated.size, state.notNominated.toMessage()
        ).translateTo(K18N_DEFAULT_LANGUAGE)
    }

    context(iData: InteractionData, state: NominateState, btn: NominateButton)
    fun render() {
        iData.edit(
            embeds = Embed(
                title = K18n_Nominate.EmbedTitle.t(), color = Constants.EMBED_COLOR, description = generateDescription()
            ).into(), components = state.mons.map {
                val s = it.showdownId
                val isNom = isNominated(s)
                btn(
                    s.value.k18n, if (isNom) ButtonStyle.PRIMARY else ButtonStyle.SECONDARY
                ) {
                    data = s; mode =
                    if (isNom) NominateButton.Mode.UNNOMINATE else NominateButton.Mode.NOMINATE
                }
            }.chunked(5).map { ActionRow.of(it) }.toMutableList().apply {
                add(
                    ActionRow.of(
                        btn(
                            buttonStyle = ButtonStyle.SUCCESS,
                            emoji = Emoji.fromUnicode("✅"),
                            disabled = state.nominated.size != MON_AMOUNT
                        ) { mode = NominateButton.Mode.FINISH; data = "notnow".toShowdownID() })
                )
            })
    }

    context(state: NominateState)
    private fun buildJSONList(): List<Int> {
        return state.nominated.toJSON() + state.notNominated.toJSON()
    }

    context(iData: InteractionData, state: NominateState, btn: NominateButton)
    suspend fun finish(now: Boolean) {
        if (now) {
            val result = nominateService.handleFinish(state.nomUser, buildJSONList())
            iData.reply(result.msg())
            if (result.isSuccess()) {
                state.delete()
            }
        }
        if (state.nominated.size != MON_AMOUNT) {
            iData.reply(content = K18n_Nominate.AmountIncorrect(MON_AMOUNT), ephemeral = true)
        } else {
            iData.edit(
                embeds = Embed(
                    title = K18n_Nominate.ConfirmationQuestion.t(),
                    color = Constants.EMBED_COLOR,
                    description = generateDescription()
                ).into(), components = listOf(btn(K18n_Yes, ButtonStyle.SUCCESS) {
                    mode = NominateButton.Mode.FINISH_NOW
                }, btn(K18n_No, ButtonStyle.DANGER) {
                    mode = NominateButton.Mode.CANCEL
                }).into()
            )
        }
    }
}