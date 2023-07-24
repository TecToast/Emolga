package de.tectoast.emolga.commands

import com.google.api.services.sheets.v4.model.CellData
import com.google.api.services.sheets.v4.model.Color
import com.google.api.services.sheets.v4.model.RowData
import com.google.common.reflect.ClassPath
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.bot.EmolgaMain.emolgajda
import de.tectoast.emolga.bot.EmolgaMain.flegmonjda
import de.tectoast.emolga.buttons.ButtonListener
import de.tectoast.emolga.buttons.buttonsaves.Nominate
import de.tectoast.emolga.buttons.buttonsaves.PrismaTeam
import de.tectoast.emolga.buttons.buttonsaves.TrainerData
import de.tectoast.emolga.commands.Command.Companion.getAsXCoord
import de.tectoast.emolga.commands.Command.Companion.sendToMe
import de.tectoast.emolga.commands.CommandCategory.Companion.order
import de.tectoast.emolga.database.exposed.*
import de.tectoast.emolga.encryption.TokenEncrypter
import de.tectoast.emolga.modals.ModalListener
import de.tectoast.emolga.selectmenus.MenuListener
import de.tectoast.emolga.selectmenus.selectmenusaves.SmogonSet
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.Constants.CALENDAR_MSGID
import de.tectoast.emolga.utils.Constants.CALENDAR_TCID
import de.tectoast.emolga.utils.Constants.FLOID
import de.tectoast.emolga.utils.Constants.SOULLINK_MSGID
import de.tectoast.emolga.utils.Constants.SOULLINK_TCID
import de.tectoast.emolga.utils.annotations.ToTest
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.draft.isEnglish
import de.tectoast.emolga.utils.json.*
import de.tectoast.emolga.utils.json.emolga.draft.*
import de.tectoast.emolga.utils.json.showdown.Learnset
import de.tectoast.emolga.utils.json.showdown.Pokemon
import de.tectoast.emolga.utils.json.showdown.TypeData
import de.tectoast.emolga.utils.music.GuildMusicManager
import de.tectoast.emolga.utils.showdown.*
import de.tectoast.toastilities.repeat.RepeatTask
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.*
import dev.minn.jda.ktx.messages.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import mu.KotlinLogging
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.audio.hooks.ConnectionListener
import net.dv8tion.jda.api.audio.hooks.ConnectionStatus
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.Message.Attachment
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import org.apache.commons.collections4.queue.CircularFifoQueue
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.transactions.transaction
import org.litote.kmongo.*
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.MarkerFactory
import java.io.*
import java.lang.reflect.Modifier
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function
import java.util.function.Predicate
import java.util.regex.Pattern
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.reflect.KMutableProperty0

