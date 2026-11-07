package ir.sarejam.core.database

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context, keyManager: DatabaseKeyManager): SarejamDatabase {
        val key = keyManager.getOrCreateKey()
        return try {
            SarejamDatabase.open(context, key)
        } finally {
            key.fill(0)
        }
    }

    @Provides fun provideAccountDao(database: SarejamDatabase): AccountDao = database.accountDao()
    @Provides fun provideTransactionDao(database: SarejamDatabase): TransactionDao = database.transactionDao()
    @Provides fun provideCategoryDao(database: SarejamDatabase): CategoryDao = database.categoryDao()
    @Provides fun provideRawMessageDao(database: SarejamDatabase): RawMessageDao = database.rawMessageDao()
}
