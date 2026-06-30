package de.tectoast.emolga.domain.statestore.service

import de.tectoast.emolga.domain.statestore.model.StateStore
import de.tectoast.emolga.utils.handler.BaseHandler

interface StateStoreHandler<T : StateStore> : BaseHandler<T>