package de.tectoast.emolga.features.interaction

/**
 * Additional data that is delivered with the interaction event. This is used to avoid having to access Discord API objects if we already have the data.
 */
data class AdditionalDeliveredData(
    /**
     * The roles of the member that triggered the interaction.
     */
    val memberRoles: List<Long> = emptyList(),
    /**
     * The associated message id of the interaction. This is only available for component interactions.
     */
    val messageId: Long? = null,
    /**
     * The voice channel id of the member that triggered the interaction if the member is in a voice channel.
     */
    val voiceChannel: Long? = null,
    /**
     * True if the member that triggered the interaction is an admin on the guild, false otherwise.
     */
    val adminOnGuild: Boolean = false,
)