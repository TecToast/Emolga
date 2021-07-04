package de.tectoast.emolga.buttons;

import com.google.common.reflect.ClassPath;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.LinkedList;

public abstract class ButtonListener {

    public static LinkedList<ButtonListener> listener = new LinkedList<>();
    String name;

    public ButtonListener(String name) {
        this.name = name;
        listener.add(this);
    }

    public static void check(ButtonClickEvent e) {
        for (ButtonListener l : listener) {
            String[] split = e.getComponentId().split(";");
            if(split[0].equals(l.name)) l.process(e, split[1]);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void init() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            for (ClassPath.ClassInfo classInfo : ClassPath.from(loader).getTopLevelClassesRecursive("de.tectoast.emolga.buttons")) {
                Class<?> cl = classInfo.load();
                if (cl.getSuperclass().getSimpleName().endsWith("ButtonListener") && !Modifier.isAbstract(cl.getModifiers())) {
                    //System.out.println(classInfo.getName());
                    cl.getConstructors()[0].newInstance();
                }
            }
        } catch (IOException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public abstract void process(ButtonClickEvent e, String name);
}
