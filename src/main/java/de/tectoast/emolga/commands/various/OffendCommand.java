package de.tectoast.emolga.commands.various;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

public class OffendCommand extends Command {

    public OffendCommand() {
        super("offend", "Offended Leute", CommandCategory.Various, 940283708953473075L);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("person", "Person", "Die Person", ArgumentManagerTemplate.Text.any())
                .setExample("!offend Emre")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) throws Exception {
        String person = e.getArguments().getText("person");
        switch (person.toLowerCase()) {
            case "emre" -> e.reply("Emre stinkt :^)");
            case "discus" -> e.reply("Tower heißt Turret auf Englisch c:");
            case "tobi" -> e.reply("Tobi kann Farben am Geruch erkennen \uD83D\uDC43");
            case "sven" -> e.reply("Scheinbar unoffendbar \uD83D\uDC40");
        }
    }
}