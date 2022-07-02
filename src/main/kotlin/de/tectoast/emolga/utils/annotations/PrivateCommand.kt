package de.tectoast.emolga.utils.annotations

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@Retention(RetentionPolicy.RUNTIME)
annotation class PrivateCommand(val name: String, val aliases: Array<String> = [])