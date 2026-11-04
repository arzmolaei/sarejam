package ir.sarejam.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ir.sarejam.core.model.AccountKind
import ir.sarejam.core.model.Decision
import ir.sarejam.core.model.EditableField
import ir.sarejam.core.model.MoneyDirection
import ir.sarejam.core.model.SourceType
import ir.sarejam.core.model.Space
import ir.sarejam.core.model.TransactionFields
import ir.sarejam.core.model.TransactionKind
import ir.sarejam.core.model.TransactionStatus

/** Money columns are whole rials in Long (CLAUDE.md rule 8). Times are epoch millis. */

@Entity(tableName = "account")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: AccountKind,
    val bank: String?,
    val name: String,
    val last4: String?,
    val space: Space = Space.PERSONAL,
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
    indices = [Index("accountId"), Index("categoryId"), Index("occurredAt"), Index("transferGroup"), Index("feeOfId"), Index("reversalOfId")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    /** Always positive; direction says which way. */
    val amount: Long,
    val direction: MoneyDirection,
    val occurredAt: Long,
    val balanceAfter: Long?,
    val kind: TransactionKind,
    val categoryId: Long?,
    val counterparty: String?,
    val note: String?,
    @ColumnInfo(defaultValue = "1") val parserVersion: Int = 1,
    val createdAt: Long,
    val updatedAt: Long,
    // --- version 2 ---
    @ColumnInfo(defaultValue = "CONFIRMED") val status: TransactionStatus = TransactionStatus.CONFIRMED,
    /** Shared by the two legs of a transfer between the user's own accounts. */
    val transferGroup: String? = null,
    /** This fee belongs to that transaction. */
    val feeOfId: Long? = null,
    /** This refund or reversal undoes that transaction. */
    val reversalOfId: Long? = null,
    /** Fields the user changed by hand; never overwritten automatically (CLAUDE.md rule 10). */
    @ColumnInfo(defaultValue = "") val userEditedFields: Set<EditableField> = emptySet(),
    /** How sure the automatic reading/categorizing was. */
    @ColumnInfo(defaultValue = "CERTAIN") val decision: Decision = Decision.CERTAIN,
    /** Human «چرا؟» reason shown under the automatic result. */
    val reason: String? = null,
    /** 0–100. Not money. */
    @ColumnInfo(defaultValue = "100") val confidence: Int = 100,
) {
    fun fields() = TransactionFields(accountId, amount, direction, kind, occurredAt, balanceAfter, categoryId, counterparty, note)

    fun withFields(f: TransactionFields, now: Long) = copy(
        accountId = f.accountId, amount = f.amount, direction = f.direction, kind = f.kind, occurredAt = f.occurredAt,
        balanceAfter = f.balanceAfter, categoryId = f.categoryId, counterparty = f.counterparty, note = f.note, updatedAt = now,
    )
}

/** The fingerprint of one input (SMS, notification, manual entry) behind a transaction. */
@Entity(
    tableName = "source",
    foreignKeys = [ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index(value = ["textHash"], unique = true), Index("accountId"), Index("receivedAt")],
)
data class SourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: SourceType,
    /** SMS sender or notification package. */
    val sender: String,
    /** SIM slot identity, so two SIMs of the same bank never mix. */
    val subscriptionId: Int?,
    /** Hash of the normalized text: the same SMS twice is one source. */
    val textHash: String,
    val receivedAt: Long,
    val notificationKey: String?,
    val accountId: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "transaction_source",
    primaryKeys = ["transactionId", "sourceId"],
    foreignKeys = [
        ForeignKey(entity = TransactionEntity::class, parentColumns = ["id"], childColumns = ["transactionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = SourceEntity::class, parentColumns = ["id"], childColumns = ["sourceId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("sourceId")],
)
data class TransactionSourceEntity(val transactionId: Long, val sourceId: Long)

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
    // --- version 2 ---
    val subscriptionId: Int? = null,
)