@Suppress("unused", "SameParameterValue", "MemberVisibilityCanBePrivate")
abstract class Command(
    /**
     * The name of the command, used to check if the command was used in [.check]
     */
    val name: String,
    /**
     * The help string of the command, shown in the help messages by the bot
     */
    val help: String,
    /**
     * The [CommandCategory] which this command is in
     */
    val category: CommandCategory?, vararg guilds: Long
) {
    /**
     * List containing guild ids where this command is enabled, empty if it is enabled in all guilds
     */
    private val allowedGuilds: List<Long>

    /**
     * Set containing all aliases of this command
     */
    val aliases: MutableSet<String> = mutableSetOf()

    /**
     * HashMap containing a help for a guild id which should be shown for this command on that guild instead of the [.help]
     */
    private val overrideHelp: Map<Long, String> = HashMap()

    /**
     * HashMap containing a channel list which should be used for this command instead of [.emolgaChannel]
     */
    protected val overrideChannel: MutableMap<Long, MutableList<Long>> = HashMap()

    /**
     * True if this command should use the `e!` instead of `!`
     */
    protected var otherPrefix = false

    /**
     * The ArgumentManagerTemplate of this command, used for checking arguments
     */
    lateinit var argumentTemplate: ArgumentManagerTemplate

    /**
     * If true, this command is only allowed for me because I'm working on it
     */
    var wip = false

    /**
     * If true, sends a beta information when using this command
     */
    protected var beta = false

    /**
     * True if this command should bypass all channel restrictions
     */
    protected var everywhere = false

    /**
     * Predicate which checks if a member is allowed to use this command, ignored if [.customPermissions] is false
     */
    private var allowsMember: Predicate<Member> = Predicate { false }

    /**
     * True if this command should not use the permissions of the CommandCategory but [.allowsMember] to test if a user is allowed to use the command
     */
    private var customPermissions = false

    /**
     * If true, this command is disabled and cannot be used
     */
    var disabled = false
    protected var allowedBotId: Long = -1
    var isSlash = false
    protected var onlySlash = false
    var adminSlash = false
    val slashGuilds: MutableSet<Long> = HashSet()
    val childCommands: MutableMap<String, Command> = LinkedHashMap()

    init {
        allowedGuilds = guilds.toList()
    }

    constructor(name: String, help: String) : this(name, help, null)

    private fun addToMap() {
        val prefix = if (otherPrefix) "e!" else "!"
        commands[prefix + name] = this
        aliases.forEach { commands[prefix + it] = this }
        if (!this::argumentTemplate.isInitialized) {
            logger.error("{} has no argument manager!", this::class.qualifiedName)
            uninitializedCommands.add(this::class.qualifiedName ?: "ERROR")
        }
    }

    private fun addToChildren(c: Command) {
        c.childCommands[name] = this
    }

    protected fun setCustomPermissions(predicate: Predicate<Member>) {
        allowsMember = predicate.or { it.idLong == FLOID }
        customPermissions = true
    }

    protected fun addCustomChannel(guildId: Long, vararg channelIds: Long) {
        val l = overrideChannel.computeIfAbsent(guildId) { mutableListOf() }
        for (channelId in channelIds) {
            l.add(channelId)
        }
    }

    protected fun disable() {
        disabled = true
    }

    protected fun slash(onlySlash: Boolean, vararg guilds: Long) {
        this.onlySlash = onlySlash
        isSlash = true
        slashGuilds.addAll(guilds.toList())
        slashGuilds.add(Constants.G.MY)
    }

    protected fun slashInAllowedGuilds(onlySlash: Boolean) {
        slash(onlySlash, *allowedGuilds.toLongArray())
    }

    protected fun slash() {
        this.slash(false)
    }

    protected fun wip() {
        wip = true
    }

    protected fun beta() {
        beta = true
    }

    fun allowsMember(mem: Member): Boolean {
        return (!customPermissions && category!!.allowsMember(mem)) || (customPermissions && allowsMember.test(mem))
    }

    fun allowsGuild(g: Guild): Boolean {
        return allowsGuild(g.idLong)
    }

    private fun allowsGuild(gid: Long): Boolean {
        if (gid == Constants.G.MY) return true
        return if (allowedGuilds.isEmpty() && category!!.allowsGuild(gid)) true else allowedGuilds.isNotEmpty() && allowedGuilds.contains(
            gid
        )
    }

    fun checkPermissions(gid: Long, mem: Member): PermissionCheck {
        if (!allowsGuild(gid)) return PermissionCheck.GUILD_NOT_ALLOWED
        return if (!allowsMember(mem)) PermissionCheck.PERMISSION_DENIED else PermissionCheck.GRANTED
    }

    fun checkBot(jda: JDA, guildid: Long): Boolean {
        return allowedBotId == -1L || allowedBotId == jda.selfUser.idLong || guildid == Constants.G.CULT
    }

    val prefix: String
        get() = if (otherPrefix) "e!" else "!"

    /**
     * Abstract method, which is called on the subclass with the corresponding command when the command was received
     *
     * @param e A GuildCommandEvent containing the information about the command
     * @throws Exception Every exception that can be thrown in any command
     */
    @Throws(Exception::class)
    abstract suspend fun process(e: GuildCommandEvent)


    fun getHelp(g: Guild?): String {
        val args = argumentTemplate //?: return "`" + prefix + name + "` " + overrideHelp.getOrDefault(g.idLong, help)
        return buildString {
            append("`")
            append(
                (if (args.hasSyntax()) args.syntax else (if (isSlash) "/" else prefix) + name + (if (args.arguments.isNotEmpty()) " " else "") + args.arguments.joinToString(
                    " "
                ) { "${if (it.isOptional) "[" else "<"}${it.name}${if (it.isOptional) "]" else ">"}" })
            )
            append("` ")
            append(
                (overrideHelp[g?.idLong ?: -1] ?: help)
            )
            append(if (wip) " (**W.I.P.**)" else "")
        }
    }

    fun getHelpWithoutCmd(g: Guild): String {
        return overrideHelp.getOrDefault(g.idLong, help) + if (wip) " (**W.I.P.**)" else ""
    }

    enum class Bot(val jDA: KMutableProperty0<JDA>) {
        EMOLGA(::emolgajda), FLEGMON(::flegmonjda);

        companion object {
            fun byJDA(jda: JDA): Bot {
                for (value in entries) {
                    if (jda.selfUser.idLong == value.jDA.get().selfUser.idLong) {
                        return value
                    }
                }
                throw IllegalStateException("Bot not found by user ${jda.selfUser.name}")
            }
        }
    }

    enum class PermissionCheck {
        GRANTED, PERMISSION_DENIED, GUILD_NOT_ALLOWED
    }

    class ValidationData(
        val guildId: Long = -1,
        val language: Language = Language.GERMAN,
        val message: Message? = null,
        val asFar: Int = -1,
        val channel: MessageChannel? = null
    )

    interface ArgumentType {
        suspend fun validate(str: String, data: ValidationData): Any?
        fun getName(): String
        val customHelp: String?
            get() = null

        fun asOptionType(): OptionType
        fun needsValidate(): Boolean
        fun hasAutoComplete(): Boolean {
            return false
        }

        suspend fun autoCompleteList(arg: String, event: CommandAutoCompleteInteractionEvent): List<String>? {
            return null
        }
    }

    object PermissionPreset {
        val CULT = fromRole(781457314846343208)
        val HENNY = fromIDs(297010892678234114)
        fun fromRole(roleId: Long): Predicate<Member> {
            return Predicate { it.guild.roleCache.none { r -> r.idLong == roleId } || it.roles.any { r -> r.idLong == roleId } }
        }

        fun fromIDs(vararg ids: Long): Predicate<Member> {
            return Predicate { ids.any { l: Long -> it.idLong == l } }
        }
    }

    fun hasChildren(): Boolean {
        return childCommands.isNotEmpty()
    }

    open class ArgumentException : Exception {
        var argument: ArgumentManagerTemplate.Argument? = null
        var subcommands: Set<String>? = null

        constructor(argument: ArgumentManagerTemplate.Argument) {
            this.argument = argument
        }

        constructor(subcommands: Set<String>) {
            this.subcommands = subcommands
        }

        val isSubCmdMissing: Boolean
            get() = subcommands != null
    }

    class MissingArgumentException : ArgumentException {
        constructor(argument: ArgumentManagerTemplate.Argument) : super(argument)
        constructor(subcommands: Set<String>) : super(subcommands)
    }

    class ArgumentManager(val map: MutableMap<String, Any>, val executor: Command) {

        inline operator fun <reified T> get(key: String): T {
            return map[key] as T
        }

        inline fun <reified T> getNullable(key: String): T? {
            return map[key] as? T?
        }

        fun getMember(key: String): Member {
            return map[key] as Member
        }

        fun getTranslation(key: String): Translation {
            return map[key] as Translation
        }

        fun getText(key: String): String {
            return when (val x = map[key]!!) {
                is String -> x
                is DraftName -> x.official
                else ->
                    throw IllegalStateException("Unknown type ${x::class.java}")

            }
        }

        fun getDraftName(key: String): DraftName {
            return map[key] as DraftName
        }

        fun isText(key: String, text: String): Boolean {
            return map.getOrDefault(key, "") == text
        }

        fun isTextIgnoreCase(key: String, text: String): Boolean {
            return (map.getOrDefault(key, "") as String).equals(text, ignoreCase = true)
        }

        fun has(key: String): Boolean {
            return map.containsKey(key)
        }

        fun <T> getOrDefault(key: String, defvalue: T): T {
            @Suppress("UNCHECKED_CAST") return map.getOrDefault(key, defvalue) as T
        }

        @Suppress("UNCHECKED_CAST")
        fun <T> getOrDefault(key: String, defvalue: () -> T): T {
            return if (key in map) {
                map[key] as T
            } else {
                defvalue()
            }
        }

        fun optString(key: String): String? = map[key] as? String

        fun getID(key: String): Long {
            return (map[key] as String).toLong()
        }

        fun getInt(key: String): Int {
            return map[key] as Int
        }

        fun getLong(key: String): Long {
            val o: Any = map[key]!!
            return if (o is Int) o.toLong() else o as Long
        }

        fun getChannel(key: String): TextChannel {
            return map[key] as TextChannel
        }

        fun getAttachment(key: String): Attachment {
            return map[key] as Attachment
        }
    }

    class SubCommand(val name: String, val help: String) {

        companion object {
            fun of(name: String, help: String? = null): SubCommand {
                return SubCommand(name, help ?: name)
            }
        }
    }

    class ArgumentManagerTemplate {
        private val noCheck: Boolean
        val arguments: List<Argument>
        var example: String? = null
            private set
        var syntax: String? = null
            private set

        private constructor(arguments: List<Argument>, noCheck: Boolean, example: String?, syntax: String?) {
            this.arguments = arguments
            this.noCheck = noCheck
            this.example = example
            this.syntax = syntax
        }

        private constructor() {
            noCheck = true
            arguments = mutableListOf()
        }

        fun findForAutoComplete(name: String): Argument? {
            return arguments.firstOrNull {
                it.name.lowercase().replace(" ", "-").replace(Regex("[^\\w-]"), "").equals(name, ignoreCase = true)
            }
        }

        fun hasExample(): Boolean {
            return example != null
        }

        fun hasSyntax(): Boolean {
            return syntax != null
        }

        @Throws(ArgumentException::class)
        suspend fun construct(e: SlashCommandInteractionEvent, c: Command): ArgumentManager {
            val map: MutableMap<String, Any> = HashMap()
            if (c.hasChildren()) {
                val childCmd = c.childCommands[e.subcommandName]
                return childCmd!!.argumentTemplate.construct(e, childCmd)
            } else if (arguments.isNotEmpty() && arguments[0].type.asOptionType() == OptionType.SUB_COMMAND) {
                map[arguments[0].id] = e.subcommandName!!
            } else {
                for (arg in arguments) {
                    val argName = arg.name.lowercase().replace(" ", "-").replace(Regex("[^\\w-]"), "")
                    if (e.getOption(argName) == null && !arg.isOptional) {
                        throw MissingArgumentException(arg)
                    }
                    val type = arg.type
                    val o = e.getOption(argName)
                    if (o != null) {
                        map[arg.id] = if (type.needsValidate()) {
                            type.validate(
                                o.asString, ValidationData(
                                    guildId = e.guild?.idLong ?: -1, language = arg.language, channel = e.channel
                                )
                            ) ?: throw MissingArgumentException(arg)
                        } else {
                            when (o.type) {
                                OptionType.ROLE -> o.asRole
                                OptionType.CHANNEL -> o.asChannel
                                OptionType.USER -> o.asMember!!
                                OptionType.INTEGER -> o.asLong
                                OptionType.BOOLEAN -> o.asBoolean
                                OptionType.ATTACHMENT -> o.asAttachment
                                else -> o.asString
                            }
                        }
                    }
                }
            }
            return ArgumentManager(map, c)
        }

        @Throws(ArgumentException::class)
        suspend fun construct(e: MessageReceivedEvent, c: Command): ArgumentManager {
            val m = e.message
            val mid = m.idLong
            val raw = m.contentRaw
            val split = WHITESPACES_SPLITTER.split(raw).toMutableList()
            split.removeAt(0)
            val map: MutableMap<String, Any> = HashMap()
            if (c.hasChildren()) {
                if (split.isEmpty()) {
                    throw MissingArgumentException(c.childCommands.keys)
                }
                val sc = c.childCommands[split.removeAt(0)] ?: throw MissingArgumentException(c.childCommands.keys)
                return sc.argumentTemplate.construct(e, sc)
            } else if (split.size > 0 && arguments.isNotEmpty() && arguments[0].type.asOptionType() == OptionType.SUB_COMMAND) {
                map[arguments[0].id] = split[0]
                return ArgumentManager(map, c)
            }
            if (noCheck) return ArgumentManager(mutableMapOf(), c)
            val asFar: MutableMap<ArgumentType, Int> = HashMap()
            var argumentI = 0
            var i = 0
            while (i < split.size) {
                if (i >= arguments.size) {
                    break
                }
                if (argumentI >= arguments.size) break
                val a = arguments[argumentI]
                if (a.disabled.contains(mid)) break
                val str =
                    if (argumentI + 1 == arguments.size) split.subList(i, split.size)
                        .joinToString(" ") else split[i]
                val type = a.type
                var o: Any?
                if (type is DiscordType || type is DiscordFile) {
                    o = type.validate(
                        str, ValidationData(
                            message = m, asFar = asFar.getOrDefault(type, 0), channel = e.channel
                        )
                    )
                    if (o != null) asFar[type] = asFar.getOrDefault(type, 0) + 1
                } else {
                    o = type.validate(
                        str, ValidationData(
                            guildId = if (e.isFromGuild) e.guild.idLong else -1,
                            language = a.language,
                            channel = e.channel
                        )
                    )
                    if (o == null) {
                        var b = true
                        for (j in argumentI + 1 until arguments.size) {
                            if (!arguments[j].isOptional) {
                                b = false
                                break
                            }
                        }
                        if (b && arguments.size > argumentI + 1) {
                            arguments[argumentI + 1].disabled.add(mid)
                        }
                        if (b) {
                            o = type.validate(
                                split.subList(i, split.size).joinToString(" "), ValidationData(
                                    guildId = if (e.isFromGuild) e.guild.idLong else -1,
                                    language = a.language,
                                    channel = e.channel
                                )
                            )
                        }
                    }
                }
                if (o == null) {
                    if (!a.isOptional) {
                        clearDisable(mid)
                        throw MissingArgumentException(a)
                    }
                } else {
                    map[a.id] = o
                    i++
                }
                argumentI++
            }
            clearDisable(mid)
            if (map.isEmpty()) {
                arguments.firstOrNull { !it.isOptional }?.let { throw MissingArgumentException(it) }
            }
            return ArgumentManager(map, c)
        }

        private fun clearDisable(l: Long) {
            arguments.forEach { it.disabled.remove(l) }
        }

        enum class DiscordType(
            private val pattern: Pattern,
            private val typeName: String,
            private val female: Boolean,
            private val optionType: OptionType
        ) : ArgumentType {
            USER(
                Pattern.compile("<@!*\\d{18,22}>"), "User", false, OptionType.USER
            ),
            CHANNEL(Pattern.compile("<#*\\d{18,22}>"), "Channel", false, OptionType.CHANNEL), ROLE(
                Pattern.compile("<@&*\\d{18,22}>"), "Rolle", true, OptionType.ROLE
            ),
            ID(Pattern.compile("\\d{18,22}"), "ID", true, OptionType.STRING), INTEGER(
                Pattern.compile("\\d{1,9}"), "Zahl", true, OptionType.INTEGER
            );

            override suspend fun validate(str: String, data: ValidationData): Any? {
                if (pattern.matcher(str).find()) {
                    if (mentionable()) {
                        val m = data.message ?: error("Message is not set in discord validation")
                        val soFar = data.asFar
                        val mentions = m.mentions
                        return if (this == USER) mentions.members[soFar] else if (this == CHANNEL) mentions.channels[soFar] else mentions.roles[soFar]
                    }
                    if (this == ID) return str.toLong()
                    if (this == INTEGER) return str.toInt()
                }
                return null
            }

            private fun mentionable(): Boolean {
                return this == USER || this == CHANNEL || this == ROLE
            }

            override fun getName(): String {
                return "ein" + (if (female) "e" else "") + " **" + typeName + "**"
            }

            override fun asOptionType(): OptionType {
                return optionType
            }

            override fun needsValidate(): Boolean {
                return false
            }
        }

        object ArgumentBoolean : ArgumentType {
            override suspend fun validate(str: String, data: ValidationData) = str.toBoolean()

            override fun getName() = "Wahrheitswert"

            override fun asOptionType() = OptionType.BOOLEAN

            override fun needsValidate() = false

        }

        class Text : ArgumentType {
            private val texts: MutableList<SubCommand> = mutableListOf()
            private val any: Boolean
            private val slashSubCmd: Boolean
            private var mapper: (String) -> String = { it }
            private var autoComplete: (suspend (String, CommandAutoCompleteInteractionEvent) -> List<String>?)? = null

            private constructor(possible: List<SubCommand>, slashSubCmd: Boolean = false) {
                texts.addAll(possible)
                any = false
                this.slashSubCmd = slashSubCmd
            }

            private constructor() {
                any = true
                slashSubCmd = false
            }

            fun setMapper(mapper: (String) -> String): Text {
                this.mapper = mapper
                return this
            }

            override suspend fun validate(str: String, data: ValidationData): Any? {
                return if (any) str else texts.map { it.name }
                    .filter { it.equals(str, ignoreCase = true) }.map(mapper)
                    .firstOrNull()
            }

            override fun getName(): String {
                return "ein Text"
            }

            override val customHelp: String?
                get() = if (any) null else texts.joinToString("\n") { sc: SubCommand -> "`" + sc.name + "`" + sc.help }

            override fun asOptionType(): OptionType {
                return if (slashSubCmd) OptionType.SUB_COMMAND else OptionType.STRING
            }

            fun hasOptions(): Boolean {
                return !slashSubCmd && texts.isNotEmpty() && texts.size <= 25
            }

            fun toChoiceArray(): Array<net.dv8tion.jda.api.interactions.commands.Command.Choice> {
                return texts.map { net.dv8tion.jda.api.interactions.commands.Command.Choice(it.help, it.name) }
                    .toTypedArray()
            }

            override fun needsValidate(): Boolean {
                return true
            }

            override fun hasAutoComplete(): Boolean {
                return (!(slashSubCmd || texts.isEmpty()) || autoComplete != null) && !hasOptions()
            }

            override suspend fun autoCompleteList(
                arg: String,
                event: CommandAutoCompleteInteractionEvent
            ): List<String>? {
                if (autoComplete != null) {
                    return autoComplete!!(arg, event)
                }
                return texts.asSequence().filter { c: SubCommand -> c.name.lowercase().startsWith(arg) }
                    .map { obj: SubCommand -> obj.name }.toList()
            }

            fun asSubCommandData(): List<SubcommandData> {
                check(slashSubCmd) { "Cannot call asSubCommandData on no SubCommand" }
                return texts.map { SubcommandData(it.name.lowercase(), it.help) }
            }

            companion object {
                fun of(vararg possible: SubCommand): Text {
                    return of(possible.toList())
                }

                fun of(possible: List<SubCommand>, slashSubCmd: Boolean = false): Text {
                    return Text(possible, slashSubCmd)
                }

                fun any(): Text {
                    return Text()
                }

                fun withAutocomplete(autoComplete: (suspend (String, CommandAutoCompleteInteractionEvent) -> List<String>?)?): Text {
                    return Text().apply {
                        this.autoComplete = autoComplete
                    }
                }

                fun draftTiers(): Text {
                    return withAutocomplete { _, event ->
                        League.onlyChannel(event.channel!!.idLong)?.getPossibleTiers()?.filter { it.value > 0 }
                            ?.map { it.key }
                    }
                }
            }
        }

        class Number private constructor(possible: Array<Int>) : ArgumentType {
            private val numbers = mutableListOf<Int>()
            private val any: Boolean
            private var range: IntRange? = null
            private var hasRange: Boolean

            init {
                if (possible.isEmpty()) {
                    any = true
                    hasRange = false
                } else {
                    numbers.addAll(listOf(*possible))
                    any = false
                    hasRange = false
                }
            }

            private fun setRange(range: IntRange): Number {
                this.range = range
                hasRange = true
                return this
            }

            private fun hasRange(): Boolean {
                return hasRange
            }

            override suspend fun validate(str: String, data: ValidationData): Any? {
                return str.toIntOrNull()?.let { num ->
                    if (any) num else if (hasRange()) {
                        if (num in range!!) num else null
                    } else {
                        if (num in numbers) num else null
                    }
                }
            }

            override fun getName(): String {
                return "eine Zahl"
            }

            override val customHelp: String
                get() = if (hasRange()) {
                    range!!.run { "$first-$last" }
                } else numbers.joinToString()

            override fun asOptionType(): OptionType {
                return OptionType.INTEGER
            }

            override fun needsValidate(): Boolean {
                return true
            }

            companion object {
                fun range(range: IntRange): Number {
                    return of().setRange(range)
                }

                fun of(vararg possible: Int): Number {
                    return Number(possible.toTypedArray())
                }

                fun any(): Number = Number(emptyArray())
            }
        }

        class DiscordFile(private val fileType: String) : ArgumentType {
            override suspend fun validate(str: String, data: ValidationData): Any? {
                val att = (data.message ?: error("Message is null")).attachments
                if (att.size == 0) return null
                val a = att[data.asFar]
                return if (a.fileName.endsWith(".$fileType") || fileType == "*") {
                    a
                } else null
            }

            override fun getName(): String {
                return "eine " + (if (fileType == "*") "" else "$fileType-") + "Datei"
            }

            override fun asOptionType(): OptionType {
                return OptionType.ATTACHMENT
            }

            override fun needsValidate(): Boolean {
                return false
            }

            companion object {
                fun of(fileType: String): DiscordFile {
                    return DiscordFile(fileType)
                }
            }
        }

        class Argument(
            val id: String,
            val name: String,
            val helpmsg: String,
            val type: ArgumentType,
            val isOptional: Boolean,
            val language: Language,
            val customErrorMessage: String?
        ) {
            val disabled: MutableList<Long> = mutableListOf()

            fun buildHelp(): String {
                val b = StringBuilder(helpmsg)
                if (type.customHelp == null) return b.toString()
                if (helpmsg.isNotEmpty()) {
                    b.append("\n")
                }
                return b.append("Möglichkeiten:\n").append(type.customHelp).toString()
            }

            fun hasCustomErrorMessage(): Boolean {
                return customErrorMessage != null
            }
        }

        class Builder {
            private val arguments: MutableList<Argument> = mutableListOf()
            var noCheck = false
            var example: String? = null
            var customDescription: String? = null


            fun add(
                id: String,
                name: String,
                help: String,
                type: ArgumentType,
                optional: Boolean = false,
                customErrorMessage: String? = null
            ): Builder {
                arguments.add(
                    Argument(
                        id,
                        name,
                        help,
                        type,
                        optional,
                        Language.GERMAN,
                        customErrorMessage
                    )
                )
                return this
            }


            fun addEngl(
                id: String,
                name: String,
                help: String,
                type: ArgumentType,
                optional: Boolean = false,
                customErrorMessage: String? = null
            ): Builder {
                arguments.add(
                    Argument(
                        id, name, help, type, optional, Language.ENGLISH, customErrorMessage
                    )
                )
                return this
            }

            fun setNoCheck(noCheck: Boolean): Builder {
                this.noCheck = noCheck
                return this
            }

            fun setExample(example: String?): Builder {
                this.example = example
                return this
            }

            fun setCustomDescription(customDescription: String?): Builder {
                this.customDescription = customDescription
                return this
            }

            fun build(): ArgumentManagerTemplate {
                return ArgumentManagerTemplate(arguments, noCheck, example, customDescription)
            }
        }

        companion object {
            private val noCheckTemplate: ArgumentManagerTemplate = ArgumentManagerTemplate()

            fun builder(): Builder {
                return Builder()
            }

            fun create(b: Builder.() -> Unit) = Builder().apply(b).build()

            fun noArgs(): ArgumentManagerTemplate {
                return noCheckTemplate
            }

            fun noSpecifiedArgs(syntax: String, example: String): ArgumentManagerTemplate {
                return ArgumentManagerTemplate(mutableListOf(), true, example, syntax)
            }

            fun draft(): ArgumentType {
                return withPredicate(
                    "Draftname", { db.drafts.findOne(League::leaguename eq it) != null }, false
                )
            }

            fun draftPokemon(
                autoComplete: (suspend (String, CommandAutoCompleteInteractionEvent) -> List<String>?)? = null
            ): ArgumentType {
                return withPredicate(
                    "Pokemon",
                    false,
                    { str, data ->
                        val guildId = data.channel?.let { League.onlyChannel(it.idLong)?.guild } ?: data.guildId
                        NameConventionsDB.getDiscordTranslation(
                            str,
                            guildId,
                            english = Tierlist[guildId].isEnglish
                        )
                    },
                    autoComplete
                )
            }

            fun withPredicate(name: String, check: suspend (String) -> Boolean, female: Boolean): ArgumentType {
                return object : ArgumentType {
                    override suspend fun validate(str: String, data: ValidationData): Any? {
                        return if (check(str)) str else null
                    }

                    override fun getName(): String {
                        return "ein" + (if (female) "e" else "") + " **" + name + "**"
                    }

                    override fun asOptionType(): OptionType {
                        return OptionType.STRING
                    }

                    override fun needsValidate(): Boolean {
                        return true
                    }
                }
            }

            private fun withPredicate(
                name: String,
                female: Boolean,
                mapper: suspend (String, ValidationData) -> Any?,
                autoComplete: (suspend (String, CommandAutoCompleteInteractionEvent) -> List<String>?)?
            ): ArgumentType {
                return object : ArgumentType {
                    override suspend fun validate(str: String, data: ValidationData): Any? {
                        return mapper(str, data)
                    }

                    override fun getName(): String {
                        return "ein" + (if (female) "e" else "") + " **" + name + "**"
                    }

                    override fun asOptionType(): OptionType {
                        return OptionType.STRING
                    }

                    override fun needsValidate(): Boolean {
                        return true
                    }

                    override fun hasAutoComplete(): Boolean {
                        return autoComplete != null
                    }

                    override suspend fun autoCompleteList(
                        arg: String, event: CommandAutoCompleteInteractionEvent
                    ): List<String>? {
                        return autoComplete!!(arg, event)
                    }
                }
            }
        }
    }


    companion object {
        /**
         * NO PERMISSION Message
         */
        const val NOPERM = "Dafür hast du keine Berechtigung!"

        /**
         * List of all commands of the bot
         */
        val commands: MutableMap<String, Command> = HashMap()

        /**
         * List of guilds where the chill playlist is playing
         */
        val chill: MutableList<Guild> = ArrayList()

        /**
         * List of guilds where the deep playlist is playing
         */
        val deep: MutableList<Guild> = ArrayList()

        /**
         * List of guilds where the music playlist is playing
         */
        val music: MutableList<Guild> = ArrayList()

        /**
         * Some pokemon extensions for showdown
         */
        val sdex: MutableMap<String, String> = HashMap()


        /**
         * saves all channels where emoteSteal is enabled
         */
        val emoteSteal: MutableList<Long> = ArrayList()

        /**
         * saves all replay channel with their result channel
         */
        val replayAnalysis: MutableMap<Long, Long> = HashMap()

        /**
         * saves all guilds where spoiler tags should be used in the showdown results
         */

        val spoilerTags: MutableList<Long> = ArrayList()

        /**
         * Cache for german translations
         */
        val translationsCacheGerman: MutableMap<String, Translation> = HashMap()

        /**
         * Order of the cached items, used to delete the oldest after enough caching
         */
        val translationsCacheOrderGerman = mutableListOf<String>()

        /**
         * Cache for english translations
         */
        val translationsCacheEnglish: MutableMap<String, Translation> = HashMap()

        /**
         * Order of the cached items, used to delete the oldest after enough caching
         */
        val translationsCacheOrderEnglish = mutableListOf<String>()

        /**
         * MusicManagers for Lavaplayer
         */
        val musicManagers: MutableMap<Long, GuildMusicManager> = HashMap()
        private val playerManagers: MutableMap<Long, AudioPlayerManager> = HashMap()
        val trainerDataButtons: MutableMap<Long, TrainerData> = HashMap()
        val nominateButtons: MutableMap<Long, Nominate> = HashMap()
        val smogonMenu: MutableMap<String, SmogonSet> = HashMap()
        val prismaTeam: MutableMap<Long, PrismaTeam> = HashMap()
        private val customResult = emptyList<Long>()
        val clips: MutableMap<Long, CircularFifoQueue<ByteArray>> = HashMap()
        val uninitializedCommands: MutableList<String> = mutableListOf()


        @JvmStatic
        protected val soullinkIds = mapOf(
            448542640850599947 to "Pascal",
            726495601021157426 to "David",
            867869302808248341 to "Jesse",
            541214204926099477 to "Felix"
        )
        protected val soullinkNames = listOf("Pascal", "David", "Jesse", "Felix")
        private val logger = LoggerFactory.getLogger(Command::class.java)
        private val otherFormatRegex = Regex("(\\S+)-(Mega|Alola|Galar)")
        val typeIcons: Map<String, String> = load("typeicons.json")
        private val SD_NAME_PATTERN = Regex("[^a-zA-Z\\däöüÄÖÜß♂♀é+]+")
        private val DURATION_PATTERN = Regex("\\d{1,8}[smhd]?")
        private val DURATION_SPLITTER = Regex("[.|:]")
        val WHITESPACES_SPLITTER = Regex("\\s+")
        private val EMPTY_PATTERN = Regex("")
        private val HYPHEN = Regex("-")
        private val USERNAME_PATTERN = Regex("[^a-zA-Z\\d]+")
        private val CUSTOM_GUILD_PATTERN = Regex("(\\d{18,})")
        val TRIPLE_HASHTAG = Regex("###")
        const val DEXQUIZ_BUDGET = 10
        val draftGuilds =
            longArrayOf(
                Constants.G.FPL,
                Constants.G.NDS,
                Constants.G.ASL,
                Constants.G.BLOCKI,
                Constants.G.VIP,
                Constants.G.FLP,
                Constants.G.WARRIOR,
                Constants.G.BSP,
                Constants.G.PIKAS,
                Constants.G.WFS
            )
        private val draftPrefixes = mapOf(
            "M" to "Mega", "A" to "Alola", "G" to "Galar", "Mega" to "Mega", "Alola" to "Alola", "Galar" to "Galar"
        )
        val draftPokemonArgumentType = ArgumentManagerTemplate.draftPokemon { s, event ->
            val gid = event.guild!!.idLong
            val league = League.onlyChannel(event.channel!!.idLong)
            //val alreadyPicked = league?.picks?.values?.flatten()?.map { it.name } ?: emptyList()
            val tierlist = Tierlist[league?.guild ?: gid]
            val strings =
                (tierlist?.autoComplete ?: allNameConventions).filterStartsWithIgnoreCase(s)
            if (strings.size > 25) return@draftPokemon listOf("Zu viele Ergebnisse, bitte spezifiziere deine Suche!")

            (if (league == null || tierlist == null) strings
            else strings.map {
                if (league.picks.values.flatten()
                        .any { p ->
                            p.name == tierlist.tlToOfficialCache.getOrPut(it) {
                                NameConventionsDB.getDiscordTranslation(it, league.guild)!!.official
                            }
                        }
                ) "$it (GEPICKT)" else it
            }).sorted()
        }
        val allNameConventions by lazy(NameConventionsDB::getAll)

        lateinit var tokens: Tokens
        lateinit var catchrates: Map<String, Int>
        val replayCount = AtomicInteger()
        protected var lastClipUsed: Long = -1


        @JvmStatic
        protected var calendarService: ScheduledExecutorService = Executors.newScheduledThreadPool(5)
        protected val moderationService: ScheduledExecutorService = Executors.newScheduledThreadPool(5)
        protected val birthdayService: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
        const val BOT_DISABLED = false
        const val DISABLED_TEXT =
            "Es finden derzeit große interne Umstrukturierungen statt, ich werde voraussichtlich heute Mittag/Nachmittag wieder einsatzbereit sein :)"


        fun newCalendarService() {
            calendarService = Executors.newScheduledThreadPool(5)
        }


        private fun registerCommands() {
            val loader = Thread.currentThread().contextClassLoader
            for (classInfo in ClassPath.from(loader)
                .getTopLevelClassesRecursive("de.tectoast.emolga.commands")) {
                val cl = classInfo.load()
                if (cl.isInterface) continue
                val name = cl.superclass.simpleName
                if (name.endsWith("Command") && !Modifier.isAbstract(cl.modifiers)) {
                    val cmd = cl.constructors[0].newInstance()
                    if (cl.isAnnotationPresent(ToTest::class.java)) {
                        logger.warn("{} has to be tested!", cl.name)
                    }
                    if (cmd is Command) {
                        cmd.addToMap()
                        for (dc in cl.declaredClasses) {
                            if (dc.superclass.simpleName.endsWith("Command") && !Modifier.isAbstract(dc.modifiers)) {
                                val ccmd = dc.constructors[0].newInstance()
                                if (ccmd is Command) {
                                    ccmd.addToChildren(cmd)
                                }
                            }
                        }
                    }
                }
            }

        }

        fun getFirst(str: String): String {
            return str.split("-").dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        }

        fun getFirstAfterUppercase(s: String): String {
            return if (!s.contains("-")) s else s[0].toString() + s.substring(1, 2).uppercase() + s.substring(2)
        }

        fun invertImage(mon: String, shiny: Boolean): File {
            logger.info("mon = $mon")
            val f = File(
                "../Showdown/sspclient/sprites/gen5" + (if (shiny) "-shiny" else "") + "/" + mon.lowercase() + ".png"
            )
            logger.info("f.getAbsolutePath() = " + f.absolutePath)
            val inputFile = ImageIO.read(f)
            val width = inputFile.width
            val height = inputFile.height
            for (x in 0 until width) {
                for (y in 0 until height) {
                    val rgba = inputFile.getRGB(x, y)
                    var col = java.awt.Color(rgba, true)
                    col = java.awt.Color(
                        255 - col.red, 255 - col.green, 255 - col.blue
                    )
                    inputFile.setRGB(x, y, col.rgb)
                }
            }
            val outputFile = File("tempimages/invert-$mon.png")
            ImageIO.write(inputFile, "png", outputFile)
            return outputFile
        }


        fun loadPlaylist(channel: TextChannel, track: String, mem: Member, cm: String?, random: Boolean = false) {
            val musicManager = getGuildAudioPlayer(channel.guild)
            logger.info(track)
            getPlayerManager(channel.guild).loadItemOrdered(musicManager, track, object : AudioLoadResultHandler {
                override fun trackLoaded(track: AudioTrack) {}
                override fun playlistLoaded(playlist: AudioPlaylist) {
                    val toplay = mutableListOf<AudioTrack>()
                    if (random) {
                        val list = ArrayList(playlist.tracks)
                        list.shuffle()
                        toplay.addAll(list)
                    } else {
                        toplay.addAll(playlist.tracks)
                    }
                    for (playlistTrack in toplay) {
                        play(channel.guild, musicManager, playlistTrack, mem, channel)
                    }
                    channel.sendMessage(cm ?: "Loaded playlist!").queue()
                }

                override fun noMatches() {
                    channel.sendMessage("Es wurde unter `$track` nichts gefunden!").queue()
                }

                override fun loadFailed(exception: FriendlyException) {
                    exception.printStackTrace()
                    channel.sendMessage("Der Track konnte nicht abgespielt werden: " + exception.message).queue()
                }
            })
        }


        fun byName(name: String): Command? {
            return commands.values.firstOrNull {
                it.name.equals(name, ignoreCase = true) || it.aliases.any { alias ->
                    name.equals(
                        alias, ignoreCase = true
                    )
                }
            }
        }

        fun playSound(vc: AudioChannel?, path: String?, tc: MessageChannelUnion) {
            val flegmon = vc!!.jda.selfUser.idLong != 723829878755164202L
            if (System.currentTimeMillis() - lastClipUsed < 10000 && flegmon) {
                tc.sendMessage("Warte bitte noch kurz...").queue()
                return
            }
            val g = vc.guild
            val gmm = getGuildAudioPlayer(g)
            getPlayerManager(g).loadItemOrdered(gmm, path, object : AudioLoadResultHandler {
                override fun trackLoaded(track: AudioTrack) {
                    if (flegmon) lastClipUsed = System.currentTimeMillis()
                    play(g, gmm, track, vc)
                }

                override fun playlistLoaded(playlist: AudioPlaylist) {}
                override fun noMatches() {}
                override fun loadFailed(exception: FriendlyException) {
                    exception.printStackTrace()
                }
            })
        }

        fun parseShortTime(timestring: String): Int {
            var timestr = timestring
            timestr = timestr.lowercase()
            if (!DURATION_PATTERN.matches(timestr)) return -1
            var multiplier = 1
            when (timestr[timestr.length - 1]) {
                'd' -> {
                    multiplier *= 24
                    multiplier *= 60
                    multiplier *= 60
                    timestr = timestr.substring(0, timestr.length - 1)
                }

                'h' -> {
                    multiplier *= 60
                    multiplier *= 60
                    timestr = timestr.substring(0, timestr.length - 1)
                }

                'm' -> {
                    multiplier *= 60
                    timestr = timestr.substring(0, timestr.length - 1)
                }

                's' -> timestr = timestr.substring(0, timestr.length - 1)
                else -> {}
            }
            return multiplier * timestr.toInt()
        }


        fun secondsToTime(timesec: Long): String {
            var timeseconds = timesec
            val builder = StringBuilder(20)
            val years = (timeseconds / (60 * 60 * 24 * 365)).toInt()
            if (years > 0) {
                builder.append("**").append(years).append("** ").append(pluralise(years.toLong(), "Jahr", "Jahre"))
                    .append(", ")
                timeseconds %= (60 * 60 * 24 * 365)
            }
            val weeks = (timeseconds / (60 * 60 * 24 * 7)).toInt()
            if (weeks > 0) {
                builder.append("**").append(weeks).append("** ")
                    .append(pluralise(weeks.toLong(), "Woche", "Wochen"))
                    .append(", ")
                timeseconds %= (60 * 60 * 24 * 7)
            }
            val days = (timeseconds / (60 * 60 * 24)).toInt()
            if (days > 0) {
                builder.append("**").append(days).append("** ").append(pluralise(days.toLong(), "Tag", "Tage"))
                    .append(", ")
                timeseconds %= (60 * 60 * 24)
            }
            val hours = (timeseconds / (60 * 60)).toInt()
            if (hours > 0) {
                builder.append("**").append(hours).append("** ")
                    .append(pluralise(hours.toLong(), "Stunde", "Stunden"))
                    .append(", ")
                timeseconds %= (60 * 60)
            }
            val minutes = (timeseconds / 60).toInt()
            if (minutes > 0) {
                builder.append("**").append(minutes).append("** ")
                    .append(pluralise(minutes.toLong(), "Minute", "Minuten")).append(", ")
                timeseconds %= 60
            }
            if (timeseconds > 0) {
                builder.append("**").append(timeseconds).append("** ")
                    .append(pluralise(timeseconds, "Sekunde", "Sekunden"))
            }
            var str = builder.toString()
            if (str.endsWith(", ")) str = str.substring(0, str.length - 2)
            if (str.isEmpty()) str = "**0** Sekunden"
            return str
        }

        private fun pluralise(x: Long, singular: String, plural: String): String {
            return if (x == 1L) singular else plural
        }

        fun getGameDay(league: League, uid1: String, uid2: String): Int {
            return getGameDay(league, uid1.toLong(), uid2.toLong())
        }

        fun getGameDay(league: League, uid1: Long, uid2: Long): Int {
            val table = league.table
            val u1 = table.indexOf(uid1)
            val u2 = table.indexOf(uid2)
            return league.battleorder.asIterable()
                .firstNotNullOf { if (it.value.any { l -> l.containsAll(listOf(u1, u2)) }) it.key else null }
        }

        fun tempMute(tco: GuildMessageChannel, mod: Member, mem: Member, time: Int, reason: String) {
            val g = tco.guild
            if (!g.selfMember.canInteract(mem)) {
                val builder = EmbedBuilder()
                builder.setColor(java.awt.Color.RED)
                builder.setTitle("Ich kann diesen User nicht muten!")
                tco.sendMessageEmbeds(builder.build()).queue()
                return
            }
            MutedRolesDB.getMutedRole(g.idLong)?.let { g.addRoleToMember(mem, g.getRoleById(it)!!).queue() }
            val expires = (System.currentTimeMillis() + time * 1000L) / 1000 * 1000
            muteTimer(g, expires, mem.idLong)
            val builder = EmbedBuilder()
            builder.setAuthor(
                mem.effectiveName + " wurde für " + secondsToTime(time.toLong()).replace(
                    "*", ""
                ) + " gemutet", null, mem.user.effectiveAvatarUrl
            )
            builder.setColor(java.awt.Color.CYAN)
            builder.setDescription("**Grund:** $reason")
            tco.sendMessageEmbeds(builder.build()).queue()
            //Database.insert("mutes", "userid, modid, guildid, reason, expires", mem.getIdLong(), mod.getIdLong(), g.getIdLong(), reason, new Timestamp(expires));
            MuteDB.mute(mem.idLong, mod.idLong, g.idLong, reason, expires)
        }

        fun muteTimer(g: Guild, expires: Long, mem: Long) {
            if (expires == -1L) return
            moderationService.schedule({
                val gid = g.idLong
                if (MuteDB.unmute(mem, gid) != 0) {
                    MutedRolesDB.getMutedRole(gid)?.let {
                        g.removeRoleFromMember(mem.usersnowflake, g.getRoleById(it)!!).queue()
                    }
                }
            }, expires - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
        }

        fun kick(tco: GuildMessageChannel, mod: Member, mem: Member, reason: String) {
            val g = tco.guild
            if (!g.selfMember.canInteract(mem)) {
                val builder = EmbedBuilder()
                builder.setColor(java.awt.Color.RED)
                builder.setTitle("Ich kann diesen User nicht kicken!")
                tco.sendMessageEmbeds(builder.build()).queue()
                return
            }
            if (mem.idLong == FLOID) {
                val builder = EmbedBuilder()
                builder.setColor(java.awt.Color.RED)
                builder.setTitle("Das lässt du lieber bleiben :3")
                tco.sendMessageEmbeds(builder.build()).queue()
                return
            }
            g.kick(mem).reason(reason).queue()
            val builder = EmbedBuilder()
            builder.setAuthor(mem.effectiveName + " wurde gekickt", null, mem.user.effectiveAvatarUrl)
            builder.setColor(java.awt.Color.CYAN)
            builder.setDescription("**Grund:** $reason")
            tco.sendMessageEmbeds(builder.build()).queue()
            //Database.insert("kicks", "userid, modid, guildid, reason", mem.getIdLong(), mod.getIdLong(), tco.getGuild().getIdLong(), reason);
            KicksDB.kick(mem.idLong, mod.idLong, tco.guild.idLong, reason)
        }

        fun ban(tco: GuildMessageChannel, mod: Member, mem: Member, reason: String) {
            val g = tco.guild
            if (!g.selfMember.canInteract(mem)) {
                val builder = EmbedBuilder()
                builder.setColor(java.awt.Color.RED)
                builder.setTitle("Ich kann diesen User nicht bannen!")
                tco.sendMessageEmbeds(builder.build()).queue()
                return
            }
            if (mem.idLong == FLOID) {
                val builder = EmbedBuilder()
                builder.setColor(java.awt.Color.RED)
                builder.setTitle("Das lässt du lieber bleiben :3")
                tco.sendMessageEmbeds(builder.build()).queue()
                return
            }
            g.ban(mem, 0, TimeUnit.SECONDS).reason(reason).queue()
            val builder = EmbedBuilder()
            builder.setAuthor(mem.effectiveName + " wurde gebannt", null, mem.user.effectiveAvatarUrl)
            builder.setColor(java.awt.Color.CYAN)
            builder.setDescription("**Grund:** $reason")
            tco.sendMessageEmbeds(builder.build()).queue()
            BanDB.ban(mem.idLong, mem.user.name, mod.idLong, g.idLong, reason, null)
        }

        fun mute(tco: GuildMessageChannel, mod: Member, mem: Member, reason: String) {
            val g = tco.guild
            val gid = g.idLong
            if (!g.selfMember.canInteract(mem)) {
                val builder = EmbedBuilder()
                builder.setColor(java.awt.Color.RED)
                builder.setTitle("Ich kann diesen User nicht muten!")
                tco.sendMessageEmbeds(builder.build()).queue()
                return
            }
            MutedRolesDB.getMutedRole(gid)?.let { g.addRoleToMember(mem, g.getRoleById(it)!!).queue() }
            val builder = EmbedBuilder()
            builder.setAuthor(mem.effectiveName + " wurde gemutet", null, mem.user.effectiveAvatarUrl)
            builder.setColor(java.awt.Color.CYAN)
            builder.setDescription("**Grund:** $reason")
            tco.sendMessageEmbeds(builder.build()).queue()
            MuteDB.mute(mem.idLong, mod.idLong, tco.guild.idLong, reason, null)
        }

        fun unmute(tco: GuildMessageChannel, mem: Member) {
            val g = tco.guild
            val gid = g.idLong
            MutedRolesDB.getMutedRole(gid)?.let { g.removeRoleFromMember(mem, g.getRoleById(it)!!).queue() }
            val builder = EmbedBuilder()
            builder.setAuthor(mem.effectiveName + " wurde entmutet", null, mem.user.effectiveAvatarUrl)
            builder.setColor(java.awt.Color.CYAN)
            tco.sendMessageEmbeds(builder.build()).queue()
            MuteDB.unmute(mem.idLong, gid)
        }

        fun tempBan(tco: GuildMessageChannel, mod: Member, mem: Member, time: Int, reason: String) {
            val g = tco.guild
            if (!g.selfMember.canInteract(mem)) {
                val builder = EmbedBuilder()
                builder.setColor(java.awt.Color.RED)
                builder.setTitle("Ich kann diesen User nicht bannen!")
                tco.sendMessageEmbeds(builder.build()).queue()
                return
            }
            g.ban(mem, 0, TimeUnit.SECONDS).reason(reason).queue()
            val expires = System.currentTimeMillis() + time * 1000L
            banTimer(g, expires, mem.idLong)
            val builder = EmbedBuilder()
            builder.setAuthor(
                mem.effectiveName + " wurde für " + secondsToTime(time.toLong()).replace(
                    "*", ""
                ) + " gebannt", null, mem.user.effectiveAvatarUrl
            )
            builder.setColor(java.awt.Color.CYAN)
            builder.setDescription("**Grund:** $reason")
            tco.sendMessageEmbeds(builder.build()).queue()
            BanDB.ban(mem.idLong, mem.user.name, mod.idLong, tco.guild.idLong, reason, Instant.ofEpochMilli(expires))
        }

        fun banTimer(g: Guild, expires: Long, mem: Long) {
            if (expires == -1L) return
            moderationService.schedule({
                val gid = g.idLong
                if (BanDB.unban(mem, gid) != 0) {
                    g.unban(UserSnowflake.fromId(mem)).queue()
                }
            }, expires - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
        }

        fun warn(tco: GuildMessageChannel, mod: Member, mem: Member, reason: String) {
            val g = tco.guild
            if (!g.selfMember.canInteract(mem)) {
                val builder = EmbedBuilder()
                builder.setColor(java.awt.Color.RED)
                builder.setTitle("Ich kann diesen User nicht warnen!")
                tco.sendMessageEmbeds(builder.build()).queue()
                return
            }
            val builder = EmbedBuilder()
            builder.setAuthor(mem.effectiveName + " wurde verwarnt", null, mem.user.effectiveAvatarUrl)
            builder.setColor(java.awt.Color.CYAN)
            builder.setDescription("**Grund:** $reason")
            tco.sendMessageEmbeds(builder.build()).queue()
            //Database.insert("warns", "userid, modid, guildid, reason", mem.getIdLong(), mod.getIdLong(), tco.getGuild().getIdLong(), reason);
            WarnsDB.warn(mem.idLong, mod.idLong, tco.guild.idLong, reason)
        }


        fun getNumber(map: Map<String, Int>, pick: String): String {
            //logger.info(map);
            for ((s, value) in map) {
                if (s == pick || pick == "M-$s" || s.split("-").first().let {
                        when (it) {
                            "A", "Alola", "G", "Galar", "M", "Mega", "Kapu" -> null
                            else -> it
                        }
                    } == pick.split("-").first()) return value.toString()
            }
            return ""
        }


        fun indexPick(picks: List<String>, mon: String): Int {
            for (pick in picks) {
                if (pick.equals(mon, ignoreCase = true) || pick.substring(2)
                        .equals(mon, ignoreCase = true)
                ) return picks.indexOf(pick)
                if (pick.equals("Amigento", ignoreCase = true) && mon.contains("Amigento")) return picks.indexOf(
                    pick
                )
            }
            return -1
        }


        fun formatToTime(toformat: Long): String {
            var l = toformat
            l /= 1000
            val hours = (l / 3600).toInt()
            val minutes = ((l - hours * 3600) / 60).toInt()
            val seconds = (l - hours * 3600 - minutes * 60).toInt()
            var str = ""
            if (hours > 0) str += getWithZeros(hours, 2) + ":"
            str += getWithZeros(minutes, 2) + ":"
            str += getWithZeros(seconds, 2)
            return str
        }

        fun getWithZeros(i: Int, lenght: Int): String {
            val str = StringBuilder(i.toString())
            while (str.length < lenght) str.insert(0, "0")
            return str.toString()
        }

        private fun <T> getXTimes(`object`: T, times: Int): List<T> {
            val list = ArrayList<T>()
            for (i in 0 until times) {
                list.add(`object`)
            }
            return list
        }

        fun getCellsAsRowData(`object`: CellData, x: Int, y: Int): List<RowData> {
            val list: MutableList<RowData> = mutableListOf()
            for (i in 0 until y) {
                list.add(RowData().setValues(getXTimes(`object`, x)))
            }
            return list
        }


        fun compareColumns(o1: List<Any>, o2: List<Any>, vararg columns: Int): Int {
            for (column in columns) {
                val i1: Int = if (o1[column] is Int) o1[column] as Int else (o1[column] as String).toInt()
                val i2: Int = if (o2[column] is Int) o2[column] as Int else (o2[column] as String).toInt()
                if (i1 != i2) {
                    return i1.compareTo(i2)
                }
            }
            return 0
        }

        val calendarFormat = SimpleDateFormat("dd.MM. HH:mm")

        @JvmStatic
        protected fun buildCalendar(): String {
            return CalendarDB.allFloEntries.sortedBy { it.expires }
                .joinToString("\n") { o: CalendarEntry -> "**${calendarFormat.format(o.expires.toEpochMilli())}:** ${o.message}" }
                .ifEmpty { "_leer_" }
        }

        fun scheduleCalendarEntry(ce: CalendarEntry) {
            calendarService.schedule(
                {
                    try {
                        transaction {
                            if (runCatching { ce.refresh() }.let {
                                    println(
                                        it.exceptionOrNull()?.stackTraceToString()
                                    ); it.isFailure
                                }) return@transaction null
                            ce.delete()
                        } ?: return@schedule
                        println(ce.person)
                        ce.person?.let { p ->
                            val tc = emolgajda.getTextChannelById(p.tcid)!!
                            tc.editMessageComponentsById(
                                ce.messageid!!,
                                danger("homework;done", "Gemacht", emoji = Emoji.fromUnicode("✅")).into()
                            ).queue()
                            tc.sendMessage("<@${p.uid}> Hausaufgabe fällig :)").setMessageReference(ce.messageid!!)
                                .addActionRow(
                                    primary("calendar;delete", "Benachrichtigung löschen")
                                ).queue()
                        } ?: run {
                            val calendarTc: TextChannel = emolgajda.getTextChannelById(CALENDAR_TCID)!!
                            calendarTc.sendMessage("(<@$FLOID>) ${ce.message}")
                                .setActionRow(Button.primary("calendar;delete", "Löschen")).queue()
                            calendarTc.editMessageById(CALENDAR_MSGID, buildCalendar()).queue()
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                },
                (ce.expires.toEpochMilli() - System.currentTimeMillis()).also { println("DELAY: $it") },
                TimeUnit.MILLISECONDS
            )
        }


        @JvmStatic
        @Throws(NumberFormatException::class)
        protected fun parseCalendarTime(str: String): Long {
            var timestr = str.lowercase()
            if (!DURATION_PATTERN.matches(timestr)) {
                val calendar = Calendar.getInstance()
                calendar[Calendar.SECOND] = 0
                var hoursSet = false
                for (s in str.split(";", " ")) {
                    val split = DURATION_SPLITTER.split(s)
                    if (s.contains(".")) {
                        calendar[Calendar.DAY_OF_MONTH] = split[0].toInt()
                        calendar[Calendar.MONTH] = split[1].toInt() - 1
                    } else if (s.contains(":")) {
                        calendar[Calendar.HOUR_OF_DAY] = split[0].toInt()
                        calendar[Calendar.MINUTE] = split[1].toInt()
                        hoursSet = true
                    }
                }
                if (!hoursSet) {
                    calendar[Calendar.HOUR_OF_DAY] = 15
                    calendar[Calendar.MINUTE] = 0
                }
                return calendar.timeInMillis
            }
            var multiplier = 1000
            when (timestr[timestr.length - 1]) {
                'w' -> {
                    multiplier *= 7
                    multiplier *= 24
                    multiplier *= 60
                    multiplier *= 60
                    timestr = timestr.substring(0, timestr.length - 1)
                }

                'd' -> {
                    multiplier *= 24
                    multiplier *= 60
                    multiplier *= 60
                    timestr = timestr.substring(0, timestr.length - 1)
                }

                'h' -> {
                    multiplier *= 60
                    multiplier *= 60
                    timestr = timestr.substring(0, timestr.length - 1)
                }

                'm' -> {
                    multiplier *= 60
                    timestr = timestr.substring(0, timestr.length - 1)
                }

                's' -> timestr = timestr.substring(0, timestr.length - 1)
            }
            return System.currentTimeMillis() + multiplier.toLong() * timestr.toInt()
        }

        suspend fun updateShinyCounts(id: Long) {
            emolgajda.getTextChannelById(id)?.run {
                editMessageById(
                    if (id == 778380440078647296L) 778380596413464676L else 925446888772239440L,
                    buildShinyCounts()
                ).queue()
            }
        }

        suspend fun updateSoullink() {
            emolgajda.getTextChannelById(SOULLINK_TCID)!!.editMessageById(SOULLINK_MSGID, buildSoullink()).queue()
        }

        private fun soullinkCols(): List<String> {
            return listOf(*soullinkNames.toTypedArray(), "Fundort", "Status")
        }

        private suspend fun buildSoullink(): String {
            val statusOrder = listOf("Team", "Box", "RIP")
            val soullink = db.soullink.only()
            val mons = soullink.mons
            val order = soullink.order
            val maxlen = max(order.maxOfOrNull { it.length } ?: -1,
                max(mons.values.flatMap { o -> o.values }.maxOfOrNull { obj -> obj.length } ?: -1, 7)) + 1
            val b = StringBuilder("```")
            soullinkCols().map { ew(it, maxlen) }.forEach { b.append(it) }
            b.append("\n")
            for (s in order.sortedBy {
                statusOrder.indexOf(
                    //    mons.createOrGetJSON(it).optString("status", "Box")
                    mons.getOrPut(it) { mutableMapOf() }["status"] ?: "Box"
                )
            }) {
                val o = mons.getOrPut(s) { mutableMapOf() }
                val status = o["status"] ?: "Box"
                b.append(soullinkCols().joinToString("") {
                    ew(
                        when (it) {
                            "Fundort" -> s
                            "Status" -> status
                            else -> o[it] ?: ""
                        }, maxlen
                    )
                }).append("\n")
            }
            return b.append("```").toString()
        }

        /**
         * Expand whitespaces
         *
         * @param str the string
         * @param len the length
         * @return the whitespaced string
         */
        private fun ew(str: String, len: Int): String {
            return str + " ".repeat(max(0, len - str.length))
        }

        suspend fun updateShinyCounts(e: ButtonInteractionEvent) {
            e.editMessage(buildShinyCounts()).queue()
        }

        private suspend fun buildShinyCounts(): String {
            return buildString {
                db.shinycount.only().run {
                    methodorder.forEach { method ->
                        val m = counter[method]!!
                        append(method)
                        append(": ")
                        append(emolgajda.getGuildById(745934535748747364)!!.getEmojiById(m["emote"]!!)!!.asMention)
                        append("\n")
                        userorder.forEach {
                            append(names[it])
                            append(": ")
                            append(m[it.toString()] ?: 0)
                            append("\n")
                        }
                        append("\n")
                    }
                }
            }
        }

        fun loadAndPlay(channel: GuildMessageChannel, trackUrl: String, vc: VoiceChannel) {
            val musicManager = getGuildAudioPlayer(vc.guild)
            getPlayerManager(vc.guild).loadItemOrdered(musicManager, trackUrl, object : AudioLoadResultHandler {
                override fun trackLoaded(track: AudioTrack) {
                    channel.sendMessage("`" + track.info.title + "` wurde zur Warteschlange hinzugefügt!").queue()
                    play(vc.guild, musicManager, track, vc)
                }

                override fun playlistLoaded(playlist: AudioPlaylist) {
                    var firstTrack = playlist.selectedTrack
                    if (firstTrack == null) {
                        firstTrack = playlist.tracks[0]
                    }
                    channel.sendMessage("Adding to queue " + firstTrack!!.info.title + " (first track of playlist " + playlist.name + ")")
                        .queue()
                    play(vc.guild, musicManager, firstTrack, vc)
                }

                override fun noMatches() {
                    channel.sendMessage("Es wurde unter `$trackUrl` nichts gefunden!").queue()
                }

                override fun loadFailed(exception: FriendlyException) {
                    channel.sendMessage("Der Track konnte nicht abgespielt werden: " + exception.message).queue()
                    exception.printStackTrace()
                }
            })
        }

        fun play(guild: Guild, musicManager: GuildMusicManager, track: AudioTrack, mem: Member, tc: TextChannel) {
            val audioManager = guild.audioManager
            if (!audioManager.isConnected) {
                if (mem.voiceState!!.inAudioChannel()) {
                    audioManager.openAudioConnection(mem.voiceState!!.channel)
                } else {
                    tc.sendMessage("Du musst dich in einem Voicechannel befinden!").queue()
                }
            }
            musicManager.scheduler.queue(track)
        }

        fun playFlo(guild: Guild, musicManager: GuildMusicManager, track: AudioTrack, mem: Member) {
            val audioManager = guild.audioManager
            if (!audioManager.isConnected) {
                if (mem.voiceState!!.inAudioChannel()) {
                    audioManager.openAudioConnection(mem.voiceState!!.channel)
                } else {
                    sendToMe("Du musst dich in einem Voicechannel befinden!")
                }
            }
            musicManager.scheduler.queue(track)
        }

        fun play(guild: Guild, musicManager: GuildMusicManager, track: AudioTrack, vc: AudioChannel) {
            val audioManager = guild.audioManager
            if (!audioManager.isConnected) {
                audioManager.openAudioConnection(vc)
            }
            musicManager.scheduler.queue(track)
        }

        fun skipTrack(channel: TextChannel) {
            val musicManager = getGuildAudioPlayer(channel.guild)
            musicManager.scheduler.nextTrack()
            channel.sendMessage("Skipped :)").queue()
        }

        @Synchronized
        fun getGuildAudioPlayer(guild: Guild): GuildMusicManager {
            val guildId = guild.idLong
            var musicManager = musicManagers[guildId]
            if (musicManager == null) {
                musicManager = GuildMusicManager(getPlayerManager(guild))
                musicManagers[guildId] = musicManager
            }
            guild.audioManager.sendingHandler = musicManager.sendHandler
            return musicManager
        }

        @Synchronized
        fun getPlayerManager(guild: Guild): AudioPlayerManager {
            val guildId = guild.idLong
            var playerManager = playerManagers[guildId]
            if (playerManager == null) {
                playerManager = DefaultAudioPlayerManager()
                playerManagers[guildId] = playerManager
            }
            AudioSourceManagers.registerRemoteSources(playerManager)
            AudioSourceManagers.registerLocalSource(playerManager)
            return playerManager
        }


        fun trim(s: String, pokemon: String): String {
            return s.replace(pokemon, stars(pokemon.length)).replace(pokemon.uppercase(), stars(pokemon.length))
        }

        private fun stars(n: Int): String {
            return "+".repeat(0.coerceAtLeast(n))
        }

        inline fun <reified T> load(filename: String, json: Json = otherJSON): T {
            if (T::class.simpleName == "JsonObject") {
                return json.parseToJsonElement(filename.file().readText()) as T
            }
            return json.decodeFromString(filename.file().readText())
        }

        val dataJSON: Map<String, Pokemon>
            get() = ModManager.default.dex

        operator fun Map<String, Pokemon>.invoke(name: String): Pokemon {
            return this[name]!!
        }

        val typeJSON: Map<String, TypeData>
            get() = ModManager.default.typechart
        val learnsetJSON: Map<String, Learnset>
            get() = ModManager.default.learnsets
        val movesJSON: JsonObject
            get() = ModManager.default.moves


        fun awaitNextDay() {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_MONTH, 1)
            c[Calendar.HOUR_OF_DAY] = 0
            c[Calendar.MINUTE] = 0
            c[Calendar.SECOND] = 0
            val tilnextday = c.timeInMillis - System.currentTimeMillis() + 1000
            logger.info("System.currentTimeMillis() = " + System.currentTimeMillis())
            logger.info("tilnextday = $tilnextday")
            logger.info("System.currentTimeMillis() + tilnextday = " + (System.currentTimeMillis() + tilnextday))
            logger.info("SELECT REQUEST: " + "SELECT * FROM birthdays WHERE month = " + (c[Calendar.MONTH] + 1) + " AND day = " + c[Calendar.DAY_OF_MONTH])
            if (EmolgaMain.NOTEMPVERSION) {
                birthdayService.schedule({
                    BirthdayDB.checkBirthdays(c, flegmonjda.getTextChannelById(605650587329232896L)!!)
                    awaitNextDay()
                }, tilnextday, TimeUnit.MILLISECONDS)
            }
        }

        fun getTrainerDataActionRow(dt: TrainerData, withMoveset: Boolean): Collection<ActionRow> {
            return listOf(
                ActionRow.of(StringSelectMenu.create("trainerdata").addOptions(dt.monsList.map {
                    SelectOption.of(
                        it, it
                    ).withDefault(dt.isCurrent(it))
                }).build()), ActionRow.of(
                    if (withMoveset) Button.success(
                        "trainerdata;CHANGEMODE", "Mit Moveset"
                    ) else Button.secondary("trainerdata;CHANGEMODE", "Ohne Moveset")
                )
            )
        }

        fun getGenerationFromDexNumber(dexnumber: Int): Int {
            return when (dexnumber) {
                in 1..151 -> 1
                in 152..251 -> 2
                in 252..386 -> 3
                in 387..493 -> 4
                in 494..649 -> 5
                in 650..721 -> 6
                in 722..809 -> 7
                in 810..905 -> 8
                in 906..1010 -> 9
                else -> -1
            }
        }

        fun executeTipGameSending(league: League, num: Int) {
            defaultScope.launch {
                val docEntry = league.docEntry!!
                val tip = league.tipgame!!
                val channel = emolgajda.getTextChannelById(tip.channel)!!
                val matchups = docEntry.getMatchups(num)
                val names = emolgajda.getGuildById(league.guild)!!.retrieveMembersByIds(matchups.flatten()).await()
                    .associate { it.idLong to it.effectiveName }
                val table = league.table
                channel.send(
                    embeds = Embed(
                        title = "Spieltag $num", color = java.awt.Color.YELLOW.rgb
                    ).into()
                ).queue()
                for ((index, matchup) in matchups.withIndex()) {
                    val u1 = matchup[0]
                    val u2 = matchup[1]
                    val baseid = "tipgame;${league.leaguename}:$num:$index"
                    channel.send(
                        embeds = Embed(
                            title = "${names[u1]} vs. ${names[u2]}", color = embedColor
                        ).into(), components = ActionRow.of(
                            primary("$baseid:${u1.indexedBy(table)}", names[u1]),
                            primary("$baseid:${u2.indexedBy(table)}", names[u2]),
                        ).into()
                    ).queue()
                }
            }
        }

        fun executeTipGameLockButtons(league: League, gameday: Int) {
            defaultScope.launch {
                emolgajda.getTextChannelById(league.tipgame!!.channel)!!.iterableHistory.takeAsync(league.table.size / 2)
                    .await().forEach {
                        it.editMessageComponents(
                            ActionRow.of(it.actionRows[0].buttons.map { button -> button.asDisabled() })
                        ).queue()
                    }
                league.onTipGameLockButtons(gameday)
            }
        }

        suspend fun setupRepeatTasks() {
            setupManualRepeatTasks()
            db.drafts.find().toFlow().collect { l ->
                l.takeIf { it.docEntry != null }?.tipgame?.let { tip ->
                    val duration = Duration.ofSeconds(parseShortTime(tip.interval).toLong())
                    RepeatTask(
                        tip.lastSending.toInstant(),
                        tip.amount,
                        duration,
                        { executeTipGameSending(runBlocking { db.league(l.leaguename) }, it) },
                        true
                    )
                    RepeatTask(
                        tip.lastLockButtons.toInstant(), tip.amount, duration,
                        { executeTipGameLockButtons(runBlocking { db.league(l.leaguename) }, it) }, true
                    )
                }
            }

        }

        private fun setupManualRepeatTasks() {
            NDS.setupRepeatTasks()
            ASL.setupRepeatTasks()
        }


        fun init(key: String) {
            loadJSONFiles(key)
            ModManager("default", "./ShowdownData/")
            Tierlist.setup()
            defaultScope.launch {
                ButtonListener.init()
                MenuListener.init()
                ModalListener.init()
                registerCommands()
                setupRepeatTasks()
                sdex["Burmadame-Pflz"] = ""
                sdex["Burmadame-Pflanze"] = ""
                sdex["Burmadame-Sand"] = "-sandy"
                sdex["Burmadame-Boden"] = "-sandy"
                sdex["Burmadame-Lumpen"] = "-trash"
                sdex["Burmadame-Stahl"] = "-trash"
                sdex["Boreos-T"] = "-therian"
                sdex["Demeteros-T"] = "-therian"
                sdex["Deoxys-Def"] = "-defense"
                sdex["Deoxys-Speed"] = "-speed"
                sdex["Hoopa-U"] = "-unbound"
                sdex["Wulaosu-Wasser"] = "-rapidstrike"
                sdex["Demeteros-I"] = ""
                sdex["Rotom-Heat"] = "-heat"
                sdex["Rotom-Wash"] = "-wash"
                sdex["Rotom-Mow"] = "-mow"
                sdex["Rotom-Fan"] = "-fan"
                sdex["Rotom-Frost"] = "-frost"
                sdex["Wolwerock-Zw"] = "-dusk"
                sdex["Wolwerock-Dusk"] = "-dusk"
                sdex["Wolwerock-Tag"] = ""
                sdex["Wolwerock-Nacht"] = "-midnight"
                sdex["Boreos-I"] = ""
                sdex["Voltolos-T"] = "-therian"
                sdex["Voltolos-I"] = ""
                sdex["Zygarde-50%"] = ""
                sdex["Zygarde-10%"] = "-10"
                sdex["Psiaugon-W"] = "f"
                sdex["Psiaugon-M"] = ""
                sdex["Nidoran-M"] = "m"
                sdex["Nidoran-F"] = "f"
            }
        }

        fun convertColor(hexcode: Int): Color {
            val c = java.awt.Color(hexcode)
            return Color().setRed(c.red.toFloat() / 255f).setGreen(c.green.toFloat() / 255f)
                .setBlue(c.blue.toFloat() / 255f)
        }

        val monList: List<String>
            get() = dataJSON.keys.filter { !it.endsWith("gmax") && !it.endsWith("totem") }

        fun loadJSONFiles(key: String? = null) {
            key?.let {
                println("Begin decrypt...")
                tokens = TokenEncrypter.decrypt(it)
            }
            defaultScope.launch {
                //emolgaJSON = load("./emolgadata.json")
                //datajson = loadSD("pokedex.ts", 59);
                //movejson = loadSD("learnsets.ts", 62);
                catchrates = load("./catchrates.json")
                with(tokens.google) {
                    Google.setCredentials(refreshtoken, clientid, clientsecret)
                    Google.generateAccessToken()
                }
            }
        }

        fun getWithCategory(category: CommandCategory, g: Guild, mem: Member): List<Command> {
            return commands.values.toSet().filter {
                !it.disabled && it.category === category && it.allowsGuild(g) && it.allowsMember(mem)
            }.sortedBy { it.name }
        }


        fun updatePresence() {
            if (BOT_DISABLED) {
                emolgajda.presence.setPresence(
                    OnlineStatus.DO_NOT_DISTURB,
                    Activity.watching("auf den Wartungsmodus")
                )
                return
            }
            val count = StatisticsDB.analysisCount
            replayCount.set(count)
            if (count % 100 == 0) {
                emolgajda.getTextChannelById(904481960527794217L)!!
                    .sendMessage(SimpleDateFormat("dd.MM.yyyy").format(Date()) + ": " + count).queue()
            }
            emolgajda.presence.setPresence(OnlineStatus.ONLINE, Activity.watching("auf $count analysierte Replays"))
        }

        fun getHelpButtons(g: Guild, mem: Member): List<ActionRow> {
            return getActionRows(order.filter { cat: CommandCategory ->
                cat.allowsGuild(g) && cat.allowsMember(
                    mem
                )
            }) { s: CommandCategory ->
                Button.primary("help;" + s.categoryName.lowercase(), s.categoryName).withEmoji(
                    Emoji.fromCustom(
                        g.jda.getEmojiById(s.emote)!!
                    )
                )
            }
        }

        fun help(tco: TextChannel, mem: Member) {
            tco.sendMessage(
                MessageCreate(
                    embeds = Embed(title = "Commands", color = embedColor).into(),
                    components = getHelpButtons(tco.guild, mem)
                )
            ).queue()
        }

        suspend fun check(e: MessageReceivedEvent) {
            val mem = e.member!!
            val msg = e.message.contentDisplay
            val tco = e.channel
            val gid = e.guild.idLong
            val bot = Bot.byJDA(e.jda)
            if (bot == Bot.FLEGMON || gid == 745934535748747364L) {
                val dir = File("audio/clips/")
                for (file in dir.listFiles()!!) {
                    if (msg.equals(
                            "!" + file.name.split(".").dropLastWhile { it.isEmpty() }.toTypedArray()[0],
                            ignoreCase = true
                        )
                    ) {
                        val voiceState = e.member!!.voiceState
                        val pepe = mem.idLong == 349978348010733569L
                        if (voiceState!!.inAudioChannel() || pepe) {
                            val am = e.guild.audioManager
                            if (!am.isConnected) {
                                if (voiceState.inAudioChannel()) {
                                    am.openAudioConnection(voiceState.channel)
                                    am.connectionListener = object : ConnectionListener {
                                        override fun onPing(ping: Long) {}
                                        override fun onStatusChange(status: ConnectionStatus) {
                                            logger.info("status = $status")
                                            if (status == ConnectionStatus.CONNECTED) {
                                                playSound(
                                                    voiceState.channel,
                                                    "/home/florian/Discord/audio/clips/hi.mp3",
                                                    tco
                                                )
                                                playSound(voiceState.channel, file.path, tco)
                                            }
                                        }

                                        override fun onUserSpeaking(user: User, speaking: Boolean) {}
                                    }
                                }
                            } else {
                                playSound(am.connectedChannel, file.path, tco)
                            }
                        }
                    }
                }
            }
            val command = commands[WHITESPACES_SPLITTER.split(msg)[0].lowercase()]
            if (command != null) {
                if (command.disabled || command.onlySlash) return
                if (!command.checkBot(e.jda, gid)) return
                val check = command.checkPermissions(gid, mem)
                if (check == PermissionCheck.GUILD_NOT_ALLOWED) return
                if (check == PermissionCheck.PERMISSION_DENIED) {
                    tco.sendMessage(NOPERM).queue()
                    return
                }
                if (mem.idLong != FLOID) {
                    if (BOT_DISABLED) {
                        e.channel.sendMessage(DISABLED_TEXT).queue()
                        return
                    }
                    if (command.wip) {
                        tco.sendMessage("Diese Funktion ist derzeit noch in Entwicklung und ist noch nicht einsatzbereit!")
                            .queue()
                        return
                    }
                }
                if (!command.everywhere && !command.category!!.isEverywhere) {
                    if (command.overrideChannel.containsKey(gid)) {
                        val l: List<Long> = command.overrideChannel[gid]!!
                        if (!l.contains(e.channel.idLong)) {
                            if (e.author.idLong == FLOID) {
                                tco.sendMessage("Eigentlich dürfen hier keine Commands genutzt werden, aber weil du es bist, mache ich das c:")
                                    .queue()
                            } else {
                                e.channel.sendMessage("<#" + l[0] + ">").queue()
                                return
                            }
                        }
                    } else {
                        db.emolgachannel.findOne(
                            EmolgaChannelConfig::guild eq gid,
                            not(EmolgaChannelConfig::channels contains e.channel.idLong),
                            not(EmolgaChannelConfig::channels.size(0)),
                        )?.let { l ->
                            if (e.author.idLong == FLOID) {
                                tco.sendMessage("Eigentlich dürfen hier keine Commands genutzt werden, aber weil du es bist, mache ich das c:")
                                    .queue()
                            } else {
                                e.channel.sendMessage("<#${l.channels.first()}>").queue()
                                return
                            }
                        }
                    }
                }
                StatisticsDB.increment("cmd_" + command.name)
                val randnum = Random.nextInt(4096)
                logger.info("randnum = $randnum")
                if (randnum == 133) {
                    e.channel.sendMessage("No, I don't think I will :^)\n||Gib mal den Command nochmal ein, die Wahrscheinlichkeit, dass diese Nachricht auftritt, liegt bei 1:4096 :D||")
                        .queue()
                    sendToMe(e.guild.name + " " + e.channel.asMention + " " + e.author.id + " " + e.author.asMention + " HAT IM LOTTO GEWONNEN!")
                    return
                }
                if (command.beta) e.channel.sendMessage(
                    "Dieser Command befindet sich zurzeit in der Beta-Phase! Falls Fehler auftreten, kontaktiert bitte ${Constants.MYTAG} durch einen Ping oder eine PN!"
                ).queue()
                try {
                    GuildCommandEvent(command, e).execute()
                } catch (ex: MissingArgumentException) {
                    if (ex.isSubCmdMissing) {
                        val subcommands = ex.subcommands!!
                        tco.sendMessage("Dieser Command beinhaltet Sub-Commands: " + subcommands.joinToString { "`$it`" })
                            .queue()
                    } else {
                        val arg = ex.argument!!
                        if (arg.hasCustomErrorMessage()) tco.sendMessage(arg.customErrorMessage!!).queue() else {
                            tco.sendMessage(
                                "Das benötigte Argument `" + arg.name + "`, was eigentlich " + buildEnumeration(
                                    arg.type.getName()
                                ) + " sein müsste, ist nicht vorhanden!\n" + "Nähere Informationen über die richtige Syntax für den Command erhältst du unter `e!help " + command.name + "`."
                            ).queue()
                        }
                        if (mem.idLong != FLOID) {
                            sendToMe("MissingArgument " + tco.asMention + " Server: " + tco.asGuildMessageChannel().guild.name)
                        }
                    }
                }

            }
        }

        fun eachWordUpperCase(s: String): String {
            return s.split(" ").joinToString(" ") { firstUpperCase(it) }
        }

        private fun firstUpperCase(s: String): String {
            return s.firstOrNull()?.uppercaseChar()?.plus(s.drop(1)) ?: ""
        }

        private fun getType(str: String): String {
            val s = str.lowercase()
            if (s.contains("-normal")) return "Normal" else if (s.contains("-kampf") || s.contains("-fighting")) return "Kampf" else if (s.contains(
                    "-flug"
                ) || s.contains("-flying")
            ) return "Flug" else if (s.contains("-gift") || s.contains("-poison")) return "Gift" else if (s.contains(
                    "-boden"
                ) || s.contains(
                    "-ground"
                )
            ) return "Boden" else if (s.contains("-gestein") || s.contains("-rock")) return "Gestein" else if (s.contains(
                    "-käfer"
                ) || s.contains("-bug")
            ) return "Käfer" else if (s.contains("-geist") || s.contains("-ghost")) return "Geist" else if (s.contains(
                    "-stahl"
                ) || s.contains(
                    "-steel"
                )
            ) return "Stahl" else if (s.contains("-feuer") || s.contains("-fire")) return "Feuer" else if (s.contains(
                    "-wasser"
                ) || s.contains(
                    "-water"
                )
            ) return "Wasser" else if (s.contains("-pflanze") || s.contains("-grass")) return "Pflanze" else if (s.contains(
                    "-elektro"
                ) || s.contains("-electric")
            ) return "Elektro" else if (s.contains("-psycho") || s.contains("-psychic")) return "Psycho" else if (s.contains(
                    "-eis"
                ) || s.contains("-ice")
            ) return "Eis" else if (s.contains("-drache") || s.contains("-dragon")) return "Drache" else if (s.contains(
                    "-unlicht"
                ) || s.contains("-dark")
            ) return "Unlicht" else if (s.contains("-fee") || s.contains("-fairy")) return "Fee"
            return ""
        }

        private fun getClass(str: String): String {
            val s = str.lowercase()
            if (s.contains("-phys")) return "Physical" else if (s.contains("-spez")) return "Special" else if (s.contains(
                    "-status"
                )
            ) return "Status"
            return ""
        }

        fun getAllForms(monname: String): List<Pokemon> {
            val json = dataJSON
            val mon = json[getSDName(monname)]!!
            return mon.formeOrder?.asSequence()?.map { toSDName(it) }?.distinct()?.mapNotNull { json[it] }
                ?.filterNot { it.forme?.endsWith("Totem") == true }?.toList() ?: listOf(mon)
        }

        @Suppress("UNUSED_PARAMETER")
        private fun moveFilter(msg: String, move: String): Boolean {
            /*val o = Emolga.get.movefilter
            for (s in o.keys) {
                if (msg.lowercase().contains("--$s") && move !in o[s]!!) return false
            }
            return true*/
            return true
        }

        fun getAttacksFrom(pokemon: String, msg: String, form: String, maxgen: Int): List<String> {
            val already: MutableList<String> = ArrayList()
            val type = getType(msg)
            val dmgclass = getClass(msg)
            val movejson = learnsetJSON
            val atkdata = movesJSON
            val data = dataJSON
            try {
                var str: String? = getSDName(pokemon) + if (form == "Normal") "" else form.lowercase()
                while (str != null) {
                    val learnset = movejson[str]!!()
                    val set = TranslationsDB.getEnglishIdsAndGermanNames(learnset.keys)
                    for ((moveengl, move) in set) {
                        //logger.info("moveengl = " + moveengl);
                        //logger.info("move = " + move);
                        val moveData = atkdata[moveengl]!!.jsonObject
                        if (type.isEmpty() || moveData["type"].string == getEnglName(type) && (dmgclass.isEmpty() || moveData["category"].string == dmgclass) && (!msg.lowercase()
                                .contains("--prio") || moveData["priority"].int > 0) && containsGen(
                                learnset, moveengl, maxgen
                            ) && moveFilter(
                                msg, move
                            ) && !already.contains(move)
                        ) {
                            already.add(move)
                        }
                    }
                    val mon = data[str]!!
                    str = mon.prevo?.let {
                        (if (it.endsWith("-Alola") || it.endsWith("-Galar") || it.endsWith("-Unova")) HYPHEN.replace(
                            it, ""
                        ) else it).lowercase()
                    }
                }
            } catch (ex: Exception) {
                sendToMe("Schau in die Konsole du kek!")
                ex.printStackTrace()
            }
            return already
        }

        fun sendToMe(msg: String, bot: Bot = Bot.EMOLGA) {
            sendToUser(FLOID, msg, bot)
        }


        fun sendDexEntry(msg: String) {
            emolgajda.getTextChannelById(839540004908957707L)!!.sendMessage(msg).queue()
        }

        fun sendStacktraceToMe(t: Throwable) {
            sendToMe(t.stackTraceToString())
        }

        fun sendToUser(user: User, msg: String) {
            user.openPrivateChannel().flatMap {
                it.sendMessage(msg)
            }.queue()
        }

        fun sendToUser(id: Long, msg: String, bot: Bot = Bot.EMOLGA) {
            val jda = bot.jDA.get()
            jda.retrieveUserById(id).flatMap { obj: User -> obj.openPrivateChannel() }.flatMap { pc ->
                pc.sendMessage(
                    msg.substring(0, min(msg.length, 2000))
                )
            }.queue()
        }

        private fun containsGen(learnset: Map<String, List<String>>, move: String, gen: Int): Boolean {
            for (s in learnset[move]!!) {
                for (i in 1..gen) {
                    if (s.startsWith(i.toString())) return true
                }
            }
            return false
        }


        fun getAsXCoord(xc: Int): String {
            var i = xc
            var x = 0
            while (i - 26 > 0) {
                i -= 26
                x++
            }
            return (if (x > 0) (x + 64).toChar() else "").toString() + (i + 64).toChar().toString()
        }

        fun getGen5SpriteWithoutGoogle(o: Pokemon, shiny: Boolean = false): String {
            return "https://play.pokemonshowdown.com/sprites/gen5" + (if (shiny) "-shiny" else "") + "/" + toSDName(
                o.baseSpeciesOrName
            ).notNullAppend(o.formeSuffix) + ".png"
        }

        suspend fun getSpriteForTeamGraphic(str: String, data: RandomTeamData, guild: Long): String {
            if (str == "Sen-Long") data.hasDrampa = true
            val o = getDataObject(str, guild)
            val odds = db.config.only().teamgraphicShinyOdds
            return buildString {
                append("gen5_cropped")
                if (Random.nextInt(odds) == 0) {
                    append("_shiny")
                    data.shinyCount.incrementAndGet()
                }
                append("/")
                append((o.baseSpecies ?: o.name).toSDName())
                o.forme?.let {
                    append("-${it.toSDName()}")
                }
                append(".png")
            }
        }

        fun getGen5Sprite(o: Pokemon): String {
            return buildString {
                append("=IMAGE(\"https://play.pokemonshowdown.com/sprites/gen5/")
                append(
                    (o.baseSpecies ?: o.name).toSDName().notNullAppend(o.forme?.toSDName()?.let { "-$it" })
                )
                append(".png\"; 1)")
            }
        }

        suspend fun getGen5Sprite(str: String, guildId: Long = 0): String {
            return getGen5Sprite(getDataObject(str, guildId))
        }

        suspend fun getDataObject(mon: String, guild: Long = 0): Pokemon {
            return dataJSON[NameConventionsDB.getDiscordTranslation(mon, guild, true)!!.official.toSDName()]!!
        }

        fun <T> getActionRows(c: Collection<T>, mapper: Function<T, Button>): List<ActionRow> {
            val currRow = mutableListOf<Button>()
            val rows = mutableListOf<ActionRow>()
            for (s in c) {
                currRow.add(mapper.apply(s))
                if (currRow.size == 5) {
                    rows.addAll(currRow.into())
                    currRow.clear()
                }
            }
            if (currRow.size > 0) rows.addAll(currRow.into())
            return rows
        }

        fun analyseReplay(
            url: String,
            customReplayChannel: GuildMessageChannel? = null,
            resultchannelParam: GuildMessageChannel,
            message: Message? = null,
            fromAnalyseCommand: InteractionHook? = null,
            fromReplayCommand: InteractionHook? = null,
            customGuild: Long? = null
        ) {
            //defaultScope.launch {
            if (BOT_DISABLED && resultchannelParam.guild.idLong != Constants.G.MY) {
                (message?.channel ?: resultchannelParam).sendMessage(DISABLED_TEXT).queue()
                return
            }

            logger.info("REPLAY! Channel: {}", message?.channel?.id ?: resultchannelParam.id)
            fun send(msg: String) {
                fromReplayCommand?.sendMessage(msg)?.queue() ?: fromAnalyseCommand?.sendMessage(msg)
                    ?.queue()
                ?: resultchannelParam.sendMessage(msg).queue()
            }
            if (fromReplayCommand != null && !resultchannelParam.guild.selfMember.hasPermission(
                    resultchannelParam,
                    Permission.VIEW_CHANNEL,
                    Permission.MESSAGE_SEND
                )
            ) {
                send("Ich habe keine Berechtigung, im konfigurierten Channel ${resultchannelParam.asMention} zu schreiben!")
                return
            }
            val (game, ctx) = try {
                runBlocking { Analysis.analyse(url, ::send) }
                //game = Analysis.analyse(url, m);
            } catch (ex: Exception) {
                when (ex) {
                    is ShowdownDoesNotAnswerException -> {
                        send("Showdown antwortet nicht. Versuche es später nochmal.")
                    }

                    is ShowdownParseException -> {
                        send("Das Replay konnte nicht analysiert werden! Sicher dass es ein valides Replay ist? Wenn ja, melde dich bitte auf meinem im Profil verlinkten Support-Server.")
                    }

                    else -> {
                        val msg =
                            "Beim Auswerten des Replays ist ein Fehler aufgetreten! Sehr wahrscheinlich liegt es an einem Bug in der neuen Engine, mein Programmierer wurde benachrichtigt."
                        sendToMe("Fehler beim Auswerten des Replays: $url ${resultchannelParam.guild.name} ${resultchannelParam.asMention} ChannelID: ${resultchannelParam.id}")
                        send(msg)
                        ex.printStackTrace()
                    }
                }
                return
            }
            val g = resultchannelParam.guild
            val gid = customGuild ?: g.idLong
            val u1 = game[0].nickname
            val u2 = game[1].nickname
            val uid1 = SDNamesDB.getIDByName(u1)
            val uid2 = SDNamesDB.getIDByName(u2)
            logger.info("Analysed!")
            val league = runBlocking { db.leagueByGuild(gid, uid1, uid2) }
//                if (league is ASL) {
//                    val i1 = league.table.indexOf(uid1)
//                    val i2 = league.table.indexOf(uid2)
//                    val gameday = league.battleorder.asIterable().reversed()
//                        .firstNotNullOfOrNull {
//                            if (it.value.any { l ->
//                                    l.containsAll(listOf(i1, i2))
//                                }) it.key else null
//                        }
//                        ?: -1
//                    if (gameday == 10) {
//                        message?.channel?.sendMessage("Replay ist angekommen, wird aber erst später ausgewertet!")
//                            ?.queue()
//                        return
//                    }
//                }
            val jda = resultchannelParam.jda
            val replayChannel =
                league?.provideReplayChannel(jda).takeIf { customGuild == null } ?: customReplayChannel
            val resultChannel =
                league?.provideResultChannel(jda).takeIf { customGuild == null } ?: resultchannelParam
            logger.info("uid1 = $uid1")
            logger.info("uid2 = $uid2")

            //if (gid == Constants.G.ASL && league == null) return@launch
            //logger.info(g.getName() + " -> " + (m.isFromType(ChannelType.PRIVATE) ? "PRIVATE " + m.getAuthor().getId() : m.getTextChannel().getAsMention()));
            val spoiler = spoilerTags.contains(gid)
            game.forEach {
                it.pokemon.addAll(List(it.teamSize - it.pokemon.size) { SDPokemon("_unbekannt_", -1) })
            }
            val monNames: MutableMap<String, DraftName> = mutableMapOf()
            val activePassive = runBlocking { ActivePassiveKillsDB.hasEnabled(gid) }
            val str = game.mapIndexed { index, sdPlayer ->
                mutableListOf(
                    sdPlayer.nickname, sdPlayer.pokemon.count { !it.isDead }.minus(if (ctx.vgc) 2 else 0)
                ).apply { if (spoiler) add(1, "||") }.let { if (index % 2 > 0) it.asReversed() else it }
            }.joinToString(":") { it.joinToString(" ") }
                .condAppend(ctx.vgc, "\n(VGC)") + "\n\n" + game.joinToString("\n\n") { player ->
                "${player.nickname}:".condAppend(
                    player.allMonsDead && !spoiler, " (alle tot)"
                ) + "\n".condAppend(spoiler, "||") + player.pokemon.joinToString("\n") { mon ->
                    runBlocking { getMonName(mon.pokemon, gid) }.also {
                            monNames[mon.pokemon] = it
                        }.displayName.let {
                            if (activePassive) {
                                "$it (${mon.activeKills} aktive Kills, ${mon.passiveKills} passive Kills)"
                            } else {
                                it.condAppend(mon.kills > 0, " ${mon.kills}")
                            }
                        }.condAppend((!player.allMonsDead || spoiler) && mon.isDead, " X")
                    }.condAppend(spoiler, "||")
                }
                logger.info("u1 = $u1")
                logger.info("u2 = $u2")
                if (fromAnalyseCommand != null) {
                    fromAnalyseCommand.sendMessage(str).queue()
                } else if (!customResult.contains(gid)) {
                    resultChannel.sendMessage(str).queue()
                }
                replayChannel?.sendMessage(url)?.queue()
                fromReplayCommand?.sendMessage(url)?.queue()
                if (resultchannelParam.guild.idLong != Constants.G.MY) {
                    StatisticsDB.increment("analysis")
                    game.forEach { player ->
                        player.pokemon.filterNot { "unbekannt" in it.pokemon }.forEach {
                            FullStatsDB.add(
                                monNames[it.pokemon]!!.official, it.kills, if (it.isDead) 1 else 0, player.winner
                            )
                        }
                    }
                    defaultScope.launch {
                    updatePresence()
                }
            }
            var shouldSendZoro = false
            for (ga in game) {
                if (ga.pokemon.any { "Zoroark" in it.pokemon || "Zorua" in it.pokemon }) {
                    resultchannelParam.sendMessage(
                        "Im Team von ${ga.nickname} befindet sich ein Pokemon mit Illusion! Bitte noch einmal die Kills überprüfen!"
                    ).queue()
                    shouldSendZoro = true
                }
            }
            if (shouldSendZoro) {
                jda.getTextChannelById(1016636599305515018)!!.sendMessage(url).queue()
            }
            logger.info("In Emolga Listener!")
            //if (gid != 518008523653775366L && gid != 447357526997073930L && gid != 709877545708945438L && gid != 736555250118295622L && )
            //  return;
            val kills =
                game.map { it.pokemon.associate { mon -> monNames[mon.pokemon]!!.official to mon.kills } }
            val deaths =
                game.map { it.pokemon.associate { mon -> monNames[mon.pokemon]!!.official to if (mon.isDead) 1 else 0 } }
            if (uid1 == -1L || uid2 == -1L) return
            league?.docEntry?.analyse(
                ReplayData(
                    game = game,
                    uid1 = uid1,
                    uid2 = uid2,
                    kills = kills,
                    deaths = deaths,
                    mons = game.map { it.pokemon.map { mon -> monNames[mon.pokemon]!!.official } },
                    url = url
                )
            )
            //}
        }

        fun getGerNameWithForm(name: String): String {
            var toadd = StringBuilder(name)
            val split =
                ArrayList(listOf(*toadd.toString().split("-").dropLastWhile { it.isEmpty() }.toTypedArray()))
            if (toadd.toString().contains("-Alola")) {
                toadd = StringBuilder("Alola-" + getGerNameNoCheck(split[0]))
                for (i in 2 until split.size) {
                    toadd.append("-").append(split[i])
                }
            } else if (toadd.toString().contains("-Galar")) {
                toadd = StringBuilder("Galar-" + getGerNameNoCheck(split[0]))
                for (i in 2 until split.size) {
                    toadd.append("-").append(split[i])
                }
            } else if (toadd.toString().contains("-Mega")) {
                toadd = StringBuilder("Mega-" + getGerNameNoCheck(split[0]))
                for (i in 2 until split.size) {
                    toadd.append("-").append(split[i])
                }
            } else if (split.size > 1) {
                toadd =
                    StringBuilder(getGerNameNoCheck(split.removeAt(0)) + "-" + java.lang.String.join("-", split))
            } else toadd = StringBuilder(getGerNameNoCheck(toadd.toString()))
            return toadd.toString()
        }

        fun getGerName(s: String): Translation {
            val id = toSDName(s)
            if (translationsCacheGerman.containsKey(id)) return translationsCacheGerman.getValue(id)
            return TranslationsDB.getTranslation(id, false, Language.GERMAN)?.also {
                addToCache(true, id, it)
            } ?: Translation.empty()
        }


        fun getGerNameNoCheck(s: String): String {
            return getGerName(s).translation
        }

        private fun addToCache(german: Boolean, sd: String, t: Translation) {
            if (german) {
                translationsCacheGerman[sd] = t
                translationsCacheOrderGerman.add(sd)
                if (translationsCacheOrderGerman.size > 1000) {
                    translationsCacheGerman.remove(translationsCacheOrderGerman.removeFirst())
                }
            } else {
                translationsCacheEnglish[sd] = t
                translationsCacheOrderEnglish.add(sd)
                if (translationsCacheOrderEnglish.size > 1000) {
                    translationsCacheEnglish.remove(translationsCacheOrderEnglish.removeFirst())
                }
            }
        }


        fun removeNickFromCache(sd: String) {
            translationsCacheGerman.remove(sd)
            translationsCacheEnglish.remove(sd)
            translationsCacheOrderGerman.remove(sd)
            translationsCacheOrderEnglish.remove(sd)
        }


        fun getEnglName(s: String): String {
            val str = getEnglNameWithType(s)
            return if (str.isEmpty) "" else str.translation
        }

        fun getEnglNameWithType(s: String): Translation {
            val id = toSDName(s)
            if (translationsCacheEnglish.containsKey(id)) return translationsCacheEnglish.getValue(id)
            return TranslationsDB.getTranslation(id, false, Language.ENGLISH)?.also {
                addToCache(false, id, it)
            } ?: Translation.empty()
        }

        suspend fun getTypeGerName(type: String): String =
            (Translation.Type.TYPE.validate(type, ValidationData()) as Translation).translation

        suspend fun getTypeGerNameOrNull(type: String): String? =
            (Translation.Type.TYPE.validate(type, ValidationData()) as Translation?)?.translation

        fun getSDName(str: String): String {
            //logger.info("getSDName s = $str")
            val op = sdex.keys.firstOrNull { anotherString: String -> str.equals(anotherString, ignoreCase = true) }
            val gitname: String = if (op != null) {
                val englname = getEnglName(op.split("-")[0])
                return toSDName(englname + sdex[str])
            } else {
                if (str.startsWith("M-")) {
                    val sub = str.substring(2)
                    if (str.endsWith("-X")) getEnglName(
                        sub.substring(
                            0, sub.length - 2
                        )
                    ) + "megax" else if (str.endsWith("-Y")) getEnglName(
                        sub.substring(
                            0, sub.length - 2
                        )
                    ) + "megay" else getEnglName(sub) + "mega"
                } else if (str.startsWith("A-")) {
                    getEnglName(str.substring(2)) + "alola"
                } else if (str.startsWith("G-")) {
                    getEnglName(str.substring(2)) + "galar"
                } else if (str.startsWith("Amigento-")) {
                    "silvally" + getEnglName(str.split("-").dropLastWhile { it.isEmpty() }.toTypedArray()[1])
                } else {
                    getEnglName(str)
                }
            }
            return toSDName(gitname)
        }


        fun toSDName(s: String): String {
            return SD_NAME_PATTERN.replace(s.lowercase().replace('é', 'e'), "")
        }


        fun toUsername(s: String): String {
            return USERNAME_PATTERN.replace(
                s.lowercase().trim().replace("ä", "a").replace("ö", "o").replace("ü", "u").replace("ß", "ss"), ""
            )
        }

        fun canLearn(pokemon: String, form: String, atk: String, msg: String, maxgen: Int): Boolean {
            return getAttacksFrom(pokemon, msg, form, maxgen).contains(atk)
        }

        suspend fun getMonName(s: String, guildId: Long, withDebug: Boolean = false): DraftName {
            if (withDebug) logger.info("s = $s")
            val split = s.split("-")
            val withoutLast = split.dropLast(1).joinToString("-")
            if (split.last() == "*") return getMonName(withoutLast, guildId, withDebug)
            return if (s == "_unbekannt_") DraftName("_unbekannt_", "UNKNOWN")
            else
                NameConventionsDB.getSDTranslation(
                    dataJSON[toSDName(s)]?.takeIf { it.requiredAbility != null }?.baseSpecies ?: s, guildId
                ) ?: DraftName(
                    s,
                    s
                )
            //}
        }

        fun buildEnumeration(vararg types: ArgumentType): String {
            return buildEnumeration(*types.map { it.getName() }.toTypedArray())
        }

        fun buildEnumeration(vararg types: String): String {
            val builder = StringBuilder(types.size shl 3)
            for (i in types.indices) {
                if (i > 0) {
                    if (i + 1 == types.size) builder.append(" oder ") else builder.append(", ")
                }
                builder.append(types[i])
            }
            return builder.toString()
        }

        suspend fun isChannelAllowed(tc: TextChannel): Boolean {
            val gid = tc.guild.idLong
            val c = db.emolgachannel.findOne(EmolgaChannelConfig::guild eq gid)
            return c == null || c.channels.isEmpty() || tc.idLong in c.channels
        }
    }
}

fun <T> T.indexedBy(list: List<T>) = list.indexOf(this)
val embedColor = java.awt.Color.CYAN.rgb
fun Int.x(factor: Int, summand: Int) = getAsXCoord(y(factor, summand))
fun Int.xdiv(divident: Int, factor: Int, summand: Int) = getAsXCoord(ydiv(divident, factor, summand))
fun Int.xmod(mod: Int, factor: Int, summand: Int) = getAsXCoord(ymod(mod, factor, summand))

@Suppress("unused")
fun Int.xc() = getAsXCoord(this)

fun Int.y(factor: Int, summand: Int) = this * factor + summand
fun Int.ydiv(divident: Int, factor: Int, summand: Int) = (this / divident) * factor + summand
fun Int.ymod(mod: Int, factor: Int, summand: Int) = (this % mod) * factor + summand
fun coord(sheet: String, x: String, y: Int) = "$sheet!$x$y"
fun Int.coordXMod(sheet: String, num: Int, xFactor: Int, xSummand: Int, yFactor: Int, ySummand: Int) =
    coord(sheet, this.xmod(num, xFactor, xSummand), this.ydiv(num, yFactor, ySummand))

fun Int.coordYMod(sheet: String, num: Int, xFactor: Int, xSummand: Int, yFactor: Int, ySummand: Int) =
    coord(sheet, this.xdiv(num, xFactor, xSummand), this.ymod(num, yFactor, ySummand))

fun <T> MutableList<T>.replace(toreplace: T, replacer: T) {
    this[this.indexOf(toreplace)] = replacer
}

fun <T, R> Map<T, R>.reverseGet(value: R): T? = this.entries.firstOrNull { it.value == value }?.key

fun List<DraftPokemon>?.names() = this!!.map { it.name }

fun String.toSDName() = Command.toSDName(this)

fun <T> Boolean.ifTrue(value: T) = if (this) value else null

inline fun <T> T.ifMatches(value: T, predicate: (T) -> Boolean) = if (predicate(this)) value else this

val webJSON = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = false
}

val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(webJSON)
    }
}

