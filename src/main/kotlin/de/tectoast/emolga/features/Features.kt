@file:Suppress("UNCHECKED_CAST")

package de.tectoast.emolga.features

import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.draft.isEnglish
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import dev.minn.jda.ktx.interactions.components.Modal
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import dev.minn.jda.ktx.interactions.components.button
import mu.KotlinLogging
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInput
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSubclassOf

sealed class Feature<out T : FeatureSpec, out E : GenericInteractionCreateEvent, A : Arguments>(
    val argsFun: () -> @UnsafeVariance A,
    val spec: T,
    val eventClass: KClass<@UnsafeVariance E>,
    val eventToName: (@UnsafeVariance E) -> String
) : ListenerProvider() {
    var checkSpecial: AllowedResultCheck = { Allowed }
        private set
    var check: BooleanCheck = { true }
        private set

    val defaultArgs by lazy { argsFun().args }
    abstract suspend fun populateArgs(data: InteractionData, e: @UnsafeVariance E, args: A)

    fun createComponentId(argsBuilder: ArgBuilder<@UnsafeVariance A>, checkCompId: Boolean = false) =
        spec.name + ";" + argsFun().apply(argsBuilder).args.filter { !checkCompId || it.compIdOnly }
            .joinToString(";") { it.parsed?.toString() ?: "" }

    fun buildArgs(argsBuilder: ArgBuilder<A>) = argsFun().apply(argsBuilder)

    protected suspend inline fun populateArgs(
        data: InteractionData, args: List<Arg<*, *>>, parser: (String, Int) -> Any?
    ) {
        for ((index, arg) in args.withIndex()) {
            val m = parser(arg.name.nameToDiscordOption(), index)
            if (m == null && !arg.optional) {
                throw MissingArgumentException(arg)
            }
            if (m != null) arg.parse(data, m)
        }
    }

    suspend fun permissionCheck(data: InteractionData) = if (data.user == Constants.FLOID) Allowed else {
        if (!check(data)) NotAllowed else checkSpecial(data)
    }

    fun restrict(check: BooleanCheck) {
        this.check = check
    }

    fun restrictResult(check: AllowedResultCheck) {
        this.checkSpecial = check
    }

    fun user(uid: Long): BooleanCheck = {
        user == uid
    }

    fun members(vararg members: Long): BooleanCheck = {
        user in members
    }

    fun roles(vararg roles: Long): BooleanCheck = {
        member().roles.any { it.idLong in roles }
    }

    val admin: BooleanCheck = {
        member().hasPermission(Permission.ADMINISTRATOR)
    }
    val flo: BooleanCheck = { false } // flo may use any feature regardless of this configuration
    val henny = user(Constants.M.HENNY)

    context (InteractionData)
    abstract suspend fun exec(e: A)

    context(InteractionData)
    suspend fun exec(argsBuilder: ArgBuilder<@UnsafeVariance A> = {}) = exec(buildArgs(argsBuilder))

    companion object {
        val draftGuilds = longArrayOf(
            Constants.G.FPL,
            Constants.G.NDS,
            Constants.G.ASL,
            Constants.G.BLOCKI,
            Constants.G.VIP,
            Constants.G.FLP,
            Constants.G.WARRIOR,
            Constants.G.BSP,
            Constants.G.PIKAS,
            Constants.G.WFS,
            Constants.G.ADK,
            Constants.G.COMMUNITY,
            Constants.G.HELBIN
        )
    }
}
typealias ArgBuilder<A> = (@UnsafeVariance A).() -> Unit
typealias BooleanCheck = suspend InteractionData.() -> Boolean
typealias AllowedResultCheck = suspend InteractionData.() -> AllowedResult

sealed class AllowedResult {
    companion object {
        operator fun invoke(b: Boolean) = if (b) Allowed else NotAllowed
    }
}

data object Allowed : AllowedResult()
open class NotAllowed(val reason: String) : AllowedResult() {
    companion object : NotAllowed("Du bist nicht berechtigt, diese Interaktion auszuführen!")
}

/**
 * Marker for classes which are part of the feature system (at the moment Feature & ListenerProvider)
 */
