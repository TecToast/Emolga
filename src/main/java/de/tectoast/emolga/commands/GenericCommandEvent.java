package de.tectoast.emolga.commands;

import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.records.DeferredSlashResponse;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class GenericCommandEvent {
    private static final Logger logger = LoggerFactory.getLogger(GenericCommandEvent.class);
    private final Message message;
    private final User author;
    private final String msg;
    private final MessageChannel channel;
    private final JDA jda;
    private final List<String> args;
    private final List<TextChannel> mentionedChannels;
    private final List<Member> mentionedMembers;
    private final List<Role> mentionedRoles;
    private final int argsLength;
    private final SlashCommandInteractionEvent slashCommandEvent;

    public GenericCommandEvent(Message message) {
        this.message = message;
        this.author = message.getAuthor();
        this.msg = message.getContentDisplay();
        this.channel = message.getChannel();
        this.jda = message.getJDA();
        Mentions mentions = this.message.getMentions();
        this.mentionedChannels = mentions.getChannels(TextChannel.class);
        this.mentionedMembers = mentions.getMembers();
        this.mentionedRoles = mentions.getRoles();
        this.args = new ArrayList<>(Arrays.asList(msg.split("\\s+")));
        this.args.remove(0);
        this.argsLength = this.args.size();
        this.slashCommandEvent = null;
    }

    public GenericCommandEvent(SlashCommandInteractionEvent e) {
        this.message = null;
        this.author = e.getUser();
        this.msg = null;
        this.channel = e.getChannel();
        this.jda = e.getJDA();
        this.args = e.getOptions().stream().map(OptionMapping::getAsString).collect(Collectors.toList());
        this.mentionedChannels = e.getOptions().stream().filter(o -> o.getType() == OptionType.CHANNEL).map(o -> (TextChannel) o.getAsMessageChannel()).collect(Collectors.toList());
        this.mentionedMembers = e.getOptions().stream().filter(o -> o.getType() == OptionType.USER).map(OptionMapping::getAsMember).collect(Collectors.toList());
        this.mentionedRoles = e.getOptions().stream().filter(o -> o.getType() == OptionType.ROLE).map(OptionMapping::getAsRole).collect(Collectors.toList());
        this.argsLength = this.args.size();
        this.slashCommandEvent = e;
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

    public List<String> getArgs() {
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

    public void reply(String msg, boolean ephermal) {
        if (msg.length() == 0) return;
        if (slashCommandEvent != null) slashCommandEvent.reply(msg).setEphemeral(ephermal).queue();
        else this.channel.sendMessage(msg).queue();
    }

    public void reply(String msg) {
        reply(msg, false);
    }

    public void reply(String msg, @Nullable Consumer<MessageAction> ma, @Nullable Consumer<ReplyCallbackAction> ra, @Nullable Consumer<Message> m, @Nullable Consumer<InteractionHook> ih) {
        if (slashCommandEvent != null) {
            ReplyCallbackAction reply = slashCommandEvent.reply(msg);
            if (ra != null)
                ra.accept(reply);
            reply.queue(ih);
        } else {
            MessageAction ac = getChannel().sendMessage(msg);
            if (ma != null)
                ma.accept(ac);
            ac.queue(m);
        }
    }

    public void reply(MessageEmbed msg, @Nullable Consumer<MessageAction> ma, @Nullable Consumer<ReplyCallbackAction> ra, @Nullable Consumer<Message> m, @Nullable Consumer<InteractionHook> ih) {
        if (slashCommandEvent != null) {
            ReplyCallbackAction reply = slashCommandEvent.replyEmbeds(msg);
            if (ra != null)
                ra.accept(reply);
            reply.queue(ih);
        } else {
            MessageAction ac = getChannel().sendMessageEmbeds(msg);
            if (ma != null)
                ma.accept(ac);
            ac.queue(m);
        }
        logger.info("QUEUED! " + System.currentTimeMillis());
    }

    public CompletableFuture<Message> replyMessage(String msg) {
        if (msg.length() == 0) return null;
        if (slashCommandEvent != null) {
            slashCommandEvent.reply("\uD83D\uDC4D").setEphemeral(true).queue();
        }
        return this.channel.sendMessage(msg).submit();
    }

    @SuppressWarnings("UnusedReturnValue")
    public void reply(MessageEmbed message) {
        if (slashCommandEvent != null) slashCommandEvent.replyEmbeds(message).queue();
        else this.channel.sendMessageEmbeds(message).queue();
    }

    public void replyToMe(String message) {
        Command.sendToMe(message, Command.Bot.byJDA(jda));
    }

    public void done() {
        reply("Done!");
    }

    public boolean isNotFlo() {
        return this.author.getIdLong() != Constants.FLOID;
    }

    public DeferredSlashResponse deferReply() {
        if (slashCommandEvent != null) return new DeferredSlashResponse(slashCommandEvent.deferReply().submit());
        return null;
    }

    public SlashCommandInteractionEvent getSlashCommandEvent() {
        return slashCommandEvent;
    }

    public boolean isSlash() {
        return slashCommandEvent != null;
    }
}
