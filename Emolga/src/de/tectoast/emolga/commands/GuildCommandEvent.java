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
    private final String usedName;
    private Command.ArgumentManager manager;

    public GuildCommandEvent(Command c, GuildMessageReceivedEvent e) throws Exception {
        super(e.getMessage());
        this.member = e.getMember();
        this.guild = e.getGuild();
        this.tco = e.getChannel();
        event = e;
        Command.ArgumentManagerTemplate template = c.getArgumentTemplate();
        if (template != null)
            this.manager = template.construct(e);
        this.usedName = getMsg().split("\\s+")[0].substring(c.getPrefix().length());
        c.process(this);
    }

    public String getUsedName() {
        return usedName;
    }

    public Member getMember() {
        return member;
    }

    public Guild getGuild() {
        return guild;
    }

    public Command.ArgumentManager getArguments() {
        return manager;
    }

    public TextChannel getChannel() {
        return tco;
    }

    public GuildMessageReceivedEvent getEvent() {
        return event;
    }

    public void deleteMessage() {
        this.getMessage().delete().queue();
    }
}
