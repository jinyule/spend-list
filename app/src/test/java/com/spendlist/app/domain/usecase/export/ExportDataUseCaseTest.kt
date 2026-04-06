package com.spendlist.app.domain.usecase.export

import com.google.common.truth.Truth.assertThat
import com.spendlist.app.domain.model.BillingCycle
import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.model.SubscriptionStatus
import com.spendlist.app.domain.repository.SubscriptionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class ExportDataUseCaseTest {

    private lateinit var repository: SubscriptionRepository
    private lateinit var useCase: ExportDataUseCase

    private val sampleSubs = listOf(
        Subscription(
            id = 1, name = "Claude Pro", categoryId = 1,
            amount = BigDecimal("150"), currency = Currency.CNY,
            billingCycle = BillingCycle.Monthly,
            startDate = LocalDate.of(2024, 1, 12),
            nextRenewalDate = LocalDate.of(2024, 2, 12),
            note = "AI assistant", manageUrl = "https://claude.ai",
            status = SubscriptionStatus.ACTIVE
        ),
        Subscription(
            id = 2, name = "ChatGPT Plus", categoryId = 1,
            amount = BigDecimal("20"), currency = Currency.USD,
            billingCycle = BillingCycle.Monthly,
            startDate = LocalDate.of(2024, 1, 1),
            nextRenewalDate = LocalDate.of(2024, 2, 1),
            status = SubscriptionStatus.ACTIVE
        )
    )

    @Before
    fun setup() {
        repository = mockk()
        useCase = ExportDataUseCase(repository)
    }

    @Test
    fun exportJson_containsAllFields() = runTest {
        every { repository.getAll() } returns flowOf(sampleSubs)

        val json = useCase.exportJson()

        assertThat(json).contains("Claude Pro")
        assertThat(json).contains("ChatGPT Plus")
        assertThat(json).contains("150")
        assertThat(json).contains("CNY")
        assertThat(json).contains("USD")
        assertThat(json).contains("2024-01-12")
    }

    @Test
    fun exportJson_emptyData_returnsValidJson() = runTest {
        every { repository.getAll() } returns flowOf(emptyList())

        val json = useCase.exportJson()

        assertThat(json).isEqualTo("[]")
    }

    @Test
    fun exportCsv_hasHeader() = runTest {
        every { repository.getAll() } returns flowOf(sampleSubs)

        val csv = useCase.exportCsv()

        val lines = csv.lines()
        assertThat(lines[0]).contains("name")
        assertThat(lines[0]).contains("amount")
        assertThat(lines[0]).contains("currency")
    }

    @Test
    fun exportCsv_containsAllRows() = runTest {
        every { repository.getAll() } returns flowOf(sampleSubs)

        val csv = useCase.exportCsv()

        val lines = csv.lines().filter { it.isNotBlank() }
        // 1 header + 2 data rows
        assertThat(lines).hasSize(3)
        assertThat(lines[1]).contains("Claude Pro")
        assertThat(lines[2]).contains("ChatGPT Plus")
    }

    @Test
    fun exportCsv_emptyData_returnsHeaderOnly() = runTest {
        every { repository.getAll() } returns flowOf(emptyList())

        val csv = useCase.exportCsv()

        val lines = csv.lines().filter { it.isNotBlank() }
        assertThat(lines).hasSize(1) // header only
    }
}
