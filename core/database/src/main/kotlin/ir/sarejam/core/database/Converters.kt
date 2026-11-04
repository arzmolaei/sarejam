package ir.sarejam.core.database

import androidx.room.TypeConverter
import ir.sarejam.core.model.AccountKind
import ir.sarejam.core.model.Decision
import ir.sarejam.core.model.EditableField
import ir.sarejam.core.model.MoneyDirection
import ir.sarejam.core.model.SourceType
import ir.sarejam.core.model.Space
import ir.sarejam.core.model.TransactionKind
import ir.sarejam.core.model.TransactionStatus

/** Enums are stored by name (TEXT), so renaming an enum constant needs a migration. */
class Converters {
    @TypeConverter fun kind(v: TransactionKind) = v.name
    @TypeConverter fun kind(v: String) = TransactionKind.valueOf(v)
    @TypeConverter fun direction(v: MoneyDirection) = v.name
    @TypeConverter fun direction(v: String) = MoneyDirection.valueOf(v)
    @TypeConverter fun status(v: TransactionStatus) = v.name
    @TypeConverter fun status(v: String) = TransactionStatus.valueOf(v)
    @TypeConverter fun decision(v: Decision) = v.name
    @TypeConverter fun decision(v: String) = Decision.valueOf(v)
    @TypeConverter fun source(v: SourceType) = v.name
    @TypeConverter fun source(v: String) = SourceType.valueOf(v)
    @TypeConverter fun account(v: AccountKind) = v.name
    @TypeConverter fun account(v: String) = AccountKind.valueOf(v)
    @TypeConverter fun space(v: Space) = v.name
    @TypeConverter fun space(v: String) = Space.valueOf(v)

    @TypeConverter fun edited(v: Set<EditableField>): String = v.map { it.name }.sorted().joinToString(",")
    @TypeConverter fun edited(v: String): Set<EditableField> =
        if (v.isBlank()) emptySet() else v.split(',').mapNotNull { n -> EditableField.entries.firstOrNull { it.name == n } }.toSet()
}
