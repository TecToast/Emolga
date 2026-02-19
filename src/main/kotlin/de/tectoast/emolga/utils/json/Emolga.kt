@file:OptIn(
    ExperimentalSerializationApi::class,
    ExperimentalTime::class
) @file:UseSerializers(InstantAsDateSerializer::class)

package de.tectoast.emolga.utils.json

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.credentials.Credentials
import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.database.exposed.GuildLanguageDB
import de.tectoast.emolga.database.exposed.LogoChecksumDB
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.RealInteractionData
import de.tectoast.emolga.features.draft.K18n_Signup
import de.tectoast.emolga.features.draft.LogoCommand.allowedFileFormats
import de.tectoast.emolga.features.draft.SignupManager
import de.tectoast.emolga.features.draft.TipGame
import de.tectoast.emolga.features.various.ShinyEvent
import de.tectoast.emolga.features.various.ShinyEvent.SingleGame
import de.tectoast.emolga.ktor.InstantAsDateSerializer
import de.tectoast.emolga.league.League
import de.tectoast.emolga.league.NDS
import de.tectoast.emolga.league.config.LeagueConfig
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.json.emolga.ASLCoachData
import de.tectoast.emolga.utils.json.showdown.Pokemon
import de.tectoast.emolga.utils.repeat.IntervalTaskKey
import de.tectoast.emolga.utils.repeat.ScheduledTask
import de.tectoast.emolga.utils.showdown.BattleContext
import de.tectoast.emolga.utils.showdown.SDPlayer
import de.tectoast.emolga.utils.teamgraphics.ImageUtils
import de.tectoast.emolga.utils.teamgraphics.TeamGraphicGenerator
import de.tectoast.generic.K18n_SignupNoun
import de.tectoast.k18n.generated.K18nMessage
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.*
import dev.minn.jda.ktx.messages.editMessage
import dev.minn.jda.ktx.messages.into
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.*
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.components.Component
import net.dv8tion.jda.api.components.attachmentupload.AttachmentUpload
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.modals.Modal
import net.dv8tion.jda.api.utils.FileUpload
import org.bson.BsonDocument
import org.bson.conversions.Bson
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.security.MessageDigest
import javax.imageio.ImageIO
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.roundToInt
import kotlin.reflect.KProperty1
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.measureTimedValue
import org.litote.kmongo.serialization.configuration as mongoConfiguration

val mdb: MongoEmolga get() = delegateDb ?: error("MongoDB not initialized!")

private const val DEFAULT_DB_NAME = "emolga"
private var delegateDb: MongoEmolga? = null

private val logger = KotlinLogging.logger {}

fun initMongo(dbUrl: String = Credentials.tokens.mongoDB, dbName: String = DEFAULT_DB_NAME) {
    delegateDb?.let { error("MongoDB already initialized!") }
    delegateDb = MongoEmolga(dbUrl, dbName)
}

class MongoEmolga(dbUrl: String, dbName: String) {
    private val logger = KotlinLogging.logger {}
    val db = run {
        mongoConfiguration = mongoConfiguration.copy(classDiscriminator = "type", encodeDefaults = false)
        KMongo.createClient(dbUrl).coroutine.getDatabase(dbName)
    }

    val ndsQuery by lazy { League::leaguename regex "^NDS" }

    val config by lazy { db.getCollection<GeneralConfig>("config") }
    val signups by lazy { db.getCollection<LigaStartData>("signups") }
    val league by lazy { db.getCollection<League>("league") }
    val nameconventions by lazy { db.getCollection<NameConventions>("nameconventions") }
    val pokedex by lazy { db.getCollection<Pokemon>("pokedex") }
    val pickedMons by lazy { db.getCollection<PickedMonsData>("pickedmons") }
    val tierlist by lazy { db.getCollection<Tierlist>("tierlist") }
    val shinyEventConfig by lazy { db.getCollection<ShinyEventConfig>("shinyeventconfig") }
    val shinyEventResults by lazy { db.getCollection<ShinyEventResult>("shinyeventresults") }
    val aslcoach by lazy { db.getCollection<ASLCoachData>("aslcoachdata") }
    val matchresults by lazy { db.getCollection<LeagueEvent>("matchresults") }
    val statestore by lazy { db.getCollection<StateStore>("statestore") }
    val intervaltaskdata by lazy { db.getCollection<IntervalTaskData>("intervaltaskdata") }
    val scheduledtask by lazy { db.getCollection<ScheduledTask>("scheduledtask") }
    val remoteServerControl by lazy { db.getCollection<RemoteServerControl>("remoteservercontrol") }
    val ladderTournament by lazy { db.getCollection<LadderTournament>("laddertournament") }
    val defaultNameConventions = OneTimeCache {
        nameconventions.find(NameConventions::guild eq 0).first()!!.data
    }

    suspend fun league(name: String) = getLeague(name)!!
    suspend fun getLeague(name: String) = league.findOne(League::leaguename eq name)
    suspend fun nds() = (league.findOne(ndsQuery) as NDS)

    suspend fun leagueByGuild(gid: Long, vararg uids: Long) = league.findOne(
        League::guild eq gid, *(if (uids.isEmpty()) emptyArray() else arrayOf(League::table all uids.toList()))
    )

    suspend fun leaguesByGuild(gid: Long, vararg uids: Long): List<League> {
        return league.find(
            League::guild eq gid, *(if (uids.isEmpty()) emptyArray() else arrayOf(League::table all uids.toList()))
        ).toList()
    }

    suspend fun leagueByDisplayName(gid: Long, displayName: String) = league.findOne(
        League::guild eq gid,
        or(League::leaguename eq displayName, League::config / LeagueConfig::tipgame / TipGame::withName eq displayName)
    )

