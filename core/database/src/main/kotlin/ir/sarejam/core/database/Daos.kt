package ir.sarejam.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import ir.sarejam.core.model.TransactionKind
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert suspend fun insert(account: AccountEntity): Long
    @Query("SELECT * FROM account WHERE archived = 0 ORDER BY name") fun active(): Flow<List<AccountEntity>>
    @Query("SELECT * FROM account WHERE id = :id") suspend fun byId(id: Long): AccountEntity?
    @Query("SELECT * FROM account WHERE bank = :bank AND ((:last4 IS NULL AND last4 IS NULL) OR last4 = :last4) AND archived = 0 LIMIT 1")
    suspend fun find(bank: String, last4: String?): AccountEntity?
    @Query("UPDATE account SET lastBalance = :balance, updatedAt = :now WHERE id = :id")
    suspend fun updateBalance(id: Long, balance: Long, now: Long)
}

@Dao
interface CategoryDao {
    @Insert suspend fun insert(category: CategoryEntity): Long
    @Query("SELECT * FROM category ORDER BY name") fun all(): Flow<List<CategoryEntity>>
}

@Dao
interface TransactionDao {
    @Insert suspend fun insert(transaction: TransactionEntity): Long
    @Update suspend fun update(transaction: TransactionEntity)
    @Query("SELECT * FROM `transaction` WHERE id = :id") suspend fun byId(id: Long): TransactionEntity?
    @Query("SELECT COUNT(*) FROM `transaction`") suspend fun count(): Int
    @Query("SELECT * FROM `transaction` ORDER BY occurredAt DESC LIMIT :limit") fun latest(limit: Int): Flow<List<TransactionEntity>>

    /** Kinds and amounts in a time window, for Reports.totals. */
    @Query("SELECT kind, amount FROM `transaction` WHERE occurredAt >= :from AND occurredAt < :to AND status = 'CONFIRMED'")
    suspend fun kindAmounts(from: Long, to: Long): List<KindAmount>
    @Query("SELECT * FROM `transaction` WHERE accountId = :accountId ORDER BY occurredAt DESC")
    suspend fun forAccount(accountId: Long): List<TransactionEntity>
}

@Dao
interface RawMessageDao {
    @Insert suspend fun insert(message: RawMessageEntity): Long
    @Query("SELECT COUNT(*) FROM raw_message") suspend fun count(): Int
    @Query("SELECT * FROM raw_message WHERE textHash = :hash") suspend fun byHash(hash: String): RawMessageEntity?
}

data class KindAmount(val kind: TransactionKind, val amount: Long)

@Dao
interface SourceDao {
    /** Returns -1 when the same input (same text hash) was already stored: dedup. */
    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE) suspend fun insert(source: SourceEntity): Long
    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE) suspend fun link(link: TransactionSourceEntity)
    @Query("SELECT * FROM source WHERE textHash = :hash") suspend fun byHash(hash: String): SourceEntity?
}
