package de.tectoast.emolga.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class GuildCommandEvent extends GenericCommandEvent {
    private final Member member;
    private final Guild guild;
    private final TextChannel tco;
    private final GuildMessageReceivedEvent event;

    public GuildCommandEvent(GuildMessageReceivedEvent e) {
        super(e.getMessage());
        this.member = e.getMember();
        this.guild = e.getGuild();
        this.tco = e.getChannel();
        event = e;
    }

    public Member getMember() {
        return member;
    }

    public Guild getGuild() {
        return guild;
    }

    public TextChannel getChannel() {
        return tco;
    }

    public GuildMessageReceivedEvent getEvent() {
        return event;
    }
}
