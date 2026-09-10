package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.JarvisDatabase
import com.example.data.local.entity.ReminderEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var inMemoryDb: JarvisDatabase

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    inMemoryDb = Room.inMemoryDatabaseBuilder(context, JarvisDatabase::class.java)
      .allowMainThreadQueries()
      .build()
  }

  @After
  fun tearDown() {
    inMemoryDb.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Jarvis AI", appName)
  }

  @Test
  fun `insert and retrieve reminder in room`() = runBlocking {
    val reminderDao = inMemoryDb.reminderDao()
    val reminder = ReminderEntity(
      text = "College assignment presentation",
      datetimeString = "Today 6:00 PM"
    )
    val id = reminderDao.insertReminder(reminder)
    assertNotNull(id)

    val active = reminderDao.getActiveReminders()
    assertEquals(1, active.size)
    assertEquals("College assignment presentation", active[0].text)
  }
}