abstract class ListenerProvider {
    val registeredListeners: MutableSet<Pair<KClass<out GenericEvent>, suspend (GenericEvent) -> Unit>> = mutableSetOf()
    inline fun <reified T : GenericEvent> ListenerProvider.registerListener(noinline listener: suspend (T) -> Unit) {
        registeredListeners += (T::class to listener) as Pair<KClass<out GenericEvent>, suspend (GenericEvent) -> Unit>
    }

    fun registerPNListener(prefix: String = "", listener: suspend (MessageReceivedEvent) -> Unit) =
        registerListener<MessageReceivedEvent> {
            if (!it.author.isBot && it.channelType == ChannelType.PRIVATE && it.message.contentRaw.startsWith(prefix)) listener(
                it
            )
        }
}


/**
 * Marker for classes which provide listeners (and are not a feature itself)
 */


abstract class CommandFeature<A : Arguments>(argsFun: () -> A, spec: CommandSpec) :
    Feature<CommandSpec, SlashCommandInteractionEvent, A>(
        argsFun, spec, SlashCommandInteractionEvent::class, eventToName
    ) {
    private val autoCompletableOptions by lazy {
        defaultArgs.mapNotNull { (it.spec as? CommandArgSpec)?.autocomplete?.let { ac -> it.name.nameToDiscordOption() to ac } }
            .toMap()
    }
    val children = (this::class.nestedClasses as Collection<KClass<out CommandFeature<*>>>).filter {
        it.isSubclassOf(CommandFeature::class)
    }.map { it.objectInstance!! }
    val childCommands = children.associateBy { it.spec.name }
    var slashPermissions: DefaultMemberPermissions = DefaultMemberPermissions.ENABLED

    init {
        registerListener<CommandAutoCompleteInteractionEvent> {
            if (it.name != spec.name && it.name != it.subcommandName) return@registerListener
            permissionCheck(RealInteractionData(it)).let { result ->
                if (result is NotAllowed) {
                    return@registerListener it.replyChoiceStrings(result.reason).queue()
                }
            }
            val focusedOption = it.focusedOption
            val options = childCommands[it.subcommandName]?.autoCompletableOptions ?: autoCompletableOptions
            options[focusedOption.name]?.let { ac ->
                val list = ac(focusedOption.value, it)?.takeIf { l -> l.size <= 25 }
                it.replyChoiceStrings(list ?: listOf("Zu viele Ergebnisse, bitte spezifiziere deine Suche!")).queue()
            }
        }
    }

    override suspend fun populateArgs(data: InteractionData, e: SlashCommandInteractionEvent, args: A) {
        populateArgs(data, args.args) { str, _ ->
            e.getOption(str)
        }
    }

    protected fun slashPrivate() {
        slashPermissions = DefaultMemberPermissions.DISABLED
    }

    companion object {
        val eventToName: (SlashCommandInteractionEvent) -> String = { it.name }
    }
}

abstract class ButtonFeature<A : Arguments>(argsFun: () -> A, spec: ButtonSpec) :
    Feature<ButtonSpec, ButtonInteractionEvent, A>(argsFun, spec, ButtonInteractionEvent::class, eventToName) {
    open val buttonStyle = ButtonStyle.PRIMARY
    open val label = spec.name
    open val emoji: Emoji? = null
    override suspend fun populateArgs(data: InteractionData, e: ButtonInteractionEvent, args: A) {
        val argsFromEvent = e.componentId.substringAfter(";").split(";")
        for ((index, arg) in args.args.withIndex()) {
            val m = argsFromEvent.getOrNull(index)?.takeIf { it.isNotBlank() }
            arg.parse(data, m.orEmpty())
        }
    }

    operator fun invoke(
        label: String = this.label,
        buttonStyle: ButtonStyle = this.buttonStyle,
        emoji: Emoji? = this.emoji,
        disabled: Boolean = false,
        argsBuilder: ArgBuilder<A> = {}
    ) = button(createComponentId(argsBuilder), label, style = buttonStyle, emoji = emoji, disabled = disabled)


    companion object {
        val eventToName: (ButtonInteractionEvent) -> String = { it.componentId.substringBefore(";") }
    }
}

