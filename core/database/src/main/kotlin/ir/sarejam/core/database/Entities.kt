package ir.sarejam.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Money columns are whole rials in Long (CLAUDE.md rule 8). Times are epoch millis. */

@Entity(tableName = "account")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** BANK, CASH, WALLET, CREDIT, DEPOSIT */
    val kind: String,
    val bank: String?,
    val name: String,
    val last4: String?,
    /** PERSONAL or WORK */
    val space: String = "PERSONAL",
    val currency: String = "IRR",
    val lastBalance: Long?,
    val archived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "category",
    foreignKeys = [ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["parentId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("parentId")],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String?,
    val color: Long?,
    /** Two levels at most. */
    val parentId: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "transaction",
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("accountId"), Index("categoryId"), Index("occurredAt")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    /** Always positive; direction says which way. */
    val amount: Long,
    /** IN or OUT */
    val direction: String,
    val occurredAt: Long,
    val balanceAfter: Long?,
    /** EXPENSE, INCOME, TRANSFER, FEE, REFUND, REVERSAL, ADJUSTMENT, FAILED */
    val kind: String,
    val categoryId: Long?,
    val counterparty: String?,
    val note: String?,
    @ColumnInfo(defaultValue = "1") val parserVersion: Int = 1,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Never written for OTP or personal messages (CLAUDE.md rules 3 and 4). */
@Entity(tableName = "raw_message", indices = [Index("receivedAt"), Index(value = ["textHash"], unique = true)])
data class RawMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val body: String,
    val textHash: String,
    val receivedAt: Long,
    /** TRANSACTION, NEEDS_REVIEW, IGNORED */
    val parseResult: String,
    val parserVersion: Int,
    val createdAt: Long,
)
