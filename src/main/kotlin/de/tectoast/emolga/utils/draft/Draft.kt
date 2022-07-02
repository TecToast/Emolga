package de.tectoast.emolga.utils.draft

import de.tectoast.emolga.bot.EmolgaMain.emolgajda
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.draft.Draft
import de.tectoast.jsolf.JSONArray
import de.tectoast.jsolf.JSONObject
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.regex.Pattern
import java.util.stream.Collectors

class Draft @JvmOverloads constructor(
    val tc: TextChannel,
    val name: String,
    tcid: String?,
    fromFile: Boolean,
    val isSwitchDraft: Boolean = false,
) {
    val picks: MutableMap<Long, MutableList<DraftPokemon>> = mutableMapOf()
    val order: MutableMap<Int, MutableList<Long>> = mutableMapOf()
    val originalOrder: MutableMap<Int, List<Long>> = mutableMapOf()
    val points: MutableMap<Long, Int> = mutableMapOf()
    private val afterDraft: List<Long> = ArrayList()
    val guild: String
    val isPointBased: Boolean
    val scheduler: ScheduledExecutorService = ScheduledThreadPoolExecutor(2)
    lateinit var members: List<Long>
    var current: Long = 0
    var round = 0
    var cooldown: ScheduledFuture<*>? = null
    private var ts: TextChannel? = null
    var ended = false
    val tierlist: Tierlist by Tierlist.Delegate()

    init {
        val json = Command.emolgaJSON
        val drafts = json.getJSONObject("drafts")
        val aslpattern = Pattern.compile("^S\\d")
        val league = if (aslpattern.matcher(name).find()) drafts.getJSONObject("ASLS9")
            .getJSONObject(name) else drafts.getJSONObject(
            name
        )
        guild = if (league.has("guild")) league.getString("guild") else tc.guild.id
        isPointBased = tierlist.isPointBased
        logger.info("isPointBased = $isPointBased")
        val o = league.getJSONObject("order")
        Thread({
            logger.info(name)
            for (i in o.keySet()) {
                val arr = o.getJSONArray(i)
                val list: List<Long> = if (arr[0] is JSONArray) {
                    arr.toLongListList().stream().map { l: List<Long?> -> l[0] }.collect(
                        Collectors.toCollection { mutableListOf() }
                    )
                } else {
                    arr.toLongList()
                }
                order[Integer.valueOf(i)] = list.toMutableList()
                originalOrder[Integer.valueOf(i)] = ArrayList(list)
            }
            logger.info("order = $order")
            members = ArrayList(order[1]!!)
            ts = if (tcid != null) tc.guild.getTextChannelById(tcid) else null
            if (!fromFile && !isSwitchDraft) {
                round++
                league.put("skippedturns", JSONObject())
                for (member in members) {
                    picks[member] = ArrayList()
                    points[member] = tierlist.points
                }
                if (ts != null) {
                    for (member in members) {
                        ts!!.sendMessage("**<@$member>:**").queue()
                    }
                }
                current = order[1]!!.removeAt(0)
                cooldown =
                    scheduler.schedule({ timer() }, Command.calculateDraftTimer(), TimeUnit.MILLISECONDS)
                tc.sendMessage("Runde $round!").queue()
                if (isPointBased) tc.sendMessage(getMention(current) + " ist dran! (" + points[current] + " mögliche Punkte)")
                    .queue() else tc.sendMessage(
                    getMention(current) + " ist dran! (Mögliche Tiers: " + getPossibleTiersAsString(
                        current
                    ) + ")"
                ).complete().id
                Command.saveEmolgaJSON()
            } else {
                if (isSwitchDraft && !fromFile) {
                    round++
                    current = order[1]!!.removeAt(0)
                    val pick = league.getJSONObject("picks")
                    for (member in members) {
                        logger.info("member = $member")
                        if (pick.has(member)) {
                            val arr = pick.getJSONArray(member)
                            val list = ArrayList<DraftPokemon>()
                            for (ob in arr) {
                                val obj = ob as JSONObject
                                list.add(DraftPokemon(obj.getString("name"), obj.getString("tier")))
                            }
                            picks[member] = list
                        } else {
                            picks[member] = ArrayList()
                        }
                        if (isPointBased) {
                            points[member] = tierlist.points
                            for (mon in picks[member]!!) {
                                points[member] = points[member]!! - tierlist.prices[mon.tier]!!
                            }
                        }
                    }
                    logger.info("For finished")
                    val delay = Command.calculateDraftTimer()
                    league.put("cooldown", System.currentTimeMillis() + delay)
                    cooldown = scheduler.schedule({ timer() }, delay, TimeUnit.MILLISECONDS)
                    logger.info("Before send")
                    tc.sendMessage("Runde $round!").queue()
                    if (isPointBased) tc.sendMessage(getMention(current) + " ist dran! (" + points[current] + " mögliche Punkte)")
                        .queue() else tc.sendMessage(
                        getMention(current) + " ist dran! (Mögliche Tiers: " + getPossibleTiersAsString(
                            current
                        ) + ")"
                    ).complete().id
                    Command.saveEmolgaJSON()
                } else {
                    round = league.optInt("round", 1)
                    current = league.getLong("current")
                    var x = 0
                    for (member in order[round]!!) {
                        x++
                        if (current == member) break
                    }
                    if (x > 0) {
                        order[round]!!.subList(0, x).clear()
                    }
                    if (league.has("finished")) {
                        listOf(*league.getString("finished").split(",".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()).forEach(
                            Consumer { s: String -> order.values.forEach(Consumer { l: MutableList<Long> -> l.removeIf { me: Long -> me == s.toLong() } }) })
                    }
                    logger.info("order.size() = " + order[round]!!.size)
                    val pick = league.getJSONObject("picks")
                    for (member in members) {
                        if (pick.has(member)) {
                            val arr = pick.getJSONArray(member)
                            val list = ArrayList<DraftPokemon>()
                            for (ob in arr) {
                                val obj = ob as JSONObject
                                list.add(DraftPokemon(obj.getString("name"), obj.getString("tier")))
                            }
                            picks[member] = list
                        } else {
                            picks[member] = ArrayList()
                        }
                        if (isPointBased) {
                            points[member] = tierlist.points
                            for (mon in picks[member]!!) {
                                val t = tierlist
                                if (t.prices[mon.tier] == null) {
                                    logger.info(mon.name + " ERROR " + mon.tier)
                                }
                                points[member] = points[member]!! - t.prices[mon.tier]!!
                            }
                        }
                    }
                    val delay: Long = if (league.has("cooldown")) {
                        league.getLong("cooldown") - System.currentTimeMillis()
                    } else {
                        Command.calculateDraftTimer()
                    }
                    if (!league.has("cooldown")) league.put("cooldown", System.currentTimeMillis() + delay)
                    cooldown = scheduler.schedule({ timer() }, delay, TimeUnit.MILLISECONDS)
                    Command.saveEmolgaJSON()
                    //sendToMe("Aufgesetzt! " + delay);
                }
            }
            Companion.drafts.add(this)
            logger.info("Initialized Draft $name !")
        }, "CreateDraft $name").start()
    }

    fun nextPlayer(tco: TextChannel, tierlist: Tierlist, league: JSONObject) {
        if (order[round]!!.size == 0) {
            if (round == tierlist.rounds) {
                tco.sendMessage("Der Draft ist vorbei!").queue()
                ended = true
                //ndsdoc(tierlist, pokemon, d, mem, tier, round);
                //aslCoachDoc(tierlist, pokemon, d, mem, needed, round, null);
                if (afterDraft.isNotEmpty()) tco.sendMessage(
                    """
    Reihenfolge zum Nachdraften:
    ${afterDraft.stream().map { mem: Long -> getMention(mem) }.collect(Collectors.joining("\n"))}
    """.trimIndent()
                ).queue()
                Command.saveEmolgaJSON()
                drafts.remove(this)
                return
            }
            round++
            tc.sendMessage("Runde $round!").queue()
            league.put("round", round)
        }
        val normal =  /*round != 12 || picks.get(current.getIdLong()).size() == tierlist.rounds;*/true
        if (normal) {
            current = order[round]!!.removeAt(0)
            league.put("current", current)
            cooldown!!.cancel(false)
        }
        league.getJSONObject("picks").put(current, getTeamAsArray(current))
        if (isPointBased) //tco.sendMessage(getMention(current) + " (<@&" + asl.getLongList("roleids").get(getIndex(current.getIdLong())) + ">) ist dran! (" + points.get(current.getIdLong()) + " mögliche Punkte)").queue();
            tco.sendMessage(getMention(current) + " ist dran! (" + points[current] + " mögliche Punkte)")
                .queue() else tco.sendMessage(
            getMention(current) + " ist dran! (Mögliche Tiers: " + getPossibleTiersAsString(
                current
            ) + ")"
        ).queue()
        if (normal) {
            val delay = Command.calculateDraftTimer()
            league.put("cooldown", System.currentTimeMillis() + delay)
            cooldown = scheduler.schedule({ timer() }, delay, TimeUnit.MILLISECONDS)
        }
        Command.saveEmolgaJSON()
    }

    fun getMention(mem: Long): String {
        val league = league
        if (league.has("mentions")) {
            val mentions = league.getJSONObject("mentions")
            if (mentions.has(mem)) return "<@" + mentions.getString(mem) + ">"
        }
        return "<@$mem>"
    }

    val league: JSONObject
        get() {
            val drafts = Command.emolgaJSON.getJSONObject("drafts")
            val aslpattern = Pattern.compile("^S\\d")
            return if (aslpattern.matcher(name).find()) drafts.getJSONObject("ASLS9")
                .getJSONObject(name) else drafts.getJSONObject(
                name
            )
        }

    fun isNotCurrent(mem: Long): Boolean {
        if (current == mem) return false
        if (league.has("table1") && league.getJSONArray("table1").toLongListList().stream()
                .anyMatch { l: List<Long?> -> l.contains(current) && l.contains(mem) }
        ) return false
        if (league.has("table2") && league.getJSONArray("table2").toLongListList().stream()
                .anyMatch { l: List<Long?> -> l.contains(current) && l.contains(mem) }
        ) return false
        if (mem == Constants.FLOID) return false
        //if (getTeamMembers(mem.getIdLong()).contains(current.getIdLong())) return false;
        val league = league
        return if (!league.has("allowed")) true else league.getJSONObject("allowed")
            .optLong(mem.toString(), -1) != current
    }

    fun hasMega(mem: Long): Boolean {
        return picks[mem]!!.stream().anyMatch { mon: DraftPokemon -> mon.name.startsWith("M-") }
    }

    fun hasInAnotherForm(mem: Long, pokemon: String): Boolean {
        val regex = Regex("-")
        val split = pokemon.split(regex)
        return picks[mem]!!.stream()
            .anyMatch { split[0] == it.name.split(regex)[0] && !(split[0] == "Porygon" && it.name.split(regex)[0] == "Porygon") }
    }

    fun isPicked(pokemon: String?): Boolean {
        return picks.values.stream().flatMap { obj: List<DraftPokemon> -> obj.stream() }
            .anyMatch { mon: DraftPokemon -> mon.name.equals(pokemon, ignoreCase = true) }
    }

    fun isPickedBy(oldmon: String?, mem: Long): Boolean {
        return picks[mem]!!.stream().anyMatch { dp: DraftPokemon -> dp.name.equals(oldmon, ignoreCase = true) }
    }

    @JvmOverloads
    fun timer(tr: TimerReason = TimerReason.REALTIMER) {
        if (ended) return
        val drafts = Command.emolgaJSON.getJSONObject("drafts")
        val league = drafts.getJSONObject(name)
        if (!league.has("skippedturns")) league.put("skippedturns", JSONObject())
        val st = league.getJSONObject("skippedturns")
        val rounds = tierlist.rounds
        st.put(current, st.createOrGetArray(current).put(round))
        if (order[round]!!.size == 0) {
            if (round == rounds) {
                Command.saveEmolgaJSON()
                tc.sendMessage("Der Draft ist vorbei!").queue()
                Companion.drafts.remove(this)
                return
            }
            round++
            tc.sendMessage("Runde $round!").queue()
        }
        /*String msg = tr == TimerReason.REALTIMER ? "**" + current.getEffectiveName() + "** war zu langsam und deshalb ist jetzt " + getMention(order.get(round).get(0)) + " (<@&" + asl.getLongList("roleids").get(getIndex(order.get(round).get(0).getIdLong())) + ">) dran! "
                : "Der Pick von " + current.getEffectiveName() + " wurde " + (isSwitchDraft ? "geskippt" : "verschoben") + " und deshalb ist jetzt " + getMention(order.get(round).get(0)) + " dran!";*/
        var msg = if (tr == TimerReason.REALTIMER) "**<@$current>** war zu langsam und deshalb ist jetzt " + getMention(
            order[round]!![0]
        ) + " dran! " else "Der Pick von <@" + current + "> wurde " + (if (isSwitchDraft) "geskippt" else "verschoben") + " und deshalb ist jetzt " + getMention(
            order[round]!![0]
        ) + " dran!"
        current = order[round]!!.removeAt(0)
        league.put("current", current)
        msg += if (isPointBased) "(" + points[current] + " mögliche Punkte)" else "(Mögliche Tiers: " + getPossibleTiersAsString(
            current
        ) + ")"
        tc.sendMessage(msg).queue()
        cooldown!!.cancel(false)
        val delay = Command.calculateDraftTimer()
        league.put("cooldown", System.currentTimeMillis() + delay)
        Command.saveEmolgaJSON()
        cooldown = scheduler.schedule({ timer() }, delay, TimeUnit.MILLISECONDS)
    }

    fun getTeamAsArray(mem: Long): JSONArray {
        val arr = JSONArray()
        for (mon in picks[mem]!!) {
            val obj = JSONObject()
            obj.put("name", mon.name)
            obj.put("tier", mon.tier)
            arr.put(obj)
        }
        return arr
    }

    /*public static String getTeamMsgFromString(Guild g, String mem, String str) {
        Member member = g.retrieveMemberById(mem).complete();
        StringBuilder msg = new StringBuilder("**" + member.getEffectiveName() + "**\n");
        Tierlist tierlist = Tierlist.getByGuild(g.getId());
        for (String o : tierlist.order) {
            ArrayList<DraftPokemon> mons = Arrays.stream(str.split(",")).filter(s -> s.tier.equals(o)).sorted(Comparator.comparing(o2 -> o2.name)).collect(Collectors.toCollection(ArrayList::new));
            for (DraftPokemon mon : mons) {
                msg.append(o).append(": ").append(mon.name).append("\n");
            }
        }
        return msg.toString();
    }*/
    fun getPossibleTiers(mem: Long): Map<String?, Int?> {
        val possible: MutableMap<String?, Int?> = HashMap(tierlist.prices)
        for (mon in picks[mem]!!) {
            possible[mon.tier] = possible[mon.tier]!! - 1
        }
        return possible
    }

    fun getPossibleTiersAsString(mem: Long): String {
        val possible = getPossibleTiers(mem)
        val list = ArrayList<String>()
        for (s in tierlist.order) {
            if (possible[s] == 0) continue
            list.add(possible[s].toString() + "x **" + s + "**")
        }
        return java.lang.String.join(", ", list)
    }

    enum class TimerReason {
        REALTIMER, SKIP
    }

    companion object {
        val drafts: MutableList<Draft> = ArrayList()
        private val logger = LoggerFactory.getLogger(Draft::class.java)

        @JvmStatic
        fun doMatchUps(gameday: String?) {
            val nds = Command.emolgaJSON.getJSONObject("drafts").getJSONObject("NDS")
            val teamnames = nds.getJSONObject("teamnames")
            val battleorder = nds.getJSONObject("battleorder").getString(gameday)
            val b = RequestBuilder(nds.getString("sid"))
            for (battle in battleorder.split(";".toRegex())) {
                val users = battle.split(":".toRegex())
                for (index in 0..1) {
                    val team = teamnames.getString(users[index])
                    val oppo = teamnames.getString(users[1 - index])
                    b.addSingle("%s!B18".formatted(team), "={'%s'!B16:AE16}".formatted(oppo))
                    b.addSingle("%s!B19".formatted(team), "={'%s'!B15:AE15}".formatted(oppo))
                    b.addSingle("%s!B21".formatted(team), "={'%s'!B14:AF14}".formatted(oppo))
                    b.addColumn(
                        "%s!A18".formatted(team), listOf<Any>(
                            "='%s'!Y2".formatted(oppo),
                            "='%s'!B2".formatted(oppo)
                        )
                    )
                }
            }
            b
                .withRunnable {
                    emolgajda.getTextChannelById(837425690844201000L)!!.sendMessage(
                        "Jo, kurzer Reminder, die Matchups des nächsten Spieltages sind im Doc, vergesst das Nominieren nicht :)\n<@&856205147754201108>"
                    ).queue()
                }
                .execute()
        }

        @JvmStatic
        @JvmOverloads
        fun doNDSNominate(prevDay: Boolean = false) {
            val nds = Command.emolgaJSON.getJSONObject("drafts").getJSONObject("NDS")
            val nom = nds.getJSONObject("nominations")
            val teamnames = nds.getJSONObject("teamnames")
            val table = nds.getStringList("table")
            var cday = nom.getInt("currentDay")
            if (prevDay) cday--
            val o = nom.getJSONObject(cday.toString())
            val picks = nds.getJSONObject("picks")
            val sid = nds.getString("sid")
            val b = RequestBuilder(sid)
            val tiers = listOf("S", "A", "B")
            for (u in picks.keySet()) {
                //String u = "297010892678234114";
                if (!o.has(u)) {
                    if (cday == 1) {
                        val mons = picks.getJSONList(u)
                        val comp = Comparator.comparing { pk: JSONObject -> tiers.indexOf(pk.getString("tier")) }
                            .thenComparing { pk: JSONObject -> pk.getString("name") }
                        o.put(u, mons.stream().sorted(comp).limit(11).map { obj: JSONObject -> obj.getString("name") }
                            .collect(Collectors.joining(";")) + "###"
                                + mons.stream().sorted(comp).skip(11).map { obj: JSONObject -> obj.getString("name") }
                            .collect(Collectors.joining(";")))
                    } else {
                        o.put(u, nom.getJSONObject((cday - 1).toString()).getString(u))
                    }
                }
                //logger.info("o.get(u) = " + o.get(u));
                val str = o.getString(u)
                val mons: MutableList<String> =
                    Arrays.stream(str.replace("###", ";").split(";".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()).collect(Collectors.toList())
                logger.info("mons = $mons")
                logger.info("u = $u")
                val index = table.indexOf(teamnames.getString(u))
                b.addColumn("Data!F%d".formatted(index * 17 + 2), mons)
            }
            b.withRunnable {
                emolgajda.getTextChannelById(837425690844201000L)!!
                    .sendMessage(
                        """
                Jo ihr alten Zipfelklatscher! Eure jämmerlichen Versuche eine Liga zu gewinnen werden scheitern ihr Arschgeigen, da ihr zu inkompetent seid euch zu merken, wann alles nach meiner Schwerstarbeit automatisch eingetragen wird. Daher erinnere ich euer Erbsenhirn mithilfe dieser noch nett formulierten Nachricht daran, dass ihr nun anfangen könnt zu builden. Dann bis nächste Woche Mittwoch!
                PS: Bannt Henny, der Typ ist broken! Und gebt ihm keinen Gehstocktänzer!

                _written by Henny_""".trimIndent()
                    )
                    .queue()
            }
                .execute()
            if (!prevDay) nom.increment("currentDay")
            Command.saveEmolgaJSON()
        }

        fun doNDSPredictionGame() {
            val json = Command.emolgaJSON
            val league = json.getJSONObject("drafts").getJSONObject("NDS")
            val lastDay = league.getInt("lastDay")
            val gameday = lastDay + 1
            league.put("lastDay", lastDay + 1)
            val tc: TextChannel = emolgajda.getTextChannelById(844806306027929610L)!!
            tc.sendMessage("**Spieltag $gameday:**").queue()
            val bo = league.getJSONObject("battleorder").getString(gameday.toString())
            val g: Guild = emolgajda.getGuildById(Constants.NDSID)!!
            for (str in bo.split(";".toRegex())) {
                val split = str.split(":".toRegex())
                val u1 = split[0]
                val u2 = split[1]
                logger.info("u1 = $u1")
                logger.info("u2 = $u2")
                val teamnames = league.getJSONObject("teamnames")
                val t1 = teamnames.getString(u1)
                val t2 = teamnames.getString(u2)
                logger.info("t1 = $t1")
                logger.info("t2 = $t2")
                val e1 = g.getEmojisByName(Command.toSDName(t1), true)[0]
                val e2 = g.getEmojisByName(Command.toSDName(t2), true)[0]
                //logger.info("<@" + u1 + "> (" + e1.getAsMention() + ") vs. <@" + u2 + "> (" + e2.getAsMention() + ")");
                tc.sendMessage("<@" + u1 + "> (" + e1.asMention + ") vs. <@" + u2 + "> (" + e2.asMention + ")")
                    .queue { m: Message ->
                        m.addReaction(e1).queue()
                        m.addReaction(e2).queue()
                    }
            }
        }

        fun init() {
            Tierlist.setup()
        }

        fun getDraftByMember(member: Long, tco: TextChannel): Draft? {
            //logger.info("member.getId() = " + member.getId());
            for (draft in drafts) {
                //logger.info(draft.members.stream().map(mem -> mem.getId() + ":" + mem.getEffectiveName()).collect(Collectors.joining("\n")));
                if (draft.tc.id != tco.id) continue
                if (member == Constants.FLOID) return draft
                if (draft.members.stream().anyMatch { mem: Long -> mem == member }) return draft
                if (draft.league.has("table1") && draft.league.getJSONArray("table1").toLongListList().stream()
                        .anyMatch { l: List<Long?> -> l.contains(member) }
                ) return draft
                if (draft.league.has("table2") && draft.league.getJSONArray("table2").toLongListList().stream()
                        .anyMatch { l: List<Long?> -> l.contains(member) }
                ) return draft
                /*if (getTeamMembers(member.getIdLong()).stream().anyMatch(l -> draft.members.stream().anyMatch(mem -> mem.getIdLong() == l)))
                return draft;*/
                val league = draft.league
                if (league.has("allowed")) {
                    val allowed = league.getJSONObject("allowed")
                    if (allowed.has(member)) return draft
                }
            }
            return null
        }

        /*public static boolean isDraftIn(TextChannel tc) {
        return drafts.stream().anyMatch(d -> d.tc.getId().equals(tc.getId()));
    }*/
        fun getDraftByChannel(tc: TextChannel): Draft? {
            for (draft in drafts) {
                if (draft.tc.id == tc.id) return draft
            }
            return null
        }

        fun getTeamMembers(userid: Long): List<Long> {
            val asl = Command.emolgaJSON.getJSONObject("drafts").getJSONObject("ASLS9")
            val index = getIndex(userid)
            val l = LinkedList<Long>()
            if (index == -1) return l
            for (i in 1..4) {
                l.add(asl.getJSONObject("S$i").getLongList("table")[index])
            }
            return l
        }

        fun getTeamMembers(team: String?): List<Long> {
            val asl = Command.emolgaJSON.getJSONObject("drafts").getJSONObject("ASLS9")
            val index = asl.getStringList("teams").indexOf(team)
            val l = LinkedList<Long>()
            if (index == -1) return l
            for (i in 1..4) {
                l.add(asl.getJSONObject("S$i").getLongList("table")[index])
            }
            return l
        }

        fun getTeamName(userid: Long): String {
            return Command.emolgaJSON.getJSONObject("drafts").getJSONObject("ASLS9")
                .getStringList("teams")[getIndex(userid)]
        }

        private fun getLevelJSON(userid: Long): JSONObject {
            return Command.emolgaJSON.getJSONObject("drafts").getJSONObject("ASLS9")
                .getJSONObject("S" + getLevel(userid))
        }

        fun getLevel(userid: Long): Int {
            val asl = Command.emolgaJSON.getJSONObject("drafts").getJSONObject("ASLS9")
            for (i in 1..4) {
                val o = asl.getJSONObject("S$i")
                if (o.getLongList("table").contains(userid)) {
                    return i
                }
            }
            return -1
        }

        fun getIndex(userid: Long): Int {
            val levelJSON = getLevelJSON(userid)
            return levelJSON.getLongList("table").indexOf(userid)
        }

        fun getLeagueStatic(name: String): JSONObject {
            val drafts = Command.emolgaJSON.getJSONObject("drafts")
            val aslpattern = Pattern.compile("^S\\d")
            return if (aslpattern.matcher(name).find()) drafts.getJSONObject("ASLS9")
                .getJSONObject(name) else drafts.getJSONObject(name)
        }
    }
}