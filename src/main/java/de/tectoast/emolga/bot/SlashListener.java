package de.tectoast.emolga.bot;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import static de.tectoast.emolga.commands.Command.buildEnumeration;
import static de.tectoast.emolga.commands.Command.sendToMe;
import static de.tectoast.emolga.utils.Constants.FLOID;


public class SlashListener extends ListenerAdapter {
    final JDA jda;

    public SlashListener(JDA jda) {
        this.jda = jda;
    }

    @Override
    public void onReady(@NotNull ReadyEvent e) {

    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent e) {
        Command command = Command.byName(e.getName());
        User u = e.getUser();
        TextChannel tco = e.getTextChannel();
        try {
            new GuildCommandEvent(command, e);
        } catch (Command.MissingArgumentException ex) {
            Command.ArgumentManagerTemplate.Argument arg = ex.getArgument();
            if (arg.hasCustomErrorMessage()) e.reply(arg.getCustomErrorMessage()).queue();
            else {
                e.reply("Das benötigte Argument `" + arg.getName() + "`, was eigentlich " + buildEnumeration(arg.getType().getName()) + " sein müsste, ist nicht vorhanden!\n" +
                        "Nähere Informationen über die richtige Syntax für den Command erhältst du unter `e!help " + command.getName() + "`.").queue();
            }
            if (u.getIdLong() != FLOID) {
                sendToMe("MissingArgument " + tco.getAsMention() + " Server: " + tco.getGuild().getName());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            e.getHook().sendMessage("Es ist ein Fehler beim Ausführen des Commands aufgetreten!\nWenn du denkst, dass dies ein interner Fehler beim Bot ist, melde dich bitte bei Flo/TecToast.\n" + command.getHelp(e.getGuild()) + (u.getIdLong() == FLOID ? "\nJa Flo, du sollst dich auch bei ihm melden du Kek! :^)" : "")).queue();
        }
    }
}
