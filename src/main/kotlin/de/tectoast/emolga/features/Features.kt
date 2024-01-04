package de.tectoast.emolga.features

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.commands.filterStartsWithIgnoreCase
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.draft.isEnglish
import de.tectoast.emolga.utils.json.emolga.draft.League
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface Feature<T : FeatureSpec>

abstract class CommandFeature<T : Arguments>(val argsFun: () -> T) : Feature<CommandSpec> {
    context (InteractionData)
    abstract suspend fun exec(e: T)

    fun x() {

    }
}

interface FeatureSpec
data class CommandSpec(val name: String, val help: String) : FeatureSpec

open class Arguments {
    protected val args = mutableListOf<Arg<*, *>>()
    protected fun string(name: String, help: String, builder: Arg<String, String>.() -> Unit = {}) =
        createArg(name, help, builder)

    protected fun draftPokemon(name: String, help: String) =
        createArg(name, help) {
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
        createArg(name, help, builder)


    private fun <DiscordType, ParsedType> createArg(
        name: String,
        help: String,
        builder: Arg<DiscordType, ParsedType>.() -> Unit
    ) =
        Arg<DiscordType, ParsedType>(name, help).also {
            it.builder()
            args += it
        }
}

class TestCommandArgs : Arguments() {
    var test by string("yay", "nay")
    var anotherOne by string("huhu", "lol") {
        default = test

    }
}

open class Arg<DiscordType, ParsedType>(val name: String, val help: String) :
    ReadWriteProperty<Arguments, ParsedType> {
    private var parsed: ParsedType? = null
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

    suspend fun parse(data: InteractionData, m: OptionMapping) {
        val raw = when (m.type) {
            OptionType.STRING -> m.asString
            OptionType.INTEGER -> m.asLong
            OptionType.BOOLEAN -> m.asBoolean
            OptionType.USER -> m.asMember ?: m.asUser
            OptionType.CHANNEL -> m.asChannel
            OptionType.ROLE -> m.asRole
            OptionType.NUMBER -> m.asDouble
            OptionType.ATTACHMENT -> m.asAttachment
            else -> throw IllegalArgumentException("Unknown OptionType ${m.type}")
        } as DiscordType
        parsed = data.validator(raw)
        success = true
    }

    fun nullable(): Arg<DiscordType, ParsedType?> {
        return Arg<DiscordType, ParsedType?>(name, help).also {
            it.default = default
            it.defaultFunction = defaultFunction
            it.validator = validator
            it.autocomplete = autocomplete
            it.nullable = true
        }
    }
}
