package de.tectoast.emolga.commands

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.commands.Command.ArgumentManagerTemplate
import de.tectoast.emolga.commands.Command.Companion.dataJSON
import de.tectoast.emolga.commands.Command.Translation
import de.tectoast.emolga.database.Database
import de.tectoast.emolga.database.exposed.NameConventions
import de.tectoast.emolga.database.exposed.TipGames
import de.tectoast.emolga.ktor.subscribeToYTChannel
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.Constants.EMOLGA_KI
import de.tectoast.emolga.utils.Google
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.json.Emolga
import de.tectoast.emolga.utils.json.LigaStartData
import de.tectoast.emolga.utils.json.emolga.draft.ASL
import de.tectoast.emolga.utils.sql.managers.AnalysisManager
import de.tectoast.emolga.utils.sql.managers.SDNamesManager
import de.tectoast.emolga.utils.sql.managers.TranslationsManager
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.Modal
import dev.minn.jda.ktx.interactions.components.primary
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import io.ktor.server.application.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.awt.Color
import java.io.File
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.regex.Pattern

@Suppress("unused")
object PrivateCommands {
    private val logger = LoggerFactory.getLogger(PrivateCommands::class.java)
    private val DOUBLE_BACKSLASH = Pattern.compile("\\\\")


    fun updateTierlist(e: GenericCommandEvent) {
        Tierlist.setup()
        e.reply("Die Tierliste wurde aktualisiert!")
    }


    fun checkGuild(e: GenericCommandEvent) {
        e.jda.getGuildById(e.getArg(0))?.let { e.reply(it.name) }
    }


    fun edit(e: GenericCommandEvent) {
        e.msg?.let { e.jda.getTextChannelById(e.getArg(0))?.editMessageById(e.getArg(1), it.substring(43))?.queue() }
    }


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


    fun sendPN(e: GenericCommandEvent) {
        val message = e.message
        logger.info(message!!.contentRaw)
        val s = DOUBLE_BACKSLASH.matcher(message.contentRaw.substring(26)).replaceAll("")
        val userid = e.getArg(0)
        Command.sendToUser(userid.toLong(), s)
    }


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


    fun ban(e: GenericCommandEvent) {
        e.jda.getGuildById(e.getArg(0))?.ban(UserSnowflake.fromId(e.getArg(1)), 0, TimeUnit.SECONDS)?.queue()
    }


    fun banwithreason(e: GenericCommandEvent) {
        e.jda.getGuildById(e.getArg(0))?.ban(UserSnowflake.fromId(e.getArg(1)), 0, TimeUnit.SECONDS)
            ?.reason(e.msg?.substring(53))?.queue()
    }


    fun updateDatabase(e: GenericCommandEvent) {
        Command.loadJSONFiles()
        e.done()
    }


    fun del(e: GenericCommandEvent) {
        e.jda.getTextChannelById(e.getArg(0))?.deleteMessageById(e.getArg(1))?.queue()
    }


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


    fun addReactions(e: GenericCommandEvent) {
        e.jda.getTextChannelById(e.getArg(0))?.retrieveMessageById(e.getArg(1))?.queue { m: Message ->
            m.reactions.forEach(Consumer {
                m.addReaction(it.emoji).queue()
            })
            e.done()
        }
    }


    fun saveEmolga(e: GenericCommandEvent) {
        saveEmolgaJSON()
        e.done()
    }


    fun incrPdg(e: GenericCommandEvent) {
        for (arg in e.args) {
            Database.incrementPredictionCounter(arg.toLong())
        }
    }


    fun testVolume(e: GenericCommandEvent) {
        logger.info("Start!")
        Command.musicManagers[673833176036147210L]!!.player.volume = e.getArg(0).toInt()
        logger.info("musicManagers.get(673833176036147210L).player.getVolume() = " + Command.musicManagers[673833176036147210L]!!.player.volume)
    }


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


    fun clearCache(e: GenericCommandEvent) {
        Command.translationsCacheGerman.clear()
        Command.translationsCacheOrderGerman.clear()
        Command.translationsCacheEnglish.clear()
        Command.translationsCacheOrderEnglish.clear()
        e.done()
    }


    fun getGuildCmd(e: GenericCommandEvent) {
        e.jda.getTextChannelById(e.getArg(0))?.guild?.let { e.reply(it.name) }
    }


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


    fun removeDuplicates() {
        TranslationsManager.removeDuplicates()
    }


    fun ndsNominate(e: GenericCommandEvent) {
        Command.doNDSNominate(e.getArg(0).toBooleanStrict())
    }


