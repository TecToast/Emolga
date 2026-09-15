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
            formattedName = formattedName.replace(removeString, "")
        }
        for ((key, value) in replaceStrings) {
            formattedName = formattedName.replace(key, value)
        }
        return formattedName
    }
}