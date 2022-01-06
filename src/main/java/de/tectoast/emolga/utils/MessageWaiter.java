package de.tectoast.emolga.utils;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MessageWaiter extends ListenerAdapter {
    private final Set<GuildWaitingEvent> guildset = new HashSet<>();
    private final Set<PrivateWaitingEvent> privateset = new HashSet<>();
    private final ScheduledExecutorService threadpool = Executors.newSingleThreadScheduledExecutor();


    public synchronized void waitForGuildMessageReceived(Predicate<GuildMessageReceivedEvent> condition, Consumer<GuildMessageReceivedEvent> action,
                                                         long timeout, TimeUnit unit, Runnable timeoutAction) {
        GuildWaitingEvent we = new GuildWaitingEvent(condition, action);
        guildset.add(we);

        if (timeout > 0 && unit != null) {
            threadpool.schedule(() ->
            {
                if (guildset.remove(we) && timeoutAction != null)
                    timeoutAction.run();
            }, timeout, unit);
        }
    }

    public synchronized void waitForPrivateMessageReceived(Predicate<PrivateMessageReceivedEvent> condition, Consumer<PrivateMessageReceivedEvent> action) {
        PrivateWaitingEvent we = new PrivateWaitingEvent(condition, action);
        privateset.add(we);
    }

    @Override
    public synchronized void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent e) {
        GuildWaitingEvent[] toRemove = guildset.toArray(new GuildWaitingEvent[0]);

        // WaitingEvent#attempt invocations that return true have passed their condition tests
        // and executed the action. We filter the ones that return false out of the toRemove and
        // remove them all from the set.
        guildset.removeAll(Stream.of(toRemove).filter(i -> i.attempt(e)).collect(Collectors.toSet()));
    }

    @Override
    public synchronized void onPrivateMessageReceived(@NotNull PrivateMessageReceivedEvent e) {
        PrivateWaitingEvent[] toRemove = privateset.toArray(new PrivateWaitingEvent[0]);

        // WaitingEvent#attempt invocations that return true have passed their condition tests
        // and executed the action. We filter the ones that return false out of the toRemove and
        // remove them all from the set.
        privateset.removeAll(Stream.of(toRemove).filter(i -> i.attempt(e)).collect(Collectors.toSet()));
    }

    private record GuildWaitingEvent(
            Predicate<GuildMessageReceivedEvent> condition,
            Consumer<GuildMessageReceivedEvent> action) {

        boolean attempt(GuildMessageReceivedEvent event) {
            if (condition.test(event)) {
                action.accept(event);
                return true;
            }
            return false;
        }
    }

    private record PrivateWaitingEvent(
            Predicate<PrivateMessageReceivedEvent> condition,
            Consumer<PrivateMessageReceivedEvent> action) {

        boolean attempt(PrivateMessageReceivedEvent event) {
            if (condition.test(event)) {
                action.accept(event);
                return true;
            }
            return false;
        }
    }
}
