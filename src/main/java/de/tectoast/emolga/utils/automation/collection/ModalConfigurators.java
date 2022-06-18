package de.tectoast.emolga.utils.automation.collection;

import de.tectoast.emolga.commands.dexquiz.DexQuizTip;
import de.tectoast.emolga.utils.automation.structure.ModalConfigurator;
import de.tectoast.emolga.utils.records.ModalConfiguration;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import static de.tectoast.emolga.commands.Command.DEXQUIZ_BUDGET;

public class ModalConfigurators {
    public static final ModalConfigurator DEXQUIZ = ModalConfigurator.create()
            .id("dexquiz")
            .title("DexQuiz (Preis von -1 bedeutet deaktiviert)")
            .actionRows(
                    TextInput.create(
                            "totalbudget", "Größe an Tipp-Budget", TextInputStyle.SHORT
                    ).setPlaceholder(String.valueOf(DEXQUIZ_BUDGET)).setRequired(false).build())
            .actionRows(DexQuizTip.buildActionRows())
            .mapper(s -> {
                try {
                    int i = Integer.parseInt(s);
                    if (i < -1) return null;
                    return i;
                } catch (NumberFormatException e) {
                    return null;
                }
            }, Stream.concat(Stream.of("totalbudget"), Arrays.stream(DexQuizTip.values()).map(Enum::name))
                    .toArray(String[]::new));
    public static final Map<String, ModalConfiguration> configurations = Map.of(
            "dexquiz", new ModalConfiguration("DexQuiz", () -> ModalConfigurators.DEXQUIZ)
    );

    private ModalConfigurators() {
    }


}
