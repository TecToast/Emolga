package de.tectoast.emolga.features

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.bot.EmolgaMain.flegmonjda
import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.features.draft.SignupManager.Button
import de.tectoast.emolga.features.draft.during.DraftPermissionCommand
import de.tectoast.emolga.features.flegmon.RoleManagement
import de.tectoast.emolga.features.flo.FlorixButton
import de.tectoast.emolga.ktor.setupYTSuscribtions
import de.tectoast.emolga.ktor.subscribeToYTChannel
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.NDS
import de.tectoast.emolga.league.VideoProvideStrategy
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.dconfigurator.impl.TierlistBuilderConfigurator
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.json.*
import de.tectoast.emolga.utils.json.emolga.ASLCoachData
import de.tectoast.emolga.utils.json.emolga.Config
import de.tectoast.emolga.utils.json.emolga.Statistics
import de.tectoast.emolga.utils.json.emolga.TeamData
import de.tectoast.emolga.utils.repeat.RepeatTask
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.interactions.components.ActionComponent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.utils.FileUpload
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.litote.kmongo.*
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.regex.Pattern
import javax.imageio.ImageIO
import kotlin.time.measureTime

@Suppress("unused")
object PrivateCommands {
    private val logger = LoggerFactory.getLogger(PrivateCommands::class.java)
    private val DOUBLE_BACKSLASH = Pattern.compile("\\\\")

    context(InteractionData)
    fun printCache() {
        Translation.translationsCacheGerman.forEach { (str, t) ->
            logger.info(str)
            logger.info(t.toString())
            logger.info("=====>")
        }
        logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>")
        Translation.translationsCacheEnglish.forEach { (str, t) ->
            logger.info(str)
            logger.info(t.toString())
            logger.info("<=====")
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
            onlySpecifiedUsers = args.drop(2).map { it.toInt() }.toIntArray()
        )
    }

    context(InteractionData)
    suspend fun matchUps(args: PrivateData) {
        NDS.doMatchUps(args[0].toInt(), args[1].toBooleanStrict())
    }

    context(InteractionData)
    suspend fun tipGameLockButtons(args: PrivateData) {
        db.league(args[0]).executeTipGameLockButtons(args[1].toInt())
    }