val defaultScope = CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineExceptionHandler { _, t ->
    t.printStackTrace()
    sendToMe("Error in default scope, look in console")
})
val myJSON = Json {
    serializersModule = SerializersModule {
        polymorphic(League::class) {
            subclass(NDS::class)
            subclass(GDL::class)
            subclass(Prisma::class)
            subclass(ASLCoach::class)
            subclass(DoR::class)
            subclass(FPL::class)
            subclass(Paldea::class)
        }
    }
    prettyPrint = true
}

val otherJSON = Json {
    isLenient = true
    ignoreUnknownKeys = true
}

val defaultTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm")

object DateToStringSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeString(defaultTimeFormat.format(value))
    }

    override fun deserialize(decoder: Decoder): Date {
        return defaultTimeFormat.parse(decoder.decodeString())
    }
}

fun String.file() = File(this)

fun Collection<String>.filterStartsWithIgnoreCase(other: String) =
    filter { it.startsWith(other, ignoreCase = true) }

val String.marker: Marker get() = MarkerFactory.getMarker(this)

fun String.condAppend(check: Boolean, value: String) = if (check) this + value else this
inline fun String.condAppend(check: Boolean, value: () -> String) = if (check) this + value() else this

fun String.notNullAppend(value: String?) = if (value != null) this + value else this

