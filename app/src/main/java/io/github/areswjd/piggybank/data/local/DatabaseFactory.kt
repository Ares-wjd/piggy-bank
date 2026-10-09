package io.github.areswjd.piggybank.data.local

import android.content.Context
import androidx.room.Room
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

object DatabaseFactory {
    private const val DATABASE_NAME = "piggybank.db"

    /** SQLCipher로 암호화된 앱 DB를 만든다. */
    fun createEncrypted(context: Context): AppDatabase {
        System.loadLibrary("sqlcipher")
        val passphrase = DatabaseKeyProvider(context).getOrCreatePassphrase(DATABASE_NAME)
        return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .build()
    }
}
