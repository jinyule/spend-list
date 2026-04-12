package com.spendlist.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class BillingCycleTest {

    @Test
    fun monthly_calculateNextRenewalDate_addsOneMonth() {
        val cycle = BillingCycle.Monthly
        val from = LocalDate.of(2024, 1, 15)
        val next = cycle.calculateNextRenewalDate(from)
        assertThat(next).isEqualTo(LocalDate.of(2024, 2, 15))
    }

    @Test
    fun monthly_calculateNextRenewalDate_crossesYear() {
        val cycle = BillingCycle.Monthly
        val from = LocalDate.of(2024, 12, 15)
        val next = cycle.calculateNextRenewalDate(from)
        assertThat(next).isEqualTo(LocalDate.of(2025, 1, 15))
    }

    @Test
    fun monthly_calculateNextRenewalDate_endOfMonth() {
        val cycle = BillingCycle.Monthly
        val from = LocalDate.of(2024, 1, 31)
        val next = cycle.calculateNextRenewalDate(from)
        // Feb doesn't have 31 days, so it adjusts to Feb 29 (2024 is leap year)
        assertThat(next).isEqualTo(LocalDate.of(2024, 2, 29))
    }

    @Test
    fun yearly_calculateNextRenewalDate_addsOneYear() {
        val cycle = BillingCycle.Yearly
        val from = LocalDate.of(2024, 3, 10)
        val next = cycle.calculateNextRenewalDate(from)
        assertThat(next).isEqualTo(LocalDate.of(2025, 3, 10))
    }

    @Test
    fun yearly_calculateNextRenewalDate_leapYearFeb29() {
        val cycle = BillingCycle.Yearly
        val from = LocalDate.of(2024, 2, 29) // Leap year
        val next = cycle.calculateNextRenewalDate(from)
        // 2025 is not a leap year, so Feb 29 -> Feb 28
        assertThat(next).isEqualTo(LocalDate.of(2025, 2, 28))
    }

    @Test
    fun custom_calculateNextRenewalDate_addsCustomDays() {
        val cycle = BillingCycle.Custom(90)
        val from = LocalDate.of(2024, 1, 1)
        val next = cycle.calculateNextRenewalDate(from)
        assertThat(next).isEqualTo(LocalDate.of(2024, 3, 31))
    }

    @Test
    fun custom_calculateNextRenewalDate_crossesYear() {
        val cycle = BillingCycle.Custom(45)
        val from = LocalDate.of(2024, 12, 1)
        val next = cycle.calculateNextRenewalDate(from)
        assertThat(next).isEqualTo(LocalDate.of(2025, 1, 15))
    }

    @Test
    fun monthly_monthlyFactor_isOne() {
        assertThat(BillingCycle.Monthly.monthlyFactor()).isEqualTo(1.0)
    }

    @Test
    fun yearly_monthlyFactor_isOneOverTwelve() {
        assertThat(BillingCycle.Yearly.monthlyFactor()).isWithin(0.0001).of(1.0 / 12.0)
    }

    @Test
    fun custom_monthlyFactor_isThirtyOverDays() {
        val cycle = BillingCycle.Custom(90)
        assertThat(cycle.monthlyFactor()).isWithin(0.0001).of(30.0 / 90.0)
    }

    // --- Quarterly tests ---

    @Test
    fun quarterly_calculateNextRenewalDate_addsThreeMonths() {
        val cycle = BillingCycle.Quarterly
        val from = LocalDate.of(2024, 1, 15)
        val next = cycle.calculateNextRenewalDate(from)
        assertThat(next).isEqualTo(LocalDate.of(2024, 4, 15))
    }

    @Test
    fun quarterly_calculateNextRenewalDate_crossesYear() {
        val cycle = BillingCycle.Quarterly
        val from = LocalDate.of(2024, 11, 15)
        val next = cycle.calculateNextRenewalDate(from)
        assertThat(next).isEqualTo(LocalDate.of(2025, 2, 15))
    }

    @Test
    fun quarterly_calculateNextRenewalDate_endOfMonth() {
        val cycle = BillingCycle.Quarterly
        val from = LocalDate.of(2024, 11, 30)
        val next = cycle.calculateNextRenewalDate(from)
        // Nov 30 + 3 months = Feb 28 (2025 is not a leap year)
        assertThat(next).isEqualTo(LocalDate.of(2025, 2, 28))
    }

    @Test
    fun quarterly_toDays_is90() {
        assertThat(BillingCycle.Quarterly.toDays()).isEqualTo(90)
    }

    @Test
    fun quarterly_monthlyFactor_isOneThird() {
        assertThat(BillingCycle.Quarterly.monthlyFactor()).isWithin(0.0001).of(1.0 / 3.0)
    }

    // --- billingDayOfMonth overload tests ---

    @Test
    fun monthly_withBillingDay15_from_jan1_goesToFeb15() {
        val next = BillingCycle.Monthly.calculateNextRenewalDate(
            LocalDate.of(2024, 1, 1), billingDayOfMonth = 15
        )
        assertThat(next).isEqualTo(LocalDate.of(2024, 2, 15))
    }

    @Test
    fun monthly_withBillingDay31_from_jan15_goesToFeb29_inLeapYear() {
        val next = BillingCycle.Monthly.calculateNextRenewalDate(
            LocalDate.of(2024, 1, 15), billingDayOfMonth = 31
        )
        // 2024 is leap year, Feb has 29 days -> clamp to 29
        assertThat(next).isEqualTo(LocalDate.of(2024, 2, 29))
    }

    @Test
    fun monthly_withBillingDay31_from_jan15_goesToFeb28_inNonLeapYear() {
        val next = BillingCycle.Monthly.calculateNextRenewalDate(
            LocalDate.of(2025, 1, 15), billingDayOfMonth = 31
        )
        // 2025 is not leap year, Feb has 28 days -> clamp to 28
        assertThat(next).isEqualTo(LocalDate.of(2025, 2, 28))
    }

    @Test
    fun monthly_withBillingDay31_fromFeb28_goesToMar31() {
        // After clamping to Feb 28, next renewal should still use billingDay=31
        val next = BillingCycle.Monthly.calculateNextRenewalDate(
            LocalDate.of(2025, 2, 28), billingDayOfMonth = 31
        )
        assertThat(next).isEqualTo(LocalDate.of(2025, 3, 31))
    }

    @Test
    fun quarterly_withBillingDay15_from_jan1_goesToApr15() {
        val next = BillingCycle.Quarterly.calculateNextRenewalDate(
            LocalDate.of(2024, 1, 1), billingDayOfMonth = 15
        )
        assertThat(next).isEqualTo(LocalDate.of(2024, 4, 15))
    }

    @Test
    fun yearly_withBillingDay15_usesCorrectDay() {
        val next = BillingCycle.Yearly.calculateNextRenewalDate(
            LocalDate.of(2024, 3, 1), billingDayOfMonth = 15
        )
        assertThat(next).isEqualTo(LocalDate.of(2025, 3, 15))
    }

    @Test
    fun custom_ignoresBillingDay() {
        val cycle = BillingCycle.Custom(30)
        val next = cycle.calculateNextRenewalDate(
            LocalDate.of(2024, 1, 1), billingDayOfMonth = 15
        )
        // Custom ignores billingDayOfMonth, uses default behavior
        assertThat(next).isEqualTo(LocalDate.of(2024, 1, 31))
    }

    @Test
    fun monthly_withNullBillingDay_usesDefaultBehavior() {
        val next = BillingCycle.Monthly.calculateNextRenewalDate(
            LocalDate.of(2024, 1, 15), billingDayOfMonth = null
        )
        assertThat(next).isEqualTo(LocalDate.of(2024, 2, 15))
    }
}
