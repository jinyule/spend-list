package com.spendlist.app.domain.usecase.export

import com.google.common.truth.Truth.assertThat
import com.spendlist.app.domain.repository.SubscriptionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ImportDataUseCaseTest {

    private lateinit var repository: SubscriptionRepository
    private lateinit var useCase: ImportDataUseCase

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        useCase = ImportDataUseCase(repository)
    }

    @Test
    fun importJson_validData_returnsSuccess() = runTest {
        val json = """
        [
            {
                "name": "Claude Pro",
                "amount": "150",
                "currency": "CNY",
                "billingCycleType": "MONTHLY",
                "startDate": "2024-01-12",
                "nextRenewalDate": "2024-02-12",
                "status": "ACTIVE"
            }
        ]
        """.trimIndent()

        coEvery { repository.insert(any()) } returns 1L

        val result = useCase.importJson(json)

        assertThat(result).isInstanceOf(ImportDataUseCase.Result.Success::class.java)
        assertThat((result as ImportDataUseCase.Result.Success).count).isEqualTo(1)
        coVerify(exactly = 1) { repository.insert(any()) }
    }

    @Test
    fun importJson_multipleItems_importsAll() = runTest {
        val json = """
        [
            {"name": "Sub1", "amount": "10", "currency": "USD", "billingCycleType": "MONTHLY", "startDate": "2024-01-01", "nextRenewalDate": "2024-02-01", "status": "ACTIVE"},
            {"name": "Sub2", "amount": "20", "currency": "EUR", "billingCycleType": "YEARLY", "startDate": "2024-01-01", "nextRenewalDate": "2025-01-01", "status": "ACTIVE"}
        ]
        """.trimIndent()

        coEvery { repository.insert(any()) } returns 1L

        val result = useCase.importJson(json)

        assertThat(result).isInstanceOf(ImportDataUseCase.Result.Success::class.java)
        assertThat((result as ImportDataUseCase.Result.Success).count).isEqualTo(2)
    }

    @Test
    fun importJson_invalidFormat_returnsError() = runTest {
        val result = useCase.importJson("not valid json")

        assertThat(result).isInstanceOf(ImportDataUseCase.Result.Error::class.java)
    }

    @Test
    fun importCsv_validData_returnsSuccess() = runTest {
        val csv = """
            name,amount,currency,billingCycleType,startDate,nextRenewalDate,status,categoryId,note,manageUrl
            Claude Pro,150,CNY,MONTHLY,2024-01-12,2024-02-12,ACTIVE,,AI assistant,https://claude.ai
        """.trimIndent()

        coEvery { repository.insert(any()) } returns 1L

        val result = useCase.importCsv(csv)

        assertThat(result).isInstanceOf(ImportDataUseCase.Result.Success::class.java)
        assertThat((result as ImportDataUseCase.Result.Success).count).isEqualTo(1)
    }

    @Test
    fun importCsv_emptyFile_returnsZeroCount() = runTest {
        val csv = "name,amount,currency,billingCycleType,startDate,nextRenewalDate,status,categoryId,note,manageUrl"

        val result = useCase.importCsv(csv)

        assertThat(result).isInstanceOf(ImportDataUseCase.Result.Success::class.java)
        assertThat((result as ImportDataUseCase.Result.Success).count).isEqualTo(0)
    }

    @Test
    fun importCsv_invalidFormat_returnsError() = runTest {
        val result = useCase.importCsv("")

        assertThat(result).isInstanceOf(ImportDataUseCase.Result.Error::class.java)
    }
}
