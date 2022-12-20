package de.tectoast.emolga.utils.annotations


@Retention
annotation class PrivateCommand(val name: String, val aliases: Array<String> = [], val ack: Boolean = false)
