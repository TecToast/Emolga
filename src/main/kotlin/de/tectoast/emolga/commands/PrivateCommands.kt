package de.tectoast.emolga.commands

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.commands.Command.ArgumentManagerTemplate
import de.tectoast.emolga.commands.Command.Companion.getAsXCoord
import de.tectoast.emolga.commands.Command.Companion.load
import de.tectoast.emolga.commands.Command.Companion.save
import de.tectoast.emolga.commands.Command.Translation
import de.tectoast.emolga.database.Database
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.Constants.EMOLGA_KI
import de.tectoast.emolga.utils.Constants.MYSERVER
import de.tectoast.emolga.utils.Google
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.annotations.PrivateCommand
import de.tectoast.emolga.utils.automation.collection.DocEntries
import de.tectoast.emolga.utils.draft.Draft
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.showdown.Analysis
import de.tectoast.emolga.utils.sql.managers.AnalysisManager
import de.tectoast.emolga.utils.sql.managers.DasorUsageManager
import de.tectoast.emolga.utils.sql.managers.TranslationsManager
import de.tectoast.jsolf.JSONArray
import de.tectoast.jsolf.JSONObject
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.AudioChannel
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.interactions.components.buttons.Button
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.awt.Color
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.regex.Pattern
import java.util.stream.Collectors

object PrivateCommands {
    private val logger = LoggerFactory.getLogger(PrivateCommands::class.java)
    private val DOUBLE_BACKSLASH = Pattern.compile("\\\\")
    private val TRIPLE_HASHTAG = Pattern.compile("###")

    @PrivateCommand(name = "updatetierlist")
    fun updateTierlist(e: GenericCommandEvent) {
        Tierlist.setup()
        e.reply("Die Tierliste wurde aktualisiert!")
    }

    @PrivateCommand(name = "checkguild")
    fun checkGuild(e: GenericCommandEvent) {
        e.jda.getGuildById(e.getArg(0))?.let { e.reply(it.name) }
    }

    @PrivateCommand(name = "edit")
    fun edit(e: GenericCommandEvent) {
        e.msg?.let { e.jda.getTextChannelById(e.getArg(0))?.editMessageById(e.getArg(1), it.substring(43))?.queue() }
    }

    @PrivateCommand(name = "send")
    fun send(e: GenericCommandEvent) {
        val message = e.message
        logger.info(message!!.contentRaw)
        var s = DOUBLE_BACKSLASH.matcher(message.contentRaw.substring(24)).replaceAll("")
        val tc = e.jda.getTextChannelById(e.getArg(0))
        val g = tc!!.guild
        for (emote in g.retrieveEmojis().complete()) {
            s = s.replace("<<" + emote.name + ">>", emote.asMention)
        }
        tc.sendMessage(s).queue()
    }

    @PrivateCommand(name = "sendpn")
    fun sendPN(e: GenericCommandEvent) {
        val message = e.message
        logger.info(message!!.contentRaw)
        val s = DOUBLE_BACKSLASH.matcher(message.contentRaw.substring(26)).replaceAll("")
        val userid = e.getArg(0)
        Command.sendToUser(userid.toLong(), s)
    }

    @PrivateCommand(name = "react")
    fun react(e: GenericCommandEvent) {
        val msg = e.msg
        var s = msg!!.substring(45)
        val tc = e.jda.getTextChannelById(e.getArg(0))
        val m = tc!!.retrieveMessageById(e.getArg(1)).complete()!!
        if (s.contains("<")) {
            s = s.substring(1)
            logger.info("s = $s")
            val finalS = s
            tc.guild.retrieveEmojis().complete().stream()
                .filter { it.name.equals(finalS, ignoreCase = true) }
                .forEach { m.addReaction(it!!).queue() }
        } else {
            m.addReaction(Emoji.fromUnicode(s)).queue()
        }
    }

    @PrivateCommand(name = "ban")
    fun ban(e: GenericCommandEvent) {
        e.jda.getGuildById(e.getArg(0))?.ban(UserSnowflake.fromId(e.getArg(1)), 0)?.queue()
    }

    @PrivateCommand(name = "banwithreason")
    fun banwithreason(e: GenericCommandEvent) {
        e.jda.getGuildById(e.getArg(0))?.ban(UserSnowflake.fromId(e.getArg(1)), 0, e.msg?.substring(53))?.queue()
    }

    @PrivateCommand(name = "updatedatabase")
    fun updateDatabase(e: GenericCommandEvent) {
        Command.loadJSONFiles()
        e.done()
    }

    @PrivateCommand(name = "emolgajson", aliases = ["ej"])
    fun emolgajson(e: GenericCommandEvent) {
        Command.emolgaJSON = load("./emolgadata.json")
        e.done()
    }

    @PrivateCommand(name = "del")
    fun del(e: GenericCommandEvent) {
        e.jda.getTextChannelById(e.getArg(0))?.deleteMessageById(e.getArg(1))?.queue()
    }

