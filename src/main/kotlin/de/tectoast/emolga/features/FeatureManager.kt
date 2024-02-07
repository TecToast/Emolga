@file:Suppress("UNCHECKED_CAST")

package de.tectoast.emolga.features

import com.google.common.reflect.ClassPath
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.RealInteractionData
import de.tectoast.emolga.commands.condAppend
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.annotations.ToTest
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.only
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation

class FeatureManager(private val loadListeners: Set<ListenerProvider>) {
    private val listenerScope = CoroutineScope(Dispatchers.Default)

    constructor(packageName: String) : this(packageName.let {
        ClassPath.from(Thread.currentThread().contextClassLoader).getTopLevelClassesRecursive(packageName).mapNotNull {
            val cl = it.load().kotlin
            if (cl.hasAnnotation<ToTest>()) {
                logger.warn("Feature ${cl.simpleName} needs to be tested!")
            }
            findAllFeaturesRecursively(cl)
        }.flatten().toSet()
    })

    private val eventToName: Map<KClass<*>, (GenericInteractionCreateEvent) -> String>
    private val features: Map<KClass<*>, Map<String, Feature<*, *, Arguments>>>
    private val listeners: Map<KClass<out GenericEvent>, List<suspend (GenericEvent) -> Unit>>

    init {
        val featuresSet = loadListeners.filterIsInstance<Feature<*, *, *>>()
        eventToName = featuresSet.associate { it.eventClass to it.eventToName }
        features = featuresSet.groupBy { it.eventClass }
            .mapValues { it.value.associate { k -> k.spec.name to k as Feature<*, GenericInteractionCreateEvent, Arguments> } }
        listeners = loadListeners.flatMap { it.registeredListeners }.groupBy { it.first }.mapValues {
            it.value.map { v -> v.second }
        }
    }


    suspend fun handleEvent(e: GenericEvent) {
        val kClass = e::class
        listeners[kClass]?.forEach {
            listenerScope.launch {
                it(e)
            }
        }
        if (e is GenericInteractionCreateEvent) {
            val eventFeatures = features[kClass] ?: return
            val feature = eventFeatures[eventToName[kClass]!!(e)]!!
            execute(
                if (e is SlashCommandInteractionEvent && feature is CommandFeature<*> && feature.children.isNotEmpty()) feature.childCommands[e.subcommandName]!! as Feature<*, *, Arguments>
                else feature, e
            )
        }
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
                when (val result = if (user == Constants.FLOID) Allowed else {
                    if (!feature.check(data)) NotAllowed else feature.checkSpecial(data)
                }) {
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

    suspend fun updateFeatures(jda: JDA, updateGuilds: List<Long>? = null) {
        val guildSlashFeatures: MutableMap<Long, MutableSet<SlashCommandData>> = mutableMapOf()
        generateSlashCommandDescriptions().forEach { (feature, desc) ->
            (feature.spec.guilds + Constants.G.MY).forEach { guildId ->
                guildSlashFeatures.getOrPut(guildId) { mutableSetOf() }.add(desc)
            }
        }
        for (gid in updateGuilds ?: db.config.only().guildsToUpdate.ifEmpty { guildSlashFeatures.keys }) {
            val commands = guildSlashFeatures[gid] ?: continue
            (if (gid == -1L) jda.updateCommands()
            else jda.getGuildById(gid)?.updateCommands())?.addCommands(commands)?.queue()
        }
    }

    fun generateSlashCommandDescriptions() = loadListeners.filterIsInstance<CommandFeature<*>>().associateWith { cmd ->
        Commands.slash(cmd.spec.name, cmd.spec.help).apply {
            defaultPermissions = cmd.slashPermissions
            if (cmd.children.isNotEmpty()) {
                cmd.children.forEach {
                    addSubcommands(SubcommandData(it.spec.name, it.spec.help).addOptions(generateOptionData(it)))
                }
            } else addOptions(generateOptionData(cmd))
        }
    }

    private fun generateOptionData(feature: CommandFeature<*>) = feature.defaultArgs.mapNotNull { a ->
        if (a.onlyInCode) return@mapNotNull null
        val spec = a.spec as? CommandArgSpec
        OptionData(a.optionType, a.name.nameToDiscordOption(), a.help, !a.optional, spec?.autocomplete != null).apply {
            if (spec?.choices != null) addChoices(spec.choices)
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}

        fun findAllFeaturesRecursively(cl: KClass<*>): List<ListenerProvider> {
            val o = cl.objectInstance ?: return emptyList()
            return if (o is ListenerProvider) listOf(o) else {
                cl.nestedClasses.flatMap { findAllFeaturesRecursively(it) }
            }
        }
    }
}

