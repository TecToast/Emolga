package de.tectoast.emolga.utils.dconfigurator

import de.tectoast.emolga.utils.condAppend
import dev.minn.jda.ktx.interactions.components.danger
import dev.minn.jda.ktx.interactions.components.primary
import dev.minn.jda.ktx.interactions.components.success
import dev.minn.jda.ktx.messages.into
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.modals.Modal
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction
import kotlin.reflect.KClass

abstract class DConfigurator(val name: String, private val userId: Long, private val channelId: Long) {
    private var currentIndex = 0
    val nextIndex get() = currentIndex + 1
    abstract val steps: List<Step<*>>

    init {
        DConfiguratorManager.addConfigurator(this, userId, channelId)
    }

    suspend fun handle(e: GenericInteractionCreateEvent) {
        if (e is ButtonInteractionEvent && e.componentId.startsWith("reopen;")) {
            val index = e.componentId.substring(7).toInt()
            e.replyModal((steps[index] as ModalStep).modalBuilder(index)).queue()
            return
        }
        val modalCloseHandler =
            steps[currentIndex] is ModalStep && e is GenericComponentInteractionCreateEvent && e.componentId.startsWith(
                (currentIndex - 1).toString()
            )
        if (e is GenericComponentInteractionCreateEvent && !e.componentId.startsWith(currentIndex.toString())
            && !modalCloseHandler
        ) {
            e.reply("Diese Einstellung ist nicht mehr verfügbar!").queue()
            return
        }
        val options = StepOptions()
        try {
            if (modalCloseHandler) currentIndex--
            steps[currentIndex].handle(e, options) ?: run {
                if (e is GenericComponentInteractionCreateEvent && e.componentId.first()
                        .isDigit()
                ) e.reply("Diese Konfiguration ist nicht mehr verfügbar!").queue()
                return
            }
        } catch (ex: InvalidArgumentException) {
            (e as IReplyCallback).reply(ex.message.condAppend(e is ModalInteractionEvent) { "\nDrücke auf den Button, um das Menü erneut zu öffnen!" })
                .apply {
                    if (e is ModalInteractionEvent) addComponents(
                        primary(
                            "reopen;$currentIndex",
                            "Menü erneut öffnen"
                        ).into()
                    )
                }.queue()
            return
        }
        options.run {
            if (dontSkip) return
            currentIndex += options.skipNextSteps + 1
        }
        if (steps.size == currentIndex) {
            DConfiguratorManager.removeConfigurator(userId, channelId)
            return
        }
    }

    val nextModal get() = (steps[nextIndex] as ModalStep).modalBuilder(nextIndex)


    fun WebhookMessageCreateAction<Message>.yesNoButtons() = nextIndex.let {
        addComponents(ActionRow.of(success("$it$name;yes", "Ja"), danger("$it$name;no", "Nein")))
    }

    operator fun ModalInteractionEvent.get(s: String) = getValue(s)!!.asString
    operator fun ModalInteractionEvent.invoke(s: String) = getValue(s)?.asString

    val ButtonInteractionEvent.isYes get() = componentId.endsWith("yes")

    inline fun <reified T : Any> step(noinline action: suspend T.(StepOptions) -> Unit) = Step(T::class, action)
}

@Suppress("UNCHECKED_CAST")
open class Step<T : Any>(private val neededEvent: KClass<T>, private val action: suspend T.(StepOptions) -> Unit) {
    suspend fun handle(e: GenericInteractionCreateEvent, options: StepOptions): Unit? {
        return if (neededEvent.isInstance(e)) {
            (e as T).action(options)
        } else null
    }
}

data class StepOptions(
    var dontSkip: Boolean = false,
    var skipNextSteps: Int = 0
) {
    fun dontSkip() {
        dontSkip = true
    }
}

class ModalStep(val modalBuilder: (Int) -> Modal, action: suspend ModalInteractionEvent.(StepOptions) -> Unit) :
    Step<ModalInteractionEvent>(ModalInteractionEvent::class, action)

class InvalidArgumentException(override val message: String) : Exception(message)

abstract class DGuildConfigurator(name: String, userId: Long, channelId: Long, val guildId: Long) :
    DConfigurator(name, userId, channelId)