    @PrivateCommand(name = "troll")
    fun troll(e: GenericCommandEvent) {
        val category = e.jda.getCategoryById(e.getArg(0))
        val g = category!!.guild
        val user = g.retrieveMemberById(e.getArg(1)).complete()
        val list: MutableList<AudioChannel?> = ArrayList(category.voiceChannels)
        list.shuffle()
        val old = user.voiceState!!.channel
        list.remove(old)
        val service = Executors.newScheduledThreadPool(3)
        var x = 1
        for (voiceChannel in list) {
            service.schedule({ g.moveVoiceMember(user, voiceChannel).queue() }, x++.toLong(), TimeUnit.SECONDS)
        }
        service.schedule({ g.moveVoiceMember(user, old).queue() }, x.toLong(), TimeUnit.SECONDS)
    }

    @PrivateCommand(name = "addreactions")
    fun addReactions(e: GenericCommandEvent) {
        e.jda.getTextChannelById(e.getArg(0))?.retrieveMessageById(e.getArg(1))?.queue { m: Message ->
            m.reactions.forEach(Consumer {
                m.addReaction(it.emoji).queue()
            })
            e.done()
        }
    }

    @PrivateCommand(name = "replaceplayer")
    fun replacePlayer(e: GenericCommandEvent) {
        val league = Command.emolgaJSON.getJSONObject("drafts").getJSONObject(e.getArg(0))
        val battleOrder = league.getJSONObject("battleorder")
        val oldid = e.getArg(1)
        val newid = e.getArg(2)
        for (i in 1..battleOrder.length()) {
            battleOrder.put(i.toString(), battleOrder.getString(i.toString()).replace(oldid, newid))
        }
        league.put("table", league.getString("table").replace(oldid, newid))
        val order = league.getJSONObject("order")
        for (i in 1..order.length()) {
            order.put(i, order.getString(i.toString()).replace(oldid, newid))
        }
        val picks = league.getJSONObject("picks")
        picks.put(newid, picks.getJSONArray(oldid))
        picks.remove(oldid)
        if (league.has("results")) {
            val results = league.getJSONObject("results")
            val keys = ArrayList(results.keySet())
            for (key in keys) {
                if (key.contains(oldid)) {
                    results.put(key.replace(oldid, newid), results.getString(key))
                    results.remove(key)
                }
            }
        }
        Command.saveEmolgaJSON()
        e.done()
    }

    @PrivateCommand(name = "saveemolgajson")
    fun saveEmolga(e: GenericCommandEvent) {
        Command.saveEmolgaJSON()
        e.done()
    }

    @PrivateCommand(name = "pdg")
    fun pdg(e: GenericCommandEvent) {
        Command.evaluatePredictions(
            Command.emolgaJSON.getJSONObject("drafts").getJSONObject(e.getArg(0)),
            e.getArg(1).toBooleanStrict(),
            e.getArg(2)
                .toInt(),
            e.getArg(3),
            e.getArg(4)
        )
        e.done()
    }

    @PrivateCommand(name = "incrpdg")
    fun incrPdg(e: GenericCommandEvent) {
        for (arg in e.args) {
            Database.incrementPredictionCounter(arg.toLong())
        }
    }

    @PrivateCommand(name = "testvolume")
    fun testVolume(e: GenericCommandEvent) {
        logger.info("Start!")
        Command.musicManagers[673833176036147210L]!!.player.volume = e.getArg(0).toInt()
        logger.info("musicManagers.get(673833176036147210L).player.getVolume() = " + Command.musicManagers[673833176036147210L]!!.player.volume)
    }

    @PrivateCommand(name = "printcache")
    fun printCache(e: GenericCommandEvent) {
        Command.translationsCacheGerman.forEach { (str: String?, t: Translation) ->
            logger.info(str)
            t.print()
            logger.info("=====")
        }
        logger.info(Command.translationsCacheOrderGerman.toString())
        logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>")
        Command.translationsCacheEnglish.forEach { (str: String?, t: Translation) ->
            logger.info(str)
            t.print()
            logger.info("=====")
        }
        logger.info(Command.translationsCacheOrderEnglish.toString())
        e.done()
    }

    @PrivateCommand(name = "clearcache")
    fun clearCache(e: GenericCommandEvent) {
        Command.translationsCacheGerman.clear()
        Command.translationsCacheOrderGerman.clear()
        Command.translationsCacheEnglish.clear()
        Command.translationsCacheOrderEnglish.clear()
        e.done()
    }

    @PrivateCommand(name = "getguildbytc")
    fun getGuildCmd(e: GenericCommandEvent) {
        e.jda.getTextChannelById(e.getArg(0))?.guild?.let { e.reply(it.name) }
    }

    @PrivateCommand(name = "inviteme")
    fun inviteMe(e: GenericCommandEvent) {
        for (guild in e.jda.guilds) {
            try {
                guild.retrieveMemberById(Constants.FLOID).complete()
            } catch (exception: Exception) {
                Command.sendToMe(guild.textChannels[0].createInvite().complete().url)
            }
        }
    }

