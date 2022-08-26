package de.tectoast.emolga.commands

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.commands.Command.ArgumentManagerTemplate
import de.tectoast.emolga.commands.Command.Companion.getAsXCoord
import de.tectoast.emolga.commands.Command.Companion.getDataObject
import de.tectoast.emolga.commands.Command.Companion.load
import de.tectoast.emolga.commands.Command.Companion.save
import de.tectoast.emolga.commands.Command.Translation
import de.tectoast.emolga.database.Database
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.Constants.EMOLGA_KI
import de.tectoast.emolga.utils.Google
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.annotations.PrivateCommand
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.json.Emolga
import de.tectoast.emolga.utils.json.emolga.draft.ASL
import de.tectoast.emolga.utils.showdown.Analysis
import de.tectoast.emolga.utils.sql.managers.AnalysisManager
import de.tectoast.emolga.utils.sql.managers.DasorUsageManager
import de.tectoast.emolga.utils.sql.managers.TranslationsManager
import de.tectoast.jsolf.JSONArray
import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.AudioChannel
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
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
import java.util.function.Consumer
import java.util.regex.Pattern
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation

object PrivateCommands {
    private val logger = LoggerFactory.getLogger(PrivateCommands::class.java)
    private val DOUBLE_BACKSLASH = Pattern.compile("\\\\")

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
    suspend fun send(e: GenericCommandEvent) {
        val message = e.message
        logger.info(message!!.contentRaw)
        var s = DOUBLE_BACKSLASH.matcher(message.contentRaw.substring(24)).replaceAll("")
        val tc = e.jda.getTextChannelById(e.getArg(0))
        val g = tc!!.guild
        for (emote in g.retrieveEmojis().await()) {
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
    suspend fun react(e: GenericCommandEvent) {
        val msg = e.msg
        var s = msg!!.substring(45)
        val tc = e.jda.getTextChannelById(e.getArg(0))
        val m = tc!!.retrieveMessageById(e.getArg(1)).await()!!
        if (s.contains("<")) {
            s = s.substring(1)
            logger.info("s = $s")
            val finalS = s
            tc.guild.retrieveEmojis().await().filter { it.name.equals(finalS, ignoreCase = true) }
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
        Command.loadEmolgaJSON()
        e.done()
    }

    @PrivateCommand(name = "del")
    fun del(e: GenericCommandEvent) {
        e.jda.getTextChannelById(e.getArg(0))?.deleteMessageById(e.getArg(1))?.queue()
    }

    @PrivateCommand(name = "troll")
    suspend fun troll(e: GenericCommandEvent) {
        val category = e.jda.getCategoryById(e.getArg(0))
        val g = category!!.guild
        val user = g.retrieveMemberById(e.getArg(1)).await()
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

    @PrivateCommand(name = "saveemolgajson")
    fun saveEmolga(e: GenericCommandEvent) {
        saveEmolgaJSON()
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
    suspend fun inviteMe(e: GenericCommandEvent) {
        coroutineScope {
            for (guild in e.jda.guilds) {
                launch {
                    try {
                        guild.retrieveMemberById(Constants.FLOID).await()
                    } catch (exception: Exception) {
                        Command.sendToMe(guild.textChannels[0].createInvite().await().url)
                    }
                }
            }
        }
    }

    @PrivateCommand(name = "removeduplicates")
    fun removeDuplicates() {
        TranslationsManager.removeDuplicates()
    }

    @PrivateCommand(name = "ndsnominate")
    fun ndsNominate(e: GenericCommandEvent) {
        Command.doNDSNominate(e.getArg(0).toBooleanStrict())
    }

    @PrivateCommand(name = "matchups")
    fun matchUps(e: GenericCommandEvent) {
        Command.doMatchUps(e.getArg(0).toInt())
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
        val b = /*RequestBuilder("1wI291CWkKkWqQhY_KNu7GVfdincRz74omnCOvEVTDrc").withAdditionalSheets(
            "1tFLd9Atl9QpMgCQBclpeU1WlMqSRGMeX8COUVDIf4TU",
            "1A040AYoiqTus1wSq_3CXgZpgcY3ECpphVRJWHmXyxsQ",
            "1p8DSvd3vS5s5z-1UGPjKUhFVskYjQyrn-HbvU0pb5WE",
            "1nEJvV5UESfuJvsJplXi_RXXnq9lY2vD5NyrTF3ObcvU"
        )*/ RequestBuilder("1VWjAc370NQvuybaQZOTQ2uBVGC36D2_n63DOghkoE2k")
        var x = 0
        for (s in t!!.order) {
            val mons = t.tierlist[s]!!.map { str: String ->
                if (str.startsWith("M-")) {
                    if (str.endsWith("-X")) return@map "M-" + Command.getEnglName(
                        str.substring(
                            2, str.length - 2
                        )
                    ) + "-X"
                    if (str.endsWith("-Y")) return@map "M-" + Command.getEnglName(
                        str.substring(
                            2, str.length - 2
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
                    else -> Command.getEnglName(str.split("-").dropLastWhile { it.isEmpty() }
                        .toTypedArray()[0]) + "-" + str.split("-").dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                }
            }.sorted().toMutableList()
            if (s != "D") b.addColumn(
                "Tierliste [englisch]!${getAsXCoord((x shl 1) + 1)}5", mons
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

    @PrivateCommand(name = "ndsteamsite")
    fun ndsTeamsite() {
        val nds = Emolga.get.nds()
        val picks = nds.picks
        val teamnames = nds.teamnames
        val b = RequestBuilder(nds.sid)
        val clear: MutableList<List<Any>> = LinkedList()
        val temp: MutableList<Any> = LinkedList()
        for (j in 0..9) {
            temp.add(0)
        }
        for (i in 0..14) {
            clear.add(temp)
        }
        for (s in picks.keys) {
            val teamname = teamnames[s]
            b.addColumn("$teamname!A200", picks[s]!!.map { it.name })
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
                msg.split("\n").asSequence()
                    .filter { s: String -> s.contains("https://replay.pokemonshowdown.com") || s.contains("http://florixserver.selfhost.eu:228/") }
                    .map { s: String ->
                        s.substring(
                            s.indexOf("http"), if (s.indexOf(' ', s.indexOf("http") + 1) == -1) s.length else s.indexOf(
                                ' ', s.indexOf("http") + 1
                            )
                        )
                    }.firstOrNull()?.run {
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
                msg.split("\n").asSequence()
                    .filter { s: String -> s.contains("https://replay.pokemonshowdown.com") || s.contains("http://florixserver.selfhost.eu:228/") }
                    .map { s: String ->
                        s.substring(
                            s.indexOf("http"), if (s.indexOf(' ', s.indexOf("http") + 1) == -1) s.length else s.indexOf(
                                ' ', s.indexOf("http") + 1
                            )
                        )
                    }.firstOrNull()?.run {
                        logger.info(this)
                        Command.analyseReplay(this, null, e.jda.getTextChannelById(929686912048975882L)!!, m, null)
                    }
            }
            if (m.idLong == 946505526060122112L) break
        }
    }

    @PrivateCommand(name = "ndsprepares2rrjson")
    fun prepareNDSJSON() {
        val nds = Emolga.get.nds()
        val picks = nds.picks
        val tierorder = listOf("S", "A", "B", "C", "D")
        for (s in picks.keys) {
            picks[s]!!.sortWith(compareBy({ it.tier.indexedBy(tierorder) }, { it.name }))
        }
        saveEmolgaJSON()
    }

    @PrivateCommand(name = "ndsprepares2rrdoc")
    fun prepareNDSDoc() {
        val nds = Emolga.get.nds()
        val picks = nds.picks
        val get: MutableList<List<List<Any>>?> = LinkedList()
        val sid = "1ZwYlgwA7opD6Gdc5KmpjYk5JsnEZq3dZet2nJxB0EWQ"
        for ((temp, s) in picks.keys.withIndex()) {
            logger.info(MarkerFactory.getMarker("important"), "{} {}", temp, s)
            get.add(Google[sid, nds.teamnames[s] + "!B15:O29", true])
        }
        val builder = RequestBuilder(sid)
        for ((x, u) in picks.keys.withIndex()) {
            //String u = "297010892678234114";
            //logger.info("o.get(u) = " + o.get(u));
            val range = nds.teamnames[u] + "!B15:O29"
            logger.info("u = $u")
            logger.info("range = $range")
            val comp = Comparator.comparing { l1: List<Any> -> l1[7].toString().toInt() }.reversed()
                .thenComparing { l: List<Any> -> l[2].toString() }
            builder.addAll(
                range, get[x]!!.filter { n: List<Any> -> n[2] != "" }.sortedWith(comp)
            )
        }
        builder.execute()
    }

    @PrivateCommand(name = "asls10fixswitches")
    fun asls10fixswitches(e: GenericCommandEvent) {
        val league = Emolga.get.league("ASLS10L${e.getArg(0)}")
        val picks = league.picks
        val tierorder = listOf("S", "A", "B", "C", "D")
        val s = e.getArg(1)
        picks[s.toLong()]!!.sortWith(compareBy({ it.tier.indexedBy(tierorder) }, { it.name }))
        saveEmolgaJSON()
    }

    @PrivateCommand(name = "ndscorrectpkmnnames")
    fun ndscorrektpkmnnames() {
        val nds = Emolga.get.nds()
        val picks = nds.picks
        val table = nds.teamtable
        val b = RequestBuilder(nds.sid)
        val teamnames = nds.teamnames
        for (s in table) {
            b.addColumn("$s!A200", picks[teamnames.reverseGet(s)!!.toLong()]!!.map { it.name })
        }
        b.execute()
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
    suspend fun dasorLol() {
        val load = load("dasorfights.json")
        val fights = load.getJSONList("fights")
        for (f in fights) {
            val id = f.getString("id")
            if (id.contains("doubles")) continue
            val game = Analysis("https://replay.pokemonshowdown.com/$id", null).analyse(null)
            for (player in game) {
                if (Command.toUsername(player.nickname) == "dasor54") {
                    for (mon in player.mons) {
                        val monName = Command.getMonName(mon.pokemon, Constants.G.MY)
                        DasorUsageManager.addPokemon(monName)
                    }
                }
            }
        }
    }

    @PrivateCommand(name = "updateslashcommands")
    fun updateSlashCommands() {
        val jda = EmolgaMain.emolgajda
        val map: MutableMap<Long, MutableList<SlashCommandData>> = HashMap()
        Command.commands.values.filter { it.isSlash }.distinct().filter { it.slashGuilds.isNotEmpty() }.forEach {
            val dt = Commands.slash(it.name, it.help)
            if (it.category!!.isAdmin) dt.defaultPermissions = DefaultMemberPermissions.DISABLED
            logger.info(it.name)
            val mainCmdArgs = it.argumentTemplate.arguments
            if (it.hasChildren()) {
                val childCommands = it.childCommands
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
            for (slashGuild in it.slashGuilds) {
                map.computeIfAbsent(slashGuild) { LinkedList() }.add(dt)
            }
        }
        for ((guild, value) in map) {
            (if (guild == -1L) jda.updateCommands() else jda.getGuildById(guild)!!.updateCommands()).addCommands(value)
                .queue({ l ->
                    logger.info("guild = {}", guild)
                    logger.info("l = {}", l.joinToString { it.name })
                }) { it.printStackTrace() }
        }
    }

    @PrivateCommand("startasls11drafts")
    suspend fun startasls11drafts(e: GenericCommandEvent) {
        val jda = e.jda
        Emolga.get.apply {
            league("ASLS11L0").startDraft(jda.getTextChannelById(999775837106745415), false)
            league("ASLS11L1").startDraft(jda.getTextChannelById(1000773968418054164), false)
            league("ASLS11L2").startDraft(jda.getTextChannelById(999775875761438740), false)
            league("ASLS11L3").startDraft(jda.getTextChannelById(999775925610750022), false)
            league("ASLS11L4").startDraft(jda.getTextChannelById(999775970498199592), false)
        }
    }

    private fun buildOptionData(args: List<ArgumentManagerTemplate.Argument>): List<OptionData> {
        return args.map { arg: ArgumentManagerTemplate.Argument ->
            val argType = arg.type
            OptionData(
                argType.asOptionType(),
                arg.name.lowercase().replace(" ", "-").replace(Regex("[^\\w-]"), ""),
                arg.helpmsg,
                !arg.isOptional,
                argType.hasAutoComplete()
            )
        }
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
                val split = str.split("-")
                Command.possibleForms.firstOrNull { str.startsWith("$it-", ignoreCase = true) }?.also { form ->
                    return@map "$form-${Command.getEnglName(split[1])}${split.getOrNull(2)?.let { "-${it}" } ?: ""}"
                }
                logger.info(str)
                val engl = Command.getEnglNameWithType(str)
                if (engl.isSuccess) return@map engl.translation
                logger.info("str = {}", str)
                when (str) {
                    "Kapu-Riki" -> "Tapu Koko"
                    "Kapu-Toro" -> "Tapu Bulu"
                    "Kapu-Kime" -> "Tapu Fini"
                    "Kapu-Fala" -> "Tapu Lele"
                    "Furnifra" -> "Heatmor"
                    else -> Command.getEnglName(split[0]) + "-" + split[1]
                }
            }.forEach { all.add(it) }
        }
        val path = "Tierlists/$guild.json"
        val o = load(path)
        o.put("englishnames", all)
        save(o, path)
    }

    @PrivateCommand("asls11data")
    fun asls11DataSheet() {
        val tl = Tierlist.getByGuild(Constants.G.ASL)!!
        val b = RequestBuilder("1VWjAc370NQvuybaQZOTQ2uBVGC36D2_n63DOghkoE2k")
        val send = tl.tierlist.entries.flatMap { en ->
            en.value.map {
                val data = getDataObject(it)
                listOf(
                    it,
                    en.key,
                    tl.prices[en.key]!!,
                    data.getJSONObject("baseStats").getInt("spe"),
                    Command.getGen5Sprite(data)
                )
            }
        }
        b.addAll("Data!A1", send).execute()
    }

    @PrivateCommand("asls11doc")
    fun asls11doc(e: GenericCommandEvent) {
        val sid = "1VWjAc370NQvuybaQZOTQ2uBVGC36D2_n63DOghkoE2k"
        val b = RequestBuilder(sid)
        val tindex = e.args[0].toInt()
        val level = e.args[1].toInt()
        val asl = Emolga.get.asls11
        val team = asl.table[tindex]
        val uid = asl.data[team]!!.members[level]!!
        val aslleague = Emolga.get.leagueByGuild(Constants.G.ASL, uid)!!
        val mons = Google[sid, "Data$level!B${tindex.y(15, 3)}:B${tindex.y(15, 14)}", false].map { it[0] as String }
        val picks = aslleague.picks[uid]!!
        val tl = Tierlist.getByGuild(Constants.G.ASL)!!
        picks.clear()
        picks.addAll(mons.map { DraftPokemon(it, tl.getTierOf(it)) })
        b.addColumn("$team!C${level.y(26, 23)}", picks.let { pi ->
            pi.sortedWith((aslleague as ASL).comparator).map { it.indexedBy(pi) }
                .map { "=Data$level!B${tindex.y(15, 3) + it}" }
        })
        b.execute()
    }

    @PrivateCommand("breakpoint")
    fun breakpoint(e: GenericCommandEvent) {
        e.done()
    }

    suspend fun execute(message: Message) {
        val msg = message.contentRaw
        for (method in PrivateCommands::class.declaredMemberFunctions) {
            val a = method.findAnnotation<PrivateCommand>() ?: continue
            if (msg.lowercase().startsWith("!" + a.name.lowercase() + " ") || msg.equals(
                    "!" + a.name, ignoreCase = true
                ) || a.aliases.any {
                    msg.startsWith(
                        "!$it "
                    ) || msg.equals("!$it", ignoreCase = true)
                }
            ) {
                //Thread({
                try {
                    if (method.parameters.run { isEmpty() || size == 1 }) method.callSuspend(PrivateCommands)
                    else method.callSuspend(
                        PrivateCommands, PrivateCommandEvent(message)
                    )
                } catch (e: IllegalAccessException) {
                    logger.error("PrivateCommand " + a.name, e)
                } catch (e: InvocationTargetException) {
                    logger.error("PrivateCommand " + a.name, e)
                }
                //}, "PrivateCommand " + a.name).start()
            }
        }
    }
}