val Long.usersnowflake: UserSnowflake get() = UserSnowflake.fromId(this)

val JsonElement?.string: String get() = this!!.jsonPrimitive.content
val JsonElement?.int: Int get() = this!!.jsonPrimitive.intOrNull!!

inline val User.isFlo: Boolean get() = this.idLong == FLOID
inline val User.isNotFlo: Boolean get() = this.idLong != FLOID
inline val Interaction.fromFlo: Boolean get() = this.user.isFlo
inline val Interaction.notFromFlo: Boolean get() = !this.fromFlo

inline fun <T> Collection<T>.randomWithCondition(condition: (T) -> Boolean) = this.filter(condition).randomOrNull()

fun <K> MutableMap<K, Int>.add(key: K, value: Int) = compute(key) { _, v ->
    v?.plus(value) ?: value
}

operator fun <K, V> Map<K, V>.invoke(key: K) = getValue(key)

data class RandomTeamData(val shinyCount: AtomicInteger = AtomicInteger(), var hasDrampa: Boolean = false)

data class DocRange(val sheet: String, val xStart: String, val yStart: Int, val xEnd: String, val yEnd: Int) {
    override fun toString() = "$sheet!$xStart$yStart:$xEnd$yEnd"
    val firstHalf: String get() = "$sheet!$xStart$yStart"

