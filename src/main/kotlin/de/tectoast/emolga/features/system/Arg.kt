package de.tectoast.emolga.features.system

import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.model.GuildChecker
import de.tectoast.emolga.features.system.model.InvalidArgumentException
import de.tectoast.emolga.features.system.types.CommandArgSpec
import de.tectoast.emolga.features.system.types.ModalArgOption
import de.tectoast.emolga.features.system.types.ModalArgSpec
import de.tectoast.emolga.features.system.types.ModalKey
import de.tectoast.generic.K18n_InvalidArgument
import de.tectoast.k18n.generated.K18nMessage
import dev.minn.jda.ktx.interactions.components.InlineTextInput
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@Suppress("UNCHECKED_CAST")
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
    private var customErrorMessage: K18nMessage? = null
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
        placeholder: K18nMessage? = null,
        label: K18nMessage? = null,
        builder: InlineTextInput.() -> Unit = {}
    ) {
        modal(ModalArgOption.Text(short, placeholder, builder), modalKey, label)
    }

    fun modal(
        argOption: ModalArgOption = ModalArgOption.Text(),
        modalKey: ModalKey? = null,
        label: K18nMessage? = null,
    ) {
        spec = (spec as? ModalArgSpec)?.let { oldSpec ->
            oldSpec.copy(
                argOption = argOption,
                modalEnableKey = modalKey ?: oldSpec.modalEnableKey,
            )
        } ?: ModalArgSpec(argOption, modalKey, label)
    }

    override fun getValue(thisRef: Arguments, property: KProperty<*>): ParsedType {
        if (success) return parsed as ParsedType
        if (defaultValueSet) return default as ParsedType
        defaultFunction?.let { return it() }
        throw IllegalStateException("No value set for $property")
    }

    fun getValueOrNull() : ParsedType? {
        if (success) return parsed as ParsedType
        if (defaultValueSet) return default as ParsedType
        defaultFunction?.let { return it() }
        return null
    }

    override fun setValue(thisRef: Arguments, property: KProperty<*>, value: ParsedType) {
        parsed = value
        success = true
    }

    /**
     * Parses the given data and sets the value of this argument
     * @param data The general data of this interaction
     * @param m The value of this argument, this MUST ONLY BE OptionMapping, String or `DiscordType`
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