package de.tectoast.emolga.utils.json

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.litote.kmongo.json

class EmolgaConfigHelperTest : FunSpec({
    test("valid update") {
        mockkObject(DiscordEntityValidator)
        every { DiscordEntityValidator.validateChannelId(any()) } returns true
        EmolgaConfigHelper.parseRemoteDelta(
            LigaStartConfig.serializer().descriptor,
            buildJsonObject {
                put("signupChannel", "1130465632031363173")
                putJsonObject("signupStructure") {
                    putJsonObject("2") {
                        put("type", "OfList")
                        put("name", "Regionlol")
                    }
                }
            }
        ).shouldNotBeNull {
            this.json shouldBe $$"""{"$set": {"signupChannel": 1130465632031363173, "signupStructure.2.name": "Regionlol"}}"""
        }
    }
})