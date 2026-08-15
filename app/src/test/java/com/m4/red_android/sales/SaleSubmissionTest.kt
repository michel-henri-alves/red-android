package com.m4.red_android.sales

import com.m4.red_android.data.enums.PaymentMethod
import com.m4.red_android.data.models.Item
import com.m4.red_android.data.repository.SaleSubmissionResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SaleSubmissionTest {

    @Test
    fun confirmedSuccessResetsAndEmitsCompletionExactlyOnce() = runTest {
        val repository = FakeSalesRepository()
        var resetCount = 0
        val coordinator = SaleSubmissionCoordinator(repository) { resetCount++ }
        val effects = mutableListOf<SaleSubmissionEffect>()
        val effectCollector = launch { coordinator.effects.take(1).toList(effects) }

        coordinator.submit(snapshot())
        coordinator.submit(snapshot())
        advanceUntilIdle()

        assertEquals(1, repository.requestCount)
        assertEquals(1, resetCount)
        assertEquals(
            listOf(SaleSubmissionEffect.Completed("submission-1")),
            effects,
        )
        assertTrue(coordinator.state.value is SaleSubmissionState.Succeeded)
        effectCollector.cancel()
    }

    @Test
    fun concurrentSubmitAttemptsCreateOneRequestWhileFirstIsInFlight() = runTest {
        val repository = FakeSalesRepository().apply {
            releaseSubmission = CompletableDeferred()
        }
        var resetCount = 0
        val coordinator = SaleSubmissionCoordinator(repository) { resetCount++ }
        val sale = snapshot()

        val first = launch { coordinator.submit(sale) }
        val second = launch { coordinator.submit(sale) }
        runCurrent()

        assertEquals(1, repository.requestCount)
        assertEquals(SaleSubmissionState.Submitting(sale), coordinator.state.value)
        assertEquals(0, resetCount)

        repository.releaseSubmission?.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.requestCount)
        assertEquals(1, resetCount)
        assertTrue(coordinator.state.value is SaleSubmissionState.Succeeded)
        first.cancel()
        second.cancel()
    }

    @Test
    fun failurePreservesSnapshotAndRetrySubmitsItWithoutDuplicatingPayments() = runTest {
        val failure = IllegalStateException("server unavailable")
        val repository = FakeSalesRepository(SaleSubmissionResult.Failure(failure))
        var resetCount = 0
        val coordinator = SaleSubmissionCoordinator(repository) { resetCount++ }
        val sale = snapshot()
        val effects = mutableListOf<SaleSubmissionEffect>()
        val effectCollector = launch { coordinator.effects.take(1).toList(effects) }

        coordinator.submit(sale)

        val failed = coordinator.state.value as SaleSubmissionState.Failed
        assertSame(sale, failed.snapshot)
        assertTrue(failed.error.isRetryable)
        assertEquals(0, resetCount)
        assertTrue(effects.isEmpty())
        assertEquals(1, repository.requestCount)

        repository.result = SaleSubmissionResult.Success
        coordinator.retry()
        advanceUntilIdle()

        assertEquals(2, repository.requestCount)
        assertSame(repository.submittedSnapshots[0], repository.submittedSnapshots[1])
        assertEquals(1, repository.submittedSnapshots[1].payments.size)
        assertEquals(1, resetCount)
        assertEquals(
            listOf(SaleSubmissionEffect.Completed("submission-1")),
            effects,
        )
        assertTrue(coordinator.state.value is SaleSubmissionState.Succeeded)
        effectCollector.cancel()
    }

    private fun snapshot() = SaleSnapshot.create(
        submissionId = "submission-1",
        code = "sale-1",
        items = listOf(
            Item(
                smartCode = "smart-P-1",
                quantity = "1",
                productName = "Product P-1",
                unitOfMeasurement = "UN",
                price = 10.0,
                code = "P-1",
            ),
        ),
        paymentMethods = listOf(PaymentMethod.PIX),
        amountsPaid = listOf(10.0),
        discount = 0.0,
        change = 0.0,
        vendor = "app",
        realizedAt = "2026-08-15T10:30:00",
    )
}
