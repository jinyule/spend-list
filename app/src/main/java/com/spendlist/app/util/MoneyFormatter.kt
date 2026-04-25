package com.spendlist.app.util

import com.spendlist.app.domain.model.Currency
import java.math.BigDecimal
import java.text.DecimalFormat

/**
 * Single source of truth for displaying monetary amounts.
 * Always thousand-separated with two decimal places (e.g. "2,999.99").
 */
object MoneyFormatter {
    private val formatter = DecimalFormat("#,##0.00")

    fun format(amount: BigDecimal): String = formatter.format(amount)

    fun format(amount: BigDecimal, currency: Currency): String =
        "${currency.symbol}${format(amount)}"
}
