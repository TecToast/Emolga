package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.Command.Translation
import de.tectoast.emolga.commands.PrivateCommand
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.jsolf.JSONArray
import de.tectoast.jsolf.JSONObject
import de.tectoast.toastilities.interactive.ErrorMessage
import de.tectoast.toastilities.interactive.Interactive
import de.tectoast.toastilities.interactive.InteractiveTemplate
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.slf4j.LoggerFactory

class PrismaTeamCommand : PrivateCommand("prismateam") {
    private val template: InteractiveTemplate
    val current: MutableSet<Long> = HashSet()

    init {
        logger.info("Registered PrismaTeamCommand!")
        //setIsAllowed(u -> Arrays.asList(Command.getEmolgaJSON().getJSONObject("drafts").getJSONObject("Prisma").getString("table").split(",")).contains(u.getId()));
        template = InteractiveTemplate({ u: User, tc: MessageChannel, map: LinkedHashMap<String, Any?> ->
            current.remove(u.idLong)
            val league = Command.emolgaJSON.getJSONObject("drafts").getJSONObject("Prisma")
            val arr = JSONArray()
            for ((s, value) in map) {
                if (s == "check") continue
                arr.put(JSONObject().put("tier", s[0].toString()).put("name", value))
            }
            league.getJSONObject("picks").put(u.id, arr)
            Command.saveEmolgaJSON()
            tc.sendMessage("Dein Team wurde erfolgreich gespeichert!").queue()
        }, "Die Team-Eingabe wurde abgebrochen.")
        template.addCancelCommand("!cancel")
        template
            .addLayer(
                "S1",
                "Hallo zur interaktiven Team-Eingabe! Bitte gib nacheinander deine Mons ein (so wie sie in der Tierliste stehen). Falls du einen Fehler machen solltest, gib `!cancel` ein, um die Team-Eingabe abzubrechen.\nS-Mon Nr. 1:"
            ) { m: Message, i: Interactive -> test(m, i, "S") }
            .addLayer("S2", "S-Mon Nr. 2:") { m: Message, i: Interactive -> test(m, i, "S") }
            .addLayer("A1", "A-Mon Nr. 1:") { m: Message, i: Interactive -> test(m, i, "A") }
            .addLayer("A2", "A-Mon Nr. 2:") { m: Message, i: Interactive -> test(m, i, "A") }
            .addLayer("B1", "B-Mon Nr. 1:") { m: Message, i: Interactive -> test(m, i, "B") }
            .addLayer("B2", "B-Mon Nr. 2:") { m: Message, i: Interactive -> test(m, i, "B") }
            .addLayer("B3", "B-Mon Nr. 3:") { m: Message, i: Interactive -> test(m, i, "B") }
            .addLayer("C1", "C-Mon Nr. 1:") { m: Message, i: Interactive -> test(m, i, "C") }
            .addLayer("C2", "C-Mon Nr. 2:") { m: Message, i: Interactive -> test(m, i, "C") }
            .addLayer("C3", "C-Mon Nr. 3:") { m: Message, i: Interactive -> test(m, i, "C") }
            .addLayer("D1", "D-Mon Nr. 1:") { m: Message, i: Interactive -> test(m, i, "D") }
            .addLayer(
                "check", """
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

                                Ist das korrekt? Gib `ja` ein, wenn du dir sicher bist, sonst gib `!cancel` ein, um die Team-Eingabe abzubrechen und nochmal von vorn zu beginnen."""
            ) { m: Message ->
                if (m.contentDisplay == "ja") return@addLayer true
                ErrorMessage("Das ist weder ein `ja` noch `!cancel`!")
            }
            .setOnCancel { i: Interactive -> current.remove(i.user.idLong) }
    }

    override fun process(e: MessageReceivedEvent) {
        val picks = Command.emolgaJSON.getJSONObject("drafts").createOrGetJSON("Prisma").createOrGetJSON("picks")
        if (picks.has(e.author.id)) {
            e.channel.sendMessage("Du hast dein Team bereits eingegeben! Wenn du wirklich einen Fehler gemacht haben solltest, melde dich bitte bei Flo.")
                .queue()
            return
        }
        val uid = e.author.idLong
        if (current.contains(uid)) {
            e.channel.sendMessage("Du bist bereits in der Team-Eingabe!").queue()
            return
        }
        current.add(uid)
        template.createInteractive(e.author, e.channel, e.messageIdLong)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PrismaTeamCommand::class.java)
        fun test(m: Message, i: Interactive, reqtier: String): Any {
            val msg = m.contentDisplay
            if (msg.equals("!badsteam", ignoreCase = true)) {
                return ErrorMessage("")
            }
            val t = Command.getDraftGerName(msg)
            logger.info("msg = $msg, i = ${i.user.idLong} reqtier = $reqtier")
            if (!t.isFromType(Translation.Type.POKEMON)) {
                return ErrorMessage("Das ist kein Pokemon!")
            }
            return t.translation
        }

        val tierlist: Tierlist
            get() = Tierlist.getByGuild("736555250118295622")!!
    }
}