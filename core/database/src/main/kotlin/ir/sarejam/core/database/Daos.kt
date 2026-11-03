package ir.sarejam.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert suspend fun insert(account: AccountEntity): Long
    @Query("SELECT * FROM account WHERE archived = 0 ORDER BY name") fun active(): Flow<List<AccountEntity>>
}

@Dao
interface CategoryDao {
    @Insert suspend fun insert(category: CategoryEntity): Long
    @Query("SELECT * FROM category ORDER BY name") fun all(): Flow<List<CategoryEntity>>
}

@Dao
interface TransactionDao {
    @Insert suspend fun insert(transaction: TransactionEntity): Long
    @Query("SELECT * FROM `transaction` WHERE accountId = :accountId ORDER BY occurredAt DESC")
    suspend fun forAccount(accountId: Long): List<TransactionEntity>
}

@Dao
interface RawMessageDao {
    @Insert suspend fun insert(message: RawMessageEntity): Long
    @Query("SELECT COUNT(*) FROM raw_message") suspend fun count(): Int
}