    companion object {
        private val numbers = Regex("[0-9]")
        private val chars = Regex("[A-Z]")
        operator fun get(string: String): DocRange {
            val split = string.split('!')
            val range = split[1].split(':')
            return DocRange(
                split[0],
                range[0].replace(numbers, ""),
                range[0].replace(chars, "").toInt(),
                range[1].replace(numbers, ""),
                range[1].replace(chars, "").toInt()
            )
        }
    }
}

fun String.toDocRange() = DocRange[this]

data class ReplayData(
    val game: List<SDPlayer>,
    val uid1: Long,
    val uid2: Long,
    val kills: List<Map<String, Int>>,
    val deaths: List<Map<String, Int>>,
    val mons: List<List<String>>,
    val url: String
) {
    val uids by lazy { listOf(uid1, uid2) }
}

enum class Language(val translationCol: Column<String>, val otherCol: Column<String>) {
    GERMAN(TranslationsDB.GERMANNAME, TranslationsDB.ENGLISHNAME), ENGLISH(
        TranslationsDB.ENGLISHNAME,
        TranslationsDB.GERMANNAME
    )
}

class Translation(
    val translation: String,
    val type: Type,
    val language: Language,
    val otherLang: String = "",
    val forme: String? = null
) {
    val isEmpty: Boolean

    override fun toString(): String {
        val sb = StringBuilder("Translation{")
        sb.append("type=").append(type)
        sb.append(", language=").append(language)
        sb.append(", translation='").append(translation).append('\'')
        sb.append(", empty=").append(isEmpty)
        sb.append(", otherLang='").append(otherLang).append('\'')
        sb.append('}')
        return sb.toString()
    }

    fun print() {
        logger.info(
            "Translation{type=$type, language=$language, translation='$translation', empty=$isEmpty, otherLang='$otherLang'}"
        )
    }

    fun append(str: String): Translation {
        return Translation(translation + str, type, language, otherLang, forme)
    }

    fun isFromType(type: Type): Boolean {
        return this.type == type
    }

    val isSuccess: Boolean
        get() = !isEmpty

    enum class Type(val id: String, private val typeName: String, private val female: Boolean) : Command.ArgumentType {
        ABILITY("abi", "Fähigkeit", true), EGGGROUP("egg", "Eigruppe", true), ITEM(
            "item", "Item", false
        ),
        MOVE("atk", "Attacke", true), NATURE("nat", "Wesen", false), POKEMON("pkmn", "Pokémon", false), TYPE(
            "type", "Typ", false
        ),
        TRAINER("trainer", "Trainer", false), UNKNOWN("unknown", "Undefiniert", false);

        fun or(name: String): Command.ArgumentType {
            return object : Command.ArgumentType {
                override suspend fun validate(str: String, data: Command.ValidationData): Any? {
                    return if (str == name) Translation(
                        "Tom", TRAINER, Language.GERMAN, "Tom"
                    ) else this@Type.validate(str, data)
                }

                override fun getName(): String {
                    return this@Type.getName()
                }

                override val customHelp: String?
                    get() = this@Type.customHelp

                override fun asOptionType(): OptionType {
                    return this@Type.asOptionType()
                }

                override fun needsValidate(): Boolean {
                    return this@Type.needsValidate()
                }
            }
        }

        override fun needsValidate(): Boolean {
            return true
        }

        override suspend fun validate(str: String, data: Command.ValidationData): Any? {
            val t = if (data.language == Language.GERMAN) Command.getGerName(
                str
            ) else Command.getEnglNameWithType(str)
            if (t.isEmpty) return null
            if (t.translation == "Psychic" || t.otherLang == "Psychic") {
                if (this == TYPE) {
                    return Translation(
                        if (t.language == Language.GERMAN) "Psycho" else "Psychic",
                        TYPE,
                        t.language
                    )
                }
            }
            return if (t.type != this) null else t
        }

        override fun getName(): String {
            return "ein" + (if (female) "e" else "") + " **" + typeName + "**"
        }

        override fun asOptionType(): OptionType {
            return OptionType.STRING
        }

        companion object {

            fun fromId(id: String): Type {
                return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: UNKNOWN
            }

            fun of(vararg types: Type): Command.ArgumentType {
                return object : Command.ArgumentType {
                    override suspend fun validate(str: String, data: Command.ValidationData): Any? {
                        val t =
                            if (data.language === Language.GERMAN) Command.getGerName(str) else Command.getEnglNameWithType(
                                str
                            )
                        if (t.isEmpty) return null
                        return if (!listOf(*types).contains(t.type)) null else t
                    }

                    override fun getName(): String {
                        //return "ein **Pokémon**, eine **Attacke**, ein **Item** oder eine **Fähigkeit**";
                        return Command.buildEnumeration(*types)
                    }

                    override fun asOptionType(): OptionType {
                        return OptionType.STRING
                    }

                    override fun needsValidate(): Boolean {
                        return true
                    }
                }
            }

            fun all(): Command.ArgumentType {
                return of(*entries.filter { t: Type -> t != UNKNOWN }.toTypedArray())
            }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
        private val emptyTranslation: Translation = Translation("", Type.UNKNOWN, Language.GERMAN)

        fun empty(): Translation {
            return emptyTranslation
        }
    }

    init {
        this.isEmpty = type == Type.UNKNOWN
    }
}
