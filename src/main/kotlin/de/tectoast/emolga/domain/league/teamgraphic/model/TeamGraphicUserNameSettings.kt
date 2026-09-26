package de.tectoast.emolga.domain.league.teamgraphic.model

import kotlinx.serialization.Serializable

@Serializable
data class TeamGraphicUserNameSettings(
    val removeStrings: Set<String> = emptySet(),
    val replaceStrings: Map<String, String> = mapOf()
) {
    fun formatUserName(userName: String): String {
        var formattedName = userName
        for (removeString in removeStrings) {
            formattedName = formattedName.replace(Regex(removeString), "")
        }
        for ((key, value) in replaceStrings) {
            formattedName = formattedName.replace(Regex(key), value)
        }
        return formattedName
    }
}