    suspend fun leagueForAutocomplete(tc: Long, gid: Long, user: Long) =
        league.find(or(League::tcid eq tc, and(League::guild eq gid, League::table contains user))).toList()
            .maxByOrNull { it.tcid == tc }

    context(iData: InteractionData)
    suspend fun leagueByCommand() = leagueByGuild(iData.gid, iData.user)

    suspend fun getDataObject(mon: String, guild: Long = 0): Pokemon {
        return pokedex.get(NameConventionsDB.getDiscordTranslation(mon, guild, true)!!.official.toSDName())!!
    }


    private val scanScope = createCoroutineScope("ScanScope")

    suspend fun leagueByGuildAdvanced(
        gid: Long, game: List<SDPlayer>, ctx: BattleContext, prefetchedLeague: League? = null, vararg uids: Long
    ): LeagueResult? {
        if (ctx.randomBattle) {
            return getLeagueResultWithoutPicks(gid, uids)
        }
        val matchMons = game.map { it.pokemon.map { mon -> mon.draftname } }
        val (leagueResult, duration) = measureTimedValue {
            val allOtherFormesGerman: Array<Map<String, List<String>>> = Array(2) { mutableMapOf() }
            val dps = uids.indices.map { index ->
                scanScope.async {
                    val mons = matchMons[index]
                    val otherFormesEngl = mutableMapOf<DraftName, List<String>>()
                    val (possibleOtherForm, noOtherForm) = mons.partition {
                        ((it.data?.otherFormes?.filterNot { forme -> "-Alola" in forme || "-Galar" in forme || "-Hisui" in forme }
                            .orEmpty() + it.data?.baseSpecies).filterNotNull().also { list ->
                            otherFormesEngl[it] = list
                        }.size) > 0
                    }
                    val allSDTranslations =
                        NameConventionsDB.getAllSDTranslationOnlyOfficialGerman(possibleOtherForm.flatMap { otherFormesEngl[it].orEmpty() })
                    val otherFormesGerman = otherFormesEngl.map { (k, v) ->
                        k.official to v.map { allSDTranslations[it] ?: it }
                    }.toMap()
                    allOtherFormesGerman[index] = otherFormesGerman
                    val filters = possibleOtherForm.map {
                        or(
                            PickedMonsData::mons contains it.official,
                            otherFormesGerman[it.official].orEmpty().let { mega -> PickedMonsData::mons `in` mega })
                    }.toTypedArray()
                    val query = and(
                        *(if (noOtherForm.isNotEmpty()) arrayOf((PickedMonsData::mons all noOtherForm.map { it.official })) else emptyArray()),
                        *filters
                    )
                    val finalQuery = and(PickedMonsData::guild eq gid, query)
                    pickedMons.find(finalQuery).toList()
                }
            }.awaitAll()
            val (dp1, dp2) = dps
            var resultList: List<PickedMonsData>? = null
            outer@ for (d1 in dp1) {
                for (d2 in dp2) {
                    if (d1.leaguename == d2.leaguename) {
                        resultList = listOf(d1, d2)
                        break@outer
                    }
                }
            }
            if (resultList == null) return@measureTimedValue null
            for (i in 0..<2) {
                val pickedMons = resultList[i].mons
                val formes = allOtherFormesGerman[i]
                for (sdMon in game[i].pokemon) {
                    if (sdMon.draftname.official !in pickedMons) {
                        sdMon.draftname = NameConventionsDB.getSDTranslation(
                            formes[sdMon.draftname.official]!!.first { it in pickedMons },
                            gid
                        )!!
                    }
                }
            }
            val league = prefetchedLeague ?: league(resultList[0].leaguename)
            LeagueResult(league, resultList.map { it.idx })
        }
        logger.debug { "DURATION: ${duration.inWholeMilliseconds}" }
        return leagueResult ?: getLeagueResultWithoutPicks(gid, uids)
    }

    private suspend fun getLeagueResultWithoutPicks(gid: Long, uids: LongArray): LeagueResult? {
        val league = leagueByGuild(gid, *uids)?.takeIf { it.config.triggers.randomBattle } ?: return null
        return LeagueResult(league, uids.map { league.table.indexOf(it) })
    }
}

@Serializable
data class IntervalTaskData(
    val name: IntervalTaskKey,
    val nextExecution: Instant,
    val notAfter: Instant = Instant.DISTANT_FUTURE,
)

@Serializable
data class PickedMonsData(val leaguename: String, val guild: Long, val idx: Int, val mons: List<String>)
data class LeagueResult(val league: League, val uindices: List<Int>) {
    val mentions = uindices.map { "<@${league[it]}>" }
}

@Serializable
data class TypeIcon(
    val typename: String, val formula: String
)

@Serializable
data class GeneralConfig(
    val teamgraphicShinyOdds: Int,
    val guildsToUpdate: List<Long> = listOf(),
    val raikou: Boolean = false,
    val ytLeagues: Map<String, Long> = mapOf(),
    var maintenance: String? = null,
)

@Serializable
data class NameConventions(
    val guild: Long, val data: MutableMap<String, String> = mutableMapOf()
)

data class ModalInputOptions(
    val label: K18nMessage,
    val required: Boolean,
    val placeholder: K18nMessage? = null,
    val requiredLength: IntRange? = TextInputDefaults.requiredLength,
    val list: List<String>? = null
)

sealed interface SignUpValidateResult {
    data class Success(val data: String) : SignUpValidateResult
    data class Error(val message: K18nMessage) : SignUpValidateResult

