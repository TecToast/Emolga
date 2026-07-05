package de.tectoast.emolga.features.system

import de.tectoast.emolga.domain.config.repository.GuildConfigRepository
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.util.autocomplete.PokemonAutocompleteService
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.pokemon.repository.PokemonTypesRepository
import de.tectoast.emolga.domain.pokemon.service.PokemonResolverService
import de.tectoast.emolga.features.K18n_Arguments
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.league.draft.generic.K18n_PokemonNotFound
import de.tectoast.emolga.features.system.model.InvalidArgumentException
import de.tectoast.emolga.features.system.model.Nameable
import de.tectoast.emolga.features.system.types.ModalArgOption
import de.tectoast.emolga.features.system.types.SelectMenuArgSpec
import de.tectoast.emolga.utils.*
import de.tectoast.generic.K18n_NoResults
import de.tectoast.generic.K18n_TooManyResults
import de.tectoast.k18n.generated.K18nLanguage
import de.tectoast.k18n.generated.K18nMessage
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.OptionType
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class Arguments : KoinComponent {
    private val _args = mutableListOf<Arg<*, *>>()
    val args: List<Arg<*, *>> = Collections.unmodifiableList(_args)

    private val typesRepository by inject<PokemonTypesRepository>()
    val languageRepository by inject<GuildConfigRepository>()
    val autocompleteService by inject<PokemonAutocompleteService>()

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

    inline fun showdownIDArg(
        name: String = "",
        help: K18nMessage = EmptyMessage,
        builder: Arg<String, ShowdownID>.() -> Unit = {}
    ) =
        createArg<String, ShowdownID>(name, help, OptionType.STRING) {
            validate {
                val showdownID = it.toShowdownID()
                if (it != showdownID.value) throw InvalidArgumentException("Invalid Showdown ID: $it".k18n)
                showdownID
            }
            builder()
        }

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

    inline fun draftPokemon(
        name: String = "",
        help: K18nMessage = EmptyMessage,
        builder: Arg<String, ShowdownID>.() -> Unit = {},
        noinline autocomplete: (suspend (String, CommandAutoCompleteInteractionEvent) -> List<String>?)? = null
    ) = createArg(name, help, OptionType.STRING) {
        validateDraftPokemon()
        slashCommand(autocomplete = autocomplete ?: lambda@{ s, event ->
            val gid = event.guild!!.idLong
            val result = autocompleteService.autocompletePokemon(s, gid, event.channel.idLong, event.user.idLong, 25)
            result ?: listOf(K18n_TooManyResults.translateTo(languageRepository.getLanguage(gid)))
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
            } catch (_: IllegalArgumentException) {
                throw InvalidArgumentException(
                    K18n_Arguments.InvalidEnum(name, enumValues.joinToString())
                )
            }
        }
        if (enumValues.size <= 25) slashCommand(enumValues.map { Choice(it.name, it.name) })
        else slashCommand { s, event ->
            val nameMatching = enumValues.toSet().filterStartsWithIgnoreCase(s) { it.name }
            nameMatching.convertListToAutoCompleteReply(languageRepository.getLanguage(event.guild?.idLong))
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
                .convertListToAutoCompleteReply(languageRepository.getLanguage(event.guild?.idLong))
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
            } catch (_: IllegalArgumentException) {
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
            typesRepository.getType(it, language)
        }
        slashCommand { s, _ ->
            typesRepository.getOptions(s)
        }
        builder()
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

    fun singleOption() = createArg<String, String>(help = EmptyMessage, optionType = OptionType.STRING) {
        spec = SelectMenuArgSpec(1..1)
    }

    fun multiOption(range: IntRange) = createArg<List<String>, List<String>>(
        help = EmptyMessage,
        optionType = OptionType.STRING
    ) {
        spec = SelectMenuArgSpec(range)
    }

    fun multiOptionLong(range: IntRange) = createArg<List<Long>, List<Long>>(
        help = EmptyMessage,
        optionType = OptionType.NUMBER
    ) {
        spec = SelectMenuArgSpec(range)
    }


    suspend fun monOfTeam(query: String, guild: Long, channel: Long, user: Long): List<String>? {
        return autocompleteService.autocompletePokemonOfTeam(query, guild, channel, user)
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

    fun Arg<String, ShowdownID>.validateDraftPokemon() {
        validate {
            val guildId = get<LeagueCoreRepository>().getGuildOfDraftChannel(tc) ?: gid
            get<PokemonResolverService>().resolvePokemon(it, guildId) ?: throw InvalidArgumentException(
                K18n_PokemonNotFound(it)
            )
        }
    }


    fun List<String>?.convertListToAutoCompleteReply(language: K18nLanguage) = when (this?.size) {
        0, null -> listOf(K18n_NoResults.translateTo(language))
        in 1..25 -> this
        else -> listOf(K18n_TooManyResults.translateTo(language))
    }

}

class MessageContextArgs : Arguments() {
    var message by createArg<Message, Message> { }
}

object NoArgs : Arguments() {
    private val argsFun = { this }
    operator fun invoke() = argsFun
}