    @PrivateCommand(name = "removeduplicates")
    fun removeDuplicates() {
        TranslationsManager.removeDuplicates()
    }

    @PrivateCommand(name = "ndsnominate")
    fun ndsNominate(e: GenericCommandEvent) {
        Draft.doNDSNominate(e.getArg(0).toBooleanStrict())
    }

    @PrivateCommand(name = "ndsprediction")
    fun ndsPrediction() {
        Draft.doNDSPredictionGame()
    }

    @PrivateCommand(name = "ndsreminder")
    fun ndsReminder() {
        val nds = Command.emolgaJSON.getJSONObject("drafts").getJSONObject("NDS")
        val table: MutableCollection<String> = nds.getJSONObject("picks").keySet()
        val nominations = nds.getJSONObject("nominations")
        nominations.getJSONObject(nominations.getInt("currentDay")).keySet()
            .forEach(Consumer { o: String -> table.remove(o) })
        logger.info(MarkerFactory.getMarker("important"), table.stream().map { l: String -> "<@$l>" }
            .collect(Collectors.joining(", ")))
    }

    @PrivateCommand(name = "matchups")
    fun matchUps(e: GenericCommandEvent) {
        Draft.doMatchUps(e.getArg(0))
    }

    @PrivateCommand(name = "sortnds")
    fun sortNDSCmd(e: GenericCommandEvent) {
        val nds = Command.emolgaJSON.getJSONObject("drafts").getJSONObject("NDS")
        DocEntries.NDS.sort(nds.getString("sid"), nds)
        e.done()
    }

    @PrivateCommand(name = "ndskilllist")
    fun ndskilllist() {
        val send: MutableList<List<Any>> = LinkedList()
        val sid = "1vPYBY-IzVSPodd8W_ukVSLME0YGyWF0hT6p3kr-QvZU"
        val nds = Command.emolgaJSON.getJSONObject("drafts").getJSONObject("NDS")
        val picks = nds.getJSONObject("picks")
        val teamnames = nds.getJSONObject("teamnames")
        //BufferedWriter writer = new BufferedWriter(new FileWriter("ndskilllistorder.txt"));
        for (s in picks.keySet()) {
            val mons = Command.getPicksAsList(picks.getJSONArray(s))
            val lists = Google[sid, "${teamnames.getString(s)}!B200:K${mons.size + 199}", false]
            for (j in mons.indices) {
                send.add(lists!![j])
            }
        }
        RequestBuilder.updateAll(sid, "Killliste!S1001", send)
    }

