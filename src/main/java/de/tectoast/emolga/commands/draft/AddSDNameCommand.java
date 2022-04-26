package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.sql.DBManagers;

public class AddSDNameCommand extends Command {

    public AddSDNameCommand() {
        super("addsdname", "Registriert deinen Showdown-Namen bei Emolga", CommandCategory.Draft);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("name", "SD-Name", "Der SD-Name", ArgumentManagerTemplate.Text.any())
                .add("id", "Die ID (nur Flo)", "Nur für Flo", ArgumentManagerTemplate.DiscordType.ID, true)
                .setExample("!addsdname TecToast")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) throws Exception {
        ArgumentManager args = e.getArguments();
        if (args.has("id") && e.isNotFlo()) {
            e.reply("Nur Flo darf den Command mit einer ID verwenden!");
            return;
        }
        String name = args.getText("name");
        boolean b = DBManagers.SD_NAMES.addIfAbsent(name, args.getOrDefault("id", e.getAuthor().getIdLong()));
        if (b) {
            if (args.has("id")) {
                e.getJDA().retrieveUserById(args.getID("id")).queue(mem -> e.reply("Der Name `%s` wurde für %s registriert!".formatted(name, mem.getName())));
            } else {
                e.reply("Der Name `%s` wurde für dich registriert!".formatted(name));
            }
        } else {
            e.reply("Der Name ist bereits vergeben!");
        }
    }
}
