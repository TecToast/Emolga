package de.tectoast.emolga.buttons;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class GuildInviteButton extends ButtonListener {

    public GuildInviteButton() {
        super("guildinvite");
    }

    @Override
    public void process(ButtonInteractionEvent e, String name) throws Exception {
        e.reply(e.getJDA().getGuildById(name).getDefaultChannel().createInvite().setMaxUses(1).complete().getUrl()).queue();
    }
}
