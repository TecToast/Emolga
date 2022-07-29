package de.tectoast.emolga.commands

abstract class PepeCommand(name: String, help: String) : Command(name, help, CommandCategory.Pepe) {
    init {
        allowedBotId = 849569577343385601
    }
}