abstract class ModalFeature<A : Arguments>(argsFun: () -> A, spec: ModalSpec) :
    Feature<ModalSpec, ModalInteractionEvent, A>(
        argsFun, spec, ModalInteractionEvent::class, eventToName
    ) {

    open val title: String
        get() = throw NotImplementedError("Title not implemented for modal ${this.spec.name}")

    override suspend fun populateArgs(data: InteractionData, e: ModalInteractionEvent, args: A) {
        val (compId, regular) = args.args.partition { it.compIdOnly }
        val argsFromEvent = e.modalId.substringAfter(";").split(";")
        populateArgs(data, compId) { _, index ->
            argsFromEvent.getOrNull(index)?.takeIf { it.isNotBlank() }
        }
        populateArgs(data, regular) { name, _ ->
            e.getValue(name)?.asString?.takeIf { it.isNotBlank() }
        }
    }

    operator fun invoke(
        title: String = this.title,
        specificallyEnabledArgs: Map<ModalKey, Boolean> = emptyMap(),
        argsBuilder: ArgBuilder<A> = {}
    ) = Modal(createComponentId(argsBuilder, checkCompId = true), title) {
        argsFun().apply(argsBuilder).args.forEach { arg ->
            if (arg.compIdOnly) return@forEach
            val spec = arg.spec as? ModalArgSpec
            spec?.modalEnableKey?.let { key ->
                if (specificallyEnabledArgs[key] != true) return@forEach
            }
            val argName = arg.name
            val argId = argName.nameToDiscordOption()
            val required = spec?.required == true || !arg.optional
            val value = arg.parsed?.toString()
            val argBuilder = spec?.builder ?: {}
            if (spec?.short != false) {
                short(
                    argId, argName, required = required, value = value, builder = argBuilder
                )
            } else {
                paragraph(
                    argId, argName, required = required, value = value, builder = argBuilder
                )
            }
        }
    }

    companion object {
        val eventToName: (ModalInteractionEvent) -> String = { it.modalId.substringBefore(";") }
    }
}

interface ModalKey

abstract class SelectMenuFeature<A : Arguments>(argsFun: () -> A, spec: SelectMenuSpec) :
    Feature<SelectMenuSpec, StringSelectInteractionEvent, A>(
        argsFun, spec, StringSelectInteractionEvent::class, eventToName
    ) {
    open val options: List<SelectOption>? = null
    private val selectableOptions by lazy {
        (argsFun().args.single { !it.compIdOnly }.spec as? SelectMenuArgSpec)?.selectableOptions ?: 1..1
    }
    private val isSingle by lazy { selectableOptions.let { it.first == it.last && it.first == 1 } }
    override suspend fun populateArgs(data: InteractionData, e: StringSelectInteractionEvent, args: A) {
        val (compId, regular) = args.args.partition { it.compIdOnly }
        val argsFromEvent = e.componentId.substringAfter(";").split(";")
        populateArgs(data, compId) { _, index ->
            argsFromEvent.getOrNull(index)?.takeIf { it.isNotBlank() }
        }
        val selectArg = regular.single()
        selectArg.parse(data, if (isSingle) e.values.first() else e.values)
    }

    operator fun invoke(
        placeholder: String? = null,
        options: List<SelectOption>? = this.options,
        disabled: Boolean = false,
        menuBuilder: StringSelectMenu.Builder.() -> Unit = {},
        argsBuilder: ArgBuilder<A> = {},
    ) = StringSelectMenu(
        createComponentId(argsBuilder, checkCompId = true),
        placeholder,
        disabled = disabled,
        valueRange = selectableOptions,
        options = options.orEmpty(),
        builder = menuBuilder
    )

    companion object {
        val eventToName: (StringSelectInteractionEvent) -> String = { it.componentId.substringBefore(";") }
    }
}

abstract class MessageContextFeature(spec: MessageContextSpec) :
    Feature<MessageContextSpec, MessageContextInteractionEvent, MessageContextArgs>(
        ::MessageContextArgs,
        spec,
        MessageContextInteractionEvent::class,
        eventToName
    ) {
    override suspend fun populateArgs(
        data: InteractionData,
        e: MessageContextInteractionEvent,
        args: MessageContextArgs
    ) {
        args.message = e.target
    }

    companion object {
        val eventToName: (MessageContextInteractionEvent) -> String = { it.interaction.name }
    }
}

sealed class FeatureSpec(open val name: String)
sealed class GuildedFeatureSpec(name: String, vararg val guilds: Long) : FeatureSpec(name)
class CommandSpec(name: String, val help: String, vararg guilds: Long) : GuildedFeatureSpec(name, *guilds)
class ButtonSpec(name: String) : FeatureSpec(name)
class ModalSpec(name: String) : FeatureSpec(name)
class SelectMenuSpec(name: String) : FeatureSpec(name)
class MessageContextSpec(name: String, vararg guilds: Long) : GuildedFeatureSpec(name, *guilds)

