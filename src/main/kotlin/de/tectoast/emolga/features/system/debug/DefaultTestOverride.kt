package de.tectoast.emolga.features.system.debug

import org.koin.core.annotation.Single

@Single
class DefaultTestOverride : TestOverride {
    override val user: Long? = null
    override val tc: Long? = null
    override val gid: Long? = null
}