    companion object {
        fun wrapNullable(msg: String?, errorMsg: K18nMessage) = msg?.let { Success(it) } ?: Error(errorMsg)
    }
}

@Serializable
@Config("Anmeldungseingabe", "Eine Option, die bei der Anmeldung angegeben werden muss")
sealed class SignUpInput {
    abstract val id: String

    @Serializable
    @SerialName("SDName")
    @Config("Showdown-Name", "Selbsterklärend", prio = 2)
    data object SDName : SignUpInput() {
        override val id = SDNAME_ID

        override fun getModalInputOptions(): ModalInputOptions {
            return ModalInputOptions(label = K18n_SignupInput.SDNAME, required = true, requiredLength = 1..18)
        }

        override fun validate(data: String) = SignUpValidateResult.wrapNullable(
            data.takeIf { data.toUsername().length in 1..18 }, K18n_SignupInput.SDNAMEInvalid
        )

        override fun getDisplayTitle() = K18n_SignupInput.SDNAME
    }

    @Serializable
    @SerialName("TeamName")
    @Config("Team-Name", "Selbsterklärend", prio = 1)
    data object TeamName : SignUpInput() {
        override val id = TEAMNAME_ID

        override fun getModalInputOptions(): ModalInputOptions {
            return ModalInputOptions(label = K18n_SignupInput.TEAMNAME, required = true, requiredLength = 1..100)
        }

        override fun getDisplayTitle() = K18n_SignupInput.TEAMNAME
    }

    @Serializable
    @SerialName("OfList")
    @Config(
        "Aus einer Liste",
        "Für den Fall, dass die Teilnehmenden aus einer Menge an Optionen auswählen sollen (zum Beispiel Ligapräferenzen)"
    )
    data class OfList(
        @Config("Name", "Der Name dieser Option, z.B. Liga") val name: String = "Liga",
        @Config("Liste", "Die Liste, aus der man auswählen soll") val list: List<String> = listOf(),
        @Config(
            "Sichtbar für alle", "Ob die Auswahl in der Anmeldungsnachricht des Teilnehmenden erscheinen soll"
        ) val visibleForAll: Boolean = true
    ) : SignUpInput() {
        override val id = "$OFLIST_PREFIX_ID$name"

        override fun getModalInputOptions(): ModalInputOptions {
            return ModalInputOptions(label = name.k18n, required = true, placeholder = null, list = list)
        }

        override fun validate(data: String) = SignUpValidateResult.wrapNullable(
            list.firstOrNull { it.equals(data, ignoreCase = true) },
            K18n_SignupInput.OF_LIST_Allowed(list.joinToString(", ") { opt -> "`$opt`" })
        )

        override fun getDisplayTitle() = name.takeIf { visibleForAll }?.k18n
    }

    abstract fun getModalInputOptions(): ModalInputOptions
    open fun validate(data: String): SignUpValidateResult = SignUpValidateResult.Success(data)
    open fun getDisplayTitle(): K18nMessage? = null

    companion object {
        const val SDNAME_ID = "sdname"
        const val TEAMNAME_ID = "teamname"
        const val OFLIST_PREFIX_ID = "oflist_"
        const val LOGO_ID = "logo"
    }
}

@Serializable
@Config("Logo-Einstellungen", "Wie/wo sollen die Logos landen?")
sealed interface LogoSettings {
    @Serializable
    @SerialName("Channel")
    @Config("Logo-Channel", "Die Logos landen in einem bestimmten Channel.")
    data class Channel(
        @Config(
            "Channel",
            "Der Channel, in dem die Logos landen sollen",
            LongType.CHANNEL
        ) @Contextual val channelId: Long = 0
    ) : LogoSettings {
        override suspend fun handleLogo(
            lsData: LigaStartData, data: SignUpData, logoData: LogoInputData
        ) {
            val tc = jda.getTextChannelById(channelId)
                ?: return logger.warn { "Channel $channelId for LogoSettings not found" }
            data.logomid?.let { mid -> tc.deleteMessageById(mid).queue(null, null) }
            val logoMid = tc.sendMessage(getMsgTitle(data).translateToGuildLanguage(lsData.guild))
                .addFiles(logoData.toFileUpload()).await().idLong
            data.logomid = logoMid
            lsData.save()
        }

        private fun getMsgTitle(data: SignUpData): K18nMessage =
            K18n_SignupInput.LogoMsg(data.formatName(), data.data["teamname"]?.let { " ($it)" }.orEmpty())

        override suspend fun handleSignupChange(
            lsData: LigaStartData,
            data: SignUpData
        ) {
            val tc = jda.getTextChannelById(channelId)
                ?: return logger.warn { "Channel $channelId for LogoSettings not found" }
            data.logomid?.let { mid ->
                tc.editMessage(mid.toString(), getMsgTitle(data).translateToGuildLanguage(lsData.guild)).queue()
            }
        }

        override suspend fun handleSignupRemoved(lsData: LigaStartData, data: SignUpData) {
            val tc = jda.getTextChannelById(channelId)
                ?: return logger.warn { "Channel $channelId for LogoSettings not found" }
            data.logomid?.let { mid -> tc.deleteMessageById(mid).queue() }
        }
    }

    @Serializable
    @SerialName("WithSignupMessage")
    @Config("An der Anmeldung", "Die Logos werden an die Anmeldenachricht selbst drangehängt.")
    data object WithSignupMessage : LogoSettings {
        override suspend fun handleLogo(
            lsData: LigaStartData, data: SignUpData, logoData: LogoInputData
        ) {
            val tc = jda.getTextChannelById(lsData.config.signupChannel)
                ?: return logger.warn { "SignupChannel for LogoSettings not found" }
            tc.editMessageAttachmentsById(data.signupmid!!, logoData.toFileUpload()).queue()
        }
    }

