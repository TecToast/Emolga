@file:Suppress("UNCHECKED_CAST")

package de.tectoast.emolga.features

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.commands.Translation
import de.tectoast.emolga.commands.filterStartsWithIgnoreCase
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.draft.isEnglish
import de.tectoast.emolga.utils.json.emolga.draft.League
import dev.minn.jda.ktx.interactions.components.Modal
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import dev.minn.jda.ktx.interactions.components.button
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInput
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf

sealed class Feature<out T : FeatureSpec, out E : GenericInteractionCreateEvent, in A : Arguments>(
    val argsFun: () -> @UnsafeVariance A,
    val spec: T,
    val eventClass: KClass<@UnsafeVariance E>,
    val eventToName: (@UnsafeVariance E) -> String
) {
    val registeredListeners = mutableSetOf<Pair<KClass<out GenericEvent>, suspend (GenericEvent) -> Unit>>()
    open val checkSpecial: suspend InteractionData.() -> AllowedResult = { Allowed }
    open val check: suspend InteractionData.() -> Boolean = { true }
    abstract suspend fun populateArgs(data: InteractionData, e: @UnsafeVariance E, args: A)

    fun createComponentId(argsBuilder: ArgBuilder<@UnsafeVariance A>, checkCompId: Boolean = false) =
        spec.name + ";" + argsFun().apply(argsBuilder).args.filter { !checkCompId || it.compIdOnly }
            .joinToString(";") { it.parsed?.toString() ?: "" }

    fun buildArgs(argsBuilder: ArgBuilder<@UnsafeVariance A>) = argsFun().apply(argsBuilder)

    inline fun <reified T : GenericEvent> registerListener(noinline listener: suspend (T) -> Unit) {
        registeredListeners += (T::class to listener) as Pair<KClass<out GenericEvent>, suspend (GenericEvent) -> Unit>
    }

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

    fun members(vararg members: Long): suspend InteractionData.() -> Boolean = {
        user in members
    }

    fun roles(vararg roles: Long): suspend InteractionData.() -> Boolean = {
        member().roles.any { it.idLong in roles }
    }

    val admin: suspend InteractionData.() -> Boolean = {
        member().hasPermission(Permission.ADMINISTRATOR)
    }

    context (InteractionData)
    abstract suspend fun exec(e: A)

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
            Constants.G.COMMUNITY
        )
    }
}
typealias ArgBuilder<A> = A.() -> Unit

sealed class AllowedResult {
    companion object {
        operator fun invoke(b: Boolean) = if (b) Allowed else NotAllowed
    }
}

data object Allowed : AllowedResult()
open class NotAllowed(val reason: String) : AllowedResult() {
    companion object : NotAllowed("Du bist nicht berechtigt, diesen Command auszuführen!")
}

