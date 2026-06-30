package de.tectoast.emolga.domain.game.model.analysis


sealed class ShowdownException : Exception()
class ShowdownDoesNotAnswerException : ShowdownException()
class ShowdownParseException : ShowdownException()
class ShowdownDoesntExistException : ShowdownException()
class InvalidReplayException : ShowdownException()
