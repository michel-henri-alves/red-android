package com.m4.red_android.sales

import com.m4.red_android.data.repository.SaleSubmissionResult
import com.m4.red_android.data.repository.SalesRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SaleSubmissionCoordinator(
    private val repository: SalesRepository,
    private val onConfirmedSuccess: () -> Unit = {},
) {
    private val transitionMutex = Mutex()
    private val _state = MutableStateFlow<SaleSubmissionState>(SaleSubmissionState.Ready)
    private val completionEffects = Channel<SaleSubmissionEffect>(Channel.BUFFERED)

    val state: StateFlow<SaleSubmissionState> = _state.asStateFlow()
    val effects = completionEffects.receiveAsFlow()

    suspend fun submit(snapshot: SaleSnapshot) {
        val acceptedSnapshot = transitionMutex.withLock {
            if (_state.value !is SaleSubmissionState.Ready) {
                null
            } else {
                _state.value = SaleSubmissionState.Submitting(snapshot)
                snapshot
            }
        }

        acceptedSnapshot?.let { execute(it) }
    }

    suspend fun retry() {
        val failedSnapshot = transitionMutex.withLock {
            val current = _state.value
            if (current !is SaleSubmissionState.Failed || !current.error.isRetryable) {
                null
            } else {
                _state.value = SaleSubmissionState.Submitting(current.snapshot)
                current.snapshot
            }
        }

        failedSnapshot?.let { execute(it) }
    }

    private suspend fun execute(snapshot: SaleSnapshot) {
        when (val result = repository.submit(snapshot)) {
            SaleSubmissionResult.Success -> complete(snapshot)
            is SaleSubmissionResult.Failure -> fail(snapshot)
        }
    }

    private suspend fun complete(snapshot: SaleSnapshot) {
        val completed = transitionMutex.withLock {
            val current = _state.value
            if (current !is SaleSubmissionState.Submitting ||
                current.snapshot.submissionId != snapshot.submissionId
            ) {
                false
            } else {
                _state.value = SaleSubmissionState.Succeeded(snapshot)
                true
            }
        }

        if (completed) {
            onConfirmedSuccess()
            completionEffects.send(SaleSubmissionEffect.Completed(snapshot.submissionId))
        }
    }

    private suspend fun fail(snapshot: SaleSnapshot) {
        transitionMutex.withLock {
            val current = _state.value
            if (current is SaleSubmissionState.Submitting &&
                current.snapshot.submissionId == snapshot.submissionId
            ) {
                _state.value = SaleSubmissionState.Failed(
                    snapshot = snapshot,
                    error = SaleSubmissionError(
                        message = "Não foi possível registrar a venda. Verifique a conexão e tente novamente.",
                    ),
                )
            }
        }
    }
}
