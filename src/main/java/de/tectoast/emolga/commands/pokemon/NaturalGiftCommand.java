package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.records.NGData;
import de.tectoast.emolga.utils.sql.DBManagers;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class NaturalGiftCommand extends Command {

    public NaturalGiftCommand() {
        super("naturalgift", "Entweder alle Beeren für einen Typ, oder den Typen von einer Beere", CommandCategory.Pokemon);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("stuff", "Sache", "Entweder ein Typ oder ein Beerenname", Translation.Type.of(Translation.Type.ITEM, Translation.Type.TYPE))
                .setExample("!naturalgift Water")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        Translation t = e.getArguments().getTranslation("stuff");
        String translation = t.getTranslation();
        if (t.isFromType(Translation.Type.ITEM)) {
            NGData ngData = DBManagers.NATURAL_GIFT.fromName(translation);
            e.reply(new EmbedBuilder()
                    .setTitle(translation)
                    .addField("Typ", ngData.type(), false)
                    .addField("Basepower", String.valueOf(ngData.bp()), false)
                    .setColor(Color.CYAN)
                    .build()
            );
        } else {
            List<NGData> ngData = DBManagers.NATURAL_GIFT.fromType(translation);
            e.reply(new EmbedBuilder()
                    .setTitle(translation)
                    .setDescription(
                            ngData.stream().sorted(Comparator.comparing(NGData::bp)).map(d -> d.name() + "/" + getEnglName(d.name()) + ": " + d.bp()).collect(Collectors.joining("\n"))
                    )
                    .setColor(Color.CYAN)
                    .build()
            );
        }
    }
}
