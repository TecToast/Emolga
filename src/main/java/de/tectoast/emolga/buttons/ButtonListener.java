package de.tectoast.emolga.buttons;

import com.google.common.reflect.ClassPath;
import de.tectoast.emolga.commands.Command;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;

public abstract class ButtonListener {
    public static final HashMap<String, ButtonListener> listener = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(ButtonListener.class);
    private static final ButtonListener NULL = new ButtonListener("NULL") {
        @Override
        public void process(ButtonClickEvent e, String name) {
            Command.sendToMe("WRONG BUTTON KEY " + e.getComponentId());
        }
    };
    final String name;

    public ButtonListener(String name) {
        this.name = name;
        listener.put(name, this);
    }

    public static void check(ButtonClickEvent e) {
        logger.info("e.getComponentId() = {}", e.getComponentId());
        String[] split = e.getComponentId().split(";");
        listener.getOrDefault(split[0], NULL).process(e, split[1]);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void init() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            for (ClassPath.ClassInfo classInfo : ClassPath.from(loader).getTopLevelClassesRecursive("de.tectoast.emolga.buttons")) {
                Class<?> cl = classInfo.load();
                if (cl.getSuperclass().getSimpleName().endsWith("ButtonListener") && !Modifier.isAbstract(cl.getModifiers())) {
                    //logger.info(classInfo.getName());
                    cl.getConstructors()[0].newInstance();
                }
            }
        } catch (IOException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public abstract void process(ButtonClickEvent e, String name);
}
