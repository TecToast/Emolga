package de.tectoast.emolga.commands.various;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.jsolf.JSONObject;
import net.dv8tion.jda.api.entities.Message;

import java.io.File;

public class CreateCommandCommand extends Command {

    public CreateCommandCommand() {
        super("createcommand", "Erstellt einen Command", CommandCategory.Various);
        setCustomPermissions(PermissionPreset.CULT);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("cmdname", "Command-Name", "Der Command-Name", ArgumentManagerTemplate.Text.any())
                .add("text", "Text", "Der Text, der dann geschickt werden soll (kann leer sein, wenn man ein Bild anhängt)", ArgumentManagerTemplate.Text.any(), true)
                .setExample("!createcommand nein Tom schreit ein kraftvolles **NEIN!** in die Runde!")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) throws Exception {
        ArgumentManager args = e.getArguments();
        String cmdname = args.getText("cmdname").toLowerCase();
        JSONObject o = getEmolgaJSON().getJSONObject("customcommands");
        if (o.has(cmdname) || byName(cmdname) != null) {
            e.reply("Es existiert bereits ein !" + cmdname + " Command!");
            return;
        }
        Message m = e.getMessage();
        JSONObject json = new JSONObject();
        json.put("text", false);
        json.put("image", false);
        File file = null;
        if (m.getAttachments().size() > 0) {
            Message.Attachment a = m.getAttachments().get(0);
            file = a.getProxy().downloadToFile(new File("customcommandimages/" + a.getFileName())).get();
            json.put("image", file.getAbsolutePath());
        }
        if (!args.has("text")) {
            if(file == null) {
                e.reply("Du musst entweder einen Text oder ein Bild angeben!");
                return;
            }
        } else {
            json.put("text", args.getText("text"));
        }
        o.put(cmdname, json);
        saveEmolgaJSON();
        e.reply("Der Command wurde erfolgreich registriert!");
    }
}
