package com.example.mindlog.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Rule replacing Dispatchers.Main with TestDispatcher in tests
 * used in both Unit and Instrumental tests
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testScheduler: TestCoroutineScheduler = TestCoroutineScheduler()
) : TestWatcher() {

    val testDispatcher = StandardTestDispatcher(testScheduler)
    override fun starting(description: Description?) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description?) {
        Dispatchers.resetMain()
    }
}