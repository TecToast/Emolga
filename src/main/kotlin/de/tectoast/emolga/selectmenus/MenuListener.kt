package de.tectoast.emolga.selectmenus

import com.google.common.reflect.ClassPath
import de.tectoast.emolga.commands.Command
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import org.slf4j.LoggerFactory
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier

abstract class MenuListener(name: String) {
    init {
        listener[name] = this
    }

    abstract fun process(e: StringSelectInteractionEvent, menuname: String?)

    companion object {
        val listener = HashMap<String, MenuListener>()
        private val logger = LoggerFactory.getLogger(MenuListener::class.java)
        private val NULL: MenuListener = object : MenuListener("NULL") {
            override fun process(e: StringSelectInteractionEvent, menuname: String?) {
                Command.sendToMe("WRONG MENU KEY " + e.componentId)
            }
        }

        fun check(e: StringSelectInteractionEvent) {
            logger.info("e.getComponentId() = " + e.componentId)
            val id = e.componentId
            val split = id.split(";")
            val noArgs = split.size == 1
            val str = if (noArgs) id else split[0]
            listener.getOrDefault(str, NULL).process(e, if (noArgs) null else split[1])
        }


        fun init() {
            val loader = Thread.currentThread().contextClassLoader
            try {
                for (classInfo in ClassPath.from(loader)
                    .getTopLevelClassesRecursive("de.tectoast.emolga.selectmenus")) {
                    val cl = classInfo.load()
                    if (cl.superclass.simpleName.endsWith("MenuListener") && !Modifier.isAbstract(cl.modifiers)) {
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
