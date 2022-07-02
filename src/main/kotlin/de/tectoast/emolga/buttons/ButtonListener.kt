package de.tectoast.emolga.buttons

import com.google.common.reflect.ClassPath
import de.tectoast.emolga.commands.Command
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import org.slf4j.LoggerFactory
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier

abstract class ButtonListener(name: String) {
    init {
        listener[name] = this
    }

    @Throws(Exception::class)
    abstract fun process(e: ButtonInteractionEvent, name: String)

    companion object {
        val listener: MutableMap<String, ButtonListener> = HashMap()
        private val logger = LoggerFactory.getLogger(ButtonListener::class.java)
        private val NULL: ButtonListener = object : ButtonListener("NULL") {
            override fun process(e: ButtonInteractionEvent, name: String) {
                Command.sendToMe("WRONG BUTTON KEY " + e.componentId)
            }
        }

        @JvmStatic
        fun check(e: ButtonInteractionEvent) {
            logger.info("e.getComponentId() = {}", e.componentId)
            val split = e.componentId.split(";".toRegex())
            try {
                listener.getOrDefault(split[0], NULL).process(e, split[1])
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        @JvmStatic
        fun init() {
            val loader = Thread.currentThread().contextClassLoader
            try {
                for (classInfo in ClassPath.from(loader).getTopLevelClassesRecursive("de.tectoast.emolga.buttons")) {
                    val cl = classInfo.load()
                    if (cl.superclass.simpleName.endsWith("ButtonListener") && !Modifier.isAbstract(cl.modifiers)) {
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