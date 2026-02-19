@file:Suppress("UNCHECKED_CAST")

package de.tectoast.emolga.features

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.database.exposed.GuildLanguageDB
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.database.exposed.TypesDB
import de.tectoast.emolga.features.draft.during.generic.K18n_PokemonNotFound
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.draft.isEnglish
import de.tectoast.emolga.utils.json.mdb
import de.tectoast.generic.*
import de.tectoast.k18n.generated.K18N_DEFAULT_LANGUAGE
import de.tectoast.k18n.generated.K18nLanguage
import de.tectoast.k18n.generated.K18nMessage
import dev.minn.jda.ktx.interactions.components.*
import mu.KotlinLogging
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.Component
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.label.LabelChildComponent
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion
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
import net.dv8tion.jda.api.modals.Modal
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

    context (iData: InteractionData)
    abstract suspend fun exec(e: A)

    context(iData: InteractionData)
    suspend fun exec(argsBuilder: ArgBuilder<@UnsafeVariance A> = {}) =
        exec(buildArgs(argsBuilder))
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
open class NotAllowed(val reason: K18nMessage) : AllowedResult() {
    companion object : NotAllowed(K18n_NoPermission)
}

/**
 * Marker for classes which are part of the feature system (at the moment Feature & ListenerProvider)
 */
abstract class ListenerProvider {
    val registeredListeners: MutableSet<Pair<KClass<out GenericEvent>, suspend (GenericEvent) -> Unit>> = mutableSetOf()

    /**
     * Registers a Listener in a feature
     */
    inline fun <reified T : GenericEvent> registerListener(noinline listener: suspend (T) -> Unit) {
        registeredListeners += (T::class to listener) as Pair<KClass<out GenericEvent>, suspend (GenericEvent) -> Unit>
    }

    /**
     * Registers a DM listener
     */
    fun registerDMListener(prefix: String = "", listener: suspend (MessageReceivedEvent) -> Unit) =
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
                    return@registerListener it.replyChoiceStrings(result.reason.translateToGuildLanguage(it.guild?.idLong))
                        .queue()
                }
            }
            val focusedOption = it.focusedOption
            val options = childCommands[it.subcommandName]?.autoCompletableOptions ?: autoCompletableOptions
            options[focusedOption.name]?.let { ac ->
                val list = ac(focusedOption.value, it)?.takeIf { l -> l.size <= 25 }
                it.replyChoiceStrings(list ?: listOf(K18n_TooManyResults.translateToGuildLanguage(it.guild?.idLong)))
                    .queue()
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
    open val label: K18nMessage = spec.name.k18n
    open val emoji: Emoji? = null
    override suspend fun populateArgs(data: InteractionData, e: ButtonInteractionEvent, args: A) {
        val argsFromEvent = e.componentId.substringAfter(";").split(";")
        for ((index, arg) in args.args.withIndex()) {
            val m = argsFromEvent.getOrNull(index)?.takeIf { it.isNotBlank() }
            if (m != null) arg.parse(data, m)
        }
    }

    context(iData: InteractionData)
    operator fun invoke(
        label: K18nMessage = this.label,
        buttonStyle: ButtonStyle = this.buttonStyle,
        emoji: Emoji? = this.emoji,
        disabled: Boolean = false,
        argsBuilder: ArgBuilder<A> = {}
    ) = button(createComponentId(argsBuilder), label.t(), style = buttonStyle, emoji = emoji, disabled = disabled)

    fun withoutIData(
        language: K18nLanguage = K18N_DEFAULT_LANGUAGE,
        label: K18nMessage = this.label,
        buttonStyle: ButtonStyle = this.buttonStyle,
        emoji: Emoji? = this.emoji,
        disabled: Boolean = false,
        argsBuilder: ArgBuilder<A> = {}
    ) = button(
        createComponentId(argsBuilder),
        label.translateTo(language),
        style = buttonStyle,
        emoji = emoji,
        disabled = disabled
    )


    companion object {
        val eventToName: (ButtonInteractionEvent) -> String = { it.componentId.substringBefore(";") }
    }
}

