package org.qwallet.tests.database

import androidx.room.Room
import androidx.test.InstrumentationRegistry
import org.junit.Before
import org.qwallet.data.AppDatabase

abstract class AbstractDatabaseTest {

    lateinit var database : AppDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getTargetContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

}