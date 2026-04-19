package de.tectoast.emolga.di

import de.tectoast.emolga.utils.json.Tokens
import io.r2dbc.spi.ConnectionFactoryOptions.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module(includes = [ConfigModule::class])
class DatabaseModule {

    @Single
    fun postgres(cred: Tokens.Database): R2dbcDatabase = R2dbcDatabase.connect {
        connectionFactoryOptions {
            option(DRIVER, "pool")
            option(PROTOCOL, "postgresql")
            option(HOST, cred.host)
            option(PORT, cred.port)
            option(USER, cred.username)
            option(PASSWORD, cred.password)
            option(DATABASE, "emolga")
        }
    }
}

interface TransactionRunner {
    suspend operator fun <T> invoke(block: suspend () -> T): T
}
@Single
class ExposedTransactionRunner(private val db: R2dbcDatabase) : TransactionRunner {
    override suspend fun <T> invoke(block: suspend () -> T): T = suspendTransaction(db) {
        block()
    }
}