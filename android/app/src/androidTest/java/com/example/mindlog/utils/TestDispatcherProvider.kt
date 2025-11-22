package com.example.mindlog.utils

import com.example.mindlog.core.dispatcher.DispatcherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher

/**
 * DispatcherProvider for test environment
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestDispatcherProvider() : DispatcherProvider {
    override val main = Dispatchers.Main
    override val io = Dispatchers.Main
    override val default = Dispatchers.Main
    override val unconfined = Dispatchers.Main
}