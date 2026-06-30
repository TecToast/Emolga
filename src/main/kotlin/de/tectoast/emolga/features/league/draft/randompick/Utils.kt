package de.tectoast.emolga.features.league.draft.randompick

import de.tectoast.emolga.domain.league.draft.model.random.RandomPickAction
import de.tectoast.emolga.domain.league.draft.model.random.RandomPickResult
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.league.draft.K18n_RandomPick
import de.tectoast.emolga.utils.CalcResult
import de.tectoast.generic.K18n_Accept
import dev.minn.jda.ktx.messages.into
import net.dv8tion.jda.api.components.buttons.ButtonStyle

context(iData: InteractionData)
internal fun handleRandomPickResult(result: CalcResult<RandomPickResult>, btn: RandomPickButton) {
    when (result) {
        is CalcResult.Success<RandomPickResult> if result.value is RandomPickResult.RerollPossible -> {
            val (_, tier, displayName, jokersRemaining) = result.value
            iData.reply(
                K18n_RandomPick.Gambled(displayName, tier),
                components = listOf(
                    btn(K18n_Accept, ButtonStyle.SUCCESS) {
                        action = RandomPickAction.ACCEPT
                    },
                    btn(
                        K18n_RandomPick.JokerLabel(jokersRemaining),
                        ButtonStyle.DANGER
                    ) {
                        action = RandomPickAction.REROLL
                    }).into()
            )
        }

        is CalcResult.Error<*> -> iData.reply(result.message, ephemeral = true)
        else -> {}
    }
}