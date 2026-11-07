package ir.sarejam.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [AccountEntity::class, TransactionEntity::class, CategoryEntity::class, RawMessageEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class SarejamDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun rawMessageDao(): RawMessageDao

    // The native helper needs its own passphrase for its lifetime. Never retain the caller's array.
    private var ownedPassphrase: ByteArray? = null

    override fun close() {
        try {
            super.close()
        } finally {
            ownedPassphrase?.fill(0)
            ownedPassphrase = null
        }
    }

    companion object {
        const val DEFAULT_NAME = "sarejam.db"

        /** Opens a persistent encrypted file. Missing migrations and incorrect keys always fail closed. */
        fun open(context: Context, passphrase: ByteArray, name: String = DEFAULT_NAME): SarejamDatabase {
            require(passphrase.size == 32) { "The database requires a 32-byte key." }
            require(name.isNotBlank() && name != ":memory:") { "A persistent database name is required." }
            System.loadLibrary("sqlcipher")
            val ownedKey = passphrase.copyOf()
            try {
                return Room.databaseBuilder(context.applicationContext, SarejamDatabase::class.java, name)
                    .openHelperFactory(SupportOpenHelperFactory(ownedKey))
                    .build()
                    .also { it.ownedPassphrase = ownedKey }
            } catch (failure: Exception) {
                ownedKey.fill(0)
                throw failure
            }
        }
    }
}
