package de.tectoast.emolga.commands.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import net.dv8tion.jda.api.EmbedBuilder
import java.awt.Color

class HelpCommand : Command("help", "Zeigt Hilfe über einen Command", CommandCategory.Various) {
    init {
        otherPrefix = true
        argumentTemplate = ArgumentManagerTemplate.builder().add(
            "cmd",
            "Command",
            "Der Command, über den du Hilfe haben möchtest",
            ArgumentManagerTemplate.withPredicate("Command-Name", { str: String -> byName(str) != null }, false)
        ).build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val cmdname = e.arguments.getText("cmd")
        val c = byName(cmdname)!!
        val builder = EmbedBuilder()
        builder.setTitle(c.prefix + c.name)
        builder.setColor(Color.CYAN)
        val template = c.argumentTemplate
        builder.setDescription(c.getHelpWithoutCmd(e.guild))
        builder.addField(
            "Syntax",
            "```" + (if (template.hasSyntax()) template.syntax else c.prefix + c.name + " "
                    + template.arguments
                .joinToString(" ") { a: ArgumentManagerTemplate.Argument -> (if (a.isOptional) "[" else "<") + a.name + if (a.isOptional) "]" else ">" }) + "```",
            false
        )
        for (a in template.arguments) {
            builder.addField(a.name, (if (a.isOptional) "(Optional)\n" else "") + a.buildHelp(), true)
        }
        builder.setFooter("Aufgerufen von " + e.author.asTag)
        if (template.hasExample()) {
            builder.addField("Beispiel", "```" + template.example + "```", false)
        }
        if (c.aliases.isNotEmpty()) {
            builder.addField("Aliases", "`" + java.lang.String.join("`, `", c.aliases) + "`", false)
        }
        e.reply(builder.build())
    }
}