package de.tectoast.emolga.selectmenus

import com.google.common.reflect.ClassPath
import de.tectoast.emolga.commands.Command
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import org.slf4j.LoggerFactory
import java.lang.reflect.Modifier

abstract class MenuListener(name: String) {
    init {
        listener[name] = this
    }

    abstract suspend fun process(e: StringSelectInteractionEvent, menuname: String?)

    companion object {
        val listener = HashMap<String, MenuListener>()
        private val logger = LoggerFactory.getLogger(MenuListener::class.java)
        private val NULL: MenuListener = object : MenuListener("NULL") {
            override suspend fun process(e: StringSelectInteractionEvent, menuname: String?) {
                if (!e.componentId.first().isDigit())
                    Command.sendToMe("WRONG MENU KEY " + e.componentId)
            }
        }

        suspend fun check(e: StringSelectInteractionEvent) {
            logger.info("e.getComponentId() = " + e.componentId)
            e.componentId.split(";").let {
                listener.getOrDefault(it[0], NULL).process(e, it.getOrNull(1))
            }
        }


        fun init() {
            val loader = Thread.currentThread().contextClassLoader
            for (classInfo in ClassPath.from(loader)
                .getTopLevelClassesRecursive("de.tectoast.emolga.selectmenus")) {
                val cl = classInfo.load()
                if (cl.superclass.simpleName.endsWith("MenuListener") && !Modifier.isAbstract(cl.modifiers)) {
                    //logger.info(classInfo.getName());
                    cl.constructors[0].newInstance()
                }
            }

        }
    }
}