sealed interface ArgSpec
data class CommandArgSpec(
    val autocomplete: (suspend (String, CommandAutoCompleteInteractionEvent) -> List<String>?)? = null,
    val choices: List<Choice>? = null,
    val guildChecker: GuildChecker? = null
) : ArgSpec

data class ModalArgSpec(
    val short: Boolean, val modalEnableKey: ModalKey?, val required: Boolean, val builder: TextInput.Builder.() -> Unit
) : ArgSpec

data class SelectMenuArgSpec(val selectableOptions: IntRange) : ArgSpec
open class Arguments {
    private val _args = mutableListOf<Arg<*, *>>()
    val args: List<Arg<*, *>> = Collections.unmodifiableList(_args)
    inline fun string(name: String = "", help: String = "", builder: Arg<String, String>.() -> Unit = {}) =
        createArg(name, help, OptionType.STRING, builder)

    @JvmName("stringGeneric")
    inline fun <T> string(name: String = "", help: String = "", builder: Arg<String, T>.() -> Unit = {}) =
        createArg(name, help, OptionType.STRING, builder)

    inline fun long(name: String = "", help: String = "", builder: Arg<String, Long>.() -> Unit = {}) =
        createArg<String, Long>(name, help, OptionType.STRING) {
            validate { it.toLongOrNull() }
            builder()
        }

    inline fun int(name: String = "", help: String = "", builder: Arg<Long, Int>.() -> Unit = {}) =
        createArg<Long, Int>(name, help, OptionType.INTEGER) {
            validate { it.toInt() }
            builder()
        }

    fun draftPokemon(
        name: String = "",
        help: String = "",
        builder: Arg<String, DraftName>.() -> Unit = {},
        autocomplete: (suspend (String, CommandAutoCompleteInteractionEvent) -> List<String>?)? = null
    ) = createArg(name, help, OptionType.STRING) {
        validate {
            val guildId = League.onlyChannel(tc)?.guild ?: gid
            NameConventionsDB.getDiscordTranslation(
                it, guildId, english = Tierlist[guildId].isEnglish
            ) ?: throw InvalidArgumentException("Pokemon `$it` nicht gefunden!")
        }
        slashCommand(autocomplete = autocomplete ?: lambda@{ s, event ->
            val gid = event.guild!!.idLong
            val league = db.leagueForAutocomplete(event.channel.idLong, gid, event.user.idLong)
            val tierlist = Tierlist[league?.guild ?: gid]
            val strings =
                (tierlist?.autoComplete() ?: NameConventionsDB.allNameConventions()).filterContainsIgnoreCase(s)
            if (strings.size > 25) return@lambda listOf("Zu viele Ergebnisse, bitte spezifiziere deine Suche!")
            (if (league == null || tierlist == null) strings
            else strings.map {
                if (league.picks.values.flatten().any { p ->
                        p.name == tierlist.tlToOfficialCache.getOrPut(it) {
                            NameConventionsDB.getDiscordTranslation(it, league.guild)!!.official
                        }
                    }) "$it (GEPICKT)" else it
            }).sortedWith(compareBy({ !it.startsWith(s) }, { it }))
        })
        builder()
    }

    inline fun boolean(name: String = "", help: String = "", builder: Arg<Boolean, Boolean>.() -> Unit = {}) =
        createArg(name, help, OptionType.BOOLEAN, builder)

    inline fun <reified T : Enum<T>> enumBasic(
        name: String = "", help: String = "", builder: Arg<String, T>.() -> Unit = {}
    ) = createArg(name, help, OptionType.STRING) {
        val enumValues = enumValues<T>()
        validate {
            try {
                enumValueOf<T>(it)
            } catch (e: IllegalArgumentException) {
                throw InvalidArgumentException(
                    "Ungültiger Wert für $name! Mögliche Werte: ${enumValues.joinToString(", ")}"
                )
            }
        }
        if (enumValues.size <= 25) slashCommand(enumValues.map { Choice(it.name, it.name) })
        else slashCommand { s, _ ->
            val nameMatching = enumValues.toSet().filterStartsWithIgnoreCase(s) { it.name }
            nameMatching.convertListToAutoCompleteReply()
        }
        builder()
    }

