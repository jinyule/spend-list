package com.spendlist.app.domain.usecase.stats

import com.google.common.truth.Truth.assertThat
import com.spendlist.app.domain.model.BillingCycle
import com.spendlist.app.domain.model.Category
import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.model.SubscriptionStatus
import com.spendlist.app.domain.repository.CategoryRepository
import com.spendlist.app.domain.repository.SubscriptionRepository
import com.spendlist.app.domain.usecase.currency.ConvertCurrencyUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class GetSpendingByCategoryUseCaseTest {

    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var convertCurrency: ConvertCurrencyUseCase
    private lateinit var useCase: GetSpendingByCategoryUseCase

    private val aiCategory = Category(id = 1, name = "AI Tools", iconName = "SmartToy", color = 0xFFFF6B35, isPreset = true)
    private val infraCategory = Category(id = 2, name = "Infrastructure", iconName = "Dns", color = 0xFF4ECDC4, isPreset = true)

    @Before
    fun setup() {
        subscriptionRepository = mockk()
        categoryRepository = mockk()
        convertCurrency = mockk()
        useCase = GetSpendingByCategoryUseCase(subscriptionRepository, categoryRepository, convertCurrency)
    }

    private fun createSub(
        categoryId: Long? = 1,
        amount: BigDecimal = BigDecimal("100"),
        currency: Currency = Currency.CNY
    ) = Subscription(
        id = 0, name = "Test", categoryId = categoryId, amount = amount,
        currency = currency, billingCycle = BillingCycle.Monthly,
        startDate = LocalDate.now(), nextRenewalDate = LocalDate.now().plusMonths(1),
        status = SubscriptionStatus.ACTIVE
    )

    @Test
    fun singleCategory_calculatesAmountAndPercentage() = runTest {
        every { subscriptionRepository.getAll() } returns flowOf(listOf(createSub(categoryId = 1, amount = BigDecimal("100"))))
        every { categoryRepository.getAll() } returns flowOf(listOf(aiCategory))
        coEvery { convertCurrency(any(), any(), any()) } returns ConvertCurrencyUseCase.Result.Success(BigDecimal("100"))

        val result = useCase(Currency.CNY).first()

        assertThat(result).hasSize(1)
        assertThat(result[0].categoryName).isEqualTo("AI Tools")
        assertThat(result[0].percentage).isEqualTo(100.0f)
    }

    @Test
    fun multipleCategories_calculatesCorrectPercentages() = runTest {
        val subs = listOf(
            createSub(categoryId = 1, amount = BigDecimal("300")),
            createSub(categoryId = 2, amount = BigDecimal("100"))
        )
        every { subscriptionRepository.getAll() } returns flowOf(subs)
        every { categoryRepository.getAll() } returns flowOf(listOf(aiCategory, infraCategory))
        coEvery { convertCurrency(any(), any(), any()) } answers {
            ConvertCurrencyUseCase.Result.Success(firstArg())
        }

        val result = useCase(Currency.CNY).first()

        assertThat(result).hasSize(2)
        val aiSpend = result.find { it.categoryName == "AI Tools" }!!
        assertThat(aiSpend.percentage).isWithin(0.1f).of(75.0f)
    }

    @Test
    fun currentMonthly_cancelledSubscriptions_excluded() = runTest {
        val activeSub = createSub(categoryId = 1, amount = BigDecimal("100"))
        val cancelledSub = activeSub.copy(status = SubscriptionStatus.CANCELLED)
        every { subscriptionRepository.getAll() } returns flowOf(listOf(activeSub, cancelledSub))
        every { categoryRepository.getAll() } returns flowOf(listOf(aiCategory))
        coEvery { convertCurrency(any(), any(), any()) } answers {
            ConvertCurrencyUseCase.Result.Success(firstArg())
        }

        val result = useCase(Currency.CNY, CategoryStatsMode.CURRENT_MONTHLY).first()

        assertThat(result).hasSize(1)
        assertThat(result[0].amount.compareTo(BigDecimal("100"))).isEqualTo(0)
    }

    @Test
    fun historicalTotal_includesCancelledAndExpired() = runTest {
        // Each sub paid 3 months (startDate Jan 1 → nextRenewalDate Apr 1 = 3 cycles)
        val active = Subscription(
            id = 1, name = "ActiveSub", categoryId = 1, amount = BigDecimal("100"),
            currency = Currency.CNY, billingCycle = BillingCycle.Monthly,
            startDate = LocalDate.of(2024, 1, 1), nextRenewalDate = LocalDate.of(2024, 4, 1),
            status = SubscriptionStatus.ACTIVE
        )
        val expired = active.copy(id = 2, name = "ExpiredSub", status = SubscriptionStatus.EXPIRED)
        val cancelled = active.copy(id = 3, name = "CancelledSub", status = SubscriptionStatus.CANCELLED)
        every { subscriptionRepository.getAll() } returns flowOf(listOf(active, expired, cancelled))
        every { categoryRepository.getAll() } returns flowOf(listOf(aiCategory))
        coEvery { convertCurrency(any(), any(), any()) } answers {
            ConvertCurrencyUseCase.Result.Success(firstArg())
        }

        val result = useCase(Currency.CNY, CategoryStatsMode.HISTORICAL_TOTAL).first()

        // All three subs: each 100 × 3 cycles = 300; total 900 in one category
        assertThat(result).hasSize(1)
        assertThat(result[0].amount.compareTo(BigDecimal("900"))).isEqualTo(0)
    }

    @Test
    fun currentMonthly_and_historicalTotal_differ() = runTest {
        // One active (100/mo, 3 paid) and one cancelled (50/mo, 2 paid)
        val active = Subscription(
            id = 1, name = "Active", categoryId = 1, amount = BigDecimal("100"),
            currency = Currency.CNY, billingCycle = BillingCycle.Monthly,
            startDate = LocalDate.of(2024, 1, 1), nextRenewalDate = LocalDate.of(2024, 4, 1),
            status = SubscriptionStatus.ACTIVE
        )
        val cancelled = Subscription(
            id = 2, name = "Cancelled", categoryId = 1, amount = BigDecimal("50"),
            currency = Currency.CNY, billingCycle = BillingCycle.Monthly,
            startDate = LocalDate.of(2024, 1, 1), nextRenewalDate = LocalDate.of(2024, 3, 1),
            status = SubscriptionStatus.CANCELLED
        )
        every { subscriptionRepository.getAll() } returns flowOf(listOf(active, cancelled))
        every { categoryRepository.getAll() } returns flowOf(listOf(aiCategory))
        coEvery { convertCurrency(any(), any(), any()) } answers {
            ConvertCurrencyUseCase.Result.Success(firstArg())
        }

        val current = useCase(Currency.CNY, CategoryStatsMode.CURRENT_MONTHLY).first()
        val historical = useCase(Currency.CNY, CategoryStatsMode.HISTORICAL_TOTAL).first()

        // Current: only active sub counted → 100
        assertThat(current[0].amount.compareTo(BigDecimal("100"))).isEqualTo(0)
        // Historical: 100 × 3 + 50 × 2 = 400
        assertThat(historical[0].amount.compareTo(BigDecimal("400"))).isEqualTo(0)
    }

    @Test
    fun emptySubscriptions_returnsEmpty() = runTest {
        every { subscriptionRepository.getAll() } returns flowOf(emptyList())
        every { categoryRepository.getAll() } returns flowOf(emptyList())

        val result = useCase(Currency.CNY).first()

        assertThat(result).isEmpty()
    }
}
