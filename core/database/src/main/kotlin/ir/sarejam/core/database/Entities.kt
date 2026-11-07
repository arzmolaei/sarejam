package ir.sarejam.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class AccountKind { BANK, CASH, EWALLET, CREDIT, SAVINGS, FOREIGN }
enum class AccountSpace { PERSONAL, WORK }
enum class TransactionDirection { CREDIT, DEBIT }
enum class TransactionKind { EXPENSE, INCOME, TRANSFER, FEE, REFUND, REVERSAL, ADJUSTMENT, FAILED }
enum class DecisionState { CERTAIN, SUGGESTED, UNKNOWN }

/** Only retained bank-message classifications belong here; OTP and personal messages have no value. */
enum class RawMessageResult { TRANSACTION, NEEDS_REVIEW, IGNORED }

@Entity(tableName = "accounts", indices = [Index(value = ["bankId", "accountRef", "sourceSender", "subscriptionId"])])
data class AccountEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val bankId: String? = null,
    val name: String,
    val accountRef: String? = null,
    val sourceSender: String? = null,
    val subscriptionId: Int? = null,
    val kind: AccountKind,
    @ColumnInfo(defaultValue = "'IRR'") val currency: String = "IRR",
    val space: AccountSpace,
    val balanceRial: Long? = null,
    @ColumnInfo(defaultValue = "0") val archived: Boolean = false,
    @ColumnInfo(defaultValue = "1") val includedInSpendable: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = TransactionEntity::class, parentColumns = ["id"], childColumns = ["relatedTransactionId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index(value = ["accountId", "occurredAt"]), Index("categoryId"), Index("relatedTransactionId"), Index("transferGroupId")],
)
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val accountId: String,
    val amountRial: Long,
    val direction: TransactionDirection,
    val kind: TransactionKind,
    val occurredAt: Long,
    val balanceAfter: Long? = null,
    val categoryId: String? = null,
    val counterpartyCard: String? = null,
    val note: String? = null,
    val transferGroupId: String? = null,
    val relatedTransactionId: String? = null,
    val parserVersion: Int,
    @ColumnInfo(defaultValue = "'[]'") val userEditedFields: Set<String> = emptySet(),
    val decisionState: DecisionState,
    val reason: String? = null,
    val confidence: Double,
    val createdAt: Long,
    val updatedAt: Long,
) {
    init {
        require(amountRial >= 0) { "A transaction amount must be nonnegative integer rials." }
    }
}

@Entity(
    tableName = "categories",
    foreignKeys = [ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["parentId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("parentId")],
)
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String,
    val color: Long,
    val parentId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Callers must classify bank messages first. Never construct this from OTP or personal messages. */
@Entity(
    tableName = "raw_messages",
    indices = [Index(value = ["sourceHash"], unique = true), Index(value = ["resultType", "receivedAt"])],
)
data class RawMessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sender: String,
    val body: String,
    val receivedAt: Long,
    val subscriptionId: Int? = null,
    val resultType: RawMessageResult,
    val bankId: String? = null,
    val parserVersion: Int,
    val reviewReason: String? = null,
    val sourceHash: String,
    val createdAt: Long,
    val updatedAt: Long,
)