    @Serializable
    @SerialName("NotInDiscord")
    @Config("Nicht im Discord", "Die Logos werden nicht im Discord gespeichert.")
    data object NotInDiscord : LogoSettings

    suspend fun handleLogo(lsData: LigaStartData, data: SignUpData, logoData: LogoInputData) {}
    suspend fun handleSignupChange(lsData: LigaStartData, data: SignUpData) {}
    suspend fun handleSignupRemoved(lsData: LigaStartData, data: SignUpData) {}
}

@Serializable
@Config(name = "Initiale Anmeldungskonfiguration", "Die initiale Konfiguration einer Liga-Anmeldung")
data class LigaStartConfig(
    @Config(
        name = "Anmeldungschannel",
        "In welchem Channel sollen die Anmeldungen von Emolga gesammelt werden?",
        longType = LongType.CHANNEL
    ) @Contextual val signupChannel: Long,
    @Config(
        name = "Anmeldungsnachricht", "Was soll in der Anmeldungsnachricht von Emolga stehen?"
    ) val signupMessage: String,
    @Config(
        name = "Ankündingungschannel",
        "In welchem Channel soll die Anmeldungsnachricht stehen?",
        longType = LongType.CHANNEL
    ) @Contextual val announceChannel: Long,
    @Config(
        "Logo-Einstellungen", "Hier kannst du einstellen, ob/wie Logos eingesendet werden."
    ) val logoSettings: LogoSettings? = null,
    @Config(
        "Maximale Anzahl",
        "Hier kannst du einstellen, bei wie vielen Teilnehmenden die Anmeldung geschlossen werden soll. Bei 0 gibt es keine Begrenzung."
    ) var maxUsers: Int,
    @Config(
        "Versteckte Spieleranzahl",
        "Hier kannst du einstellen, ob die aktuelle Teilnehmeranzahl in der Anmeldungsnachricht angezeigt werden soll."
    )
    var hideUserCount: Boolean = false,
    @Config(
        "Teilnehmerrolle",
        "Hier kannst du eine Rolle einstellen, die die Teilnehmer automatisch bekommen sollen.",
        LongType.ROLE
    ) @Contextual val participantRole: Long? = null,
    @Config(
        "Anmeldungsstruktur", "Hier kannst du einstellen, was die Teilnehmer alles bei der Anmeldung angeben sollen."
    ) val signupStructure: List<SignUpInput> = listOf(),
)

