package de.tectoast.emolga.bot;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.database.Database;
import de.tectoast.emolga.utils.sql.DBManagers;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import org.jetbrains.annotations.NotNull;

public class SlashListener extends ListenerAdapter {

    final JDA jda;

    public SlashListener(JDA jda) {
        this.jda = jda;
    }

    @Override
    public void onReady(@NotNull ReadyEvent e) {
        /*CommandListUpdateAction ca = jda.getGuildById(Constants.MYSERVER).updateCommands();
        ca.addCommands(new CommandData("replaychannel", "Aktiviert/Deaktiviert die automatische Analyse von Showdown-Replays")
                .addSubcommands(new SubcommandData("add", "Fügt einen Channel hinzu").addOption(OptionType.CHANNEL, "channel", "Der Channel, der hinzugefügt werden soll", true))
                .addSubcommands(new SubcommandData("remove", "Löscht einen Channel aus der Liste").addOption(OptionType.CHANNEL, "channel", "Der Channel, der gelöscht werden soll", true))
        );
        ca.queue();*/
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent e) {
        TextChannel tco = e.getTextChannel();
        String name = e.getName();
        String sn = e.getSubcommandName();
        System.out.println("e.getName() = " + name);
        System.out.println("e.getSubcommandGroup() = " + e.getSubcommandGroup());
        System.out.println("e.getSubcommandName() = " + sn);
        if (name.equals("replaychannel")) {
            TextChannel tc = (TextChannel) e.getOption("channel").getAsMessageChannel();
            if (sn.equalsIgnoreCase("remove")) {
                if (Database.update("DELETE FROM analysis WHERE replay = " + tco.getIdLong() + " AND result = " + tc.getIdLong()) > 0) {
                    e.reply("Aus diesem Channel werden keine Ergebnisse mehr in " + tc.getAsMention() + " geschickt!").setEphemeral(true).queue();
                } else {
                    e.reply("Zurzeit werden aus diesem Channel keine Replays in " + tc.getAsMention() + " geschickt!").setEphemeral(true).queue();
                }
            } else if (sn.equalsIgnoreCase("add")) {
                if (tc == null || !PermissionUtil.checkPermission(tc, e.getGuild().getSelfMember(), Permission.MESSAGE_WRITE)) {
                    e.reply("Ich habe nicht die nötigen Rechte, um in " + tc.getAsMention() + " schreiben zu können!").setEphemeral(true).queue();
                    return;
                    // Todo das hier testen lol
                }
                long l = DBManagers.ANALYSIS.insertChannel(tco, tc);
                if (l == -1) {
                    e.reply("Alle Ergebnisse der Replays aus " + tco.getAsMention() + " werden von nun an in den Channel " + tc.getAsMention() + " geschickt!").setEphemeral(true).queue();
                    Command.replayAnalysis.put(tco.getIdLong(), tc.getIdLong());
                } else {
                    e.reply("Die Replays aus diesem Channel werden " + (l == tc.getIdLong() ? "bereits" : "zurzeit") + " in den Channel <#" + l + "> geschickt!").setEphemeral(true).queue();
                }
            }
        }
    }
}
