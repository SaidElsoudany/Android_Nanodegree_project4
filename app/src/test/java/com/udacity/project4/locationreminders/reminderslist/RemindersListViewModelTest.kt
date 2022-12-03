package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {
    // Subject under test
    private lateinit var reminderListViewModel: RemindersListViewModel

    // Use a fake repository to be injected into the viewmodel
    private lateinit var reminderDataSource: FakeDataSource

    // Set the main coroutines dispatcher for unit testing.
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
        val reminder = ReminderDTO("Title1", "Description1","location",31.0,32.2)
        val reminder2 = ReminderDTO("Title2", "Description2","location2",31.0,32.2)
        val reminder3 = ReminderDTO("Title3", "Description3","location3",31.0,32.2)

        reminderDataSource.addTasks(reminder, reminder2, reminder3)

        reminderListViewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), reminderDataSource)
    }

    @Test
    fun loadReminders_returnRemindersData() {
        reminderListViewModel.loadReminders()

        val reminderList = reminderListViewModel.remindersList.getOrAwaitValue()
        assertThat(reminderList.size, `is`(3))
    }

    @Test
    fun loadReminders_returnRemindersDataError() {
        reminderDataSource.setReturnError(true)
        reminderListViewModel.loadReminders()

        assertThat( reminderListViewModel.showSnackBar.getOrAwaitValue(), `is`("Error not found"))
    }

    @Test
    fun loadReminders_returnNoData() = mainCoroutineRule.runBlockingTest {
        reminderDataSource.deleteAllReminders()
        reminderListViewModel.loadReminders()

        val showNoData = reminderListViewModel.showNoData.getOrAwaitValue()
        assertThat(showNoData, `is`(true))
    }

    @Test
    fun showLoading() = mainCoroutineRule.runBlockingTest {
        reminderDataSource.deleteAllReminders()

        mainCoroutineRule.pauseDispatcher()
        reminderListViewModel.loadReminders()

        assertThat(reminderListViewModel.showLoading.getOrAwaitValue(), `is`(true))
        mainCoroutineRule.resumeDispatcher()

        assertThat(reminderListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }


}