package de.tectoast.emolga.commands

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.commands.Command.ArgumentManagerTemplate
import de.tectoast.emolga.commands.Command.Companion.dataJSON
import de.tectoast.emolga.database.exposed.AnalysisDB
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.database.exposed.SDNamesDB
import de.tectoast.emolga.database.exposed.TipGamesDB
import de.tectoast.emolga.ktor.subscribeToYTChannel
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.Constants.EMOLGA_KI
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.json.LigaStartData
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.ASLCoach
import de.tectoast.emolga.utils.json.emolga.draft.NDS
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.json.only
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.Modal
import dev.minn.jda.ktx.interactions.components.button
import dev.minn.jda.ktx.interactions.components.primary
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.editMessage
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.utils.FileUpload
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.regex.Pattern
import javax.imageio.ImageIO

@Suppress("unused")
object PrivateCommands {
    private val logger = LoggerFactory.getLogger(PrivateCommands::class.java)
    private val DOUBLE_BACKSLASH = Pattern.compile("\\\\")

    // MolfBestesTeam
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


    suspend fun ndsNominate(e: GenericCommandEvent) {
        val args = e.getArg(1).split(" ")
        NDS.doNDSNominate(args[0].toBooleanStrict(), *args.drop(1).map { it.toLong() }.toLongArray())
    }


    suspend fun matchUps(e: GenericCommandEvent) {
        NDS.doMatchUps(e.getArg(1).toInt())
    }