abstract class CommandFeature<A : Arguments>(argsFun: () -> A, spec: CommandSpec) :
    Feature<CommandSpec, SlashCommandInteractionEvent, A>(
        argsFun, spec, SlashCommandInteractionEvent::class, eventToName
    ) {
    val defaultArgs by lazy { argsFun().args }
    private val autoCompleatableOptions by lazy {
        defaultArgs.mapNotNull { (it.spec as? CommandArgSpec)?.autocomplete?.let { ac -> it.name.nameToDiscordOption() to ac } }
            .toMap()
    }
    val children: Collection<CommandFeature<*>>
    val childCommands: Map<String, CommandFeature<*>>
    open val slashPermissions: DefaultMemberPermissions = DefaultMemberPermissions.ENABLED

    init {
        registerListener<CommandAutoCompleteInteractionEvent> {
            val focusedOption = it.focusedOption
            autoCompleatableOptions[focusedOption.name]?.let { ac ->
                val list = ac(focusedOption.value, it)?.takeIf { l -> l.size <= 25 }
                it.replyChoiceStrings(list ?: listOf("Zu viele Ergebnisse, bitte spezifiziere deine Suche!")).queue()
            }
        }
        children = (this::class.nestedClasses as Collection<KClass<out CommandFeature<*>>>).filter {
            it.isSubclassOf(CommandFeature::class)
        }.map { it.objectInstance!! }
        childCommands = children.associateBy { it.spec.name }
    }

    override suspend fun populateArgs(data: InteractionData, e: SlashCommandInteractionEvent, args: A) {
        populateArgs(data, args.args) { str, _ ->
            e.getOption(str)
        }
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
            if (m == null && !arg.optional) {
                throw IllegalArgumentException("Missing parameter in Button! ${arg.name} -> ${e.componentId}")
            }
            if (m != null) arg.parse(data, m)
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
        title: String? = null,
        specificallyEnabledArgs: Map<KProperty1<out Arguments, *>, Boolean> = emptyMap(),
        argsBuilder: ArgBuilder<A> = {}
    ) = Modal(createComponentId(argsBuilder, checkCompId = true), title ?: this.title) {
        val checkUpMap = specificallyEnabledArgs.mapKeys { it.key.name }
        argsFun().args.forEach { arg ->
            val spec = arg.spec as? ModalArgSpec
            if (spec?.defaultNotEnabled == true && checkUpMap[arg.name] != true) return@forEach
            if (spec?.short != false) short(arg.name, arg.name, required = !arg.optional, builder = spec?.builder ?: {})
            else paragraph(arg.name, arg.name, required = !arg.optional, builder = spec.builder)
        }
    }

    companion object {
        val eventToName: (ModalInteractionEvent) -> String = { it.modalId }
    }
}

abstract class SelectMenuFeature<A : Arguments>(argsFun: () -> A, spec: SelectMenuSpec) :
    Feature<SelectMenuSpec, StringSelectInteractionEvent, A>(
        argsFun, spec, StringSelectInteractionEvent::class, eventToName
    ) {
    val options: List<SelectOption>? = null
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
        placeholder: String = spec.name,
        options: List<SelectOption>? = this.options,
        argsBuilder: ArgBuilder<A> = {},
        menuBuilder: StringSelectMenu.Builder.() -> Unit = {}
    ) = StringSelectMenu(
        createComponentId(argsBuilder, checkCompId = true),
        placeholder,
        valueRange = selectableOptions,
        options = options.orEmpty(),
        builder = menuBuilder
    )

    companion object {
        val eventToName: (StringSelectInteractionEvent) -> String = { it.componentId.substringBefore(";") }
    }
}

sealed class FeatureSpec(open val name: String)
class CommandSpec(name: String, val help: String, vararg val guilds: Long) : FeatureSpec(name)
class ButtonSpec(name: String) : FeatureSpec(name)
class ModalSpec(name: String) : FeatureSpec(name)
class SelectMenuSpec(name: String) : FeatureSpec(name)

sealed interface ArgSpec
class CommandArgSpec(
    val autocomplete: (suspend (String, CommandAutoCompleteInteractionEvent) -> List<String>?)? = null,
    val choices: List<Choice>? = null
) : ArgSpec

class ModalArgSpec(val short: Boolean, val defaultNotEnabled: Boolean, val builder: TextInput.Builder.() -> Unit) :
    ArgSpec
class SelectMenuArgSpec(val selectableOptions: IntRange) : ArgSpec
open class Arguments {
    val _args = mutableListOf<Arg<*, *>>()
    val args: List<Arg<*, *>> = Collections.unmodifiableList(_args)
    protected fun string(name: String, help: String, builder: Arg<String, String>.() -> Unit = {}) =
        createArg(name, help, OptionType.STRING, builder)
    protected fun long(name: String, help: String, builder: Arg<Long, Long>.() -> Unit = {}) =
        createArg(name, help, OptionType.INTEGER, builder)

    protected fun int(name: String, help: String, builder: Arg<Long, Int>.() -> Unit = {}) =
        createArg<Long, Int>(name, help, OptionType.INTEGER) {
            validate { it.toInt() }
            builder()
        }

