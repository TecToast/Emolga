package de.tectoast.emolga.modals

import com.google.common.reflect.ClassPath
import de.tectoast.emolga.commands.Command
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import org.slf4j.LoggerFactory
import java.lang.reflect.Modifier

abstract class ModalListener(name: String) {
    init {
        listener[name] = this
    }

    abstract suspend fun process(e: ModalInteractionEvent, name: String?)

    companion object {
        val listener = HashMap<String, ModalListener>()
        private val logger = LoggerFactory.getLogger(ModalListener::class.java)
        private val NULL: ModalListener = object : ModalListener("NULL") {
            override suspend fun process(e: ModalInteractionEvent, name: String?) {
                if (!e.modalId.first().isDigit())
                    Command.sendToMe("WRONG MODAL KEY " + e.modalId)
            }
        }

        suspend fun check(e: ModalInteractionEvent) {
            logger.info("e.getModalId() = " + e.modalId)
            val id = e.modalId
            val split = id.split(";")
            val noArgs = split.size == 1
            val str = if (noArgs) id else split[0]
            listener.getOrDefault(str, NULL).process(e, if (noArgs) null else split[1])
        }


        fun init() {
            val loader = Thread.currentThread().contextClassLoader
            for (classInfo in ClassPath.from(loader).getTopLevelClassesRecursive("de.tectoast.emolga.modals")) {
                val cl = classInfo.load()
                if (cl.superclass.simpleName.endsWith("ModalListener") && !Modifier.isAbstract(cl.modifiers)) {
                    cl.kotlin.objectInstance
                }
            }
        }
    }
}
