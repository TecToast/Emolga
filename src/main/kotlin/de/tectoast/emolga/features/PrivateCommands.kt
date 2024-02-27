package de.tectoast.emolga.features

import de.tectoast.emolga.bot.EmolgaMain.flegmonjda
import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.features.draft.SignupManager
import de.tectoast.emolga.features.draft.TipGameManager
import de.tectoast.emolga.features.flegmon.RoleManagement
import de.tectoast.emolga.features.flo.FlorixButton
import de.tectoast.emolga.features.various.ShiftUser
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.dconfigurator.impl.TierlistBuilderConfigurator
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.json.*
import de.tectoast.emolga.utils.json.emolga.ASLCoachData
import de.tectoast.emolga.utils.json.emolga.Config
import de.tectoast.emolga.utils.json.emolga.Statistics
import de.tectoast.emolga.utils.json.emolga.TeamData
import de.tectoast.emolga.utils.json.emolga.draft.IPL
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.NDS
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.editMessage
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.utils.FileUpload
import org.litote.kmongo.eq
import org.litote.kmongo.newId
import org.litote.kmongo.set
import org.litote.kmongo.setTo
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.SecureRandom
import java.util.*
import java.util.regex.Pattern
import javax.imageio.ImageIO

@Suppress("unused")
object PrivateCommands {
    private val logger = LoggerFactory.getLogger(PrivateCommands::class.java)
    private val DOUBLE_BACKSLASH = Pattern.compile("\\\\")

    context(InteractionData)
    fun printCache() {
        Translation.translationsCacheGerman.forEach { (str, t) ->
            logger.info(str)
            logger.info(t.toString())
            logger.info("=====")
        }
        logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>")
        Translation.translationsCacheEnglish.forEach { (str, t) ->
            logger.info(str)
            logger.info(t.toString())
            logger.info("=====")
        }
        done()
    }

    context(InteractionData)
    fun clearCache() {
        Translation.translationsCacheGerman.clear()
        Translation.translationsCacheEnglish.clear()
        done()
    }

    context(InteractionData)
    suspend fun ndsNominate(args: PrivateData) {
        NDS.doNDSNominate(
            prevDay = args[0].toBooleanStrict(),
            withSend = args[1].toBooleanStrict(),
            onlySpecifiedUsers = args.drop(2).map { it.toLong() }.toLongArray()
        )
    }

    context(InteractionData)
    suspend fun matchUps(args: PrivateData) {
        NDS.doMatchUps(args[0].toInt(), args[1].toBooleanStrict())
    }

    context(InteractionData)
    suspend fun printTipGame(args: PrivateData) {
        File("tipgame_${defaultTimeFormat.format(Date()).replace(" ", "_")}.txt").also { it.createNewFile() }.writeText(
            db.tipgameuserdata.find(TipGameUserData::league eq args()).toList().asSequence()
                .map { it.user to it.correctGuesses.size }
                .sortedByDescending { it.second }
                .mapIndexed { index, pair -> "${index + 1}<@${pair.first}>: ${pair.second}" }
                .joinToString("\n")
        )
    }

    context(InteractionData)
    suspend fun rankSelect(args: PrivateData) {
        val league = db.league(args[0])
        reply(components = (1..args[1].toInt()).map {
            ActionRow.of(
                TipGameManager.RankSelect.createFromLeague(
                    league,
                    it
                )
            )
        })
    }

    context(InteractionData)
    suspend fun createConventions() {
        db.pokedex.find().toFlow().filter { it.num > 0 }.map {
            val translated = Translation.getGerName(it.baseSpecies ?: it.name).translation
            it.name to translated.condAppend(it.forme != null) { "-${it.forme}" }
        }.collect { NameConventionsDB.insertDefault(it.first, it.second) }
    }