    protected fun intFromString(
        name: String,
        help: String,
        throwIfNotNumber: Boolean = false,
        builder: Arg<String, Int>.() -> Unit = {}
    ) = createArg<String, Int>(name, help, OptionType.STRING) {
        validate {
            it.toIntOrNull() ?: if (throwIfNotNumber) throw InvalidArgumentException("Ungültige Zahl bei $name!") else 0
        }
        builder()
    }

    @Suppress("SameParameterValue")
    protected fun draftPokemon(
        name: String,
        help: String,
        autocomplete: (suspend (String, CommandAutoCompleteInteractionEvent) -> List<String>?)? = null
    ) = createArg(name, help, OptionType.STRING) {
        validate {
            val guildId = League.onlyChannel(tc)?.guild ?: gid
            NameConventionsDB.getDiscordTranslation(
                it, guildId, english = Tierlist[guildId].isEnglish
            ) ?: throw IllegalArgumentException("Pokemon $it nicht gefunden!")
        }
        slashCommand(autocomplete = autocomplete ?: lambda@{ s, event ->
            val gid = event.guild!!.idLong
            val league = League.onlyChannel(event.channel.idLong)
            //val alreadyPicked = league?.picks?.values?.flatten()?.map { it.name } ?: emptyList()
            val tierlist = Tierlist[league?.guild ?: gid]
            val strings = (tierlist?.autoComplete ?: Command.allNameConventions).filterStartsWithIgnoreCase(s)
            if (strings.size > 25) return@lambda listOf("Zu viele Ergebnisse, bitte spezifiziere deine Suche!")
            (if (league == null || tierlist == null) strings
            else strings.map {
                if (league.picks.values.flatten().any { p ->
                        p.name == tierlist.tlToOfficialCache.getOrPut(it) {
                            NameConventionsDB.getDiscordTranslation(it, league.guild)!!.official
                        }
                    }) "$it (GEPICKT)" else it
            }).sorted()
        })
    }

    protected fun boolean(name: String, help: String, builder: Arg<Boolean, Boolean>.() -> Unit = {}) =
        createArg(name, help, OptionType.BOOLEAN, builder)

