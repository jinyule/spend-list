package com.spendlist.app.domain.usecase.currency

import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.repository.CurrencyRepository
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class ConvertCurrencyUseCase @Inject constructor(
    private val repository: CurrencyRepository
) {

    sealed class Result {
        data class Success(val amount: BigDecimal) : Result()
        data object NoRateAvailable : Result()
    }

    suspend operator fun invoke(
        amount: BigDecimal,
        from: Currency,
        to: Currency
    ): Result {
        if (from == to) {
            return Result.Success(amount)
        }

        val rate = repository.getRate(from.code, to.code)
            ?: return Result.NoRateAvailable

        val converted = amount.multiply(rate.rate).setScale(2, RoundingMode.HALF_UP)
        return Result.Success(converted)
    }
}
