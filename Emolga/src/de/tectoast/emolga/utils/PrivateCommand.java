package de.tectoast.emolga.utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface PrivateCommand {
    String name();

    String[] aliases() default {};
}