    context(InteractionData)
    suspend fun createConventionsCosmetic() {
        db.pokedex.find().toFlow().filter { it.num > 0 && it.cosmeticFormes != null }.collect {
            val translated = Translation.getGerName(it.baseSpecies ?: it.name).translation
            it.cosmeticFormes!!.forEach { f ->
                NameConventionsDB.insertDefaultCosmetic(
                    it.name, translated, f, translated + "-" + f.split("-").drop(1).joinToString("-")
                )
            }
        }
    }

    // Order:
    // Announcechannel mit Button
    // Channel in dem die Anmeldungen reinkommen
    // Channel in den die Logos kommen
    // AnzahlTeilnehmer
    // RollenID(oder -1)
    // experiences
    // Message
    context(InteractionData)
    suspend fun createSignup(args: PrivateData) {
        val tc = jda.getTextChannelById(args[0])!!
        val maxUsers = args[3].toInt()
        val roleId = args[4].toLong().takeIf { it > 0 }
        val experiences = args[5].toBooleanStrict()
        val text = args.drop(6).joinToString(" ")
            .replace("\\n", "\n")
        SignupManager.createSignup(tc.idLong, args[1].toLong(), args[2].toLong(), maxUsers, roleId, experiences, text)
    }

    context(InteractionData)
    suspend fun closeSignup(args: PrivateData) {
        db.signups.get(args().toLong())!!.closeSignup(forced = true)
    }

