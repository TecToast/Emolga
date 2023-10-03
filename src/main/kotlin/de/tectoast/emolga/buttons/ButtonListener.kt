package de.tectoast.emolga.buttons

import com.google.common.reflect.ClassPath
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.isNotFlo
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import org.slf4j.LoggerFactory
import java.lang.reflect.Modifier

abstract class ButtonListener(name: String) {
    init {
        if (name !in disabledListeners)
        listener[name] = this
    }

    @Throws(Exception::class)
    abstract suspend fun process(e: ButtonInteractionEvent, name: String)

    companion object {
        val listener: MutableMap<String, ButtonListener> = HashMap()
        val disabledListeners = setOf("reopen")
        private val logger = LoggerFactory.getLogger(ButtonListener::class.java)
        private val NULL: ButtonListener = object : ButtonListener("NULL") {
            override suspend fun process(e: ButtonInteractionEvent, name: String) {
                if (!e.componentId.first().isDigit() && disabledListeners.none { e.componentId.startsWith(it) })
                    Command.sendToMe("WRONG BUTTON KEY " + e.componentId)
            }
        }

        suspend fun check(e: ButtonInteractionEvent) {
            logger.info("e.getComponentId() = {}", e.componentId)
            if (Command.BOT_DISABLED && e.user.isNotFlo) e.reply(Command.DISABLED_TEXT).setEphemeral(true).queue()
            e.componentId.split(";").let {
                listener.getOrDefault(it[0], NULL).process(e, it.drop(1).joinToString(";"))
            }
        }

        fun init() {
            val loader = Thread.currentThread().contextClassLoader
            for (classInfo in ClassPath.from(loader).getTopLevelClassesRecursive("de.tectoast.emolga.buttons")) {
                val cl = classInfo.load()
                if (cl.superclass.simpleName.endsWith("ButtonListener") && !Modifier.isAbstract(cl.modifiers)) {
                    cl.kotlin.objectInstance
                }
            }
        }
    }
}
