package de.tectoast.emolga.di

import de.tectoast.emolga.utils.BotConfig
import io.r2dbc.spi.ConnectionFactoryOptions.*
import io.r2dbc.spi.IsolationLevel
import io.r2dbc.spi.Option
import org.jetbrains.exposed.v1.core.exposedLogger
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module(includes = [ConfigModule::class])
class DatabaseModule {

    @Single
    fun postgres(cred: BotConfig.Database): R2dbcDatabase = R2dbcDatabase.connect {
        connectionFactoryOptions {
            option(DRIVER, "pool")
            option(PROTOCOL, "postgresql")
            option(USER, cred.username)
            option(PASSWORD, cred.password)
            option(DATABASE, cred.database)
            when (cred) {
                is BotConfig.Database.Network -> {
                    option(HOST, cred.host)
                    option(PORT, cred.port)
                }

                is BotConfig.Database.Socket -> {
                    exposedLogger.warn("PATH: ${cred.path}")
                    option(Option.valueOf("socket"), cred.path)
                }
            }
        }
    }
}

interface TransactionRunner {
    suspend operator fun <T> invoke(serializableIsolation: Boolean = false, block: suspend () -> T): T
}

@Single
class ExposedTransactionRunner(private val db: R2dbcDatabase) : TransactionRunner {
    override suspend fun <T> invoke(serializableIsolation: Boolean, block: suspend () -> T): T = suspendTransaction(
        db,
        if (serializableIsolation) IsolationLevel.SERIALIZABLE else IsolationLevel.READ_COMMITTED
    ) {
        block()
    }
}