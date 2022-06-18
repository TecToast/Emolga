package de.tectoast.emolga.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import static de.tectoast.emolga.utils.Constants.FLOID;
import static de.tectoast.emolga.utils.Constants.MYTAG;

public class GuildCommandEvent extends GenericCommandEvent {
    private final Member member;
    private final Guild guild;
    private final TextChannel tco;
    private final String usedName;
    private Command.ArgumentManager manager;

    public GuildCommandEvent(Command c, MessageReceivedEvent e) throws Exception {
        super(e.getMessage());
        this.member = e.getMember();
        this.guild = e.getGuild();
        this.tco = e.getTextChannel();
        Command.ArgumentManagerTemplate template = c.getArgumentTemplate();
        if (template != null)
            this.manager = template.construct(e, c);
        this.usedName = Command.WHITESPACES_SPLITTER.split(getMsg())[0].substring(c.getPrefix().length());
        new Thread(() -> {
            try {
                (manager != null ? manager.executor() : c).process(this);
            } catch (Exception ex) {
                ex.printStackTrace();
                tco.sendMessage("Es ist ein Fehler beim Ausführen des Commands aufgetreten!\nWenn du denkst, dass dies ein interner Fehler beim Bot ist, melde dich bitte bei Flo (%s).\n".formatted(MYTAG) + c.getHelp(e.getGuild()) + (member.getIdLong() == FLOID ? "\nJa Flo, du sollst dich auch bei ihm melden du Kek! :^)" : "")).queue();
            }
        }, "CMD " + c.getName()).start();
    }

    public GuildCommandEvent(Command c, SlashCommandInteractionEvent e) throws Exception {
        super(e);
        this.member = e.getMember();
        this.guild = e.getGuild();
        this.tco = e.getTextChannel();
        this.usedName = e.getName();
        Command.ArgumentManagerTemplate template = c.getArgumentTemplate();
        if (template != null)
            this.manager = template.construct(e, c);
        new Thread(() -> {
            try {
                (manager != null ? manager.executor() : c).process(this);
            } catch (Exception ex) {
                ex.printStackTrace();
                tco.sendMessage("Es ist ein Fehler beim Ausführen des Commands aufgetreten!\nWenn du denkst, dass dies ein interner Fehler beim Bot ist, melde dich bitte bei Flo (%s).\n".formatted(MYTAG) + c.getHelp(e.getGuild()) + (member.getIdLong() == FLOID ? "\nJa Flo, du sollst dich auch bei ihm melden du Kek! :^)" : "")).queue();
            }
        }, "CMD " + c.getName()).start();
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

    public void deleteMessage() {
        this.getMessage().delete().queue();
    }
}
