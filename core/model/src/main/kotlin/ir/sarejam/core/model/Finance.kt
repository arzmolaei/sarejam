package ir.sarejam.core.model

/** docs/ARCHITECTURE.md «مدل داده». Money is always whole rials in Long. */
enum class TransactionKind { EXPENSE, INCOME, TRANSFER, FEE, REFUND, REVERSAL, ADJUSTMENT, FAILED }

enum class MoneyDirection { IN, OUT }

enum class TransactionStatus { PENDING, CONFIRMED }

/**
 * Every automatic result (category, link, merge) is one of three: applied silently with Undo,
 * shown as a suggestion, or asked as a question. Only CERTAIN is applied without asking.
 */
enum class Decision { CERTAIN, SUGGESTION, UNCERTAIN }

enum class SourceType { SMS, NOTIFICATION, MANUAL, VOICE, PHOTO }

enum class AccountKind { BANK, CASH, WALLET, CREDIT, DEPOSIT }

enum class Space { PERSONAL, WORK }

/** Fields a user can correct by hand. Once edited, no reparse, rule or linker may overwrite them. */
enum class EditableField { AMOUNT, DIRECTION, KIND, OCCURRED_AT, ACCOUNT, CATEGORY, COUNTERPARTY, NOTE, BALANCE_AFTER }

/** The values of a transaction that parsing or the user can set. */
data class TransactionFields(
    val accountId: Long,
    val amount: Long,
    val direction: MoneyDirection,
    val kind: TransactionKind,
    val occurredAt: Long,
    val balanceAfter: Long?,
    val categoryId: Long?,
    val counterparty: String?,
    val note: String?,
) {
    init { require(amount >= 0) { "amount is always positive; direction tells the way" } }
}

object Reparse {
    /**
     * Result of reading the message again with a newer parser: takes fresh values only for fields
     * the user never edited (CLAUDE.md rule 10). Returns the merged fields and which ones changed,
     * so the app can show the change with Undo.
     */
    fun merge(current: TransactionFields, fresh: TransactionFields, userEdited: Set<EditableField>): Pair<TransactionFields, Set<EditableField>> {
        fun <T> pick(field: EditableField, old: T, new: T): T = if (field in userEdited) old else new
        val merged = TransactionFields(
            accountId = pick(EditableField.ACCOUNT, current.accountId, fresh.accountId),
            amount = pick(EditableField.AMOUNT, current.amount, fresh.amount),
            direction = pick(EditableField.DIRECTION, current.direction, fresh.direction),
            kind = pick(EditableField.KIND, current.kind, fresh.kind),
            occurredAt = pick(EditableField.OCCURRED_AT, current.occurredAt, fresh.occurredAt),
            balanceAfter = pick(EditableField.BALANCE_AFTER, current.balanceAfter, fresh.balanceAfter),
            // A parser never knows the category better than what is already there; categorizing is insight's job.
            categoryId = current.categoryId,
            counterparty = pick(EditableField.COUNTERPARTY, current.counterparty, fresh.counterparty),
            note = pick(EditableField.NOTE, current.note, fresh.note),
        )
        return merged to changedFields(current, merged)
    }

    fun changedFields(a: TransactionFields, b: TransactionFields): Set<EditableField> = buildSet {
        if (a.accountId != b.accountId) add(EditableField.ACCOUNT)
        if (a.amount != b.amount) add(EditableField.AMOUNT)
        if (a.direction != b.direction) add(EditableField.DIRECTION)
        if (a.kind != b.kind) add(EditableField.KIND)
        if (a.occurredAt != b.occurredAt) add(EditableField.OCCURRED_AT)
        if (a.balanceAfter != b.balanceAfter) add(EditableField.BALANCE_AFTER)
        if (a.categoryId != b.categoryId) add(EditableField.CATEGORY)
        if (a.counterparty != b.counterparty) add(EditableField.COUNTERPARTY)
        if (a.note != b.note) add(EditableField.NOTE)
    }
}

/** Totals for reports (docs/ARCHITECTURE.md «گزارش‌ها»). */
data class PeriodTotals(val expense: Long, val income: Long)

object Reports {
    /**
     * Expense = EXPENSE + FEE minus REFUND/REVERSAL. Income = INCOME.
     * TRANSFER, ADJUSTMENT and FAILED never count as expense or income.
     */
    fun totals(items: Iterable<Pair<TransactionKind, Long>>): PeriodTotals {
        var spent = 0L
        var backIn = 0L
        var income = 0L
        for ((kind, amount) in items) when (kind) {
            TransactionKind.EXPENSE, TransactionKind.FEE -> spent = Math.addExact(spent, amount)
            TransactionKind.REFUND, TransactionKind.REVERSAL -> backIn = Math.addExact(backIn, amount)
            TransactionKind.INCOME -> income = Math.addExact(income, amount)
            TransactionKind.TRANSFER, TransactionKind.ADJUSTMENT, TransactionKind.FAILED -> Unit
        }
        return PeriodTotals(expense = (spent - backIn).coerceAtLeast(0), income = income)
    }
}