    suspend fun ndsTeamsite() {
        val nds = db.nds()
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


    suspend fun ndsgameplanfix(e: GenericCommandEvent) {
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
                            url = this,
                            resultchannelParam = e.jda.getTextChannelById(837425749770240001L)!!,
                            message = m
                        )
                    }
            }
            if (m.idLong == 944309573383245904L) break
        }
    }


    suspend fun wooloogameplanfix(e: GenericCommandEvent) {
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
                            url = this,
                            resultchannelParam = e.jda.getTextChannelById(929686912048975882L)!!,
                            message = m
                        )
                    }
            }
            if (m.idLong == 946505526060122112L) break
        }
    }


    suspend fun prepareNDSJSON() {
        val nds = db.nds()
        val picks = nds.picks
        val tierorder = listOf("S", "A", "B", "C", "D")
        for (s in picks.keys) {
            picks[s]!!.sortWith(compareBy({ it.tier.indexedBy(tierorder) }, { it.name }))
        }
        nds.save()
    }


    suspend fun prepareNDSDoc() {
        val nds = db.nds()
        val picks = nds.picks
        val get: MutableList<List<List<Any>>?> = LinkedList()
        val sid = "1ZwYlgwA7opD6Gdc5KmpjYk5JsnEZq3dZet2nJxB0EWQ"
        for ((temp, s) in picks.keys.withIndex()) {
            logger.info(MarkerFactory.getMarker("important"), "{} {}", temp, s)
            get.add(Google[sid, nds.teamnames[s] + "!B15:O29", true])
        }
        val builder = RequestBuilder(sid)
        for ((x, u) in picks.keys.withIndex()) {
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


    suspend fun asls10fixswitches(e: GenericCommandEvent) {
        val league = db.league("ASLS10L${e.getArg(0)}")
        val picks = league.picks
        val tierorder = listOf("S", "A", "B", "C", "D")
        val s = e.getArg(1)
        picks[s.toLong()]!!.sortWith(compareBy({ it.tier.indexedBy(tierorder) }, { it.name }))
        league.save()
    }


    suspend fun ndscorrektpkmnnames() {
        val nds = db.nds()
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
        e.reply("Deleted: " + AnalysisDB.removeUnused())
    }

    suspend fun updateSlashCommands() {
        val jda = EmolgaMain.emolgajda
        val map: MutableMap<Long, MutableList<SlashCommandData>> = HashMap()
        Command.commands.values.filter { it.isSlash }.distinct().filter { it.slashGuilds.isNotEmpty() }.forEach {
            val dt = Commands.slash(it.name, it.help)
            if (it.adminSlash) dt.defaultPermissions = DefaultMemberPermissions.DISABLED
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
        val guildsToUpdate = db.config.only().guildsToUpdate
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
        db.apply {
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


    suspend fun asls11doc(e: GenericCommandEvent) {
        val sid = "1VWjAc370NQvuybaQZOTQ2uBVGC36D2_n63DOghkoE2k"
        val b = RequestBuilder(sid)
        val tindex = e.args[0].toInt()
        val level = e.args[1].toInt()
        val asl = db.asls11
        val team = asl.table[tindex]
        val uid = asl.data[team]!!.members[level]!!
        val aslleague = db.leagueByGuild(Constants.G.ASL, uid)!!
        val mons = Google[sid, "Data$level!B${tindex.y(15, 3)}:B${tindex.y(15, 14)}", false].map { it[0] as String }
        val picks = aslleague.picks[uid]!!
        val tl = Tierlist[Constants.G.ASL]!!
        picks.clear()
        picks.addAll(mons.map { DraftPokemon(it, tl.getTierOf(it)!!) })
        b.addColumn("$team!C${level.y(26, 23)}", picks.let { pi ->
            pi.sortedWith((aslleague as ASLCoach).comparator).map { it.indexedBy(pi) }
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


    suspend fun printTipGame(e: GenericCommandEvent) {
        File("tipgame_${defaultTimeFormat.format(Date()).replace(" ", "_")}.txt").also { it.createNewFile() }.writeText(
            newSuspendedTransaction {
                TipGamesDB.run {
                    val size: Int
                    select { LEAGUE_NAME eq e.getArg(1) }.orderBy(this.CORRECT_GUESSES, SortOrder.DESC).toList()
                        .also { size = it.size }.withIndex()
                        .joinToString("\n") {
                            val row = it.value
                            "${
                                (it.index + 1).toString().padStart(size.toString().length, '0')
                            }. <@${row[this.USERID]}>: ${row[this.CORRECT_GUESSES]}"
                        }

                }
            })
    }


    suspend fun printEnterTipGame() {
        val nds = db.nds()
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
        }.forEach { NameConventionsDB.insertDefault(it.first, it.second) }
    }


    suspend fun createConventionsCosmetic() {
        dataJSON.values.asSequence().filterNot { it.num <= 0 || it.cosmeticFormes == null }.forEach {
            val translated = Command.getGerName(it.baseSpecies ?: it.name).translation
            it.cosmeticFormes!!.forEach { f ->
                NameConventionsDB.insertDefaultCosmetic(
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
        val maxUsers = args[3].toInt()
        val messageid =
            tc.sendMessage(
                args.drop(4).joinToString(" ")
                    .replace("\\n", "\n") + "\n\n**Teilnehmer: 0/${maxUsers.takeIf { it > 0 } ?: "?"}**")
                .addActionRow(
                    primary(
                        "signup", "Anmelden", Emoji.fromUnicode("✅")
                    )
                ).await().idLong
        db.signups.insertOne(
            LigaStartData(
                guild = tc.guild.idLong,
                signupChannel = args[1].toLong(),
                logoChannel = args[2].toLong(),
                maxUsers = maxUsers,
                announceChannel = tc.idLong,
                announceMessageId = messageid
            )
        )
    }

    suspend fun closeSignup(e: GuildCommandEvent) {
        val gid = e.getArg(1).toLong()
        with(db.signups.get(gid)!!) {
            val announceChannel = e.jda.getTextChannelById(announceChannel)!!
            announceChannel.editMessageComponentsById(
                announceMessageId, primary("signupclosed", "Anmeldung geschlossen", disabled = true).into()
            ).queue()
            announceChannel.sendMessage("_----------- Anmeldung geschlossen -----------_").queue()
        }
    }

    suspend fun giveGeneralSignupRole(e: GenericCommandEvent) {
        val args = e.getArg(1).split(" ")
        val g = e.jda.getGuildById(args[0])!!
        val data = db.signups.get(g.idLong)!!
        val role = g.getRoleById(args[1].toLong())!!
        data.users.entries.flatMap { listOf(it.key, *it.value.teammates.toTypedArray()) }.forEach {
            g.addRoleToMember(UserSnowflake.fromId(it), role).queue()
            delay(2000)
        }
    }

    suspend fun giveConferenceRoles(e: GenericCommandEvent) {
        val args = e.getArg(1).split(" ")
        val g = e.jda.getGuildById(args[0])!!
        val data = db.signups.get(g.idLong)!!
        val roles = args.drop(1).map { g.getRoleById(it)!! }
        val conferences = data.conferences
        data.users.entries.forEach { user ->
            val role = roles[user.value.conference.indexedBy(conferences)]
            listOf(user.key, *user.value.teammates.toTypedArray()).forEach { uid ->
                g.addRoleToMember(UserSnowflake.fromId(uid), role).queue()
                delay(2000)
            }

            // 518008523653775366 1095097314210762884 1095097593463308358 1095097684706213928 1095097835491438655 1095097904944910428
        }
    }

    // Channel, extended, conferences
    suspend fun startOrderingUsers(e: GenericCommandEvent) {
        val args = e.getArg(1).split(" ")
        val tc = e.jda.getTextChannelById(args[0])!!
        val extended = args[1].toBoolean()
        val data = db.signups.get(tc.guild.idLong)!!
        val conferences = args.drop(2)
        data.shiftChannel = tc.idLong
        data.conferences = conferences
        data.extended = extended
        data.shiftMessageIds = listOf()
        data.users.values.forEach { it.conference = null }
        if (extended) {
            val uid = data.users.keys.first()
            tc.sendMessageEmbeds(Embed(title = "Einteilung", description = "<@$uid>"))
                .addActionRow(data.conferenceSelectMenus(uid, true))
                .queue()
        } else {
            data.users.values.forEachIndexed { index, value ->
                value.conference = conferences[index % conferences.size]
            }
            data.shiftMessageIds = generateOrderingMessages(data).values.map {
                tc.send(embeds = it.first.into(), components = it.second).await().idLong
            }
        }
        data.save()
    }

    private val nameCache = mutableMapOf<Long, String>()
    suspend fun generateOrderingMessages(
        data: LigaStartData,
        vararg conferenceIndexes: Int
    ): Map<Int, Pair<MessageEmbed, List<ActionRow>>> {
        if (nameCache.isEmpty()) EmolgaMain.emolgajda.getTextChannelById(data.signupChannel)!!.guild.retrieveMembersByIds(
            data.users.keys
        ).await().forEach { nameCache[it.idLong] = it.effectiveName }
        data.save()
        return data.users.entries.groupBy { it.value.conference }.entries.filter {
            conferenceIndexes.isEmpty() || data.conferences.indexOf(
                it.key
            ) in conferenceIndexes
        }.sortedBy { data.conferences.indexOf(it.key) }.associate { (conference, users) ->
            conference.indexedBy(data.conferences) to (Embed(
                title = "Conference: $conference (${users.size}/${data.maxUsersAsString})",
                description = users.joinToString("\n") { "<@${it.key}>" }, color = embedColor
            ) to
                    users.map { (id, _) ->
                        primary("shiftuser;$id", nameCache[id]!!)
                    }.chunked(5).map { ActionRow.of(it) })
        }
    }

    suspend fun shuffleSignupConferences(e: GenericCommandEvent) {
        val data = db.signups.get(e.getArg(1).toLong())!!
        val tc = EmolgaMain.emolgajda.getTextChannelById(data.shiftChannel!!)!!
        val conferences = data.conferences
        data.users.values.shuffled().shuffled().forEachIndexed { index, value ->
            value.conference = conferences[index % conferences.size]
        }
        generateOrderingMessages(data).forEach { (index, pair) ->
            tc.editMessage(data.shiftMessageIds[index].toString(), embeds = pair.first.into(), components = pair.second)
                .queue()
        }
        data.save()
    }

    suspend fun signupUpdate(e: GenericCommandEvent) {
        val (guild, user) = e.getArg(1).split(" ").map { it.toLong() }
        val ligaStartData = db.signups.get(guild)!!
        val data = ligaStartData.users[user]!!
        e.jda.getTextChannelById(ligaStartData.signupChannel)!!.editMessageById(data.signupmid!!, data.toMessage(user))
            .queue()
    }

    suspend fun insertSDNamesInDatabase(e: GenericCommandEvent) {
        val guild = e.getArg(1).toLong()
        db.signups.get(guild)!!.users.forEach { (id, data) ->
            SDNamesDB.addIfAbsent(data.sdname, id)
        }
    }

    suspend fun subscribeYT(e: GenericCommandEvent) {
        subscribeToYTChannel(e.getArg(1))
    }

    var guildForTLSetup: Long? = null
    fun setGuildForTLSetup(e: GenericCommandEvent) {
        guildForTLSetup = e.getArg(1).toLong()
    }

    var guildForUserIDGrabbing: Long? = Constants.G.WARRIOR
    val grabbedIDs = mutableListOf<Long>()
    fun grabUserIDs(e: GenericCommandEvent) {
        guildForUserIDGrabbing = e.getArg(1).toLong()
        grabbedIDs.clear()
    }

    var userIdForSignupChange: Long? = null
    fun setUserIdForSignupChange(e: GenericCommandEvent) {
        userIdForSignupChange = e.getArg(1).toLong()
    }

    fun printUserIDs(e: GenericCommandEvent) {
        e.reply(grabbedIDs.joinToString())
    }

    fun clearUserIDs() {
        grabbedIDs.clear()
    }

    suspend fun nds() {
        val values = db.nds().picks.values.flatten().map { it.name }.filter { it != "???" }
        val tierlist = Tierlist[Constants.G.NDS]!!
        values.forEach {
            tierlist.getTierOf(NameConventionsDB.convertOfficialToTL(it, Constants.G.NDS)!!)
        }
    }

    suspend fun ndsZAndTera() {
        val nds = db.nds()
        val b = RequestBuilder(nds.sid)
        val bo = nds.battleorder[1]!!
        val table = nds.table
        val teamnames = nds.teamnames
        for (mu in bo) {
            for (i in 0..1) {
                val team = teamnames[table[mu[i]]]!!
                val oppo = teamnames[table[mu[1 - i]]]!!
                b.addSingle("$team!AE8", "='$oppo'!AA8")
                b.addSingle("$team!AE10", "='$oppo'!AA10")
                b.addSingle("$team!AC11", "='$oppo'!Y11")
            }
        }
        b.execute()
    }

    suspend fun ndsPrintMissingNominations(e: GenericCommandEvent) {
        e.reply("```" + db.nds().run { table - nominations.current().keys }.joinToString { "<@${it}>" } + "```")
    }

    suspend fun taria(e: GenericCommandEvent) {
        val guild = e.getArg(1).toLong()
        val data = db.signups.get(guild)!!
        val usersByConf = data.users.entries.groupBy { it.value.conference!! }.mapValues { it.value.map { e -> e.key } }
        myJSON.encodeToString(usersByConf).let { e.reply("```json\n$it```") }
    }

    private val teamGraphicScope = CoroutineScope(Dispatchers.IO)
    fun teamgraphics(e: GenericCommandEvent) {
        suspend fun List<DraftPokemon>.toTeamGraphics() = FileUpload.fromData(ByteArrayOutputStream().also {
            ImageIO.write(
                TeamGraphics.fromDraftPokemon(this).first,
                "png",
                it
            )
        }
            .toByteArray(), "yay.png")
        e.done(true)
        teamGraphicScope.launch {
            val args = e.getArg(1).split(" ")
            val league = db.league(args[0])
            val user = args[1].toLong()
            val tc = args.getOrNull(2)?.let { EmolgaMain.emolgajda.getTextChannelById(it)!! } ?: e.channel
            if (user > -1) {
                tc.sendMessage("Kader von <@${user}>:").addFiles(league.picks[user]!!.toTeamGraphics()).queue()
                return@launch
            }
            league.picks.entries.map { (u, l) ->
                async {
                    val (bufferedImage, _) = TeamGraphics.fromDraftPokemon(l)
                    u to FileUpload.fromData(ByteArrayOutputStream().also {
                        ImageIO.write(
                            bufferedImage,
                            "png",
                            it
                        )
                    }
                        .toByteArray(), "yay.png")

                }
            }.awaitAll().forEach { tc.sendMessage("Kader von <@${it.first}>:").addFiles(it.second).queue() }
        }

    }

    suspend fun specaslgraphics(e: GenericCommandEvent) {
        coroutineScope {
            launch {
                val m = e.getArg(1).toLong()
                val team = (1..5).map { db.league("ASLS12L$it") }.first { m in it.table }.picks[m]!!
                e.slashCommandEvent!!.replyFiles(FileUpload.fromData(ByteArrayOutputStream().also {
                    ImageIO.write(
                        TeamGraphics.fromDraftPokemon(team).first,
                        "png",
                        it
                    )
                }.toByteArray(), "yay.png")).queue()
            }
        }
    }

    fun florixcontrol(e: GenericCommandEvent) {
        val args = e.getArg(1).split(" ")
        val id = "florix;startserver:" + when (args[1]) {
            "2" -> GPIOManager.PC.FLORIX_2.messageId
            "3" -> GPIOManager.PC.FLORIX_3.messageId
            else -> throw IllegalArgumentException()
        }
        e.jda.getTextChannelById(args[0])!!.sendMessage(":)").addActionRow(button(id, "Server starten")).queue()
    }

    suspend fun tariachan(e: GenericCommandEvent) {
        e.channel.sendFiles(FileUpload.fromData(ByteArrayOutputStream().also {
            ImageIO.write(
                TeamGraphics.fromDraftPokemon(
                    listOf(
                        DraftPokemon("Viscogon-Hisui", "B"),
                        DraftPokemon("Riesenzahn", "B"),
                        DraftPokemon("Mamolida", "B"),
                        DraftPokemon("Bandelby", "B"),
                        DraftPokemon("Voltolos-Therian", "B"),
                        DraftPokemon("Diancie", "B"),
                        DraftPokemon("Lavados", "B"),
                        DraftPokemon("Calamanero", "B"),
                        DraftPokemon("Famieps", "B"),
                        DraftPokemon("Moruda", "B"),
                        DraftPokemon("Sleimok-Alola", "B"),
                        DraftPokemon("Iscalar", "B")
                    )
                ).first, "png", it
            )
        }.toByteArray(), "yay.png")).queue()
    }
}
