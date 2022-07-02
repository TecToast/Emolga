package de.tectoast.emolga.utils.automation.collection

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.dexquiz.DexQuizTip
import de.tectoast.emolga.commands.dexquiz.DexQuizTip.Companion.buildActionRows
import de.tectoast.emolga.utils.automation.structure.ModalConfigurator
import de.tectoast.emolga.utils.records.ModalConfiguration
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import java.util.*
import java.util.stream.Stream

object ModalConfigurators {
    private val DEXQUIZ = ModalConfigurator.create()
        .id("dexquiz")
        .title("DexQuiz (Preis von -1 bedeutet deaktiviert)")
        .actionRows(
            TextInput.create(
                "totalbudget", "Größe an Tipp-Budget", TextInputStyle.SHORT
            ).setPlaceholder(Command.DEXQUIZ_BUDGET.toString()).setRequired(false).build()
        )
        .actionRows(*buildActionRows())
        .mapper({ s: String ->
            try {
                val i = s.toInt()
                if (i < -1) return@mapper null
                return@mapper i
            } catch (e: NumberFormatException) {
                return@mapper null
            }
        },
            *Stream.concat(
                Stream.of("totalbudget"),
                Arrays.stream(DexQuizTip.values()).map { obj: DexQuizTip -> obj.name })
                .toArray { arrayOfNulls(it) })
    val configurations: Map<String, ModalConfiguration> = mapOf(
        Pair("dexquiz", ModalConfiguration("DexQuiz") { DEXQUIZ })
    )
}