@file:Suppress("UNCHECKED_CAST")

package de.tectoast.emolga.features

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.RealInteractionData
import de.tectoast.emolga.commands.condAppend
import de.tectoast.emolga.utils.Constants
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import kotlin.reflect.KClass

class FeatureManager(private val load: Set<Feature<*, *, *>>) {
    private val eventToName: Map<KClass<*>, (GenericInteractionCreateEvent) -> String> =
        load.associate { it.eventClass to it.eventToName }
    private val features: Map<KClass<*>, Map<String, Feature<*, *, Arguments>>> = load.groupBy { it.eventClass }
        .mapValues { it.value.associate { k -> k.spec.name to k as Feature<*, GenericInteractionCreateEvent, Arguments> } }
    private val listeners = load.flatMap { it.registeredListeners }.groupBy { it.first }.mapValues {
        it.value.map { v -> v.second }
    }

    suspend fun handleEvent(e: GenericInteractionCreateEvent) {
        val kClass = e::class
        listeners[kClass]?.forEach { it(e) }
        val eventFeatures = features[kClass] ?: return
        val feature = eventFeatures[eventToName[kClass]!!(e)]!!
        execute(
            if (e is SlashCommandInteractionEvent && feature is CommandFeature<*> && feature.children.isNotEmpty()) feature.childCommands[e.subcommandName]!! as Feature<*, *, Arguments>
            else feature, e
        )


    }

    private suspend fun execute(
        feature: Feature<*, GenericInteractionCreateEvent, Arguments>,
        e: GenericInteractionCreateEvent,
    ) {
        val args = feature.argsFun()
        val data = RealInteractionData(e)
        try {
            feature.populateArgs(data, e, args)
        } catch (ex: ArgumentException) {
            data.reply(
                ex.message + "\nWenn du denkst, dass dies ein Fehler ist, melde dich bitte bei ${Constants.MYTAG}.",
                ephemeral = true
            )
            return
        }
        with(data) {
            try {
                when (val result = feature.allowed()) {
                    Allowed -> {
                        feature.exec(args)
                    }

                    is NotAllowed -> {
                        reply(result.reason, ephemeral = true)
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                reply(
                    "Es ist ein Fehler beim Ausführen des Commands aufgetreten!\nWenn du denkst, dass dies ein interner Fehler beim Bot ist, melde dich bitte bei Flo (${Constants.MYTAG}).".condAppend(
                        data.user == Constants.FLOID, "\nJa Flo, du sollst dich auch bei ihm melden du Kek :^)"
                    )
                )
                Command.sendToMe(
                    "Error in feature ${feature.spec.name}:\n" + "Event: ${e::class.simpleName}\n" + "User: `${data.user}`\n" + "Guild: `${data.gid}` [${data.textChannel.guild.name}\n" + "Channel: `${data.tc}` [${data.textChannel.asMention}" + "Input: ```$e```"
                )
            }
        }
    }

    fun generateSlashCommandDescriptions() = load.filterIsInstance<CommandFeature<*>>().map { cmd ->
        val cd = Commands.slash(cmd.spec.name, cmd.spec.help)
        if (cmd.children.isNotEmpty()) {
            cmd.children.forEach {
                cd.addSubcommands(SubcommandData(it.spec.name, it.spec.help).addOptions(generateOptionData(it)))
            }
        } else cd.addOptions(generateOptionData(cmd))
        cd
    }

    private fun generateOptionData(feature: CommandFeature<*>) = feature.defaultArgs.map { a ->
        val spec = a.spec as? CommandArgSpec
        OptionData(a.optionType, a.name, a.help, !a.optional, spec?.autocomplete != null).apply {
            if (spec?.choices != null) addChoices(spec.choices)
        }
    }
}

