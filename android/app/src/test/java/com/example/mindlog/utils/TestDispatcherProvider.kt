package com.example.mindlog.utils

import com.example.mindlog.core.dispatcher.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * DispatcherProvider for test environment
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestDispatcherProvider(
    private val testDispatcher: CoroutineDispatcher
) : DispatcherProvider {

    override val io = testDispatcher
    override val main = testDispatcher
    override val default = testDispatcher
    override val unconfined = testDispatcher
}