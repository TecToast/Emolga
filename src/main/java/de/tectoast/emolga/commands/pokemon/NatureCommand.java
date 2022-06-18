package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.sql.DBManagers;

import java.sql.SQLException;

public class NatureCommand extends Command {

    public NatureCommand() {
        super("nature", "Zeigt an, welche Werte dieses Wesen beeinflusst", CommandCategory.Pokemon);
        setArgumentTemplate(ArgumentManagerTemplate.builder().addEngl("nature", "Wesen", "Das Wesen", Translation.Type.NATURE)
                .setExample("!nature Adamant")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) throws SQLException {
        Translation t = e.getArguments().getTranslation("nature");
        e.reply(t.getOtherLang() + "/" + t.getTranslation() + ":\n" + DBManagers.NATURE.getNatureData(t.getTranslation()));
    }
}
