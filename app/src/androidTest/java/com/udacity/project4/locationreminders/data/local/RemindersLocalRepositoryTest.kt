package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.*
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {
    private lateinit var dataSource: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        // using an in-memory database for testing, since it doesn't survive killing the process
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        dataSource =
            RemindersLocalRepository(
                database.reminderDao(),
                Dispatchers.Main
            )
    }

    @After
    fun cleanUp() {
        database.close()
    }

    /**
     * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
     * are not scheduled in the main Looper (for example when executed on a different thread).
     */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    /**
     * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
     */
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }
    @Test
    fun saveReminderAndGetItById() = runBlocking {
        // GIVEN - a new reminder saved in the database
        val reminder = ReminderDTO("Title1", "Description1","location",31.0,32.2)
        dataSource.saveReminder(reminder)

        // WHEN  - reminder retrieved by ID
        val result = dataSource.getReminder(reminder.id)

        // THEN - Same reminder is returned
        result as Result.Success
        Assert.assertThat(result.data.title, `is`(reminder.title))
        Assert.assertThat(result.data.description, `is`(reminder.description))
        Assert.assertThat(result.data.location, `is`(reminder.location))
        Assert.assertThat(result.data.latitude, `is`(reminder.latitude))
        Assert.assertThat(result.data.longitude, `is`(reminder.longitude))
    }

    @Test
    fun removeAllRemindersAndGetReminders() = runBlocking {
        // GIVEN - delete all reminders
        dataSource.deleteAllReminders()

        // WHEN - Get all reminders
        val reminders = dataSource.getReminders()

        // THEN - reminders should be empty list
        reminders as Result.Success
        assertThat(reminders.data.isEmpty(), `is`(true))
    }

    @Test
    fun addReminderAndRemove_ReturnError() = runBlocking{
        // GIVEN - insert a reminder
        val reminder = ReminderDTO("Title1", "Description1","location",31.0,32.2)
        dataSource.saveReminder(reminder)

        // delete all reminders
        dataSource.deleteAllReminders()

        // WHEN - Get reminder by id
        val loaded = dataSource.getReminder(reminder.id)

        loaded as Result.Error
        // THEN - reminders should be empty list
        assertThat(loaded.message, `is`("Reminder not found!"))
    }

}