@Serializable
@Config(name = "Anmeldung", "Alle Daten einer Anmeldung (Konfiguration und Teilnehmer)")
data class LigaStartData(
    @Contextual val guild: Long,
    @Config(name = "Konfiguration", desc = "Die Konfiguration dieser Anmeldung")
    val config: LigaStartConfig,

    val announceMessageId: Long,
    @EncodeDefault var conferences: List<String> = listOf(),
    var conferenceRoleIds: Map<String, @Contextual Long> = mapOf(),

    val users: MutableList<SignUpData> = mutableListOf(),
) {
    val maxUsersAsString
        get() = (config.maxUsers.takeIf { it > 0 } ?: "?").toString()

    @Transient
    val language = OneTimeCache {
        GuildLanguageDB.getLanguage(guild)
    }

    suspend fun buildModal(old: SignUpData?): Modal? {
        if (config.signupStructure.isEmpty()) return null
        val lang = language()
        return Modal("signup;".notNullAppend(old?.let { "change" }), K18n_SignupNoun.translateToGuildLanguage(guild)) {
            config.signupStructure.forEach {
                val options = it.getModalInputOptions()
                label(
                    label = options.label.translateTo(lang),
                    child = options.list?.let { list ->
                        StringSelectMenu(
                            customId = it.id,
                            placeholder = options.placeholder?.translateTo(lang),
                            options = list.map { opt -> SelectOption(opt, opt) })
                    } ?: TextInput(
                        customId = it.id,
                        style = TextInputStyle.SHORT,
                        required = options.required,
                        placeholder = options.placeholder?.translateTo(lang),
                        value = old?.data?.get(it.id)
                    )
                )
            }
            config.logoSettings?.let { _ ->
                label(
                    label = K18n_SignupInput.LogoLabel.translateTo(lang),
                    description = K18n_SignupInput.LogoDescription.translateTo(lang),
                    child = AttachmentUpload.create(
                        SignUpInput.LOGO_ID
                    )
                        .setRequiredRange(0, 1)
                        .setRequired(false)
                        .build()
                )
            }
        }
    }

    suspend fun handleModal(e: ModalInteractionEvent) {
        val iData = RealInteractionData(e, language())
        with(iData) {
            val change = e.modalId.substringAfter(";").isNotBlank()
            val errors = mutableListOf<String>()
            var logoAttachment: Message.Attachment? = null
            val data = e.values.associate {
                val id = it.customId
                if (id == SignUpInput.LOGO_ID) {
                    logoAttachment = it.asAttachmentList.firstOrNull()
                    return@associate "" to ""
                }
                val config = getInputConfig(id) ?: error("Modal key $id not found")
                when (val result = config.validate(
                    when (it.type) {
                        Component.Type.TEXT_INPUT -> it.asString
                        Component.Type.STRING_SELECT -> it.asStringList.first()
                        else -> error("Unsupported component type in signup modal ${it.type}")
                    }
                )) {
                    is SignUpValidateResult.Error -> {
                        errors += K18n_SignupInput.Error(config.getModalInputOptions().label.t(), result.message.t())
                            .t()
                        "" to ""
                    }

                    is SignUpValidateResult.Success -> {
                        id to result.data
                    }
                }
            }
            if (errors.isNotEmpty()) {
                iData.sendSignupErrors(errors)
                return
            }
            SignupManager.signupUser(
                gid = gid,
                uid = e.user.idLong,
                data = data.filterKeys { it.isNotEmpty() },
                logoAttachment = logoAttachment,
                isChange = change,
                iData = iData
            )
        }
    }

    private fun InteractionData.sendSignupErrors(errors: List<String>) {
        reply(K18n_Signup.SignupFailure(errors.joinToString("\n\n")), ephemeral = true)
    }

    inline fun <reified T : SignUpInput> getInputConfig() = config.signupStructure.firstOrNull { it is T } as T?
    fun getInputConfig(id: String) = config.signupStructure.firstOrNull { it.id == id }

    fun getIndexOfUser(uid: Long) = users.indexOfFirst { it.users.contains(uid) }.takeIf { it >= 0 }
    fun getDataByUser(uid: Long) = users.firstOrNull { it.users.contains(uid) }

    suspend fun save() = mdb.signups.updateOne(LigaStartData::guild eq guild, this)
    inline fun giveParticipantRole(memberfun: () -> Member) {
        config.participantRole?.let {
            val member = memberfun()
            member.guild.addRoleToMember(member, member.guild.getRoleById(it)!!).queue()
        }
    }

    suspend fun giveParticipantRoleToAll() {
        val g = jda.getGuildById(guild)!!
        val r = g.getRoleById(config.participantRole!!)!!
        users.flatMap { it.users }.forEach {
            g.addRoleToMember(UserSnowflake.fromId(it), r).await()
            delay(2000)
        }
    }

    fun giveParticipantRole(member: Member) = giveParticipantRole { member }


    suspend fun updateSignupMessage(setMaxUsersToCurrentUsers: Boolean = false) {
        jda.getTextChannelById(config.announceChannel)!!.editMessageById(
            announceMessageId,
            config.signupMessage.condAppend(!config.hideUserCount) {
                K18n_Signup.SignupMessageData(
                    users.size.toString(),
                    if (setMaxUsersToCurrentUsers) users.size.toString() else maxUsersAsString
                ).translateTo(language())
            }
        ).queue()
    }

    suspend fun closeSignup(forced: Boolean = false) {
        val channel = jda.getTextChannelById(config.announceChannel)!!
        val lang = language()
        channel.editMessageComponentsById(
            announceMessageId,
            SignupManager.Button.withoutIData(
                lang,
                K18n_Signup.SignupClosed,
                disabled = true
            ).into()
        ).queue()
        val msg = "_----------- ${K18n_Signup.SignupClosed.translateTo(lang)} -----------_"
        channel.sendMessage(msg).queue()
        if (config.announceChannel != config.signupChannel) jda.getTextChannelById(config.signupChannel)!!
            .sendMessage(msg).queue()
        if (forced) updateSignupMessage(true)
    }

    suspend fun setNewMaxUsers(newMaxUsers: Int) {
        val wasClosed = full
        config.maxUsers = newMaxUsers
        if (wasClosed) {
            jda.getTextChannelById(config.announceChannel)!!.editMessageComponentsById(
                announceMessageId, SignupManager.Button.withoutIData(language()).into()
            ).queue()
        }
        updateSignupMessage()
        save()
    }

    suspend fun handleNewUserInTeam(member: Member, data: SignUpData) {
        data.users += member.idLong
        giveParticipantRole(member)
        handleSignupChange(data)
    }

    suspend fun handleSignupChange(data: SignUpData) {
        jda.getTextChannelById(config.signupChannel)!!.editMessageById(data.signupmid!!, data.toMessage(this)).queue()
        config.logoSettings?.handleSignupChange(this, data)
        save()
    }

    suspend fun handleSignupChange(uid: Long) {
        users.firstOrNull { it.users.contains(uid) }?.let { handleSignupChange(it) }
    }

    suspend fun deleteUser(uid: Long): Boolean {
        val data = users.firstOrNull { it.users.contains(uid) } ?: return false
        data.users.remove(uid)
        if (data.users.isEmpty()) {
            data.signupmid?.let { jda.getTextChannelById(config.signupChannel)!!.deleteMessageById(it).queue() }
            config.logoSettings?.handleSignupRemoved(this, data)
            users.remove(data)
            save()
        } else {
            handleSignupChange(data)
        }
        updateSignupMessage()
        return true
    }

    suspend fun updateUser(user: Long) {
        val data = getDataByUser(user)!!
        jda.getTextChannelById(config.signupChannel)!!
            .editMessageById(data.signupmid!!, data.toMessage(this)).queue()
    }

    suspend fun insertLogo(uid: Long, logo: Message.Attachment): ErrorOrNull {
        logoUploadMutex.withLock {
            if (config.logoSettings == null) {
                return K18n_Signup.NoOwnLogos
            }
            val signUpIndex = getIndexOfUser(uid) ?: return K18n_Signup.NotSignedUp
            val signUpData = users[signUpIndex]
            val logoData = LogoInputData.fromAttachment(logo)
            if (logoData.isError()) {
                return logoData.message
            }
            config.logoSettings.handleLogo(this, signUpData, logoData.value)
            val timeSinceLastUpload = System.currentTimeMillis() - lastLogoUploadTime
            delay(GOOGLE_UPLOAD_DELAY_MS - timeSinceLastUpload)
            val checksum = Google.uploadLogoToCloud(logoData.value)
            mdb.signups.updateOne(
                LigaStartData::guild eq guild,
                set(LigaStartData::users.pos(signUpIndex) / SignUpData::logoChecksum setTo checksum)
            )
            lastLogoUploadTime = System.currentTimeMillis()
        }
        updateTeamgraphicForUser(uid)
        return null
    }

    private suspend fun updateTeamgraphicForUser(uid: Long) {
        val league = mdb.leagueByGuild(guild, uid) ?: return
        val idx = league(uid)
        TeamGraphicGenerator.editTeamGraphicForLeague(league, idx)
    }

    val full get() = config.maxUsers > 0 && users.size >= config.maxUsers

    companion object {
        private val logoUploadMutex = Mutex()
        private var lastLogoUploadTime: Long = 0
        private const val GOOGLE_UPLOAD_DELAY_MS = 5000
    }

}

