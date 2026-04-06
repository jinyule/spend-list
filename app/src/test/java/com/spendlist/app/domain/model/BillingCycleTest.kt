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
}