    fun matchUps(e: GenericCommandEvent) {
        Command.doMatchUps(e.getArg(0).toInt())
    }


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
                        Command.analyseReplay(
                            url = this, resultchannel = e.jda.getTextChannelById(837425749770240001L)!!, message = m
                        )
                    }
            }
            if (m.idLong == 944309573383245904L) break
        }
    }


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
                        Command.analyseReplay(
                            url = this, resultchannel = e.jda.getTextChannelById(929686912048975882L)!!, message = m
                        )
                    }
            }
            if (m.idLong == 946505526060122112L) break
        }
    }


    fun prepareNDSJSON() {
        val nds = Emolga.get.nds()
        val picks = nds.picks
        val tierorder = listOf("S", "A", "B", "C", "D")
        for (s in picks.keys) {
            picks[s]!!.sortWith(compareBy({ it.tier.indexedBy(tierorder) }, { it.name }))
        }
        saveEmolgaJSON()
    }


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


    fun asls10fixswitches(e: GenericCommandEvent) {
        val league = Emolga.get.league("ASLS10L${e.getArg(0)}")
        val picks = league.picks
        val tierorder = listOf("S", "A", "B", "C", "D")
        val s = e.getArg(1)
        picks[s.toLong()]!!.sortWith(compareBy({ it.tier.indexedBy(tierorder) }, { it.name }))
        saveEmolgaJSON()
    }


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


    fun createKI(e: GenericCommandEvent) {
        val jda = e.jda
        val tc = jda.getTextChannelById(e.getArg(0))
        jda.getCategoryById(EMOLGA_KI)!!.createTextChannel("${tc!!.name}-${tc.idLong}").queue()
        e.done()
    }


    fun deleteUnused(e: GenericCommandEvent) {
        e.reply("Deleted: " + AnalysisManager.removeUnused())
    }


    private val guildsToUpdate = listOf(Constants.G.FLP, Constants.G.MY)
    fun updateSlashCommands() {
        val jda = EmolgaMain.emolgajda
        val map: MutableMap<Long, MutableList<SlashCommandData>> = HashMap()
        Command.commands.values.filter { it.isSlash }.distinct().filter { it.slashGuilds.isNotEmpty() }.forEach {
            val dt = Commands.slash(it.name, it.help)
            //if (it.category!!.isAdmin) dt.defaultPermissions = DefaultMemberPermissions.DISABLED
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
            if (guildsToUpdate.isNotEmpty() && guild !in guildsToUpdate) continue
            (when (guild) {
                -1L -> jda.updateCommands()
                Constants.G.PEPE -> EmolgaMain.flegmonjda.getGuildById(
                    Constants.G.PEPE
                )!!.updateCommands()

                else -> jda.getGuildById(guild)!!.updateCommands()
            }).addCommands(value).queue({ l ->
                logger.info("guild = {}", guild)
                logger.info("l = {}", l.joinToString { it.name })
            }) { it.printStackTrace() }
        }
    }


    suspend fun startasls11drafts(e: GenericCommandEvent) {
        val jda = e.jda
        Emolga.get.apply {
            league("ASLS11L0").startDraft(
                jda.getTextChannelById(999775837106745415), fromFile = false, switchDraft = true
            )
            league("ASLS11L1").startDraft(
                jda.getTextChannelById(1000773968418054164), fromFile = false, switchDraft = true
            )
            league("ASLS11L2").startDraft(
                jda.getTextChannelById(999775875761438740), fromFile = false, switchDraft = true
            )
            league("ASLS11L3").startDraft(
                jda.getTextChannelById(999775925610750022), fromFile = false, switchDraft = true
            )
            league("ASLS11L4").startDraft(
                jda.getTextChannelById(999775970498199592), fromFile = false, switchDraft = true
            )
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
            ).apply { if (argType is ArgumentManagerTemplate.Text && argType.hasOptions()) addChoices(*argType.toChoiceArray()) }
        }
    }


    fun checkAdmin(e: GenericCommandEvent) {
        e.reply(e.jda.getGuildById(e.getArg(0))!!.selfMember.hasPermission(Permission.ADMINISTRATOR).toString())
    }


    fun testDBSpeed(e: GenericCommandEvent) {
        val l = System.nanoTime()
        TranslationsManager.getTranslation(e.getArg(0), false)
        e.reply((System.nanoTime() - l).toString())
    }


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
        val tl = Tierlist[Constants.G.ASL]!!
        picks.clear()
        picks.addAll(mons.map { DraftPokemon(it, tl.getTierOf(it)) })
        b.addColumn("$team!C${level.y(26, 23)}", picks.let { pi ->
            pi.sortedWith((aslleague as ASL).comparator).map { it.indexedBy(pi) }
                .map { "=Data$level!B${tindex.y(15, 3) + it}" }
        })
        b.execute()
    }


    fun breakpoint(e: GenericCommandEvent) {
        e.done()
    }


    fun showAllCommands() {
        Command.commands.values.toSet().groupBy { it.category }.toList().joinToString("\n\n") { (cat, cmds) ->
            "$cat:\n${cmds.sortedBy { it.name }.joinToString("\n") { it.getHelp(null) }}"
        }.let { File("allcommands.txt").writeText(it) }
    }


    suspend fun printTipGame() {
        newSuspendedTransaction {
            TipGames.run {
                selectAll().orderBy(this.correctGuesses, SortOrder.DESC).forEach {
                    println("<@${it[this.userid]}>: ${it[this.correctGuesses]}")
                }

            }
        }
    }


    fun printEnterTipGame() {
        val nds = Emolga.get.nds()
        val tips = nds.tipgame!!.tips
        val battleorder = nds.battleorder
        val table = nds.table
        for ((gameday, gddata) in tips.entries) {
            val battleindex = battleorder[gameday]!!.indexOfFirst { 2 /*wrong index*/ in it }
            println("Spieltag $gameday:")
            for ((voter, votes) in gddata.userdata.entries) {
                votes[battleindex]?.let { vote ->
                    println("<@${voter}>: <@${table[vote]}>")
                }
            }
            println()
        }
    }


    fun sdNameAdd(e: GenericCommandEvent) {
        e.slashCommandEvent!!.replyModal(Modal("addsdnamecreate", "Showdown-Namen-Button hinzufügen") {
            short("tc", "Text-Channel", required = true)
            paragraph("msg", "Nachricht", required = true)
            short("buttonname", "Button-Name", required = true)
        }).queue()
    }


    suspend fun createConventions() {
        dataJSON.values.asSequence().filterNot { it.num <= 0 }.map {
            val translated = Command.getGerName(it.baseSpecies ?: it.name).translation
            it.name to translated.condAppend(it.forme != null) { "-${it.forme}" }
        }.forEach { NameConventions.insertDefault(it.first, it.second) }
    }


    suspend fun createConventionsCosmetic() {
        dataJSON.values.asSequence().filterNot { it.num <= 0 || it.cosmeticFormes == null }.forEach {
            val translated = Command.getGerName(it.baseSpecies ?: it.name).translation
            it.cosmeticFormes!!.forEach { f ->
                NameConventions.insertDefaultCosmetic(
                    it.name, translated, f, translated + "-" + f.split("-").drop(1).joinToString("-")
                )
            }
        }
    }


    fun allGermanHyphen() {
        dataJSON.values.asSequence().filter { it.baseSpecies == null }.mapNotNull {
            Command.getGerName(it.name).translation.takeIf { c -> c.contains('-') }
        }.joinToString("\n").let { "noforms.txt".file().writeText(it) }
    }

    // Order: Announcechannel mit Button, Channel in dem die Anmeldungen reinkommen, Channel in den die Logos kommen, AnzahlTeilnehmer, Message
    suspend fun createSignup(e: GenericCommandEvent) {
        val args = e.getArg(1).split(" ")
        val tc = e.jda.getTextChannelById(args[0])!!
        val messageid = tc.sendMessage(args.drop(4).joinToString(" ").replace("\\n", "\n")).addActionRow(
            primary(
                "signup", "Anmelden", Emoji.fromUnicode("✅")
            )
        ).await().idLong
        Emolga.get.signups[tc.guild.idLong] =
            LigaStartData(
                signupChannel = args[1].toLong(),
                logoChannel = args[2].toLong(),
                maxUsers = args[3].toInt(),
                announceChannel = tc.idLong,
                announceMessageId = messageid
            )
        saveEmolgaJSON()
    }

    suspend fun startOrderingUsers(e: GenericCommandEvent) {
        val args = e.getArg(1).split(" ")
        val tc = e.jda.getTextChannelById(args[0])!!
        val data = Emolga.get.signups[tc.guild.idLong]!!
        val conferences = args.drop(1)
        data.shiftChannel = tc.idLong
        data.conferences = conferences
        val usermap = mutableMapOf<String, MutableList<Long>>()
        data.users.entries.forEachIndexed { index, (key, value) ->
            value.conference = conferences[index % conferences.size].also {
                usermap.getOrPut(it) { mutableListOf() }.add(key)
            }
        }
        data.shiftMessageIds = generateOrderingMessages(data).map {
            tc.send(embeds = it.first.into(), components = it.second).await().idLong
        }
        saveEmolgaJSON()
    }

    private val nameCache = mutableMapOf<Long, String>()
    suspend fun generateOrderingMessages(data: LigaStartData): List<Pair<MessageEmbed, List<ActionRow>>> {
        if (nameCache.isEmpty()) EmolgaMain.emolgajda.getTextChannelById(data.signupChannel)!!.guild.retrieveMembersByIds(
            data.users.keys
        ).await().forEach { nameCache[it.idLong] = it.effectiveName }
        return data.users.entries.groupBy { it.value.conference }.entries.sortedBy { data.conferences.indexOf(it.key) }
            .map { (conference, users) ->
                Embed(
                    title = "Conference: $conference (${users.size}/${data.maxUsers})",
                    description = users.joinToString("\n") { "<@${it.key}>" }, color = embedColor
                ) to
                        users.map { (id, _) ->
                            primary("shiftuser;$id", nameCache[id]!!)
                        }.chunked(5).map { ActionRow.of(it) }
            }
    }

    fun insertSDNamesInDatabase(e: GenericCommandEvent) {
        val guild = e.getArg(1).toLong()
        Emolga.get.signups[guild]!!.users.forEach { (id, data) ->
            SDNamesManager.addIfAbsent(data.sdname, id)
        }
    }

    suspend fun subscribeYT(e: GenericCommandEvent) {
        subscribeToYTChannel(e.getArg(1))
    }
}