class LogoInputData(val fileExtension: String, val bytes: ByteArray) {
    val checksum = hashBytes(bytes)
    val fileName = "$checksum.$fileExtension"

    fun toFileUpload() = FileUpload.fromData(bytes, fileName)

    companion object {
        private val logger = KotlinLogging.logger {}
        private const val MAX_SIZE = 10

        suspend fun fromAttachment(
            attachment: Message.Attachment,
            ignoreRequirements: Boolean = false
        ): CalcResult<LogoInputData> = withContext(Dispatchers.IO) {
            attachment.fileExtension?.lowercase()?.takeIf { ignoreRequirements || it in allowedFileFormats }
                ?: return@withContext CalcResult.Error(K18n_SignupInput.LogoMustBeImage)
            val bytes = try {
                attachment.proxy.download().await().readAllBytes()
            } catch (ex: Exception) {
                logger.error("Couldnt download logo", ex)
                return@withContext CalcResult.Error(K18n_SignupInput.LogoDownloadError)
            }
            if (!ignoreRequirements && bytes.size > 1024 * 1024 * MAX_SIZE) {
                return@withContext CalcResult.Error(K18n_SignupInput.LogoTooBig(MAX_SIZE))
            }
            val image = ImageIO.read(bytes.inputStream())
                ?: return@withContext CalcResult.Error(K18n_SignupInput.LogoNoValidImage)
            val croppedImage = ImageUtils.cropToContent(image)
            val baos = ByteArrayOutputStream()
            ImageIO.write(croppedImage, "png", baos)
            val finalBytes = baos.toByteArray()
            CalcResult.Success(LogoInputData("png", finalBytes))
        }
    }
}

sealed interface CalcResult<T> {
    data class Success<T>(val value: T) : CalcResult<T>
    data class Error<T>(val message: K18nMessage) : CalcResult<T>
}

@OptIn(ExperimentalContracts::class)
fun <T> CalcResult<T>.isError(): Boolean {
    contract {
        returns(true) implies (this@isError is CalcResult.Error<T>)
        returns(false) implies (this@isError is CalcResult.Success<T>)
    }
    return this is CalcResult.Error<T>
}

fun <T> CalcResult<T>.unwrap(): T {
    return when (this) {
        is CalcResult.Success -> this.value
        is CalcResult.Error -> error("Tried to unwrap an error CalcResult: $message")
    }
}


private fun hashBytes(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).fold("") { str, it ->
    str + "%02x".format(it)
}.take(15)

@Serializable
data class SignUpData(
    val users: MutableSet<Long> = mutableSetOf(),
    val data: MutableMap<String, String> = mutableMapOf(),
    var signupmid: Long? = null,
    var logomid: Long? = null,
    var logoChecksum: String? = null,
    var conference: String? = null,
) {
    suspend fun toMessage(lsData: LigaStartData): String {
        return K18n_Signup.SignupConfirmMessage(formatName(), data.entries.mapNotNull { (k, v) ->
            val displayTitle = lsData.getInputConfig(k)?.getDisplayTitle() ?: return@mapNotNull null
            "${displayTitle}: **$v**"
        }.joinToString("\n")).translateTo(lsData.language())
    }

    fun formatName() = users.joinToString(" & ") { "<@$it>" }

    suspend fun downloadLogo(): BufferedImage? {
        val checksum = logoChecksum ?: return null
        val data = LogoChecksumDB.findByChecksum(checksum) ?: return null
        return withContext(Dispatchers.IO) {
            ImageIO.read(URI(data.url).toURL())
        }
    }

}

@Serializable
data class ShinyEventResult(
    val eventName: String, val user: Long, val shinies: List<ShinyData>, val points: Int, val messageId: Long? = null
) {
    @Serializable
    data class ShinyData(
        val game: String, val method: String, val timestamp: Instant
    )
}

@Serializable
data class ShinyEventConfig(
    val name: String,
    val guild: Long,
    val methods: Map<String, Configuration> = mapOf(),
    val checkChannel: Long,
    val finalChannel: Long,
    val pointChannel: Long,
    val pointMessageId: Long? = null
) {

    @Serializable
    data class Configuration(val games: List<String>, val points: Int)

    val groupedByGame = buildMap {
        methods.entries.forEach {
            val gameName = it.key.substringBefore("(") to it.value
            it.value.games.forEach { game ->
                val result = runCatching { SingleGame.valueOf(game) }.getOrElse { ShinyEvent.groupedGames[game] }
                    ?: error("Game $game not found")
                result.games.forEach { key ->
                    getOrPut(key.name) { mutableListOf<Pair<String, Configuration>>() }.add(
                        gameName
                    )
                }
            }
        }
    }

    fun getConfigurationByNameAndGame(game: SingleGame, name: String): Configuration? {
        val list = groupedByGame[game.name] ?: return null
        return list.firstOrNull { it.first == name }?.second
    }

    suspend fun updateDiscord(jda: JDA) {
        val filter = and(ShinyEventResult::eventName eq name)
        val msg =
            "## Punktestand\n" + mdb.shinyEventResults.find(filter).sort(descending(ShinyEventResult::points)).toList()
                .mapIndexed { index, res -> "${index + 1}. <@${res.user}>: ${res.points}" }.joinToString("\n")
        val channel = jda.getTextChannelById(pointChannel)!!
        if (pointMessageId == null) {
            val pid = channel.sendMessage(msg).await().idLong
            mdb.shinyEventConfig.updateOne(
                ShinyEventConfig::name eq name, set(ShinyEventConfig::pointMessageId setTo pid)
            )
        } else {
            channel.editMessage(pointMessageId.toString(), msg).queue()
        }
    }
}


