package com.m4.red_android.testing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRuleTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun mainCoroutineRunsOnlyWhenTheTestSchedulerAdvances() =
        runTest(mainDispatcherRule.dispatcher) {
            val mainScope = CoroutineScope(Dispatchers.Main)
            var completed = false

            mainScope.launch {
                completed = true
            }

            assertFalse(completed)
            advanceUntilIdle()
            assertTrue(completed)

            mainScope.cancel()
        }
}
