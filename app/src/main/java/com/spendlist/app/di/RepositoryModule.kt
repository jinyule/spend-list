package com.spendlist.app.di

import com.spendlist.app.data.repository.CategoryRepositoryImpl
import com.spendlist.app.data.repository.CurrencyRepositoryImpl
import com.spendlist.app.data.repository.RenewalHistoryRepositoryImpl
import com.spendlist.app.data.repository.SubscriptionRepositoryImpl
import com.spendlist.app.domain.repository.CategoryRepository
import com.spendlist.app.domain.repository.CurrencyRepository
import com.spendlist.app.domain.repository.RenewalHistoryRepository
import com.spendlist.app.domain.repository.SubscriptionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(
        impl: SubscriptionRepositoryImpl
    ): SubscriptionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        impl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindCurrencyRepository(
        impl: CurrencyRepositoryImpl
    ): CurrencyRepository

    @Binds
    @Singleton
    abstract fun bindRenewalHistoryRepository(
        impl: RenewalHistoryRepositoryImpl
    ): RenewalHistoryRepository
}