    protected inline fun <reified T : Enum<T>> enumBasic(
        name: String, help: String, builder: Arg<String, T>.() -> Unit = {}
    ) = createArg(name, help, OptionType.STRING) {
        validate {
            try {
                enumValueOf<T>(it)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "Ungültiger Wert für $name! Mögliche Werte: ${enumValues<T>().joinToString(", ")}"
                )
            }
        }
        slashCommand(enumValues<T>().map { Choice(it.name, it.name) })
        builder()
    }

    protected inline fun <reified T> enumAdvanced(
        name: String,
        help: String,
        builder: Arg<String, T>.() -> Unit = {}
    ) where T : Enum<T>, T : Nameable =
        createArg(name, help, OptionType.STRING) {
            validate {
                try {
                    enumValueOf<T>(it)
                } catch (e: IllegalArgumentException) {
                    throw IllegalArgumentException(
                        "Ungültiger Wert für $name! Mögliche Werte: ${enumValues<T>().joinToString(", ")}"
                    )
                }
            }
            slashCommand(enumValues<T>().map { Choice(it.prettyName, it.name) })
            builder()
        }

    protected fun pokemontype(name: String, help: String, english: Boolean) = createArg(name, help, OptionType.STRING) {
        validate { str ->
            val t = if (english) Command.getEnglNameWithType(str)
            else Command.getGerName(str)
            if (t.isEmpty || t.type != Translation.Type.TYPE) throw InvalidArgumentException("Ungültiger Typ!")
            if (t.translation == "Psychic" || t.otherLang == "Psychic") {
                if (english) "Psychic" else "Psycho"
            } else t.translation
        }
    }
    protected fun pokemontype(name: String, help: String) = createArg(name, help, OptionType.STRING) {
        validate { str ->
            val t = Command.getGerName(str)
            if (t.isEmpty || t.type != Translation.Type.TYPE) throw InvalidArgumentException("Ungültiger Typ!")
            if (t.translation == "Psychic" || t.otherLang == "Psychic") {
                "Psycho" to "Psychic"
            } else t.translation to t.otherLang
        }
    }

    protected fun list(name: String, help: String, numOfArgs: Int, requiredNum: Int) =
        object : ReadWriteProperty<Arguments, List<String>> {
            private val argList: List<Arg<String, out String?>> = List(numOfArgs) { i ->
                createArg<String, String>(name.embedI(i), help.embedI(i), OptionType.STRING) {}.run {
                    if (i >= requiredNum) nullable() else this
                }
            }

            private fun String.embedI(i: Int) = if ("%s" in this) format(i) else plus(i)
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

    protected fun member(name: String, help: String, builder: Arg<Member, Member>.() -> Unit = {}) =
        createArg(name, help, OptionType.USER, builder)

    protected fun channel(
        name: String,
        help: String,
        builder: Arg<GuildChannelUnion, GuildChannelUnion>.() -> Unit = {}
    ) =
        createArg(name, help, OptionType.CHANNEL, builder)

    protected fun textchannel(
        name: String,
        help: String,
        builder: Arg<GuildChannelUnion, GuildMessageChannelUnion>.() -> Unit = {}
    ) =
        createArg(name, help, OptionType.CHANNEL, builder)

    protected fun attachment(
        name: String,
        help: String,
        builder: Arg<Message.Attachment, Message.Attachment>.() -> Unit = {}
    ) = createArg(name, help, OptionType.ATTACHMENT, builder)

    protected fun singleOption() = multiOption(1..1)

    @Suppress("MemberVisibilityCanBePrivate")
    protected fun multiOption(range: IntRange) = createArg<String, String>("", "", OptionType.STRING) {
        spec = SelectMenuArgSpec(range)
    }

    protected fun Collection<String>.toChoices() = map { Choice(it, it) }
    protected fun choicesOf(vararg choices: String) = choices.toList().toChoices()

    inline fun <DiscordType, ParsedType> createArg(
        name: String,
        help: String,
        optionType: OptionType = OptionType.STRING,
        builder: Arg<DiscordType, ParsedType>.() -> Unit
    ) = Arg<DiscordType, ParsedType>(name, help, optionType, this).also {
        it.builder()
        _args += it
    }

    fun replaceLastArg(arg: Arg<*, *>) {
        _args.removeLast()
        _args += arg
    }
}

interface Nameable {
    val prettyName: String
}

object NoArgs : Arguments() {
    val argsFun = { this }
    operator fun invoke() = argsFun
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
        autocomplete: (suspend (String, CommandAutoCompleteInteractionEvent) -> List<String>?)? = null
    ) {
        spec = CommandArgSpec(autocomplete, choices)
    }

    fun modal(short: Boolean = true, defaultNotEnabled: Boolean = false, builder: TextInput.Builder.() -> Unit = {}) {
        spec = ModalArgSpec(short, defaultNotEnabled, builder)
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
    @Suppress("IMPLICIT_CAST_TO_ANY")
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
            it.default = default
            it.defaultFunction = defaultFunction
            it.validator = validator
            it.spec = spec
            it.nullable = true
            it.compIdOnly = compIdOnly
            args.replaceLastArg(it)
        }
    }
}

private val nameToDiscordRegex = Regex("[^\\w-]")
fun String.nameToDiscordOption(): String {
    return lowercase().replace(" ", "-").replace(nameToDiscordRegex, "")
}

open class ArgumentException(override val message: String) : Exception(message)
class MissingArgumentException(arg: Arg<*, *>) : ArgumentException("Du musst den Parameter `${arg.name}` angeben!")

class InvalidArgumentException(override val message: String) : ArgumentException(message)
