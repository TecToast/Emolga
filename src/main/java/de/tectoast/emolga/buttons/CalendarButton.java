package de.tectoast.emolga.buttons;

import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

public class CalendarButton extends ButtonListener {
    public CalendarButton() {
        super("calendar");
    }

    @Override
    public void process(ButtonClickEvent e, String name) throws Exception {
        if (name.equals("delete")) {
            e.reply("+1").setEphemeral(true).queue();
            e.getHook().deleteMessageById(e.getMessageId()).queue();
        }
    }
}
