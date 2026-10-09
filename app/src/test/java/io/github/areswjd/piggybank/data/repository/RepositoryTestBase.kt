package io.github.areswjd.piggybank.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.areswjd.piggybank.data.local.AppDatabase
import io.github.areswjd.piggybank.model.ValidationError
import io.github.areswjd.piggybank.model.ValidationException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before

/** 메모리 DB(암호화 없음)로 저장소를 테스트한다. 시계는 호출할 때마다 1씩 증가한다. */
abstract class RepositoryTestBase {
    protected lateinit var db: AppDatabase
    protected lateinit var assets: AssetRepository
    protected lateinit var transactions: TransactionRepository
    private var now = 1_000L

    /** 저장소가 "기록이 바뀌었다"고 알린 횟수(자동 백업 대상 표시). */
    protected var changeCount = 0

    @Before
    fun setUpDatabase() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val clock = { now++ }
        val onChanged: suspend () -> Unit = { changeCount++ }
        assets = AssetRepository(db, clock, onChanged)
        transactions = TransactionRepository(db, clock, onChanged)
    }

    @After
    fun closeDatabase() {
        db.close()
    }

    protected suspend fun assertFails(expected: ValidationError, block: suspend () -> Unit) {
        try {
            block()
            fail("$expected 오류가 나야 한다")
        } catch (e: ValidationException) {
            assertEquals(expected, e.error)
        }
    }
}
