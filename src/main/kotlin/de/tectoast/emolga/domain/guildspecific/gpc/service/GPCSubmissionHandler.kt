package de.tectoast.emolga.domain.guildspecific.gpc.service

interface GPCSubmissionHandler {
    suspend fun handle(
        uid: Long,
        catId: Long,
        name: String,
        docUrl: String,
        metaInfos: String,
        otherInfos: String
    ): String
}