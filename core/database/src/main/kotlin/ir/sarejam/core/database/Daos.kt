package ir.sarejam.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// INSERT uses ABORT, never REPLACE: a collision must not silently delete history or related rows.
@Dao
interface AccountDao {
    @Insert suspend fun insert(account: AccountEntity)
    @Update suspend fun update(account: AccountEntity)
    @Delete suspend fun delete(account: AccountEntity)
    @Query("SELECT * FROM accounts WHERE id = :id") suspend fun findById(id: String): AccountEntity?
    @Query("SELECT * FROM accounts ORDER BY createdAt, id") suspend fun listAll(): List<AccountEntity>
    @Query("SELECT * FROM accounts ORDER BY createdAt, id") fun observeAll(): Flow<List<AccountEntity>>
    @Query("SELECT * FROM accounts WHERE archived = 0 ORDER BY createdAt, id") fun observeActive(): Flow<List<AccountEntity>>
    @Query("UPDATE accounts SET archived = :archived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long)
    @Query("SELECT * FROM accounts WHERE bankId IS :bankId AND accountRef IS :accountRef AND sourceSender IS :sourceSender AND subscriptionId IS :subscriptionId ORDER BY createdAt, id")
    suspend fun findBySource(bankId: String?, accountRef: String?, sourceSender: String?, subscriptionId: Int?): List<AccountEntity>
}

@Dao
interface TransactionDao {
    @Insert suspend fun insert(transaction: TransactionEntity)
    @Update suspend fun update(transaction: TransactionEntity)
    @Delete suspend fun delete(transaction: TransactionEntity)
    @Query("SELECT * FROM transactions WHERE id = :id") suspend fun findById(id: String): TransactionEntity?
    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC, id") suspend fun listAll(): List<TransactionEntity>
    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC, id") fun observeAll(): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY occurredAt DESC, id")
    suspend fun listForAccount(accountId: String): List<TransactionEntity>
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY occurredAt DESC, id")
    fun observeForAccount(accountId: String): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE transferGroupId = :transferGroupId ORDER BY occurredAt, id")
    suspend fun listForTransferGroup(transferGroupId: String): List<TransactionEntity>
}

@Dao
interface CategoryDao {
    @Insert suspend fun insert(category: CategoryEntity)
    @Update suspend fun update(category: CategoryEntity)
    @Delete suspend fun delete(category: CategoryEntity)
    @Query("SELECT * FROM categories WHERE id = :id") suspend fun findById(id: String): CategoryEntity?
    @Query("SELECT * FROM categories ORDER BY name, id") suspend fun listAll(): List<CategoryEntity>
    @Query("SELECT * FROM categories ORDER BY name, id") fun observeAll(): Flow<List<CategoryEntity>>
}

@Dao
interface RawMessageDao {
    @Insert suspend fun insert(message: RawMessageEntity)
    @Update suspend fun update(message: RawMessageEntity)
    @Delete suspend fun delete(message: RawMessageEntity)
    @Query("SELECT * FROM raw_messages WHERE id = :id") suspend fun findById(id: String): RawMessageEntity?
    @Query("SELECT * FROM raw_messages WHERE sourceHash = :sourceHash") suspend fun findBySourceHash(sourceHash: String): RawMessageEntity?
    @Query("SELECT * FROM raw_messages ORDER BY receivedAt DESC, id") suspend fun listAll(): List<RawMessageEntity>
    @Query("SELECT * FROM raw_messages ORDER BY receivedAt DESC, id") fun observeAll(): Flow<List<RawMessageEntity>>
    @Query("SELECT * FROM raw_messages WHERE resultType = 'NEEDS_REVIEW' ORDER BY receivedAt, id")
    suspend fun listReviewQueue(): List<RawMessageEntity>
    @Query("SELECT * FROM raw_messages WHERE resultType = 'NEEDS_REVIEW' ORDER BY receivedAt, id")
    fun observeReviewQueue(): Flow<List<RawMessageEntity>>
}