    inline fun fromList(
        name: String = "",
        help: String = "",
        collection: Collection<String>,
        builder: Arg<String, String>.() -> Unit = {}
    ) = createArg<String, String>(name, help) {
        validate { s ->
            s.takeIf { it in collection }
                ?: throw InvalidArgumentException("Keine valide Angabe! Halte dich an die Autovervollständigung!")
        }
        if (collection.size <= 25) slashCommand(collection.map { Choice(it, it) })
        else slashCommand { s, _ ->
            collection.filterStartsWithIgnoreCase(s).convertListToAutoCompleteReply()
        }
        builder()
    }

    inline fun fromList(
        name: String = "",
        help: String = "",
        crossinline collSupplier: suspend (CommandAutoCompleteInteractionEvent) -> Collection<String>,
        builder: Arg<String, String>.() -> Unit = {}
    ) = createArg(name, help) {
        slashCommand { s, event ->
            collSupplier(event).filterStartsWithIgnoreCase(s).convertListToAutoCompleteReply()
        }
        builder()
    }

    inline fun <reified T> enumAdvanced(
        name: String = "", help: String = "", builder: Arg<String, T>.() -> Unit = {}
    ) where T : Enum<T>, T : Nameable = createArg(name, help, OptionType.STRING) {
        validate {
            try {
                enumValueOf<T>(it)
            } catch (e: IllegalArgumentException) {
                throw InvalidArgumentException(
                    "Ungültiger Wert für $name! Mögliche Werte: ${enumValues<T>().joinToString(", ")}"
                )
            }
        }
        slashCommand(enumValues<T>().map { Choice(it.prettyName, it.name) })
        builder()
    }

    fun pokemontype(name: String = "", help: String = "", english: Boolean) = createArg(name, help, OptionType.STRING) {
        validate { str ->
            val t = if (english) Translation.getEnglNameWithType(str)
            else Translation.getGerName(str)
            if (t.isEmpty || t.type != Translation.Type.TYPE) throw InvalidArgumentException("Ungültiger Typ!")
            if (t.translation == "Psychic" || t.otherLang == "Psychic") {
                if (english) "Psychic" else "Psycho"
            } else t.translation
        }
    }

    fun pokemontype(name: String = "", help: String = "") = createArg(name, help, OptionType.STRING) {
        validate { str ->
            val t = Translation.getGerName(str)
            if (t.isEmpty || t.type != Translation.Type.TYPE) throw InvalidArgumentException("Ungültiger Typ!")
            if (t.translation == "Psychic" || t.otherLang == "Psychic") {
                "Psycho" to "Psychic"
            } else t.translation to t.otherLang
        }
    }

    fun league(name: String = "", help: String = "") = createArg(name, help, OptionType.STRING) {
        validate {
            db.getLeague(it) ?: throw InvalidArgumentException("Ungültige Liga!")
        }
    }

    fun list(name: String = "", help: String = "", numOfArgs: Int, requiredNum: Int, startAt: Int = 1) =
        object : ReadWriteProperty<Arguments, List<String>> {
            private val argList: List<Arg<String, out String?>> = List(numOfArgs) { i ->
                createArg<String, String>(name.embedI(i), help.embedI(i), OptionType.STRING) {}.run {
                    if (i >= requiredNum) nullable() else this
                }
            }

            private fun String.embedI(i: Int) = if ("%s" in this) format(i + startAt) else plus(i + startAt)
            private var parsed: List<String>? = null

            override fun getValue(thisRef: Arguments, property: KProperty<*>): List<String> {
                if (parsed == null) {
                    parsed = argList.mapNotNull { it.parsed }
                }
                return parsed!!
            }

            override fun setValue(thisRef: Arguments, property: KProperty<*>, value: List<String>) {
                parsed = value
            }
        }

    inline fun member(name: String = "", help: String = "", builder: Arg<Member, Member>.() -> Unit = {}) =
        createArg(name, help, OptionType.USER, builder)

    inline fun channel(
        name: String = "", help: String = "", builder: Arg<GuildChannelUnion, GuildChannelUnion>.() -> Unit = {}
    ) = createArg(name, help, OptionType.CHANNEL, builder)

