package de.tectoast.emolga.commands.various;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.toastilities.interactive.ErrorMessage;
import de.tectoast.toastilities.interactive.InteractiveTemplate;
import net.dv8tion.jda.api.entities.Message;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class AddSpriteCommand extends Command {

    private final InteractiveTemplate template;

    public AddSpriteCommand() {
        super("addsprite", "Fügt einen Sprite zur NML hinzu", CommandCategory.Various);
        setCustomPermissions(PermissionPreset.fromIDs(322755315953172485L, 361164404768636929L));
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
        template = new InteractiveTemplate((u, tc, map) -> {
            String mon = (String) map.get("mon");
            String back = (String) map.get("back");
            String shiny = (String) map.get("shiny");
            String form = (String) map.get("form");
            Message.Attachment at = (Message.Attachment) map.get("sprite");
            File loc = new File("/var/www/nmlclient/sprites/gen5" + back + shiny + "/" + mon + ((form.equals(".") ? "" : "-" + form.toLowerCase())) + ".png");
            if (loc.exists()) {
                try {
                    Files.copy(loc.toPath(), Paths.get("SpriteBackup/" + loc.getName()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            at.downloadToFile(loc).thenAccept(f -> {
                if (back.equals("")) {
                    try {
                        Files.copy(f.toPath(), Paths.get("/var/www/nmlclient/sprites/dex" + shiny + "/" + mon + ((form.equals(".") ? "" : "-" + form.toLowerCase())) + ".png"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                tc.sendMessage("Der Sprite wurde geändert/hinzugefügt!").queue();
            });
        }, "Das Hinzufügen des Sprites wurde abgebrochen!")
                .addLayer("mon", "Welches Pokemon ist das? (ohne Form)", m -> {
                    Translation t = getEnglNameWithType(m.getContentDisplay(), "nml");
                    if (t.isSuccess()) return toSDName(t.getTranslation());
                    return new ErrorMessage("Das ist kein Pokemon!");
                })
                .addLayer("back", "Ist das der Front oder Back Sprite?", m -> {
                    String msg = m.getContentDisplay();
                    if (msg.equalsIgnoreCase("back")) return "-back";
                    if (msg.equalsIgnoreCase("front")) return "";
                    return new ErrorMessage("Du musst `Front` oder `Back` schreiben!");
                })
                .addLayer("shiny", "Ist der Sprite Shiny oder nicht? (ja/nein)", m -> {
                    String msg = m.getContentDisplay();
                    if (msg.equalsIgnoreCase("ja")) return "-shiny";
                    if (msg.equalsIgnoreCase("nein")) return "";
                    return new ErrorMessage("Du musst ja oder nein schreiben!");
                })
                .addLayer("form", "Welche besondere Form ist das? (`.`, wenn das keine besondere Form sein sollte)", Message::getContentDisplay)
                .addLayer("sprite", "Schicke jetzt den Sprite als Bild rein.", m -> {
                    List<Message.Attachment> at = m.getAttachments();
                    if (at.size() == 0) return new ErrorMessage("Du musst ein Bild schicken!");
                    return at.get(0);
                }).addCancelCommand("cancel");

    }

    @Override
    public void process(GuildCommandEvent e) {
        template.createInteractive(e.getAuthor(), e.getChannel(), e.getMessage().getIdLong());
    }
}
