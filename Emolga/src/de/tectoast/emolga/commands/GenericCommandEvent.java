package de.tectoast.emolga.commands;

import de.tectoast.emolga.utils.Constants;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class GenericCommandEvent {
    private final Message message;
    private final User author;
    private final String msg;
    private final MessageChannel channel;
    private final JDA jda;
    private final ArrayList<String> args;
    private final List<TextChannel> mentionedChannels;
    private final List<Member> mentionedMembers;
    private final List<Role> mentionedRoles;
    private final int argsLength;

    public GenericCommandEvent(Message message) {
        this.message = message;
        this.author = message.getAuthor();
        this.msg = message.getContentDisplay();
        this.channel = message.getChannel();
        this.jda = message.getJDA();
        this.mentionedChannels = this.message.getMentionedChannels();
        this.mentionedMembers = this.message.getMentionedMembers();
        this.mentionedRoles = this.message.getMentionedRoles();
        this.args = new ArrayList<>(Arrays.asList(msg.split("\\s+")));
        this.args.remove(0);
        this.argsLength = this.args.size();
    }

    public User getAuthor() {
        return author;
    }

    public Message getMessage() {
        return message;
    }


    public String getMsg() {
        return msg;
    }

    public String getRaw() {
        return message.getContentRaw();
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public ArrayList<String> getArgs() {
        return args;
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

    public int getArgsLength() {
        return argsLength;
    }

    public String getArg(int i) {
        if (hasArg(i)) return getArgs().get(i);
        return null;
    }

    public boolean hasArg(int i) {
        return i < argsLength;
    }

    public CompletableFuture<Message> reply(String msg) {
        return this.channel.sendMessage(msg).submit();
    }

    @SuppressWarnings("UnusedReturnValue")
    public CompletableFuture<Message> reply(MessageEmbed message) {
        return this.channel.sendMessage(message).submit();
    }

    public void done() {
        reply("Done!");
    }

    public boolean isNotFlo() {
        return this.author.getIdLong() != Constants.FLOID;
    }
}
