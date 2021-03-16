package de.tectoast.emolga.utils;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandEvent {
    private final GuildMessageReceivedEvent event;
    private final Message message;
    private final Member member;
    private final String msg;
    private final TextChannel channel;
    private final Guild guild;
    private final User author;
    private final JDA jda;
    private final ArrayList<String> args;
    private final List<TextChannel> mentionedChannels;
    private final List<Member> mentionedMembers;
    private final List<Role> mentionedRoles;

    public CommandEvent(GuildMessageReceivedEvent e) {
        this.event = e;
        this.message = e.getMessage();
        this.member = e.getMember();
        this.msg = this.message.getContentDisplay();
        this.channel = e.getChannel();
        this.guild = e.getGuild();
        this.author = e.getAuthor();
        this.jda = e.getJDA();
        this.mentionedChannels = this.message.getMentionedChannels();
        this.mentionedMembers = this.message.getMentionedMembers();
        this.mentionedRoles = this.message.getMentionedRoles();
        this.args = new ArrayList<>(Arrays.asList(msg.split("\\s+")));
        this.args.remove(0);
    }

    public User getAuthor() {
        return author;
    }

    public GuildMessageReceivedEvent getEvent() {
        return event;
    }

    public Message getMessage() {
        return message;
    }

    public Member getMember() {
        return member;
    }

    public String getMsg() {
        return msg;
    }

    public TextChannel getChannel() {
        return channel;
    }

    public ArrayList<String> getArgs() {
        return args;
    }

    public Guild getGuild() {
        return guild;
    }

    public JDA getJDA() {
        return jda;
    }

    public List<TextChannel> getMentionedChannels() {
        return mentionedChannels;
    }

    public List<Member> getMentionedMembers() {
        return mentionedMembers;
    }

    public List<Role> getMentionedRoles() {
        return mentionedRoles;
    }

    public String getArg(int i) {
        if(hasArg(i)) return getArgs().get(i);
        return null;
    }

    public boolean hasArg(int i) {
        return i < getArgs().size();
    }
}
