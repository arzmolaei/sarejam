package ir.sarejam.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [AccountEntity::class, CategoryEntity::class, TransactionEntity::class, RawMessageEntity::class],
    version = SarejamDatabase.VERSION,
    exportSchema = true,
)
abstract class SarejamDatabase : RoomDatabase() {
    abstract fun accounts(): AccountDao
    abstract fun categories(): CategoryDao
    abstract fun transactions(): TransactionDao
    abstract fun rawMessages(): RawMessageDao

    companion object {
        const val VERSION = 1
        const val FILE_NAME = "sarejam.db"

        /**
         * Every schema change adds a versioned Migration here plus a test in MigrationTest.
         * Destructive fallback is forbidden (CLAUDE.md rule 7): data must survive every update.
         */
        val MIGRATIONS: Array<Migration> = arrayOf()
    }
}
