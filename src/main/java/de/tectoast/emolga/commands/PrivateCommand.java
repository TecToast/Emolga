package de.tectoast.emolga.commands;

import de.tectoast.emolga.utils.Constants;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

public abstract class PrivateCommand {

    public static final List<PrivateCommand> commands = new LinkedList<>();
    final String name;
    final List<String> aliases = new LinkedList<>();
    public Predicate<User> isAllowed = u -> true;

    public PrivateCommand(String name) {
        this.name = name;
        commands.add(this);
    }

    public static void check(MessageReceivedEvent e) {
        Message m = e.getMessage();
        User u = e.getAuthor();
        String msg = m.getContentDisplay();
        for (PrivateCommand c : commands) {
            if (!c.checkPrefix(msg)) continue;
            if (!c.isAllowed.test(u)) continue;
            c.process(e);
        }
    }

    public void setIsAllowed(Predicate<User> isAllowed) {
        this.isAllowed = isAllowed.or(user -> user.getIdLong() == Constants.FLOID);
    }

    private boolean checkPrefix(String msg) {

        return msg.toLowerCase().startsWith("!" + name.toLowerCase() + " ") || aliases.stream().anyMatch(s -> msg.toLowerCase().startsWith("!" + s.toLowerCase() + " "))
                || msg.equalsIgnoreCase("!" + name.toLowerCase()) || aliases.stream().anyMatch(s -> msg.equalsIgnoreCase("!" + s));
    }

    public abstract void process(MessageReceivedEvent e);


}
