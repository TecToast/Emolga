package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.PrivateCommand;
import de.tectoast.emolga.utils.draft.Tierlist;
import de.tectoast.toastilities.interactive.ErrorMessage;
import de.tectoast.toastilities.interactive.Interactive;
import de.tectoast.toastilities.interactive.InteractiveTemplate;
import de.tectoast.toastilities.interactive.Layer;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import org.jsolf.JSONArray;
import org.jsolf.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;

public class BadsTeamCommand extends PrivateCommand {

    private static final Logger logger = LoggerFactory.getLogger(BadsTeamCommand.class);

    final InteractiveTemplate template;

    final HashSet<Long> current = new HashSet<>();


    public BadsTeamCommand() {
        super("badsteam");
        logger.info("Registered BadsTeamCommand!");
        setIsAllowed(u -> Arrays.asList(Command.getEmolgaJSON().getJSONObject("drafts").getJSONObject("BADS").getString("table").split(",")).contains(u.getId()));
        template = new InteractiveTemplate((u, tc, map) -> {
            current.remove(u.getIdLong());
            JSONObject bads = Command.getEmolgaJSON().getJSONObject("drafts").getJSONObject("BADS");
            JSONArray arr = new JSONArray();
            for (String s : map.keySet()) {
                if (s.equals("check")) continue;
                arr.put(new JSONObject().put("tier", s.charAt(0) + "").put("name", map.get(s)));
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
                .addLayer("D1", "D-Mon Nr. 1:", (m, i) -> test(m, i, "D"))
                .addLayer("D2", "D-Mon Nr. 2:", (m, i) -> test(m, i, "D"))
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
                                D: {D1}
                                D: {D2}

                                Ist das korrekt? Gib `ja` ein, wenn du dir sicher bist, sonst gib `!cancel` ein, um die Team-Eingabe abzubrechen und nochmal von vorn zu beginnen.""",
                        m -> {
                            if (m.getContentDisplay().equals("ja")) return true;
                            return new ErrorMessage("Das ist weder ein `ja` noch `!cancel`!");
                        }
                )
                .setOnCancel(i -> current.remove(i.getUser().getIdLong()));
    }

    public Object test(Message m, Interactive i, String reqtier) {
        String msg = m.getContentDisplay();
        if (msg.equalsIgnoreCase("!badsteam")) {
            return new ErrorMessage("");
        }
        Command.Translation t = Command.getDraftGerName(msg);
        logger.info("msg = " + msg);
        if (!t.isFromType(Command.Translation.Type.POKEMON)) {
            return new ErrorMessage("Das ist kein Pokemon!");
        }
        Tierlist tierlist = getTierlist();
        String tier = tierlist.getTierOf(t.getTranslation());
        logger.info("tier = " + tier);
        if (tier.equals(""))
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

    public Tierlist getTierlist() {
        return Tierlist.getByGuild("736555250118295622");
    }

    @Override
    public void process(PrivateMessageReceivedEvent e) {
        JSONObject picks = Command.getEmolgaJSON().getJSONObject("drafts").getJSONObject("BADS").getJSONObject("picks");
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