data class LogoChecksum(
    val checksum: String, val fileId: String
) {
    val url get() = "$DOWNLOAD_URL_PREFIX${fileId}"

    companion object {
        const val DOWNLOAD_URL_PREFIX = "https://drive.google.com/uc?export=download&id="
    }
}

@Serializable
sealed class LeagueEvent {
    abstract val leaguename: String
    abstract val gameday: Int
    abstract val timestamp: Instant
    abstract val indices: List<Int>

    abstract fun manipulate(map: MutableMap<Int, UserTableData>)

    sealed class Sanction : LeagueEvent() {
        abstract val reason: String
        abstract val issuer: Long
    }

    @Serializable
    @SerialName("MatchResult")
    data class MatchResult(
        override val indices: List<Int>,
        override val leaguename: String,
        override val gameday: Int,
        override val timestamp: Instant,
        val matchNum: Int = 0,
        val data: List<Int>
    ) : LeagueEvent() {
        override fun manipulate(map: MutableMap<Int, UserTableData>) {
            for ((i, idx) in indices.withIndex()) {
                map[idx]!!.let {
                    val k = data[i]
                    val d = data[1 - i]
                    it.kills += k
                    it.deaths += d
                    it.points += if (k > d) 3 else 0
                    it.wins += if (k > d) 1 else 0
                    it.losses += if (k < d) 1 else 0
                }
            }
        }
    }


    @Serializable
    @SerialName("Zeroed")
    class ZeroedGame(
        override val indices: List<Int>,
        override val leaguename: String,
        override val gameday: Int,
        override val reason: String,
        override val issuer: Long,
        override val timestamp: Instant
    ) : Sanction() {
        override fun manipulate(map: MutableMap<Int, UserTableData>) {
            indices.forEach { idx ->
                map[idx]?.let { data ->
                    data.deaths += 6
                }
            }
        }
    }

    @Serializable
    @SerialName("PointPenalty")
    data class PointPenalty(
        val amount: Int,
        override val indices: List<Int>,
        override val leaguename: String,
        override val gameday: Int,
        override val reason: String,
        override val issuer: Long,
        override val timestamp: Instant
    ) : Sanction() {
        override fun manipulate(map: MutableMap<Int, UserTableData>) {
            indices.forEach { idx ->
                map[idx]?.let { data ->
                    data.points -= amount
                }
            }
        }
    }
}

enum class RemoteServerControlFeature {
    START, STATUS, STOP, POWEROFF
}

@Serializable
sealed class RemoteServerControl {
    val name = "Name"

    @Transient
    open val features: Set<RemoteServerControlFeature> = setOf()

    open suspend fun startServer() {}
    open suspend fun isOn(): Boolean = false
    open suspend fun stopServer() {}
    open suspend fun powerOff() {}

    @Serializable
    @SerialName("Http")
    data class Http(val url: String, val writePin: Int, val readPin: Int) : RemoteServerControl() {
        @Transient
        override val features = setOf(
            RemoteServerControlFeature.START,
            RemoteServerControlFeature.STOP,
            RemoteServerControlFeature.STATUS,
            RemoteServerControlFeature.POWEROFF
        )

        override suspend fun startServer() = push(TURN_ON_TIME)

        override suspend fun stopServer() = push(TURN_OFF_TIME)

        override suspend fun powerOff() = push(POWER_OFF)

        private suspend fun push(delay: Int) {
            withContext(Dispatchers.IO) {
                httpClient.post("$url/push/$writePin") {
                    setBody("$delay")
                }
            }
        }

        override suspend fun isOn() = withContext(Dispatchers.IO) {
            httpClient.get("$url/status/$readPin").bodyAsText().contains("level=0")
        }

        companion object {
            private const val TURN_ON_TIME = 500
            private const val TURN_OFF_TIME = 500
            private const val POWER_OFF = 5000
        }
    }

    @Serializable
    @SerialName("HomeAssistant")
    data class HomeAssistant(
        val url: String, val webhookIdOn: String, val webhookIdOff: String, val entityId: String, val token: String
    ) : RemoteServerControl() {
        @Transient
        override val features = setOf(
            RemoteServerControlFeature.START, RemoteServerControlFeature.POWEROFF, RemoteServerControlFeature.STATUS
        )

        override suspend fun startServer(): Unit = withContext(Dispatchers.IO) {
            println(httpClient.post("http://$url/api/webhook/$webhookIdOn").bodyAsText())
        }

        override suspend fun powerOff(): Unit = withContext(Dispatchers.IO) {
            println(httpClient.post("http://$url/api/webhook/$webhookIdOff").bodyAsText())
        }

        override suspend fun isOn(): Boolean {
            return when (val res = httpClient.get("http://$url/api/states/$entityId") {
                bearerAuth(token)
            }.body<HAResponseData>().state) {
                "on" -> true
                "off" -> false
                else -> error("Unknown HA response $res")
            }
        }

        @Serializable
        data class HAResponseData(val state: String)
    }

