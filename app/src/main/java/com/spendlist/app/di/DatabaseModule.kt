package com.spendlist.app.di

import android.content.Context
import androidx.room.Room
import com.spendlist.app.data.local.SpendListDatabase
import com.spendlist.app.data.local.dao.CategoryDao
import com.spendlist.app.data.local.dao.CurrencyRateDao
import com.spendlist.app.data.local.dao.SubscriptionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SpendListDatabase {
        return Room.databaseBuilder(
            context,
            SpendListDatabase::class.java,
            "spend_list.db"
        )
            .addCallback(SpendListDatabase.SeedCallback(context))
            .build()
    }

    @Provides
    fun provideSubscriptionDao(database: SpendListDatabase): SubscriptionDao {
        return database.subscriptionDao()
    }

    @Provides
    fun provideCategoryDao(database: SpendListDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideCurrencyRateDao(database: SpendListDatabase): CurrencyRateDao {
        return database.currencyRateDao()
    }
}