    // Channel, extended, conferences
    context(InteractionData)
    suspend fun startOrderingUsers(args: PrivateData) {
        val tc = jda.getTextChannelById(args[0])!!
        val extended = args[1].toBoolean()
        val data = db.signups.get(guildForMyStuff ?: tc.guild.idLong)!!
        val conferences = args.drop(2)
        val confMap = conferences.mapNotNull {
            val split = it.split(":")
            split.getOrNull(1)?.toLong()?.let { id -> split[0] to id }
        }.toMap()
        data.shiftChannel = tc.idLong
        data.conferences = conferences
        data.conferenceRoleIds = confMap
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
        if (nameCache.isEmpty()) jda.getTextChannelById(data.signupChannel)!!.guild.retrieveMembersByIds(
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
                        ShiftUser.Button(nameCache[id]!!, ButtonStyle.PRIMARY) { this.uid = id }
                    }.chunked(5).map { ActionRow.of(it) })
        }

    }

    context(InteractionData)
    suspend fun finishOrdering(args: PrivateData) {
        done()
        val gid = args().toLong()
        val guild = jda.getGuildById(gid)!!
        val data = db.signups.get(gid)!!
        val roleMap = data.conferenceRoleIds.mapValues { guild.getRoleById(it.value) }
        data.users.entries.forEach {
            val role = roleMap[it.value.conference] ?: return@forEach
            (listOf(it.key) + it.value.teammates).forEach { uid ->
                guild.addRoleToMember(UserSnowflake.fromId(uid), role).queue()
                delay(2000)
            }
        }
    }

    context(InteractionData)
    suspend fun shuffleSignupConferences(args: PrivateData) {
        val data = db.signups.get(args().toLong())!!
        val tc = jda.getTextChannelById(data.shiftChannel!!)!!
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

    context(InteractionData)
    suspend fun signupUpdate(args: PrivateData) {
        val (guild, user) = args.map { it.toLong() }
        val ligaStartData = db.signups.get(guild)!!
        val data = ligaStartData.users[user]!!
        jda.getTextChannelById(ligaStartData.signupChannel)!!
            .editMessageById(data.signupmid!!, data.toMessage(user, ligaStartData))
            .queue()
    }

    context(InteractionData)
    suspend fun sort(args: PrivateData) {
        db.league(args()).docEntry!!.sort()
    }

    var guildForMyStuff: Long? = null
    context(InteractionData)
    fun setGuildForMyStuff(args: PrivateData) {
        guildForMyStuff = args().toLong()
    }

    var guildForUserIDGrabbing: Long? = Constants.G.WARRIOR
    val grabbedIDs = mutableListOf<Long>()
    context(InteractionData)
    fun grabUserIDs(args: PrivateData) {
        guildForUserIDGrabbing = args().toLong()
        grabbedIDs.clear()
    }

    context(InteractionData)
    suspend fun setTableFromGrabUserIDS(args: PrivateData) {
        db.db.getCollection<League>(args[0])
            .updateOne(League::leaguename eq args[1], set(League::table setTo grabbedIDs))
    }

    context(InteractionData)
    fun printGrabbedIDs() {
        reply(grabbedIDs.joinToString(prefix = "```", postfix = "```"))
    }

    var userIdForSignupChange: Long? = null
    context(InteractionData)
    fun setUserIdForSignupChange(args: PrivateData) {
        userIdForSignupChange = args().toLong()
    }

    context(InteractionData)
    suspend fun ndsPrintMissingNominations() {
        reply("```" + db.nds().run { table - nominations.current().keys }.joinToString { "<@${it}>" } + "```")
    }

    private val teamGraphicScope = createCoroutineScope("TeamGraphics", Dispatchers.IO)
    context(InteractionData)
    fun teamgraphics(args: PrivateData) {
        suspend fun List<DraftPokemon>.toTeamGraphics() = FileUpload.fromData(ByteArrayOutputStream().also {
            ImageIO.write(
                TeamGraphics.fromDraftPokemon(this).first,
                "png",
                it
            )
        }
            .toByteArray(), "yay.png")
        done(true)
        teamGraphicScope.launch {
            val league = db.league(args[0])
            val user = args[1].toLong()
            val tc = args.getOrNull(2)?.let { jda.getTextChannelById(it)!! } ?: textChannel
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

    context(InteractionData)
    suspend fun specaslgraphics(args: PrivateData) {
        coroutineScope {
            launch {
                val m = args().toLong()
                val team = (1..5).map { db.league("ASLS12L$it") }.first { m in it.table }.picks[m]!!
                reply(files = FileUpload.fromData(ByteArrayOutputStream().also {
                    ImageIO.write(
                        TeamGraphics.fromDraftPokemon(team).first,
                        "png",
                        it
                    )
                }.toByteArray(), "yay.png").into())
            }
        }
    }

    context(InteractionData)
    fun florixcontrol(args: PrivateData) {
        jda.getTextChannelById(args[0])!!.send(":)", components = FlorixButton("Server starten", ButtonStyle.PRIMARY) {
            this.pc = when (args[1]) {
                "2" -> PC.FLORIX_2
                "4" -> PC.FLORIX_4
                else -> throw IllegalArgumentException()
            }
            this.action = FlorixButton.Action.START
        }.into())
    }

    context(InteractionData)
    suspend fun updateGoogleStatistics() {
        RequestBuilder("1_8eutglTucjqgo-sPsdNrlFf-vjKADXrdPDj389wwbY").addAll(
            "Data!A2",
            db.statistics.find(Statistics::meta eq "analysis").toList()
                .map { listOf(defaultTimeFormat.format(it.timestamp.toEpochMilli()), it.count) })
            .execute()
    }

    private data class CoachData(val coachId: Long, val roleId: Long, val prefix: String)

    context(InteractionData)
    suspend fun initializeCoachSeason() {
        val teams = mapOf(
            "Dragorangensaft" to CoachData(268813717863530496, 1159431733557608458, "EDS"),
            "Roserades Restaurants" to CoachData(230715385962430465, 1159432029218283551, "ROS"),
            "Let Him Cook" to CoachData(302421572004872193, 1159432116757614602, "LHC"),
            "Dönersichel" to CoachData(293827461698027521, 1159432596116209665, "DS"),
            "Keldeogg's Frosties" to CoachData(441290844381642782, 1159432673756975154, "KF"),
            "Muffin-san's little bakery" to CoachData(264333612432752640, 1159432842841948230, "MLB"),
            "Sweet Tooth" to CoachData(725650285858521128, 1159432937381576746, "ST"),
            "Verspeisen sie Barsch?" to CoachData(207211269911085056, 1159432981472096296, "VSA"),
            "Flutsch-Finger Fluffeluff" to CoachData(310517476322574338, 1159433134249627648, "FFF"),
            "Spicy Dino Nugget Gang" to CoachData(239836406594273280, 1159433205582147685, "SDG"),
            "Well-Baked Backel" to CoachData(567135876308795392, 1159433481328283760, "WBB"),
            "Ape Tower" to CoachData(324265924905402370, 1159433552413335603, "PZT")
        )
        val aslCoachData = ASLCoachData(
            newId(),
            table = teams.keys.toList(),
            data = teams.mapValues {
                val data = it.value
                TeamData(
                    members = mutableMapOf(0 to data.coachId),
                    points = 4500,
                    role = data.roleId,
                    prefix = data.prefix
                )
            },
            sid = "1U7XDcLrJT8Y4TkP1Gm6wpdpDOn256GBf8-q3zBonuso",
            originalorder = List(12) { it }.shuffled(SecureRandom()).toMutableList(),
            config = Config(),
        )
        db.aslcoach.insertOne(aslCoachData)
    }

    context(InteractionData)
    fun flegmonSendRules(args: PrivateData) {
        flegmonjda.getTextChannelById(args().toLong())!!
            .send(components = RoleManagement.RuleAcceptButton().into()).queue()
    }

    context(InteractionData)
    fun flegmonSendRoles(args: PrivateData) {
        flegmonjda.getTextChannelById(args().toLong())!!.send(components = RoleManagement.RoleGetMenu().into())
            .queue()
    }

    context(InteractionData)
    suspend fun deleteTierlist(args: PrivateData) {
        reply(Tierlist(args().toLong()).deleteAllMons().toString())
    }

    context(InteractionData)
    suspend fun checkTL(args: PrivateData) {
        TierlistBuilderConfigurator.checkTL(args().toLong())
    }

    context(InteractionData)
    suspend fun fetchYTChannelsForLeague(args: PrivateData) {
        val league = db.league(args[0])
        db.ytchannel.insertMany(league.table.zip(args.drop(1).map {
            if ("@" !in it) it.substringAfter("channel/") else
                Google.fetchChannelId(it.substringAfter("@"))
        }).mapIndexedNotNull { index, data ->
            val (id, channelId) = data
            val cid = channelId ?: return@mapIndexedNotNull run {
                logger.warn("No channel found for $id (testing ${args[index + 1]})")
                null
            }
            YTChannel(id, cid)
        })
    }

    context(InteractionData)
    fun getGuildIcon(args: PrivateData) {
        reply(jda.getGuildById(args().toLong())!!.iconUrl.toString())
    }

    context(InteractionData)
    suspend fun fixIPLTip() {
        val num = 1
        val ids = listOf(1211915623995670578, 1211915626814111785, 1211915629330829382, 1211915630857682945)
        with(db.league("IPLS4L2") as IPL) {
            val tip = tipgame!!
            val channel = jda.getTextChannelById(tip.channel)!!
            val matchups = getMatchupsIndices(num)
            for ((index, matchup) in matchups.withIndex()) {
                val u1 = matchup[0]
                val u2 = matchup[1]
                val base: ArgBuilder<TipGameManager.VoteButton.Args> = {
                    this.leaguename = this@with.leaguename
                    this.gameday = num
                    this.index = index
                }
                val t1 = teamtable[u1]
                val t2 = teamtable[u2]
                channel.editMessageComponentsById(
                    ids[index],
                    ActionRow.of(TipGameManager.VoteButton(t1, emoji = Emoji.fromFormatted(emotes[u1])) {
                        base()
                        this.userindex = u1
                    }, TipGameManager.VoteButton(t2, emoji = Emoji.fromFormatted(emotes[u2])) {
                        base()
                        this.userindex = u2
                    }).into()
                ).queue()
            }
        }
    }
}

data class PrivateData(
    val split: List<String>
) : List<String> by split {
    operator fun invoke() = split[0]
}
