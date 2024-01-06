package de.tectoast.emolga.features

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.commands.filterStartsWithIgnoreCase
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.draft.isEnglish
import de.tectoast.emolga.utils.json.emolga.draft.League
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.modals.ModalMapping
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

sealed class Feature<out T : FeatureSpec, out E : GenericInteractionCreateEvent, in A : Arguments>(
    val argsFun: () -> @UnsafeVariance A,
    val spec: T,
    val eventClass: KClass<@UnsafeVariance E>,
    val eventToName: (@UnsafeVariance E) -> String
) {
    abstract suspend fun populateArgs(data: InteractionData, e: @UnsafeVariance E, args: A)

    fun createComponentId(argsBuilder: ArgBuilder<@UnsafeVariance A>) =
        spec.name + ";" + argsFun().apply(argsBuilder).args.joinToString(";") { it.parsed?.toString() ?: "" }
    context (InteractionData)
    abstract suspend fun exec(e: A)
}
typealias ArgBuilder<A> = A.() -> Unit

abstract class CommandFeature<A : Arguments>(argsFun: () -> A, spec: CommandSpec) :
    Feature<CommandSpec, SlashCommandInteractionEvent, A>(
        argsFun,
        spec,
        SlashCommandInteractionEvent::class,
        eventToName
    ) {

    override suspend fun populateArgs(data: InteractionData, e: SlashCommandInteractionEvent, args: A) {
        for (arg in args.args) {
            val m = e.getOption(arg.name.nameToDiscordOption())
            if (m == null && !arg.optional) {
                return e.reply("Du musst den Parameter `${arg.name}` angeben!").setEphemeral(true).queue()
            }
            if (m != null) arg.parse(data, m)
        }
    }

    companion object {
        val eventToName: (SlashCommandInteractionEvent) -> String = { it.name }
    }
}

abstract class ButtonFeature<A : Arguments>(argsFun: () -> A, spec: ButtonSpec) :
    Feature<ButtonSpec, ButtonInteractionEvent, A>(argsFun, spec, ButtonInteractionEvent::class, eventToName) {

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

    companion object {
        val eventToName: (ButtonInteractionEvent) -> String = { it.componentId.substringBefore(";") }
    }
}

abstract class ModalFeature<A : Arguments>(argsFun: () -> A, spec: ModalSpec) :
    Feature<ModalSpec, ModalInteractionEvent, A>(
        argsFun,
        spec,
        ModalInteractionEvent::class,
        eventToName
    ) {

    override suspend fun populateArgs(data: InteractionData, e: ModalInteractionEvent, args: A) {
        for (arg in args.args) {
            val m = e.getValue(arg.name.nameToDiscordOption())
            if (m == null && !arg.optional) {
                return e.reply("Du musst den Parameter `${arg.name}` angeben!").setEphemeral(true).queue()
            }
            if (m != null) arg.parse(data, m)
        }
    }

    companion object {
        val eventToName: (ModalInteractionEvent) -> String = { it.modalId }
    }
}

sealed class FeatureSpec(open val name: String)
class CommandSpec(name: String, val help: String) : FeatureSpec(name)
class ButtonSpec(name: String) : FeatureSpec(name)
class ModalSpec(name: String) : FeatureSpec(name)
open class Arguments {
    private val _args = mutableListOf<Arg<*, *>>()
    val args: List<Arg<*, *>> = Collections.unmodifiableList(_args)
    protected fun string(name: String, help: String, builder: Arg<String, String>.() -> Unit = {}) =
        createArg(name, help, OptionType.STRING, builder)

    protected fun draftPokemon(name: String, help: String) = createArg(name, help, OptionType.STRING) {
        validate {
            val guildId = League.onlyChannel(tc)?.guild ?: gid
            NameConventionsDB.getDiscordTranslation(
                it, guildId, english = Tierlist[guildId].isEnglish
            ) ?: throw IllegalArgumentException("Pokemon $it nicht gefunden!")
        }
        autocomplete { s, event ->
            val gid = event.guild!!.idLong
            val league = League.onlyChannel(event.channel.idLong)
            //val alreadyPicked = league?.picks?.values?.flatten()?.map { it.name } ?: emptyList()
            val tierlist = Tierlist[league?.guild ?: gid]
            val strings = (tierlist?.autoComplete ?: Command.allNameConventions).filterStartsWithIgnoreCase(s)
            if (strings.size > 25) return@autocomplete listOf("Zu viele Ergebnisse, bitte spezifiziere deine Suche!")
            (if (league == null || tierlist == null) strings
            else strings.map {
                if (league.picks.values.flatten().any { p ->
                        p.name == tierlist.tlToOfficialCache.getOrPut(it) {
                            NameConventionsDB.getDiscordTranslation(it, league.guild)!!.official
                        }
                    }) "$it (GEPICKT)" else it
            }).sorted()
        }
    }

    protected fun boolean(name: String, help: String, builder: Arg<Boolean, Boolean>.() -> Unit = {}) =
        createArg(name, help, OptionType.BOOLEAN, builder)


    private inline fun <reified DiscordType, ParsedType> createArg(
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

open class Arg<DiscordType, ParsedType>(
    val name: String,
    val help: String,
    val optionType: OptionType,
    private val args: Arguments
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
    private var validator: (suspend InteractionData.(DiscordType) -> ParsedType) = { it as ParsedType }
    private var nullable = false
    val optional get() = defaultValueSet || defaultFunction != null || nullable
    var autocomplete: (suspend (String, CommandAutoCompleteInteractionEvent) -> List<String>?)? = null
        private set

    fun default(defaultfun: () -> ParsedType) {
        defaultFunction = defaultfun
    }

    fun validate(validator: suspend InteractionData.(DiscordType) -> ParsedType) {
        this.validator = validator
    }

    fun autocomplete(autocomplete: suspend (String, CommandAutoCompleteInteractionEvent) -> List<String>?) {
        this.autocomplete = autocomplete
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
        parsed = data.validator(
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

                is ModalMapping -> {
                    m.asString
                }

                is String -> {
                    m
                }

                else -> throw IllegalArgumentException("Unknown type ${m::class.simpleName}")
            } as DiscordType
        )
        success = true
    }

    fun nullable(): Arg<DiscordType, ParsedType?> {
        return Arg<DiscordType, ParsedType?>(name, help, optionType, args).also {
            it.default = default
            it.defaultFunction = defaultFunction
            it.validator = validator
            it.autocomplete = autocomplete
            it.nullable = true
            args.replaceLastArg(it)
        }
    }
}

private val nameToDiscordRegex = Regex("[^\\w-]")
fun String.nameToDiscordOption(): String {
    return lowercase().replace(" ", "-").replace(nameToDiscordRegex, "")
}