    @PrivateCommand(name = "ndsrr")
    fun ndsrr(e: GenericCommandEvent) {
        val lastNom =
            load("ndsdraft.json").getJSONObject("hinrunde").getJSONObject("nominations").getJSONObject("5")
        val sid = "1vPYBY-IzVSPodd8W_ukVSLME0YGyWF0hT6p3kr-QvZU"
        //String sid = "1Lbeko-7ZFuuVon_qmgavDsht5JoWQPVk2TLMN6cCROo";
        var current = ""
        val nds = Command.emolgaJSON.getJSONObject("drafts").getJSONObject("NDS")
        val allpicks = nds.getJSONObject("picks")
        val toid = nds.getJSONObject("nametoid")
        val teamnames = nds.getJSONObject("teamnames")
        val b = RequestBuilder(sid)
        val tierlist = Tierlist.getByGuild(Constants.NDSID)
        val currkills = HashMap<String, List<Int>>()
        val currdeaths = HashMap<String, List<Int>>()
        val killstoadd = HashMap<String, AtomicInteger>()
        val deathstoadd = HashMap<String, AtomicInteger>()
        for (i in 0..5) {
            val l = Google[sid, "RR Draft!${getAsXCoord((i shl 2) + 2)}5:${getAsXCoord((i shl 2) + 4)}28", false]
            for ((x, objects) in l!!.withIndex()) {
                if (x % 2 == 0) current = toid.getString((objects[0] as String).trim()) else {
                    val currorder =
                        Arrays.stream(TRIPLE_HASHTAG.split(lastNom.getString(current))).flatMap { s: String ->
                            Arrays.stream(s.split(";".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray())
                        }
                            .map { s: String ->
                                s.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray()[0]
                            }.toList()
                    val teamname = teamnames.getString(current)
                    if (!currkills.containsKey(current)) currkills[current] =
                        Google[sid, "$teamname!L200:L214", false]!!
                            .stream().map { (it[0] as String).toInt() }
                            .collect(Collectors.toList())
                    if (!currdeaths.containsKey(current)) currdeaths[current] =
                        Google[sid, "$teamname!X200:X214", false]!!
                            .stream().map { (it[0] as String).toInt() }
                            .collect(Collectors.toList())
                    if (!killstoadd.containsKey(current)) killstoadd[current] = AtomicInteger()
                    if (!deathstoadd.containsKey(current)) deathstoadd[current] = AtomicInteger()
                    val raus = objects[0] as String
                    val rein = objects[2] as String
                    val arr = allpicks.getJSONArray(current)
                    if (raus.trim() != "/" && rein.trim() != "/") {
                        val picks = Command.getPicksAsList(arr)
                        logger.info("picks = $picks")
                        logger.info("raus = $raus")
                        logger.info("rein = $rein")
                        val index = picks.indexOf(raus)
                        killstoadd[current]!!.addAndGet(currkills[current]!![index])
                        deathstoadd[current]!!.addAndGet(currdeaths[current]!![index])
                        val o = arr.getJSONObject(index)
                        o.put("name", rein)
                        o.put("tier", tierlist!!.getTierOf(rein))
                        val sdName = Command.getSDName(rein)
                        val data = Command.dataJSON.getJSONObject(sdName)
                        val currindex = currorder.indexOf(raus) + 15
                        val outloc = tierlist.getLocation(raus)
                        val inloc = tierlist.getLocation(rein)
                        b
                            .addSingle("$teamname!B$currindex", Command.getGen5Sprite(data))
                            .addSingle("$teamname!D$currindex", rein)
                        if (outloc.valid) {
                            b.addSingle(
                                "Tierliste!" + getAsXCoord((outloc.x + 1) * 6) + (outloc.y + 4),
                                "-frei-"
                            )
                        }
                        if (inloc.valid) {
                            b.addSingle(
                                "Tierliste!" + getAsXCoord((inloc.x + 1) * 6) + (inloc.y + 4),
                                "='$teamname'!B2"
                            )
                        }
                        b.addRow(
                            teamname + "!A" + (index + 200),
                            listOf<Any>(rein, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                        )
                        b.addRow(teamname + "!N" + (index + 200), listOf<Any>(0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
                        val t: MutableList<Any> = data.getStringList("types").stream()
                            .map { s: String? -> Command.typeIcons.getString(s) }
                            .collect(
                                Collectors.toCollection { LinkedList() }
                            )
                        if (t.size == 1) t.add("/")
                        b.addRow("$teamname!F$currindex", t)
                        b.addSingle("$teamname!H$currindex", data.getJSONObject("baseStats").getInt("spe"))
                        b.addSingle("$teamname!I$currindex", tierlist.getPointsNeeded(rein))
                        b.addSingle("$teamname!J$currindex", "2")
                        b.addRow(
                            "$teamname!L$currindex",
                            listOf<Any>(
                                Command.canLearnNDS(sdName, "stealthrock"),
                                Command.canLearnNDS(sdName, "defog"),
                                Command.canLearnNDS(sdName, "rapidspin"),
                                Command.canLearnNDS(sdName, "voltswitch", "uturn", "flipturn", "batonpass", "teleport")
                            )
                        )
                    }
                }
            }
        }
        for ((s, value) in killstoadd) {
            val teamname = teamnames.getString(s)
            b.addSingle("$teamname!L215", "=SUMME(L199:L214)")
            b.addSingle("$teamname!L199", value.get())
            b.addSingle("$teamname!X215", "=SUMME(X199:X214)")
            b.addSingle("$teamname!X199", deathstoadd[s]!!.get())
        }
        save(Command.emolgaJSON, "ndstestemolga.json")
        b.execute()
        e.done()
    }

    @PrivateCommand(name = "checktierlist")
    fun checkTierlist(e: GenericCommandEvent) {
        val tierlist = Tierlist.getByGuild(e.getArg(0))!!
        val mons: MutableList<String> = LinkedList()
        for (s in tierlist.order) {
            for (str in tierlist.tierlist[s]!!) {
                if (!Command.getDraftGerName(str).isFromType(Translation.Type.POKEMON)) {
                    mons.add(str)
                }
            }
        }
        if (mons.isEmpty()) e.reply("Oh, du bist wohl kein Dasor :3") else e.reply(java.lang.String.join("\n", mons))
    }

    @PrivateCommand(name = "converttierlist")
    @Throws(IOException::class)
    fun convertTierlist(e: GenericCommandEvent) {
        val arr = JSONArray()
        val curr: MutableList<String?> = LinkedList()
        for (tiercolumn in Files.readAllLines(Paths.get("Tierlists", e.getArg(0), "tiercolumns.txt"))) {
            if (tiercolumn == "NEXT") {
                arr.put(curr)
                curr.clear()
            } else {
                curr.add(tiercolumn)
            }
        }
        arr.put(curr)
        logger.info(arr.toString())
    }

    @PrivateCommand(name = "asltierlist")
    fun asltierlist() {
        val t = Tierlist.getByGuild(518008523653775366L)
        val b = RequestBuilder("1wI291CWkKkWqQhY_KNu7GVfdincRz74omnCOvEVTDrc").withAdditionalSheets(
            "1tFLd9Atl9QpMgCQBclpeU1WlMqSRGMeX8COUVDIf4TU",
            "1A040AYoiqTus1wSq_3CXgZpgcY3ECpphVRJWHmXyxsQ",
            "1p8DSvd3vS5s5z-1UGPjKUhFVskYjQyrn-HbvU0pb5WE",
            "1nEJvV5UESfuJvsJplXi_RXXnq9lY2vD5NyrTF3ObcvU"
        )
        var x = 0
        for (s in t!!.order) {
            val mons = t.tierlist[s]!!.stream().map { str: String ->
                if (str.startsWith("M-")) {
                    if (str.endsWith("-X")) return@map "M-" + Command.getEnglName(
                        str.substring(
                            2,
                            str.length - 2
                        )
                    ) + "-X"
                    if (str.endsWith("-Y")) return@map "M-" + Command.getEnglName(
                        str.substring(
                            2,
                            str.length - 2
                        )
                    ) + "-Y"
                    return@map "M-" + Command.getEnglName(str.substring(2))
                }
                if (str.startsWith("A-")) return@map "A-" + Command.getEnglName(str.substring(2))
                if (str.startsWith("G-")) return@map "G-" + Command.getEnglName(str.substring(2))
                val engl = Command.getEnglNameWithType(str)
                if (engl.isSuccess) return@map engl.translation
                logger.info("str = {}", str)
                when (str) {
                    "Kapu-Riki" -> "Tapu Koko"
                    "Kapu-Toro" -> "Tapu Bulu"
                    "Kapu-Kime" -> "Tapu Fini"
                    else -> Command.getEnglName(str.split("-".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[0]) + "-" + str.split("-".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[1]
                }
            }.sorted().collect(
                Collectors.toCollection { LinkedList() }
            )
            if (s != "D") b.addColumn(
                "Tierliste [englisch]!${getAsXCoord((x shl 1) + 1)}5",
                mons
            ) else {
                val size = mons.size / 3
                for (i in 0..2) {
                    val col: MutableList<Any> = LinkedList()
                    for (j in 0 until size) {
                        col.add(mons.removeFirst())
                    }
                    b.addColumn("Tierliste [englisch]!${getAsXCoord((x++ shl 1) + 1)}5", col)
                }
            }
            x++
        }
        b.execute()
    }

    @PrivateCommand(name = "ndsdraft")
    fun ndsdraft(e: GenericCommandEvent) {
        Draft(e.jda.getTextChannelById(837425828245667841L)!!, "NDS", null, fromFile = true, isSwitchDraft = true)
    }

    @PrivateCommand(name = "ndsgeneratekilllist")
    @Throws(IOException::class)
    fun ndsgenerateKilllist() {
        val nds = Command.emolgaJSON.getJSONObject("drafts").getJSONObject("NDS")
        val picks = nds.getJSONObject("picks")
        val send: MutableList<List<Any>> = LinkedList()
        var x = 1001
        val l: MutableList<String> = ArrayList(15 * 12)
        for (s in picks.keySet()) {
            for (mon in Command.getPicksAsList(picks.getJSONArray(s))) {
                send.add(
                    listOf<Any>(
                        Command.getGen5Sprite(mon),
                        mon.uppercase(),
                        "=SUMME(S$x:AB$x)",
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0
                    )
                )
                l.add(mon)
                x++
            }
        }
        RequestBuilder.updateAll(nds.getString("sid"), "Killliste!P1001", send)
        Files.writeString(Paths.get("ndskilllistorder.txt"), java.lang.String.join("\n", l))
    }

    @PrivateCommand(name = "ndsteamsite")
    fun ndsTeamsite() {
        val nds = Command.emolgaJSON.getJSONObject("drafts").getJSONObject("NDS")
        val picks = nds.getJSONObject("picks")
        val teamnames = nds.getJSONObject("teamnames")
        val b = RequestBuilder(nds.getString("sid"))
        val clear: MutableList<List<Any>> = LinkedList()
        val temp: MutableList<Any> = LinkedList()
        for (j in 0..9) {
            temp.add(0)
        }
        for (i in 0..14) {
            clear.add(temp)
        }
        for (s in picks.keySet()) {
            val teamname = teamnames.getString(s)
            b.addColumn(
                "$teamname!A200",
                Command.getPicksAsList(picks.getJSONArray(s))
            )
            b.addAll("$teamname!B200", clear)
            b.addAll("$teamname!N200", clear)
            b.addSingle("$teamname!L199", 0)
            b.addSingle("$teamname!X199", 0)
            b.addRow("$teamname!B216", temp)
            b.addRow("$teamname!N216", temp)
        }
        b.execute()
    }

    @PrivateCommand(name = "ndsgameplanfix")
    fun ndsgameplanfix(e: GenericCommandEvent) {
        val tc = e.jda.getTextChannelById(837425772288540682L)
        for (m in tc!!.iterableHistory) {
            val msg = m.contentDisplay
            if (msg.contains("https://") || msg.contains("http://")) {
                msg.split("\n".toRegex()).asSequence()
                    .filter { s: String -> s.contains("https://replay.pokemonshowdown.com") || s.contains("http://florixserver.selfhost.eu:228/") }
                    .map { s: String ->
                        s.substring(
                            s.indexOf("http"),
                            if (s.indexOf(' ', s.indexOf("http") + 1) == -1) s.length else s.indexOf(
                                ' ',
                                s.indexOf("http") + 1
                            )
                        )
                    }
                    .firstOrNull()?.run {
                        logger.info(this)
                        Command.analyseReplay(this, null, e.jda.getTextChannelById(837425749770240001L)!!, m, null)
                    }
            }
            if (m.idLong == 944309573383245904L) break
        }
    }

    @PrivateCommand(name = "wooloogameplanfix")
    fun wooloogameplanfix(e: GenericCommandEvent) {
        val tc = e.jda.getTextChannelById(929686889332604969L)
        for (m in tc!!.iterableHistory) {
            val msg = m.contentDisplay
            if (msg.contains("https://") || msg.contains("http://")) {
                msg.split("\n".toRegex()).asSequence()
                    .filter { s: String -> s.contains("https://replay.pokemonshowdown.com") || s.contains("http://florixserver.selfhost.eu:228/") }
                    .map { s: String ->
                        s.substring(
                            s.indexOf("http"),
                            if (s.indexOf(' ', s.indexOf("http") + 1) == -1) s.length else s.indexOf(
                                ' ',
                                s.indexOf("http") + 1
                            )
                        )
                    }
                    .firstOrNull()?.run {
                        logger.info(this)
                        Command.analyseReplay(this, null, e.jda.getTextChannelById(929686912048975882L)!!, m, null)
                    }
            }
            if (m.idLong == 946505526060122112L) break
        }
    }

    @PrivateCommand(name = "ndsprepares2rrjson")
    fun prepareNDSJSON() {
        val nds = Command.emolgaJSON.getJSONObject("drafts").getJSONObject("NDS")
        val picks = nds.getJSONObject("picks")
        val tierorder = listOf("S", "A", "B", "C", "D")
        val mc = Comparator.comparing { o1: JSONObject -> tierorder.indexOf(o1.getString("tier")) }
            .thenComparing { o: JSONObject -> o.getString("name") }
        for (s in picks.keySet()) {
            picks.put(s, picks.getJSONList(s).stream().sorted(mc).collect(Collectors.toList()))
        }
        Command.saveEmolgaJSON()
    }

    @PrivateCommand(name = "ndsprepares2rrdoc")
    fun prepareNDSDoc() {
        val nds = Command.emolgaJSON.getJSONObject("drafts").getJSONObject("NDS")
        val picks = nds.getJSONObject("picks")
        val get: MutableList<List<List<Any>>?> = LinkedList()
        val sid = "1ZwYlgwA7opD6Gdc5KmpjYk5JsnEZq3dZet2nJxB0EWQ"
        for ((temp, s) in picks.keySet().withIndex()) {
            logger.info(MarkerFactory.getMarker("important"), "{} {}", temp, s)
            get.add(Google[sid, nds.getJSONObject("teamnames").getString(s) + "!B15:O29", true])
        }
        val builder = RequestBuilder(sid)
        for ((x, u) in picks.keySet().withIndex()) {
            //String u = "297010892678234114";
            //logger.info("o.get(u) = " + o.get(u));
            val range = nds.getJSONObject("teamnames").getString(u) + "!B15:O29"
            logger.info("u = $u")
            logger.info("range = $range")
            val comp = Comparator.comparing { l1: List<Any> -> l1[7].toString().toInt() }
                .reversed().thenComparing { l: List<Any> -> l[2].toString() }
            builder.addAll(
                range, get[x]!!
                    .stream().filter { n: List<Any> -> n[2] != "" }.sorted(comp).collect(Collectors.toList())
            )
        }
        builder.execute()
    }

    @PrivateCommand(name = "asls10fixswitches")
    fun asls10fixswitches(e: GenericCommandEvent) {
        val nds = Command.emolgaJSON.getJSONObject("drafts").getJSONObject("ASLS10L" + e.getArg(0))
        val picks = nds.getJSONObject("picks")
        val tierorder = listOf("S", "A", "B", "C", "D")
        val mc = Comparator.comparing { o1: JSONObject -> tierorder.indexOf(o1.getString("tier")) }
            .thenComparing { o: JSONObject -> o.getString("name") }
        val s = e.getArg(1)
        picks.put(s, picks.getJSONList(s).stream().sorted(mc).collect(Collectors.toList()))
        Command.saveEmolgaJSON()
    }

    @PrivateCommand(name = "ndscorrectpkmnnames")
    fun ndscorrektpkmnnames() {
        val nds = Command.emolgaJSON.getJSONObject("drafts").getJSONObject("NDS")
        val picks = nds.getJSONObject("picks")
        val table = nds.getStringList("table")
        val b = RequestBuilder(nds.getString("sid"))
        val teamnames = nds.getJSONObject("teamnames")
        for (s in table) {
            b.addColumn(
                "$s!A200",
                picks.getJSONList(Command.reverseGet(teamnames, s)).stream()
                    .map { o: JSONObject -> o.getString("name") }
                    .collect(Collectors.toList()))
        }
        b.execute()
    }

    @PrivateCommand(name = "asls10start")
    fun asls10startredraft() {
        val jda = EmolgaMain.emolgajda
        Draft(jda.getTextChannelById(938744915209359361L)!!, "ASLS10L1", null, fromFile = true, isSwitchDraft = true)
        Draft(jda.getTextChannelById(938745041403379743L)!!, "ASLS10L2", null, fromFile = true, isSwitchDraft = true)
        Draft(jda.getTextChannelById(938745240829968444L)!!, "ASLS10L3", null, fromFile = true, isSwitchDraft = true)
        Draft(jda.getTextChannelById(938745399819251713L)!!, "ASLS10L4", null, fromFile = true, isSwitchDraft = true)
        Draft(jda.getTextChannelById(938745673908645909L)!!, "ASLS10L5", null, fromFile = true, isSwitchDraft = true)
    }

    @PrivateCommand(name = "setupflorixcontrol")
    fun setupFlorixControl(e: GenericCommandEvent) {
        val jda = e.jda
        jda.getTextChannelById(964528154549055558L)!!.sendMessageEmbeds(
            EmbedBuilder().setTitle("FlorixControl").setColor(Color.CYAN).build()
        ).setActionRow(
            Button.success("florix;startserver", "Server starten").withEmoji(
                jda.getEmojiById(964570148692443196L)!!
            ),
            Button.secondary("florix;stopserver", "Server stoppen").withEmoji(
                jda.getEmojiById(964570147220254810L)!!
            ),
            Button.danger("florix;poweroff", "PowerOff").withEmoji(Emoji.fromUnicode("⚠️")),
            Button.primary("florix;status", "Status").withEmoji(Emoji.fromUnicode("ℹ️"))
        ).queue()
    }

    @PrivateCommand(name = "buildtabletest")
    fun buildTableTest(e: GenericCommandEvent) {
        e.reply(
            Command.buildTable(
                listOf(
                    listOf("Pascal", "David", "Jesse", "Felix", "Fundort", "Status"),
                    listOf("", "Rutena", "", "", "Route 2", "Team"),
                    listOf("", "Floette", "", "", "Illumina City", "Team"),
                    listOf("", "Garados", "", "", "Route 22", "Team"),
                    listOf("", "Togetic", "", "", "Route 5", "Team"),
                    listOf("", "Zirpeise", "", "", "Route 3", "Box")
                )
            )
        )
    }

    @PrivateCommand(name = "createki")
    fun createKI(e: GenericCommandEvent) {
        val jda = e.jda
        val tc = jda.getTextChannelById(e.getArg(0))
        jda.getCategoryById(EMOLGA_KI)!!.createTextChannel("${tc!!.name}-${tc.idLong}").queue()
        e.done()
    }

    @PrivateCommand(name = "deleteunusedreplaychannels")
    fun deleteUnused(e: GenericCommandEvent) {
        e.reply("Deleted: " + AnalysisManager.removeUnused())
    }

    @PrivateCommand(name = "dasorlol")
    @Throws(IOException::class)
    fun dasorLol() {
        val load = load("dasorfights.json")
        val fights = load.getJSONList("fights")
        for (f in fights) {
            val id = f.getString("id")
            if (id.contains("doubles")) continue
            val game = Analysis("https://replay.pokemonshowdown.com/$id", null).analyse()
            for (player in game) {
                if (Command.toUsername(player.nickname) == "dasor54") {
                    for (mon in player.mons) {
                        val monName = Command.getMonName(mon.pokemon, MYSERVER)
                        DasorUsageManager.addPokemon(monName)
                    }
                }
            }
        }
    }

    @PrivateCommand(name = "updateslashcommands")
    fun updateSlashCommands(e: GenericCommandEvent) {
        val jda = e.jda
        val map: MutableMap<Long, MutableList<SlashCommandData>> = HashMap()
        Command.commands.values.asSequence()
            .filter { it.isSlash }

        Command.commands.values.stream().filter { obj: Command -> obj.isSlash }
            .filter { c: Command -> c.slashGuilds.isNotEmpty() }.forEach { c: Command ->
                val dt = Commands.slash(c.name, c.help)
                val mainCmdArgs = c.argumentTemplate.arguments
                if (c.hasChildren()) {
                    val childCommands = c.childCommands
                    for (childCmd in childCommands.values) {
                        val scd = SubcommandData(childCmd.name, childCmd.help)
                        scd.addOptions(buildOptionData(childCmd.argumentTemplate.arguments))
                        dt.addSubcommands(scd)
                    }
                } else if (mainCmdArgs.isNotEmpty() && mainCmdArgs[0].type.asOptionType() == OptionType.SUB_COMMAND) {
                    dt.addSubcommands((mainCmdArgs[0].type as ArgumentManagerTemplate.Text).asSubCommandData())
                } else {
                    dt.addOptions(buildOptionData(mainCmdArgs))
                }
                for (slashGuild in c.slashGuilds) {
                    map.computeIfAbsent(slashGuild) { LinkedList() }.add(dt)
                }
            }
        for ((guild, value) in map) {
            jda.getGuildById(guild)!!.updateCommands().addCommands(value)
                .queue({ l ->
                    logger.info("guild = {}", guild)
                    logger.info(
                        "l = {}",
                        l.stream()
                            .map { it.name }
                            .collect(Collectors.joining(", "))
                    )
                }) { obj: Throwable -> obj.printStackTrace() }
        }
        e.done()
    }

    private fun buildOptionData(args: List<ArgumentManagerTemplate.Argument>): List<OptionData> {
        return args.stream().map { arg: ArgumentManagerTemplate.Argument ->
            val argType = arg.type
            OptionData(
                argType.asOptionType(),
                arg.name.lowercase().replace(" ", "-").replace(Regex("[^\\w-]"), ""),
                arg.helpmsg,
                !arg.isOptional,
                argType.hasAutoComplete()
            )
        }.toList()
    }

    @PrivateCommand(name = "checkadmin")
    fun checkAdmin(e: GenericCommandEvent) {
        e.reply(e.jda.getGuildById(e.getArg(0))!!.selfMember.hasPermission(Permission.ADMINISTRATOR).toString())
    }

    @PrivateCommand(name = "testdbspeed")
    fun testDBSpeed(e: GenericCommandEvent) {
        val l = System.nanoTime()
        TranslationsManager.getTranslation(e.getArg(0), false)
        e.reply((System.nanoTime() - l).toString())
    }

    @PrivateCommand(name = "buildenglishtierlist")
    fun buildEnglishTierlist(e: GenericCommandEvent) {
        val guild = e.getArg(0)
        val t = Tierlist.getByGuild(guild)!!
        val all: MutableList<String> = mutableListOf()
        for (s in t.order) {
            t.tierlist[s]!!.asSequence().map { str: String ->
                if (str.startsWith("M-")) {
                    if (str.endsWith("-X")) return@map "M-" + Command.getEnglName(
                        str.substring(
                            2,
                            str.length - 2
                        )
                    ) + "-X"
                    if (str.endsWith("-Y")) return@map "M-" + Command.getEnglName(
                        str.substring(
                            2,
                            str.length - 2
                        )
                    ) + "-Y"
                    return@map "M-" + Command.getEnglName(str.substring(2))
                }
                if (str.startsWith("A-")) return@map "A-" + Command.getEnglName(str.substring(2))
                if (str.startsWith("G-")) return@map "G-" + Command.getEnglName(str.substring(2))
                val engl = Command.getEnglNameWithType(str)
                if (engl.isSuccess) return@map engl.translation
                logger.info("str = {}", str)
                when (str) {
                    "Kapu-Riki" -> "Tapu Koko"
                    "Kapu-Toro" -> "Tapu Bulu"
                    "Kapu-Kime" -> "Tapu Fini"
                    "Kapu-Fala" -> "Tapu Lele"
                    "Furnifra" -> "Heatmor"
                    else -> Command.getEnglName(str.split("-".toRegex())[0]) + "-" + str.split("-".toRegex())[1]
                }
            }.forEach { all.add(it) }
        }
        val path = "Tierlists/$guild.json"
        val o = load(path)
        o.put("englishNames", all)
        save(o, path)
    }

    fun execute(message: Message) {
        val msg = message.contentRaw
        for (method in PrivateCommands::class.java.declaredMethods) {
            val a = method.getAnnotation(
                PrivateCommand::class.java
            ) ?: continue
            if (msg.lowercase(Locale.getDefault())
                    .startsWith("!" + a.name.lowercase(Locale.getDefault()) + " ") || msg.equals(
                    "!" + a.name,
                    ignoreCase = true
                ) || Arrays.stream(a.aliases).anyMatch { s: String ->
                    msg.startsWith(
                        "!$s "
                    ) || msg.equals("!$s", ignoreCase = true)
                }
            ) {
                Thread({
                    try {
                        if (method.parameterCount == 0) {
                            method.invoke(this)
                        } else method.invoke(this, PrivateCommandEvent(message))
                    } catch (e: IllegalAccessException) {
                        logger.error("PrivateCommand " + a.name, e)
                    } catch (e: InvocationTargetException) {
                        logger.error("PrivateCommand " + a.name, e)
                    }
                }, "PrivateCommand " + a.name).start()
            }
        }
    }
}