    inline fun messageChannel(
        name: String = "", help: String = "", builder: Arg<GuildChannelUnion, GuildMessageChannelUnion>.() -> Unit = {}
    ) = createArg(name, help, OptionType.CHANNEL, builder)

    inline fun attachment(
        name: String = "", help: String = "", builder: Arg<Message.Attachment, Message.Attachment>.() -> Unit = {}
    ) = createArg(name, help, OptionType.ATTACHMENT, builder)

    fun singleOption() = createArg<String, String>("", "", OptionType.STRING) {
        spec = SelectMenuArgSpec(1..1)
    }

    fun <T> singleOption(validator: suspend InteractionData.(String) -> T) = createArg("", "") {
        validate(validator)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun multiOption(range: IntRange) = createArg<List<String>, List<String>>("", "", OptionType.STRING) {
        spec = SelectMenuArgSpec(range)
    }

    inline fun <DiscordType, ParsedType> createArg(
        name: String = "",
        help: String = "",
        optionType: OptionType = OptionType.STRING,
        builder: Arg<DiscordType, ParsedType>.() -> Unit
    ) = Arg<DiscordType, ParsedType>(name, help, optionType, this).also {
        it.builder()
        addArg(it)
    }

    fun addArg(arg: Arg<*, *>) {
        _args += arg
    }

    fun replaceLastArg(arg: Arg<*, *>) {
        _args.removeLast()
        _args += arg
    }

    companion object {
        val logger = KotlinLogging.logger {}
        val tlNameCache = SizeLimitedMap<String, String>(1000)

        // Helpers
        suspend inline fun monOfTeam(s: String, league: League, user: Long): List<String>? {
            return newSuspendedTransaction {
                val tl = league.tierlist
                val picks = league.picks[user] ?: return@newSuspendedTransaction null
                picks.filter { p -> p.name != "???" && !p.quit }.sortedWith(
                    compareBy({ mon -> tl.order.indexOf(mon.tier) },
                        { mon -> mon.name })
                ).map { mon ->
                    logger.debug(mon.name)
                    tlNameCache[mon.name] ?: NameConventionsDB.convertOfficialToTL(
                        mon.name, league.guild
                    )!!.also { tlName -> tlNameCache[mon.name] = tlName }
                }.filter { mon -> mon.startsWith(s, true) }

            }
        }

    }
}
/**
 * Result:
 * - null -> argument should not be present
 * - true -> argument is present and required
 * - false -> argument is present and optional

 */
typealias GuildChecker = suspend CommandProviderData.() -> Boolean?

class CommandProviderData(val gid: Long) {
    val league = OneTimeCache { db.leagueByGuild(gid) }
}

interface Nameable {
    val prettyName: String
}

object NoArgs : Arguments() {
    private val argsFun = { this }
    operator fun invoke() = argsFun
}

class MessageContextArgs : Arguments() {
    var message by createArg<Message, Message> { }
}

class Arg<DiscordType, ParsedType>(
    val name: String, val help: String, val optionType: OptionType, internal val args: Arguments
) : ReadWriteProperty<Arguments, ParsedType> {
    var parsed: ParsedType? = null
        private set
    private var success = false
    var default: ParsedType? = null
        set(value) {
            field = value
            defaultValueSet = true
        }
    private var defaultValueSet = false
    private var defaultFunction: (() -> ParsedType)? = null
    private var validator: (suspend InteractionData.(DiscordType) -> ParsedType?) = { it as ParsedType }
    private var nullable = false
    var spec: ArgSpec? = null
    var compIdOnly = false
    var onlyInCode = false
    var customErrorMessage: String? = null
    val optional get() = defaultValueSet || defaultFunction != null || nullable

    fun default(defaultfun: () -> ParsedType) {
        defaultFunction = defaultfun
    }

    fun validate(validator: suspend InteractionData.(DiscordType) -> ParsedType?) {
        this.validator = validator
    }

    fun slashCommand(
        choices: List<Choice>? = null,
        guildChecker: GuildChecker? = null,
        autocomplete: (suspend (String, CommandAutoCompleteInteractionEvent) -> List<String>?)? = null
    ) {
        spec = CommandArgSpec(autocomplete, choices, guildChecker)
    }

    fun modal(
        short: Boolean = true,
        modalKey: ModalKey? = null,
        required: Boolean = false,
        builder: TextInput.Builder.() -> Unit = {}
    ) {
        spec = ModalArgSpec(short, modalKey, required, builder)
    }

    override fun getValue(thisRef: Arguments, property: KProperty<*>): ParsedType {
        if (success) return parsed as ParsedType
        if (defaultValueSet) return default as ParsedType
        defaultFunction?.let { return it() }
        throw IllegalStateException("No value set for $property")
    }

    override fun setValue(thisRef: Arguments, property: KProperty<*>, value: ParsedType) {
        parsed = value
        success = true
    }

    /**
     * Parses the given data and sets the value of this argument
     * @param data The general data of this interaction
     * @param m The value of this argument, MUST ONLY BE OptionMapping or String
     */
    suspend fun parse(data: InteractionData, m: Any) {
        val validatorResult = data.validator(
            when (m) {
                is OptionMapping -> {
                    when (m.type) {
                        OptionType.STRING -> m.asString
                        OptionType.INTEGER -> m.asLong
                        OptionType.BOOLEAN -> m.asBoolean
                        OptionType.USER -> m.asMember ?: m.asUser
                        OptionType.CHANNEL -> m.asChannel
                        OptionType.ROLE -> m.asRole
                        OptionType.NUMBER -> m.asDouble
                        OptionType.ATTACHMENT -> m.asAttachment
                        else -> throw IllegalArgumentException("Unknown OptionType ${m.type}")
                    }
                }

                is String -> {
                    when (optionType) {
                        OptionType.STRING -> m
                        OptionType.INTEGER -> m.toLong()
                        OptionType.BOOLEAN -> m.toBoolean()
                        OptionType.NUMBER -> m.toDouble()
                        else -> throw IllegalArgumentException("Unsupported option type for string input $optionType")
                    }
                }

                else -> {
                    (m as? DiscordType) ?: throw IllegalArgumentException("Unknown type ${m::class.simpleName}")
                }
            } as DiscordType
        )

        parsed = validatorResult ?: throw InvalidArgumentException(
            customErrorMessage ?: "Das Argument `$name` konnte nicht erkannt werden!"
        )
        success = true
    }

    fun compIdOnly(): Arg<DiscordType, ParsedType> {
        compIdOnly = true
        return this
    }

    fun nullable(): Arg<DiscordType, ParsedType?> {
        return Arg<DiscordType, ParsedType?>(name, help, optionType, args).also {
            copyTo(it)
            it.nullable = true
            it.defaultValueSet = true
            args.replaceLastArg(it)
        }
    }

    fun defaultNotEnabled(key: ModalKey, required: Boolean? = null): Arg<DiscordType, ParsedType?> {
        return Arg<DiscordType, ParsedType?>(name, help, optionType, args).also {
            copyTo(it)
            val oldSpec = it.spec as? ModalArgSpec
            it.spec = ModalArgSpec(
                oldSpec?.short != false,
                key,
                (required ?: oldSpec?.required) == true,
                oldSpec?.builder ?: {})
            it.defaultValueSet = true
            args.replaceLastArg(it)
        }
    }

    private fun copyTo(arg: Arg<DiscordType, ParsedType?>) {
        val oldDefaultValueSet = defaultValueSet
        arg.default = default
        arg.defaultValueSet = oldDefaultValueSet
        arg.defaultFunction = defaultFunction
        arg.validator = validator
        arg.spec = spec
        arg.nullable = nullable
        arg.compIdOnly = compIdOnly
    }
}

private val nameToDiscordRegex = Regex("[^\\w-]")
fun String.nameToDiscordOption(): String {
    return lowercase().replace(" ", "-").replace(nameToDiscordRegex, "")
}

fun List<Button>.intoMultipleRows() = chunked(5).map { ActionRow.of(it) }

fun List<String>?.convertListToAutoCompleteReply() = when (this?.size) {
    0, null -> listOf("Keine Ergebnisse!")
    in 1..25 -> this
    else -> listOf("Zu viele Ergebnisse, bitte spezifiziere deine Suche!")
}

val GenericInteractionCreateEvent.button: ButtonInteractionEvent get() = this as ButtonInteractionEvent

open class ArgumentException(override val message: String) : Exception(message)
class MissingArgumentException(arg: Arg<*, *>) : ArgumentException("Du musst den Parameter `${arg.name}` angeben!")

class InvalidArgumentException(override val message: String) : ArgumentException(message)
