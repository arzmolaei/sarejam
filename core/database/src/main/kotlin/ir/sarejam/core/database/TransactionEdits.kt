package ir.sarejam.core.database

import androidx.room.withTransaction
import ir.sarejam.core.model.EditableField
import ir.sarejam.core.model.PeriodTotals
import ir.sarejam.core.model.Reparse
import ir.sarejam.core.model.Reports
import ir.sarejam.core.model.TransactionFields

/** The only ways transaction values change: a user edit (remembered) or a reparse (respects edits). */
class TransactionEdits(private val db: SarejamDatabase, private val clock: () -> Long = System::currentTimeMillis) {

    /** User changed fields by hand: store them and mark them as untouchable for automation. */
    suspend fun userEdit(id: Long, change: (TransactionFields) -> TransactionFields) = db.withTransaction {
        val current = db.transactions().byId(id) ?: return@withTransaction
        val updated = change(current.fields())
        val edited = current.userEditedFields + Reparse.changedFields(current.fields(), updated)
        db.transactions().update(current.withFields(updated, clock()).copy(userEditedFields = edited))
    }

    /** Apply a newer parse; returns the fields that actually changed (for Undo). */
    suspend fun reparse(id: Long, fresh: TransactionFields, parserVersion: Int): Set<EditableField> = db.withTransaction {
        val current = db.transactions().byId(id) ?: return@withTransaction emptySet()
        val (merged, changed) = Reparse.merge(current.fields(), fresh, current.userEditedFields)
        db.transactions().update(current.withFields(merged, clock()).copy(parserVersion = parserVersion))
        changed
    }

    suspend fun totals(from: Long, to: Long): PeriodTotals =
        Reports.totals(db.transactions().kindAmounts(from, to).map { it.kind to it.amount })
}
