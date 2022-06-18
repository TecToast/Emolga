package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

public class AllLearnCommand extends Command {
    public AllLearnCommand() {
        super("alllearn", "Zeigt, welche der angegeben Pokemon die angegebene Attacke lernen können.", CommandCategory.Pokemon);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("move", "Attacke", "Die Attacke, nach der geschaut werden soll", Translation.Type.MOVE)
                .add("mons", "Pokemon", "Alle Pokemon, mit Leerzeichen separiert", ArgumentManagerTemplate.Text.any())
                .setExample("!alllearn Tarnsteine Regirock Primarene Bisaflor Humanolith")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        ArgumentManager args = e.getArguments();
        String atk = args.getTranslation("move").getTranslation();
        StringBuilder str = new StringBuilder(2 << 5);
        String mons = args.getText("mons");
        if (mons.contains("\n")) {
            for (String s : mons.split("\n")) {
                str.append(Command.canLearn(s.startsWith("A-") || s.startsWith("G-") || s.startsWith("M-") ? s.substring(2) : s, s.startsWith("A-") ? "" : (s.startsWith("G-") ? "Galar" : "Normal"), atk, e.getMsg(), e.getGuild().getId().equals("747357029714231299") ? 5 : 8) ? s + "\n" : "");
            }
        } else {
            for (String s : mons.split(" ")) {
                str.append(Command.canLearn(s.startsWith("A-") || s.startsWith("G-") || s.startsWith("M-") ? s.substring(2) : s, s.startsWith("A-") ? "" : (s.startsWith("G-") ? "Galar" : "Normal"), atk, e.getMsg(), e.getGuild().getId().equals("747357029714231299") ? 5 : 8) ? s + "\n" : "");
            }
        }
        if (str.toString().isEmpty()) str.append("Kein Pokemon kann diese Attacke erlernen!");
        e.reply(str.toString());

    }
}