    @Serializable
    @SerialName("Ethernet")
    data class Ethernet(val mac: String, val host: String, val serviceHost: String) : RemoteServerControl() {
        @Transient
        override val features = setOf(RemoteServerControlFeature.START, RemoteServerControlFeature.STATUS)

        override suspend fun startServer(): Unit = withContext(Dispatchers.IO) {
            println(httpClient.post("http://$serviceHost/wol/$mac").bodyAsText())
        }

        override suspend fun isOn(): Boolean = withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    val inetSocketAddress = InetSocketAddress(host, 22)
                    socket.connect(inetSocketAddress, 500)
                    true
                }
            } catch (e: IOException) {
                false
            }
        }
    }
}

@Serializable
data class LadderTournament(
    val guild: Long,
    val adminChannel: Long,
    val signupChannel: Long,
    val formats: Map<String, String>,
    val sid: String,
    val cols: List<LadderTournamentCol>,
    val sortCols: List<LadderTournamentCol>,
    val lastExecution: Long,
    val durationInHours: Int,
    val amount: Int,
    val sdNamePrefix: String,
    val users: MutableMap<Long, LadderTournamentUserData> = mutableMapOf(),
) {
    suspend fun execute() {
        val usersPerFormat =
            users.filter { it.value.verified }.flatMap { (uid, data) -> data.formats.map { it to uid } }
                .groupBy { it.first }
                .mapValues { it.value.map { v -> v.second } }
        val userData = fetchDataForUsers()
        val b = RequestBuilder(sid)
        for ((format, targetRange) in formats) {
            val formatId = format.toSDName()
            val usersInFormat = usersPerFormat[format] ?: continue
            val tableData = usersInFormat.map { userData[it]!! }.sortedWith { a, b ->
                val dataA = a.ratings[formatId] ?: return@sortedWith 1
                val dataB = b.ratings[formatId] ?: return@sortedWith -1
                for (sortCol in sortCols) {
                    val numA = sortCol[dataA].toDouble().roundToInt()
                    val numB = sortCol[dataB].toDouble().roundToInt()
                    if (numA != numB) return@sortedWith numB - numA
                }
                0
            }.map {
                val rankData = it.ratings[formatId]
                buildList {
                    add(it.username.removePrefix(sdNamePrefix))
                    cols.forEach { col ->
                        add(col[rankData].toDouble().roundToInt().toString())
                    }
                }
            }
            b.addAll(targetRange, tableData)
        }
        b.execute()
    }

    private suspend fun fetchDataForUsers(): Map<Long, SDUserResponse> {
        return users.filter { it.value.verified }.mapValues {
            delay(5000)
            httpClient.get("https://pokemonshowdown.com/users/${it.value.sdName.toUsername()}.json")
                .body<SDUserResponse>()
        }
    }

    suspend fun save() = mdb.ladderTournament.updateOne(LadderTournament::guild eq guild, this)

    companion object {
        suspend fun executeForGuild(gid: Long) {
            mdb.ladderTournament.findOne(LadderTournament::guild eq gid)?.execute()
        }

    }
}

@Serializable
enum class LadderTournamentCol(val property: KProperty1<SDRankData, Number>) {
    WINS(SDRankData::wins),
    LOSSES(SDRankData::losses),
    TIES(SDRankData::ties),
    GXE(SDRankData::gxe),
    ELO(SDRankData::elo);

    operator fun get(data: SDRankData?) = data?.let { property.get(it) } ?: 0
}

@Serializable
data class LadderTournamentUserData(val sdName: String, val formats: List<String>, var verified: Boolean = true)

@Serializable
data class SDUserResponse(
    val username: String,
    val ratings: Map<String, SDRankData>
)

@Serializable
data class SDRankData(
    @SerialName("w") val wins: Int = 0,
    @SerialName("l") val losses: Int = 0,
    @SerialName("t") val ties: Int = 0,
    val gxe: Double,
    val elo: Double
)

suspend fun <T : Any> CoroutineCollection<T>.only() = find().first()!!
suspend fun <T : Any> CoroutineCollection<T>.updateOnly(update: Bson) =
    updateOne(BsonDocument(), update.also { logger.debug { it.json } })

@Suppress("unused") // used in other projects
suspend fun <T : Any> CoroutineCollection<T>.updateOnly(update: String) = updateOne("{}", update)

@JvmName("getLigaStartData")
suspend fun CoroutineCollection<LigaStartData>.get(guild: Long) = find(LigaStartData::guild eq guild).first()

@JvmName("getNameConventions")
suspend fun CoroutineCollection<NameConventions>.get(guild: Long) =
    find(NameConventions::guild eq guild).first()?.data ?: mdb.defaultNameConventions()

@JvmName("getPokedex")
suspend fun CoroutineCollection<Pokemon>.get(id: String) = find(Pokemon::id eq id).first()

@JvmName("getIntervalTaskData")
suspend fun CoroutineCollection<IntervalTaskData>.get(name: IntervalTaskKey) =
    find(IntervalTaskData::name eq name).first()

@JvmName("getRemoteServerControl")
suspend fun CoroutineCollection<RemoteServerControl>.get(name: String) = find(RemoteServerControl::name eq name).first()

@JvmName("getLadderTournament")
suspend fun CoroutineCollection<LadderTournament>.get(guild: Long) = find(LadderTournament::guild eq guild).first()

typealias ErrorOrNull = K18nMessage?
