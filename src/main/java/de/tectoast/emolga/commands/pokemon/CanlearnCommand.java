package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

public class CanlearnCommand extends Command {
    public CanlearnCommand() {
        super("canlearn", "Zeigt, ob das Pokemon diese Attacke erlernen kann", CommandCategory.Pokemon);
        aliases.add("canlearn5");
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("form", "Form", "Optionale alternative Form", ArgumentManagerTemplate.Text.of(
                        SubCommand.of("Alola"), SubCommand.of("Galar")
                ), true)
                .add("mon", "Pokemon", "Das Pokemon", Translation.Type.POKEMON)
                .add("move", "Attacke", "Die Attacke", Translation.Type.MOVE)
                .setExample("!canlearn Alola Vulnona Ice Beam")
                .build());
    }


    @Override
    public void process(GuildCommandEvent e) {

        ArgumentManager args = e.getArguments();
        String pokemon = args.getTranslation("mon").getTranslation();
        String atk = args.getTranslation("move").getTranslation();
        String form = args.getOrDefault("form", "Normal");
        if (form.equals("Unova") && !getModByGuild(e).equals("nml")) form = "Normal";
        try {
            e.reply((form.equals("Normal") ? "" : form + "-") + pokemon + " kann " + atk + (canLearn(pokemon, form, atk, e.getMsg(), e.getGuild().getId().equals("747357029714231299") || e.getUsedName().equalsIgnoreCase("canlearn5") ? 5 : 8, getModByGuild(e)) ? "" : " nicht") + " erlernen!");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
