package de.tectoast.emolga.commands.various;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.util.stream.Collectors;

public class HelpCommand extends Command {

    public HelpCommand() {
        super("help", "Zeigt Hilfe über einen Command", CommandCategory.Various);
        this.otherPrefix = true;
        setArgumentTemplate(ArgumentManagerTemplate.builder().add("cmd", "Command", "Der Command, über den du Hilfe haben möchtest", ArgumentManagerTemplate.withPredicate("Command-Name", str -> byName(str) != null, false)).build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        String cmdname = e.getArguments().getText("cmd");
        Command c = Command.byName(cmdname);

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(c.getPrefix() + c.getName());
        builder.setColor(Color.CYAN);
        ArgumentManagerTemplate template = c.getArgumentTemplate();
        builder.setDescription(c.getHelpWithoutCmd(e.getGuild()));
        builder.addField("Syntax", "```" + (template.hasSyntax() ? template.getSyntax() : c.getPrefix() + c.getName() + " "
                                                                                          + template.getArguments().stream().map(a -> (a.isOptional() ? "[" : "<") + a.getName() + (a.isOptional() ? "]" : ">")).collect(Collectors.joining(" "))) + "```", false);
        for (ArgumentManagerTemplate.Argument a : template.getArguments()) {
            builder.addField(a.getName(), (a.isOptional() ? "(Optional)\n" : "") + a.getHelp(), true);
        }
        builder.setFooter("Aufgerufen von " + e.getAuthor().getAsTag());
        if (template.hasExample()) {
            builder.addField("Beispiel", "```" + template.getExample() + "```", false);
        }
        if (!c.getAliases().isEmpty()) {
            builder.addField("Aliases", "`" + String.join("`, `", c.getAliases()) + "`", false);
        }
        e.reply(builder.build());
    }
}
