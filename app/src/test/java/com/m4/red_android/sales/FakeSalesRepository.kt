package com.m4.red_android.sales

import com.m4.red_android.data.repository.SaleSubmissionResult
import com.m4.red_android.data.repository.SalesRepository
import kotlinx.coroutines.CompletableDeferred

class FakeSalesRepository(
    var result: SaleSubmissionResult = SaleSubmissionResult.Success,
) : SalesRepository {
    val submittedSnapshots = mutableListOf<SaleSnapshot>()
    var releaseSubmission: CompletableDeferred<Unit>? = null

    val requestCount: Int get() = submittedSnapshots.size

    override suspend fun submit(snapshot: SaleSnapshot): SaleSubmissionResult {
        submittedSnapshots += snapshot
        releaseSubmission?.await()
        return result
    }
}