abstract class ModalFeature<A : Arguments>(argsFun: () -> A, spec: ModalSpec) :
    Feature<ModalSpec, ModalInteractionEvent, A>(
        argsFun, spec, ModalInteractionEvent::class, eventToName
    ) {

    open val title: K18nMessage
        get() = throw NotImplementedError("Title not implemented for modal ${this.spec.name}")

    override suspend fun populateArgs(data: InteractionData, e: ModalInteractionEvent, args: A) {
        val (compId, regular) = args.args.partition { it.compIdOnly }
        val argsFromEvent = e.modalId.substringAfter(";").split(";")
        populateArgs(data, compId) { _, index ->
            argsFromEvent.getOrNull(index)?.takeIf { it.isNotBlank() }
        }
        populateArgs(data, regular) { name, _ ->
            val value = e.getValue(name) ?: return@populateArgs null
            when (value.type) {
                Component.Type.STRING_SELECT -> value.asStringList
                Component.Type.USER_SELECT -> value.asLongList
                Component.Type.TEXT_INPUT -> value.asString.takeIf { it.isNotBlank() }
                else -> null
            }
        }
    }

    context(iData: InteractionData)
    suspend operator fun invoke(
        title: K18nMessage = this.title,
        specificallyEnabledArgs: Map<ModalKey, Boolean> = emptyMap(),
        argsBuilder: ArgBuilder<A> = {}
    ): Modal {
        val modalEntries = argsFun().apply(argsBuilder).args.mapNotNull { arg ->
            if (arg.compIdOnly) return@mapNotNull null
            val spec = arg.spec as? ModalArgSpec
            spec?.modalEnableKey?.let { key ->
                if (specificallyEnabledArgs[key] != true) return@mapNotNull null
            }
            val argName = arg.name
            val argId = argName.nameToDiscordOption()
            val required = spec?.required == true || !arg.optional
            val value = arg.parsed?.toString()
            Triple(
                spec?.label?.translateTo(iData.language) ?: argName,
                arg.help,
                (spec?.argOption ?: ModalArgOption.Text()).buildChildComponent(
                    iData, argId, required, value
                )
            )
        }
        return Modal(createComponentId(argsBuilder, checkCompId = true), title.t()) {
            modalEntries.forEach { (name, help, child) ->
                label(
                    label = name,
                    description = help.t(),
                    child = child
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
        valueRange: IntRange? = null,
        argsBuilder: ArgBuilder<A> = {},
    ) = StringSelectMenu(
        customId = createComponentId(argsBuilder, checkCompId = true),
        placeholder = placeholder,
        disabled = disabled,
        valueRange = valueRange ?: selectableOptions.let { if (it.isEmpty()) 1..(options?.size ?: 0) else it },
        options = options.orEmpty(),
        builder = menuBuilder
    )

    companion object {
        val eventToName: (StringSelectInteractionEvent) -> String = { it.componentId.substringBefore(";") }
    }
}

abstract class MessageContextFeature(spec: MessageContextSpec) :
    Feature<MessageContextSpec, MessageContextInteractionEvent, MessageContextArgs>(
        ::MessageContextArgs, spec, MessageContextInteractionEvent::class, eventToName
    ) {
    override suspend fun populateArgs(
        data: InteractionData, e: MessageContextInteractionEvent, args: MessageContextArgs
    ) {
        args.message = e.target
    }

    companion object {
        val eventToName: (MessageContextInteractionEvent) -> String = { it.interaction.name }
    }
}

sealed class FeatureSpec(open val name: String)
sealed class RegisteredFeatureSpec(name: String) : FeatureSpec(name) {
    var inDM = false
}

class CommandSpec(name: String, val help: K18nMessage) : RegisteredFeatureSpec(name)
class ButtonSpec(name: String) : FeatureSpec(name)
class ModalSpec(name: String) : FeatureSpec(name)
class SelectMenuSpec(name: String) : FeatureSpec(name)
class MessageContextSpec(name: String) : RegisteredFeatureSpec(name)

sealed interface ArgSpec
data class CommandArgSpec(
    val autocomplete: (suspend (String, CommandAutoCompleteInteractionEvent) -> List<String>?)? = null,
    val choices: List<Choice>? = null,
    val guildChecker: GuildChecker? = null
) : ArgSpec

data class ModalArgSpec(
    val argOption: ModalArgOption = ModalArgOption.Text(),
    val modalEnableKey: ModalKey?,
    val required: Boolean,
    val label: K18nMessage?
) : ArgSpec

interface ModalArgOption {
    suspend fun buildChildComponent(
        iData: InteractionData,
        argId: String,
        required: Boolean,
        value: String?
    ): LabelChildComponent

    data class Text(
        val short: Boolean = true,
        val placeholder: K18nMessage? = null,
        val builder: InlineTextInput.() -> Unit = {}
    ) : ModalArgOption {
        override suspend fun buildChildComponent(
            iData: InteractionData,
            argId: String,
            required: Boolean,
            value: String?
        ): LabelChildComponent {
            return TextInput(
                customId = argId,
                style = if (short) TextInputStyle.SHORT else TextInputStyle.PARAGRAPH,
                required = required,
                placeholder = placeholder?.translateTo(iData.language),
                value = value,
                builder = builder
            )
        }
    }

    data class Select(
        val placeholder: K18nMessage? = null,
        val valueRange: IntRange? = 1..1,
        val optionsProvider: suspend (InteractionData) -> List<SelectOption>,
        val builder: StringSelectMenu.Builder.() -> Unit = {}
    ) : ModalArgOption {
        override suspend fun buildChildComponent(
            iData: InteractionData,
            argId: String,
            required: Boolean,
            value: String?
        ): LabelChildComponent {
            val options = optionsProvider(iData)
            return StringSelectMenu(
                customId = argId,
                placeholder = placeholder?.translateTo(iData.language),
                valueRange = valueRange ?: 1..options.size,
                options = options,
                builder = builder,
            )
        }
    }
}

data class SelectMenuArgSpec(val selectableOptions: IntRange) : ArgSpec
open class Arguments {
    private val _args = mutableListOf<Arg<*, *>>()
    val args: List<Arg<*, *>> = Collections.unmodifiableList(_args)
    inline fun string(
        name: String = "",
        help: K18nMessage = EmptyMessage,
        builder: Arg<String, String>.() -> Unit = {}
    ) =
        createArg(name, help, OptionType.STRING, builder)

    @JvmName("stringGeneric")
    inline fun <T> string(
        name: String = "",
        help: K18nMessage = EmptyMessage,
        builder: Arg<String, T>.() -> Unit = {}
    ) =
        createArg(name, help, OptionType.STRING, builder)

    inline fun long(name: String = "", help: K18nMessage = EmptyMessage, builder: Arg<String, Long>.() -> Unit = {}) =
        createArg<String, Long>(name, help, OptionType.STRING) {
            validate { it.toLongOrNull() }
            builder()
        }

    inline fun int(name: String = "", help: K18nMessage = EmptyMessage, builder: Arg<Long, Int>.() -> Unit = {}) =
        createArg<Long, Int>(name, help, OptionType.INTEGER) {
            validate { it.toInt() }
            builder()
        }

    fun draftPokemon(
        name: String = "",
        help: K18nMessage = EmptyMessage,
        builder: Arg<String, DraftName>.() -> Unit = {},
        autocomplete: (suspend (String, CommandAutoCompleteInteractionEvent) -> List<String>?)? = null
    ) = createArg(name, help, OptionType.STRING) {
        validateDraftPokemon()
        slashCommand(autocomplete = autocomplete ?: lambda@{ s, event ->
            val gid = event.guild!!.idLong
            val league = mdb.leagueForAutocomplete(event.channel.idLong, gid, event.user.idLong)
            val tierlist = league?.tierlist ?: Tierlist[gid]
            val strings =
                (tierlist?.autoComplete() ?: NameConventionsDB.allNameConventions()).filterContainsIgnoreCase(s)
            if (strings.size > 25) return@lambda listOf(K18n_TooManyResults.translateToGuildLanguage(gid))
            (if (league == null || tierlist == null) strings
            else strings.map {
                val officialName = tierlist.tlToOfficialCache.getOrPut(it) {
                    NameConventionsDB.getDiscordTranslation(it, league.guild)!!.official
                }
                if (league.isPicked(officialName)) "$it (NICHT VERFÜGBAR)" else it
            }).sortedWith(compareBy({ !it.startsWith(s) }, { it }))
        })
        builder()
    }

    inline fun boolean(
        name: String = "",
        help: K18nMessage = EmptyMessage,
        builder: Arg<Boolean, Boolean>.() -> Unit = {}
    ) =
        createArg(name, help, OptionType.BOOLEAN, builder)

    inline fun <reified T : Enum<T>> enumBasic(
        name: String = "", help: K18nMessage = EmptyMessage, builder: Arg<String, T>.() -> Unit = {}
    ) = createArg(name, help, OptionType.STRING) {
        val enumValues = enumValues<T>()
        validate {
            try {
                enumValueOf<T>(it)
            } catch (e: IllegalArgumentException) {
                throw InvalidArgumentException(
                    K18n_Arguments.InvalidEnum(name, enumValues.joinToString())
                )
            }
        }
        if (enumValues.size <= 25) slashCommand(enumValues.map { Choice(it.name, it.name) })
        else slashCommand { s, event ->
            val nameMatching = enumValues.toSet().filterStartsWithIgnoreCase(s) { it.name }
            nameMatching.convertListToAutoCompleteReply(GuildLanguageDB.getLanguage(event.guild?.idLong))
        }
        builder()
    }

    inline fun fromListCommand(
        name: String = "",
        help: K18nMessage = EmptyMessage,
        collection: Collection<String>,
        useContainsAutoComplete: Boolean = false,
        builder: Arg<String, String>.() -> Unit = {}
    ) = createArg<String, String>(name, help) {
        validate { s ->
            s.takeIf { it in collection }
                ?: throw InvalidArgumentException(K18n_Arguments.NotAutocompleteConform)
        }
        if (collection.size <= 25) slashCommand(collection.map { Choice(it, it) })
        else slashCommand { s, event ->
            (if (useContainsAutoComplete) collection.filterContainsIgnoreCase(s)
            else collection.filterStartsWithIgnoreCase(s)).convertListToAutoCompleteReply(
                GuildLanguageDB.getLanguage(
                    event.guild?.idLong
                )
            )
        }
        builder()
    }

    inline fun fromListCommand(
        name: String = "",
        help: K18nMessage = EmptyMessage,
        crossinline collSupplier: suspend (CommandAutoCompleteInteractionEvent) -> Collection<String>,
        builder: Arg<String, String>.() -> Unit = {}
    ) = createArg(name, help) {
        slashCommand { s, event ->
            collSupplier(event).filterStartsWithIgnoreCase(s)
                .convertListToAutoCompleteReply(GuildLanguageDB.getLanguage(event.guild?.idLong))
        }
        builder()
    }

    inline fun fromListModal(
        name: String = "",
        help: K18nMessage = EmptyMessage,
        placeholder: K18nMessage? = null,
        valueRange: IntRange? = 1..1,
        noinline optionsProvider: suspend (InteractionData) -> List<SelectOption>,
        builder: Arg<List<String>, List<String>>.() -> Unit = {}
    ) = createArg(name, help, OptionType.STRING) {
        modal(ModalArgOption.Select(placeholder, valueRange, optionsProvider))
        builder()
    }

    inline fun <reified T> enumAdvanced(
        name: String = "", help: K18nMessage = EmptyMessage, builder: Arg<String, T>.() -> Unit = {}
    ) where T : Enum<T>, T : Nameable = createArg(name, help, OptionType.STRING) {
        validate {
            try {
                enumValueOf<T>(it)
            } catch (e: IllegalArgumentException) {
                throw InvalidArgumentException(
                    b { K18n_Arguments.InvalidEnum(name, enumValues<T>().joinToString { en -> en.prettyName() })() }
                )
            }
        }
        slashCommand(enumValues<T>().map {
            Choice(it.prettyName.default(), it.name).setNameLocalizations(it.prettyName.toDiscordLocaleMap())
        })
        builder()
    }

    fun pokemontype(
        name: String = "",
        help: K18nMessage = EmptyMessage,
        language: Language,
        builder: Arg<String, String>.() -> Unit = {}
    ) = createArg(name, help) {
        validate {
            TypesDB.getType(it, language)
        }
        slashCommand { s, event ->
            TypesDB.getOptions(s)
        }
        builder()
    }

    fun pokemontype(name: String = "", help: K18nMessage = EmptyMessage) = createArg(name, help, OptionType.STRING) {
        validate { str ->
            TypesDB.getTypeInBothLanguages(str)
        }
        slashCommand { s, event ->
            TypesDB.getOptions(s)
        }
    }

    fun <DiscordType, ParsedType> genericList(
        name: String, help: K18nMessage, numOfArgs: Int, requiredNum: Int, type: OptionType, startAt: Int = 1
    ) = object : ReadWriteProperty<Arguments, List<ParsedType>> {
        private val argList: List<Arg<DiscordType, out ParsedType?>> = List(numOfArgs) { i ->
            createArg<DiscordType, ParsedType>(
                name.embedI(i, startAt),
                MappedMessage(help) { it.embedI(i, startAt) },
                type
            ) {}.run {
                if (i >= requiredNum) nullable() else this
            }
        }

        private var parsed: List<ParsedType>? = null
        override fun getValue(
            thisRef: Arguments, property: KProperty<*>
        ): List<ParsedType> {
            if (parsed == null) {
                parsed = argList.mapNotNull { it.parsed }
            }
            return parsed!!
        }

        override fun setValue(
            thisRef: Arguments, property: KProperty<*>, value: List<ParsedType>
        ) {
            parsed = value
        }
    }

    private fun String.embedI(i: Int, startAt: Int) = if ("&s" in this) format(i + startAt) else plus(i + startAt)
    fun list(name: String = "", help: K18nMessage = EmptyMessage, numOfArgs: Int, requiredNum: Int, startAt: Int = 1) =
        object : ReadWriteProperty<Arguments, List<String>> {
            private val argList: List<Arg<String, out String?>> = List(numOfArgs) { i ->
                createArg<String, String>(
                    name.embedI(i, startAt),
                    MappedMessage(help) { it.embedI(i, startAt) },
                    OptionType.STRING
                ) {}.run {
                    if (i >= requiredNum) nullable() else this
                }
            }

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

    inline fun member(
        name: String = "",
        help: K18nMessage = EmptyMessage,
        builder: Arg<Member, Member>.() -> Unit = {}
    ) =
        createArg(name, help, OptionType.USER, builder)

    inline fun channel(
        name: String = "",
        help: K18nMessage = EmptyMessage,
        builder: Arg<GuildChannelUnion, GuildChannelUnion>.() -> Unit = {}
    ) = createArg(name, help, OptionType.CHANNEL, builder)

    inline fun attachment(
        name: String = "",
        help: K18nMessage = EmptyMessage,
        builder: Arg<Message.Attachment, Message.Attachment>.() -> Unit = {}
    ) = createArg(name, help, OptionType.ATTACHMENT, builder)

    fun singleOption() = createArg<String, String>("", EmptyMessage, OptionType.STRING) {
        spec = SelectMenuArgSpec(1..1)
    }

    fun <T> singleOption(validator: suspend InteractionData.(String) -> T) = createArg("", EmptyMessage) {
        validate(validator)
    }


    fun <T> multiOption(range: IntRange, validator: suspend InteractionData.(String) -> T) =
        createArg<List<String>, List<T>>("", EmptyMessage, OptionType.STRING) {
            spec = SelectMenuArgSpec(range)
            validate { list -> list.map { validator(it) } }
        }

    fun multiOption(range: IntRange) = createArg<List<String>, List<String>>("", EmptyMessage, OptionType.STRING) {
        spec = SelectMenuArgSpec(range)
    }

    inline fun <DiscordType, ParsedType> createArg(
        name: String = "",
        help: K18nMessage = EmptyMessage,
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
        suspend fun monOfTeam(s: String, league: League, idx: Int): List<String>? {
            return dbTransaction {
                val tl = league.tierlist
                val picks = league.picks[idx] ?: return@dbTransaction null
                picks.filter { p -> !p.quit }.sortedWith(
                    compareBy({ mon -> tl.order.indexOf(mon.tier) }, { mon -> mon.name })
                ).map { mon ->
                    logger.debug(mon.name)
                    tlNameCache[mon.name] ?: NameConventionsDB.convertOfficialToTL(
                        mon.name, league.guild
                    )!!.also { tlName -> tlNameCache[mon.name] = tlName }
                }.filter { mon -> mon.startsWith(s, true) }

            }
        }

        fun Arg<String, DraftName>.validateDraftPokemon() {
            validate {
                val guildId = League.onlyChannel(tc)?.guild ?: gid
                NameConventionsDB.getDiscordTranslation(
                    it, guildId, english = Tierlist[guildId].isEnglish
                ) ?: throw InvalidArgumentException(K18n_PokemonNotFound(it))
            }
        }

    }
}

typealias GuildChecker = suspend CommandProviderData.() -> ArgumentPresence

enum class ArgumentPresence {
    NOT_PRESENT, REQUIRED, OPTIONAL
}

class CommandProviderData(val gid: Long) {
    val league = OneTimeCache { mdb.leagueByGuild(gid) }
}

interface Nameable {
    val prettyName: K18nMessage
}

object NoArgs : Arguments() {
    private val argsFun = { this }
    operator fun invoke() = argsFun
}

class MessageContextArgs : Arguments() {
    var message by createArg<Message, Message> { }
}

class Arg<DiscordType, ParsedType>(
    val name: String, val help: K18nMessage, val optionType: OptionType, internal val args: Arguments
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
    var customErrorMessage: K18nMessage? = null
    val optional get() = defaultValueSet || defaultFunction != null || nullable

    fun default(value: ParsedType) = apply {
        default = value
    }

    fun default(defaultfun: () -> ParsedType) = apply {
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
        spec = (spec as? CommandArgSpec)?.let { oldSpec ->
            oldSpec.copy(
                choices = choices ?: oldSpec.choices,
                guildChecker = guildChecker ?: oldSpec.guildChecker,
                autocomplete = autocomplete ?: oldSpec.autocomplete
            )
        } ?: CommandArgSpec(autocomplete, choices, guildChecker)
    }

    fun modal(
        short: Boolean = true,
        modalKey: ModalKey? = null,
        required: Boolean = false,
        placeholder: K18nMessage? = null,
        label: K18nMessage? = null,
        builder: InlineTextInput.() -> Unit = {}
    ) {
        modal(ModalArgOption.Text(short, placeholder, builder), modalKey, required, label)
    }

    fun modal(
        argOption: ModalArgOption = ModalArgOption.Text(),
        modalKey: ModalKey? = null,
        required: Boolean = false,
        label: K18nMessage? = null,
    ) {
        spec = (spec as? ModalArgSpec)?.let { oldSpec ->
            oldSpec.copy(
                argOption = argOption,
                modalEnableKey = modalKey ?: oldSpec.modalEnableKey,
                required = required || oldSpec.required,
            )
        } ?: ModalArgSpec(argOption, modalKey, required, label)
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
            customErrorMessage ?: K18n_InvalidArgument(name)
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

fun List<String>?.convertListToAutoCompleteReply(language: K18nLanguage) = when (this?.size) {
    0, null -> listOf(K18n_NoResults.translateTo(language))
    in 1..25 -> this
    else -> listOf(K18n_TooManyResults.translateTo(language))
}

open class ArgumentException(open val msg: K18nMessage) : Exception(msg.default())
class MissingArgumentException(arg: Arg<*, *>) : ArgumentException(K18n_MissingArgument(arg.name))

class InvalidArgumentException(msg: K18nMessage) : ArgumentException(msg)
