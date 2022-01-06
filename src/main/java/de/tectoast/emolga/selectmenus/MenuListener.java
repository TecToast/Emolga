package de.tectoast.emolga.selectmenus;

import com.google.common.reflect.ClassPath;
import de.tectoast.emolga.bot.EmolgaListener;
import de.tectoast.emolga.commands.Command;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;

public abstract class MenuListener {

    private static final Logger logger = LoggerFactory.getLogger(MenuListener.class);

    public static final HashMap<String, MenuListener> listener = new HashMap<>();
    final String name;
    private static final MenuListener NULL = new MenuListener("NULL") {
        @Override
        public void process(SelectionMenuEvent e) {
            Command.sendToMe("WRONG MENU KEY " + e.getComponentId());
        }
    };

    public MenuListener(String name) {
        this.name = name;
        listener.put(name, this);
    }

    public static void check(SelectionMenuEvent e) {
        logger.info("e.getComponentId() = " + e.getComponentId());
        String id = e.getComponentId();
        listener.getOrDefault(id, NULL).process(e);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void init() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            for (ClassPath.ClassInfo classInfo : ClassPath.from(loader).getTopLevelClassesRecursive("de.tectoast.emolga.selectmenus")) {
                Class<?> cl = classInfo.load();
                if (cl.getSuperclass().getSimpleName().endsWith("MenuListener") && !Modifier.isAbstract(cl.getModifiers())) {
                    //logger.info(classInfo.getName());
                    cl.getConstructors()[0].newInstance();
                }
            }
        } catch (IOException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public abstract void process(SelectionMenuEvent e);
}
