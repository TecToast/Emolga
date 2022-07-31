package de.tectoast.emolga.modals

import com.google.common.reflect.ClassPath
import de.tectoast.emolga.commands.Command
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import org.slf4j.LoggerFactory
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier

abstract class ModalListener(name: String) {
    init {
        listener[name] = this
    }

    abstract fun process(e: ModalInteractionEvent, name: String?)

    companion object {
        val listener = HashMap<String, ModalListener>()
        private val logger = LoggerFactory.getLogger(ModalListener::class.java)
        private val NULL: ModalListener = object : ModalListener("NULL") {
            override fun process(e: ModalInteractionEvent, name: String?) {
                Command.sendToMe("WRONG MODAL KEY " + e.modalId)
            }
        }

        fun check(e: ModalInteractionEvent) {
            logger.info("e.getModalId() = " + e.modalId)
            val id = e.modalId
            val split = id.split(";")
            val noArgs = split.size == 1
            val str = if (noArgs) id else split[0]
            listener.getOrDefault(str, NULL).process(e, if (noArgs) null else split[1])
        }

        @JvmStatic
        fun init() {
            val loader = Thread.currentThread().contextClassLoader
            try {
                for (classInfo in ClassPath.from(loader).getTopLevelClassesRecursive("de.tectoast.emolga.modals")) {
                    val cl = classInfo.load()
                    if (cl.superclass.simpleName.endsWith("ModalListener") && !Modifier.isAbstract(cl.modifiers)) {
                        //logger.info(classInfo.getName());
                        cl.constructors[0].newInstance()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: InstantiationException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            }
        }
    }
}