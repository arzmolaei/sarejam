package ir.sarejam.core.database

import android.content.Context
import androidx.room.Room
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

object DatabaseFactory {
    init {
        System.loadLibrary("sqlcipher")
    }

    fun open(context: Context, key: DatabaseKey, name: String = SarejamDatabase.FILE_NAME): SarejamDatabase =
        Room.databaseBuilder(context.applicationContext, SarejamDatabase::class.java, name)
            .openHelperFactory(SupportOpenHelperFactory(key.passphrase()))
            .addMigrations(*SarejamDatabase.MIGRATIONS)
            .build()
}
