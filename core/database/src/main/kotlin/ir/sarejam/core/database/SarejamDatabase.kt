package ir.sarejam.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration

@Database(
    entities = [
        AccountEntity::class, CategoryEntity::class, TransactionEntity::class, RawMessageEntity::class,
        SourceEntity::class, TransactionSourceEntity::class,
    ],
    version = SarejamDatabase.VERSION,
    exportSchema = true,
    // Version 2 (ROADMAP 1.0): only new tables and new columns with defaults, so Room derives it.
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@TypeConverters(Converters::class)
abstract class SarejamDatabase : RoomDatabase() {
    abstract fun accounts(): AccountDao
    abstract fun categories(): CategoryDao
    abstract fun transactions(): TransactionDao
    abstract fun rawMessages(): RawMessageDao
    abstract fun sources(): SourceDao

    companion object {
        const val VERSION = 2
        const val FILE_NAME = "sarejam.db"

        /**
         * Every schema change adds a versioned Migration here plus a test in MigrationTest.
         * Destructive fallback is forbidden (CLAUDE.md rule 7): data must survive every update.
         */
        val MIGRATIONS: Array<Migration> = arrayOf()
    }
}