    context(InteractionData)
    suspend fun printTipGame(args: PrivateData) {
        reply(db.tipgameuserdata.find(TipGameUserData::league eq args()).toList().asSequence()
            .map { it.user to it.correctGuesses.values.sumOf { l -> l.size } }.sortedByDescending { it.second }
            .mapIndexed { index, pair -> "${index + 1}. <@${pair.first}>: ${pair.second}" }
            .joinToString("\n", prefix = "```", postfix = "```")
        )
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

    context(InteractionData)
    suspend fun closeSignup(args: PrivateData) {
        db.signups.get(args().toLong())!!.closeSignup(forced = true)
    }

    context(InteractionData)
    suspend fun reopenSignup(args: PrivateData) {
        db.signups.get(args[0].toLong())!!.reopenSignup(args[1].toInt())
    }

    context(InteractionData)
    suspend fun finishOrdering(args: PrivateData) {
        done()
        val gid = args().toLong()
        val guild = jda.getGuildById(gid)!!
        val data = db.signups.get(gid)!!
        val roleMap = data.conferenceRoleIds.mapValues { guild.getRoleById(it.value) }
        data.users.forEach {
            val role = roleMap[it.conference] ?: return@forEach
            it.users.forEach { uid ->
                guild.addRoleToMember(UserSnowflake.fromId(uid), role).queue()
                delay(2000)
            }
        }
    }

    context(InteractionData)
    suspend fun signupUpdate(args: PrivateData) {
        val (guild, user) = args.map { it.toLong() }
        val ligaStartData = db.signups.get(guild)!!
        val data = ligaStartData.getDataByUser(user)!!
        jda.getTextChannelById(ligaStartData.signupChannel)!!
            .editMessageById(data.signupmid!!, data.toMessage(ligaStartData)).queue()
    }

    context(InteractionData)
    suspend fun sort(args: PrivateData) {
        db.league(args()).docEntry!!.sort()
    }

    context(InteractionData)
    suspend fun unsignupUser(args: PrivateData) {
        val (guild, user) = args.map { it.toLong() }
        val signup = db.signups.get(guild)!!
        val data = signup.getDataByUser(user) ?: return
        jda.getTextChannelById(signup.signupChannel)!!.deleteMessageById(data.signupmid!!).queue()
        (signup.logoSettings as? LogoSettings.Channel)?.let { settings ->
            data.logomid?.let {
                jda.getTextChannelById(settings.channelId)!!.deleteMessageById(it).queue(null, null)
            }
        }
        val wasFull = signup.full
        signup.users.removeIf { user in it.users }
        if (wasFull) {
            val channel = jda.getTextChannelById(signup.announceChannel)!!
            channel.editMessageComponentsById(
                signup.announceMessageId, Button().into()
            ).queue()
        }
        signup.updateSignupMessage()
        signup.save()
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
        val nds = db.nds()
        reply("```" + nds.run { table.indices - nominations.current().keys }.joinToString { "<@${nds[it]}>" } + "```")
    }

    private val teamGraphicScope = createCoroutineScope("TeamGraphics", Dispatchers.IO)
    context(InteractionData)
    fun teamgraphics(args: PrivateData) {
        suspend fun List<DraftPokemon>.toTeamGraphics() = FileUpload.fromData(ByteArrayOutputStream().also {
            ImageIO.write(
                TeamGraphics.fromDraftPokemon(this).first, "png", it
            )
        }.toByteArray(), "yay.png")
        done(true)
        teamGraphicScope.launch {
            val league = db.league(args[0])
            val user = args[1].toLong()
            val tc = args.getOrNull(2)?.let { jda.getTextChannelById(it)!! } ?: textChannel
            if (user > -1) {
                tc.sendMessage("Kader von <@${user}>:").addFiles(league.picks[league(user)]!!.toTeamGraphics()).queue()
                return@launch
            }
            league.picks.entries.map { (u, l) ->
                async {
                    val (bufferedImage, _) = TeamGraphics.fromDraftPokemon(l)
                    u to FileUpload.fromData(ByteArrayOutputStream().also {
                        ImageIO.write(
                            bufferedImage, "png", it
                        )
                    }.toByteArray(), "yay.png")

                }
            }.awaitAll().forEach { tc.sendMessage("Kader von <@${it.first}>:").addFiles(it.second).queue() }
        }

    }

    context(InteractionData)
    suspend fun florixcontrol(args: PrivateData) {
        (if (args[0].toBoolean()) jda.openPrivateChannelById(args[1])
            .await() else jda.getTextChannelById(args[1])!!).send(
            ":)",
            components = FlorixButton("Server starten", ButtonStyle.PRIMARY) {
                this.pc = when (args[2]) {
                    "2" -> PC.FLORIX_2
                    "4" -> PC.FLORIX_4
                    else -> throw IllegalArgumentException()
                }
                this.action = FlorixButton.Action.START
            }.into()
        ).queue()
    }

    context(InteractionData)
    suspend fun updateGoogleStatistics() {
        RequestBuilder("1_8eutglTucjqgo-sPsdNrlFf-vjKADXrdPDj389wwbY").suppressMessages().addAll(
            "Data!A2",
            db.statistics.find(Statistics::meta eq "analysis").toList()
                .map { listOf(defaultTimeFormat.format(it.timestamp.toEpochMilli()), it.count) }).execute()
    }

    private data class CoachData(val coachId: Long, val roleId: Long, val prefix: String)

    context(InteractionData)
    suspend fun initializeCoachSeason() {
        val teams = mapOf(
            "BINDING OF ISSOO" to CoachData(324265924905402370, 1272878415342469131, "BOI"),
            "FOLLOW THE FIREFLIES" to CoachData(239836406594273280, 1272878693689331733, "FTF"),
            "KNIGHTLEY GALLADES" to CoachData(689440028505538574, 1272878151390855181, "KG"),
            "LIVE REACT OF RUIN" to CoachData(232950767454126090, 1272905894031523860, "LROR"),
            "DÖNERSICHEL" to CoachData(293827461698027521, 1273189725905223721, "DS"),
            "NEVAIO CITY NIDOQUEENS" to CoachData(317306765265862666, 1273275766184087574, "NCN"),
            "THE TERMANITAR" to CoachData(429172327985446912, 1273557295464321075, "TT"),
            "1. CPC KÖLN" to CoachData(367704349528555520, 1273626291551338496, "CPC"),
            "NOOB" to CoachData(207211269911085056, 1274280568258957333, "NOOB"),
            "SUPER TANDEBROS" to CoachData(441290844381642782, 1274280735057903658, "ST"),
            "LOW-LIFE LUCARIO" to CoachData(694816734053531678, 1274280403833982978, "LLL"),
            "HENNY'S HÄHNCHEN" to CoachData(297010892678234114, 1274280928386220113, "HENNY")
        )
        val aslCoachData = ASLCoachData(
            newId(),
            table = teams.keys.toList(),
            data = teams.mapValues {
                val data = it.value
                TeamData(
                    members = mutableMapOf(0 to data.coachId), points = 6000, role = data.roleId, prefix = data.prefix
                )
            },
            sid = "1Hd-YHH-9YVgPy23eO9-i16rsZF_vLkifKbUd0oGW5WA",
            originalorder = List(12) { it }.shuffled(SecureRandom()).toMutableList(),
            config = Config(),
        )
        db.aslcoach.insertOne(aslCoachData)
    }

    context(InteractionData)
    fun flegmonSendRules(args: PrivateData) {
        flegmonjda.getTextChannelById(args().toLong())!!.send(components = RoleManagement.RuleAcceptButton().into())
            .queue()
    }

    context(InteractionData)
    fun flegmonSendRoles(args: PrivateData) {
        flegmonjda.getTextChannelById(args().toLong())!!
            .send(components = RoleManagement.RoleGetMenu(placeholder = "Rollen auswählen").into()).queue()
    }

    context(InteractionData)
    suspend fun deleteTierlist(args: PrivateData) {
        reply(Tierlist(args().toLong()).deleteAllMons().toString())
    }

    context(InteractionData)
    suspend fun checkTL(args: PrivateData) {
        TierlistBuilderConfigurator.checkTL(args[0].toLong(), args.getOrNull(1))
    }

    context(InteractionData)
    suspend fun fetchYTChannelsForLeague(args: PrivateData) {
        val league = db.league(args[0])
        db.ytchannel.insertMany(league.table.zip(args.drop(1).map {
            if ("@" !in it) it.substringAfter("channel/") else Google.fetchChannelId(it.substringAfter("@"))
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
    suspend fun testYTSend(args: PrivateData) {
        val league = db.league(args[0])
        league.executeYoutubeSend(
            ytTC = args[1].toLong(),
            gameday = args[2].toInt(),
            battle = args[3].toInt(),
            strategy = VideoProvideStrategy.Fetch,
            overrideEnabled = args.getOrNull(4)?.toBooleanStrict() == true
        )
    }

    context(InteractionData)
    suspend fun testYTSendSub(args: PrivateData) {
        val league = db.league(args[0])
        val gameday = args[2].toInt()
        val battle = args[3].toInt()
        league.executeYoutubeSend(
            ytTC = args[1].toLong(),
            gameday = gameday,
            battle = battle,
            strategy = VideoProvideStrategy.Subscribe(league.persistentData.replayDataStore.data[gameday]!![battle]!!.ytVideoSaveData),
            overrideEnabled = args.getOrNull(4)?.toBooleanStrict() == true
        )
    }

    context(InteractionData)
    suspend fun dbSpeedLetsGo() {
        val col = db.db.getCollection<NameCon>("customnamecon")
        val test = "Emolga"
        val guildId = 0L
        logger.info(measureTime {
            newSuspendedTransaction {
                val query1 =
                    ((NameConventionsDB.GERMAN eq test) or (NameConventionsDB.ENGLISH eq test) or (NameConventionsDB.SPECIFIED eq test) or (NameConventionsDB.SPECIFIEDENGLISH eq test)) and (NameConventionsDB.GUILD eq 0 or (NameConventionsDB.GUILD eq guildId))
                NameConventionsDB.selectAll().where {
                    query1
                }
            }
        }.toString())
        val query2 = and(
            or(
                NameCon::german eq test,
                NameCon::english eq test,
                NameCon::specified eq test,
                NameCon::specenglish eq test,
            ), NameCon::guild eq guildId
        )
        logger.info(query2.json)
        logger.info(measureTime {
            col.findOne(query2)
        }.toString())
    }

    @Serializable
    data class NameCon(
        val guild: Long,
        val german: String,
        val english: String,
        val specified: String,
        val specenglish: String,
        val hasHyphen: Boolean,
        val common: Boolean
    )

    context(InteractionData)
    suspend fun disableIt(args: PrivateData) {
        for (mid in args.drop(1)) {
            val m = jda.getTextChannelById(args[0])!!.retrieveMessageById(mid).await()
            m.editMessageComponents(m.components.map {
                if (it is ActionRow) ActionRow.of(it.components.map { b ->
                    if (b is ActionComponent) b.withDisabled(true) else b
                }) else it
            }).queue()
        }
    }

    context(InteractionData)
    suspend fun enableIt(args: PrivateData) {
        for (mid in args.drop(1)) {
            val m = jda.getTextChannelById(args[0])!!.retrieveMessageById(mid).await()
            m.editMessageComponents(m.components.map {
                if (it is ActionRow) ActionRow.of(it.components.map { b ->
                    if (b is ActionComponent) b.withDisabled(false) else b
                }) else it
            }).queue()
        }
    }

    context(InteractionData)
    suspend fun analyseMatchresults(args: PrivateData) {
        val league = db.league(args[0])
        league.persistentData.replayDataStore.data[args[1].toInt()]!!.forEach { (_, replay) ->
            league.docEntry!!.analyseWithoutCheck(
                listOf(replay),
                withSort = false,
                realExecute = args[2].toBooleanStrict()
            )
        }
    }

    context(InteractionData)
    suspend fun executeTipGameSending(args: PrivateData) {
        db.league(args[0]).executeTipGameSending(args[1].toInt())
    }

    context(InteractionData)
    suspend fun speedLeague() {
        reply((0..<10).map { measureTime { db.league("IPLS4L1") }.inWholeMilliseconds }.average().toString())
        reply((0..<10).map { measureTime { db.leagueByGuild(Constants.G.VIP, 297010892678234114) }.inWholeMilliseconds }
            .average().toString())
    }

    context(InteractionData)
    suspend fun copyTLToMyServer(args: PrivateData) {
        newSuspendedTransaction {
            val gid = args().toLong()
            Tierlist.batchInsert(Tierlist[gid]!!.retrieveAll(), shouldReturnGeneratedValues = false) {
                this[Tierlist.guild] = Constants.G.MY
                this[Tierlist.pokemon] = it.name
                this[Tierlist.tier] = it.tier
            }
            NameConventionsDB.batchInsert(NameConventionsDB.selectAll().where { NameConventionsDB.GUILD eq gid }) {
                this[NameConventionsDB.GUILD] = Constants.G.MY
                this[NameConventionsDB.GERMAN] = it[NameConventionsDB.GERMAN]
                this[NameConventionsDB.ENGLISH] = it[NameConventionsDB.ENGLISH]
                this[NameConventionsDB.SPECIFIED] = it[NameConventionsDB.SPECIFIED]
                this[NameConventionsDB.SPECIFIEDENGLISH] = it[NameConventionsDB.SPECIFIEDENGLISH]
                this[NameConventionsDB.HASHYPHENINNAME] = it[NameConventionsDB.HASHYPHENINNAME]
                this[NameConventionsDB.COMMON] = it[NameConventionsDB.COMMON]
            }
        }
    }

    context(InteractionData)
    suspend fun addDraftPermission(args: PrivateData) {
        League.executeOnFreshLock(args[0]) {
            DraftPermissionCommand.performPermissionAdd(
                args[1].toLong(),
                args[2].toLong(),
                DraftPermissionCommand.Allow.Mention.valueOf(args[3])
            )
            save("addDraftPermission")
        }
    }

    context(InteractionData)
    suspend fun tipgameAdditionals(args: PrivateData) {
        val leaguename = args[0]
        val topkiller = args[1]
        val top3 = args.drop(2).map { it.toInt() } // don't access 0
        db.tipgameuserdata.find(TipGameUserData::league eq leaguename).toFlow().collect {
            val filter = and(TipGameUserData::user eq it.user, TipGameUserData::league eq leaguename)
            if (it.topkiller == topkiller) db.tipgameuserdata.updateOne(
                filter,
                set(TipGameUserData::correctTopkiller setTo true)
            )
            for (i in 1..3) {
                if (it.orderGuesses[i] == top3[i - 1]) db.tipgameuserdata.updateOne(
                    filter,
                    addToSet(TipGameUserData::correctOrderGuesses, i)
                )
            }
        }
    }

    context(InteractionData)
    suspend fun moveLeaguesToArchive(args: PrivateData) {
        val archiveLeague = db.db.getCollection<League>("oldleague")
        val currentLeague = db.drafts
        val archiveMR = db.db.getCollection<MatchResult>("oldmatchresults")
        val currentMR = db.db.getCollection<MatchResult>("matchresults")
        args.forEach {
            if (!it.matches(Regex("^NDSS\\d+$"))) {
                val league = db.league(it)
                archiveLeague.insertOne(league)
                currentLeague.deleteOne(League::leaguename eq it)
            }
            archiveMR.insertMany(currentMR.find(MatchResult::leaguename eq it).toList())
            currentMR.deleteMany(MatchResult::leaguename eq it)
        }
    }

    context(InteractionData)
    suspend fun switchUser(args: PrivateData) {
        val league = db.league(args[0])
        val old = args[1].toLong()
        val new = args[2].toLong()
        db.drafts.updateOne(
            and(League::leaguename eq league.leaguename, League::table contains old), combine(
                set(League::table.posOp setTo new),
                rename(League::picks.keyProjection(old), League::picks.keyProjection(new))
            )
        )
    }

    context(InteractionData)
    suspend fun startGroupedPoints() {
        db.shinyEventConfig.only().updateDiscord(jda)
    }

    context(InteractionData)
    suspend fun subscribeToYT(args: PrivateData) {
        subscribeToYTChannel(args())
    }

    context(InteractionData)
    suspend fun enableMaintenance(args: PrivateData) {
        val reason = args()
        db.config.updateOnly(set(de.tectoast.emolga.utils.json.Config::maintenance setTo reason))
        EmolgaMain.maintenance = reason
        EmolgaMain.updatePresence()
    }

    context(InteractionData)
    suspend fun disableMaintenance() {
        db.config.updateOnly(set(de.tectoast.emolga.utils.json.Config::maintenance setTo null))
        EmolgaMain.maintenance = null
        EmolgaMain.updatePresence()
    }

    context(InteractionData)
    suspend fun updateFlegmonSlash() {
        EmolgaMain.featureManager().updateFeatures(flegmonjda)
    }

    // Order:
    // Announcechannel mit Button
    // Channel in dem die Anmeldungen reinkommen
    // AnzahlTeilnehmer
    // Message
    context(InteractionData)
    suspend fun createSignup(args: PrivateData) {
        val tc = jda.getTextChannelById(args[0])!!
        val maxUsers = args[2].toInt()
        val text = args.drop(3).joinToString(" ").replace("\\n", "\n")
        val messageid =
            tc.sendMessage(
                text + "\n\n**Teilnehmer: 0/${maxUsers.takeIf { it > 0 } ?: "?"}**")
                .addActionRow(Button()).await().idLong
        db.signups.insertOne(
            LigaStartData(
                guild = tc.guild.idLong,
                signupChannel = args[1].toLong(),
                signupMessage = text,
                announceChannel = tc.idLong,
                announceMessageId = messageid,
                maxUsers = maxUsers,
            )
        )
    }

    context(InteractionData)
    suspend fun fixLogos() {
        val lsData = db.signups.only()
        val tc = jda.getTextChannelById(lsData.signupChannel)!!
        val messages = tc.iterableHistory.takeWhileAsync { it.author.idLong == jda.selfUser.idLong }.await()
        for (m in messages) {
            if (m.attachments.isEmpty()) continue
            val hash = m.attachments.first().url.substringBefore("?").substringAfterLast("/").substringBefore(".")
            val uid = m.mentions.users.first().idLong
            lsData.users.first { it.users.contains(uid) }.logoChecksum = hash
        }
        lsData.save()
    }

    context(InteractionData)
    suspend fun printTables(args: PrivateData) {
        reply(db.drafts.find(League::guild eq args().toLong()).toList().joinToString("\n") {
            it.leaguename + " " + it.table.joinToString { m -> "<@$m>" }
        })
    }


    context(InteractionData)
    suspend fun setupYouTubeSubscriptions(args: PrivateData) {
        setupYTSuscribtions()
    }

}

data class PrivateData(
    val split: List<String>
) : List<String> by split {
    operator fun invoke() = split[0]
}
