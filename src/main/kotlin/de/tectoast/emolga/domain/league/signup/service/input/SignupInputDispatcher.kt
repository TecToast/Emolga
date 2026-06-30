package de.tectoast.emolga.domain.league.signup.service.input

import de.tectoast.emolga.domain.league.signup.model.SignupInput
import de.tectoast.emolga.utils.handler.HandlerRegistry
import org.koin.core.annotation.Single

@Single
class SignupInputDispatcher(handlers: List<SignupInputHandler<SignupInput>>) : SignupInputOperations<SignupInput> {
    private val registry = HandlerRegistry(handlers)

    override fun getModalInputOptions(config: SignupInput) = registry.getHandler(config).getModalInputOptions(config)

    override suspend fun validate(
        config: SignupInput, data: String
    ) = registry.getHandler(config).validate(config, data)

    override fun getDisplayTitle(config: SignupInput) = registry.getHandler(config).getDisplayTitle(config)

    override fun mapValueForDisplay(
        config: SignupInput, data: String
    ) = registry.getHandler(config).mapValueForDisplay(config, data)
}