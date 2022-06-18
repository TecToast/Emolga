package de.tectoast.emolga.modals;

import com.google.common.reflect.ClassPath;
import de.tectoast.emolga.commands.Command;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;

public abstract class ModalListener {

    public static final HashMap<String, ModalListener> listener = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(ModalListener.class);
    private static final ModalListener NULL = new ModalListener("NULL") {
        @Override
        public void process(ModalInteractionEvent e, String name) {
            Command.sendToMe("WRONG MODAL KEY " + e.getModalId());
        }
    };

    public ModalListener(String name) {
        listener.put(name, this);
    }

    public static void check(ModalInteractionEvent e) {
        logger.info("e.getModalId() = " + e.getModalId());
        String id = e.getModalId();
        String[] split = id.split(";");
        boolean noArgs = split.length == 1;
        String str = noArgs ? id : split[0];
        listener.getOrDefault(str, NULL).process(e, noArgs ? null : split[1]);
    }

    public static void init() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            for (ClassPath.ClassInfo classInfo : ClassPath.from(loader).getTopLevelClassesRecursive("de.tectoast.emolga.modals")) {
                Class<?> cl = classInfo.load();
                if (cl.getSuperclass().getSimpleName().endsWith("ModalListener") && !Modifier.isAbstract(cl.getModifiers())) {
                    //logger.info(classInfo.getName());
                    cl.getConstructors()[0].newInstance();
                }
            }
        } catch (IOException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public abstract void process(ModalInteractionEvent e, String name);
}
