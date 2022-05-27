package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.PrivateCommand;
import de.tectoast.emolga.utils.draft.Tierlist;
import de.tectoast.jsolf.JSONArray;
import de.tectoast.jsolf.JSONObject;
import de.tectoast.toastilities.interactive.ErrorMessage;
import de.tectoast.toastilities.interactive.Interactive;
import de.tectoast.toastilities.interactive.InteractiveTemplate;
import de.tectoast.toastilities.interactive.Layer;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PrismaTeamCommand extends PrivateCommand {

    private static final Logger logger = LoggerFactory.getLogger(PrismaTeamCommand.class);

    final InteractiveTemplate template;

    final Set<Long> current = new HashSet<>();


    public PrismaTeamCommand() {
        super("prismateam");
        logger.info("Registered PrismaTeamCommand!");
        //setIsAllowed(u -> Arrays.asList(Command.getEmolgaJSON().getJSONObject("drafts").getJSONObject("Prisma").getString("table").split(",")).contains(u.getId()));
        template = new InteractiveTemplate((u, tc, map) -> {
            current.remove(u.getIdLong());
            String[] x = new String[]{};
            String collect = String.join("", x);
            JSONObject bads = Command.getEmolgaJSON().getJSONObject("drafts").getJSONObject("Prisma");
            JSONArray arr = new JSONArray();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String s = entry.getKey();
                if (s.equals("check")) continue;
                arr.put(new JSONObject().put("tier", String.valueOf(s.charAt(0))).put("name", entry.getValue()));
            }
            bads.getJSONObject("picks").put(u.getId(), arr);
            Command.saveEmolgaJSON();
            tc.sendMessage("Dein Team wurde erfolgreich gespeichert!").queue();
        }, "Die Team-Eingabe wurde abgebrochen.");
        template.addCancelCommand("!cancel");
        template
                .addLayer("S1", "Hallo zur interaktiven Team-Eingabe! Bitte gib nacheinander deine Mons ein (so wie sie in der Tierliste stehen). Falls du einen Fehler machen solltest, gib `!cancel` ein, um die Team-Eingabe abzubrechen.\nS-Mon Nr. 1:",
                        (m, i) -> test(m, i, "S"))
                .addLayer("S2", "S-Mon Nr. 2:", (m, i) -> test(m, i, "S"))
                .addLayer("A1", "A-Mon Nr. 1:", (m, i) -> test(m, i, "A"))
                .addLayer("A2", "A-Mon Nr. 2:", (m, i) -> test(m, i, "A"))
                .addLayer("B1", "B-Mon Nr. 1:", (m, i) -> test(m, i, "B"))
                .addLayer("B2", "B-Mon Nr. 2:", (m, i) -> test(m, i, "B"))
                .addLayer("B3", "B-Mon Nr. 3:", (m, i) -> test(m, i, "B"))
                .addLayer("C1", "C-Mon Nr. 1:", (m, i) -> test(m, i, "C"))
                .addLayer("C2", "C-Mon Nr. 2:", (m, i) -> test(m, i, "C"))
                .addLayer("C3", "C-Mon Nr. 3:", (m, i) -> test(m, i, "C"))
                .addLayer("D1", "D-Mon Nr. 1:", (m, i) -> test(m, i, "D"))
                .addLayer("check", """
                                Hier nochmal die Liste deiner Mons:
                                                                
                                S: {S1}
                                S: {S2}
                                A: {A1}
                                A: {A2}
                                B: {B1}
                                B: {B2}
                                B: {B3}
                                C: {C1}
                                C: {C2}
                                C: {C3}
                                D: {D1}

                                Ist das korrekt? Gib `ja` ein, wenn du dir sicher bist, sonst gib `!cancel` ein, um die Team-Eingabe abzubrechen und nochmal von vorn zu beginnen.""",
                        m -> {
                            if (m.getContentDisplay().equals("ja")) return true;
                            return new ErrorMessage("Das ist weder ein `ja` noch `!cancel`!");
                        }
                )
                .setOnCancel(i -> current.remove(i.getUser().getIdLong()));
    }

    public static Object test(Message m, Interactive i, String reqtier) {
        String msg = m.getContentDisplay();
        if (msg.equalsIgnoreCase("!badsteam")) {
            return new ErrorMessage("");
        }
        Command.Translation t = Command.getDraftGerName(msg);
        logger.info("msg = " + msg);
        if (!t.isFromType(Command.Translation.Type.POKEMON)) {
            return new ErrorMessage("Das ist kein Pokemon!");
        }
        if (true) return t.getTranslation();
        Tierlist tierlist = getTierlist();
        String tier = tierlist.getTierOf(t.getTranslation());
        logger.info("tier = " + tier);
        if (tier.isEmpty())
            return new ErrorMessage("Das ist zwar ein Pokemon, aber es steht so nicht in der Tierliste!");
        logger.info("tierlist.order.indexOf(tier) = " + tierlist.order.indexOf(tier));
        logger.info("reqtier = " + reqtier);
        logger.info("tierlist.order.indexOf(reqtier) = " + tierlist.order.indexOf(reqtier));
        if (tierlist.order.indexOf(tier) < tierlist.order.indexOf(reqtier)) {
            return new ErrorMessage("Du kannst ein " + tier + "-Mon nicht ins " + reqtier + " hochdraften!");
        }
        String name = getTierlist().getNameOf(t.getTranslation());
        if (i.getLayers().stream().filter(Layer::isFinished).map(Layer::getAnswerAsString).anyMatch(name::equals)) {
            return new ErrorMessage("Dieses Pokemon ist bereits in deinem Team!");
        }
        return name;
    }

    public static Tierlist getTierlist() {
        return Tierlist.getByGuild("736555250118295622");
    }

    @Override
    public void process(MessageReceivedEvent e) {
        JSONObject picks = Command.getEmolgaJSON().getJSONObject("drafts").createOrGetJSON("Prisma").createOrGetJSON("picks");
        if (picks.has(e.getAuthor().getId())) {
            e.getChannel().sendMessage("Du hast dein Team bereits eingegeben! Wenn du wirklich einen Fehler gemacht haben solltest, melde dich bitte bei Flo.").queue();
            return;
        }
        long uid = e.getAuthor().getIdLong();
        if (current.contains(uid)) {
            e.getChannel().sendMessage("Du bist bereits in der Team-Eingabe!").queue();
            return;
        }
        current.add(uid);
        template.createInteractive(e.getAuthor(), e.getChannel(), e.getMessageIdLong());
    }
}
