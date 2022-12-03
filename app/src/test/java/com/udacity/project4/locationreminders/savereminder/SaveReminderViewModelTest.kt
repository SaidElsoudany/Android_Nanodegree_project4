package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    // Subject under test
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    // Use a fake repository to be injected into the viewmodel
    private lateinit var reminderDataSource: FakeDataSource

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setupViewModel() {
        stopKoin()
        // We initialise the tasks to 3, with one active and two completed
        reminderDataSource = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), reminderDataSource)
    }

    @Test
    fun saveReminder(){
        //GIVEN reminder item
        val reminder = ReminderDataItem("Title1", "Description1","location",31.0,32.2)

        //WHEN validate and save item
        saveReminderViewModel.validateAndSaveReminder(reminder)

        //THEN assert that reminder is saved
        assertThat(
            saveReminderViewModel.showToast.getOrAwaitValue(),
            CoreMatchers.`is`("Reminder Saved !")
        )

    }
    @Test
    fun showLoading() = mainCoroutineRule.runBlockingTest {
        reminderDataSource.deleteAllReminders()

        mainCoroutineRule.pauseDispatcher()
        val reminder = ReminderDataItem("Title1", "Description1","location",31.0,32.2)
        saveReminderViewModel.saveReminder(reminder)

        assertThat(
            saveReminderViewModel.showLoading.getOrAwaitValue(),
            CoreMatchers.`is`(true)
        )
        mainCoroutineRule.resumeDispatcher()

        assertThat(
            saveReminderViewModel.showLoading.getOrAwaitValue(),
            CoreMatchers.`is`(false)
        )
    }

    @Test
    fun saveReminder_WithError(){
        //GIVEN reminder item without title
        val reminder = ReminderDataItem("", "Description1","location",31.0,32.2)

        //WHEN validate and save item
        saveReminderViewModel.validateAndSaveReminder(reminder)

        //THEN assert that error appeared
        assertThat(
            saveReminderViewModel.showSnackBar.getOrAwaitValue(),
            `is`("Please enter title")
        )

    }

}