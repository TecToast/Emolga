package de.tectoast.emolga.commands.flo;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

public class ModalTestCommand extends Command {

    public ModalTestCommand() {
        super("modaltest", "Testet Modals", CommandCategory.Flo);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
        slash(true);
    }

    @Override
    public void process(GuildCommandEvent e) throws Exception {
        TextInput email = TextInput.create("email", "Email", TextInputStyle.SHORT)
                .setPlaceholder("Enter your E-mail")
                .setMinLength(10)
                .setMaxLength(100) // or setRequiredRange(10, 100)
                .build();

        TextInput body = TextInput.create("body", "Body", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Your concerns go here")
                .setMinLength(30)
                .setMaxLength(1000)
                .build();

        Modal modal = Modal.create("support", "Support")
                .addActionRows(ActionRow.of(email), ActionRow.of(body))
                .build();
        e.getSlashCommandEvent().replyModal(modal).queue();
    }
}