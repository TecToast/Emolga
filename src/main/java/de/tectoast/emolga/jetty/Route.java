package de.tectoast.emolga.jetty;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Route {
    String route() default "/";
    String method() default "GET";
    boolean needsCookie() default true;
}
