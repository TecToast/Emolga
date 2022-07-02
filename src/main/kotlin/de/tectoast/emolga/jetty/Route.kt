package de.tectoast.emolga.jetty

@Retention(AnnotationRetention.RUNTIME)
annotation class Route(val route: String = "/", val method: String = "GET", val needsCookie: Boolean = true)