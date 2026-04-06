package com.spendlist.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.spendlist.app.data.local.dao.CategoryDao
import com.spendlist.app.data.local.dao.CurrencyRateDao
import com.spendlist.app.data.local.dao.SubscriptionDao
import com.spendlist.app.data.local.entity.CategoryEntity
import com.spendlist.app.data.local.entity.CurrencyRateEntity
import com.spendlist.app.data.local.entity.SubscriptionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SubscriptionEntity::class,
        CategoryEntity::class,
        CurrencyRateEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class SpendListDatabase : RoomDatabase() {
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun currencyRateDao(): CurrencyRateDao

    class SeedCallback(private val context: Context) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            PRESET_CATEGORIES.forEachIndexed { index, category ->
                db.execSQL(
                    """
                    INSERT INTO categories (name, name_res_key, icon_name, color, is_preset, sort_order)
                    VALUES (?, ?, ?, ?, 1, ?)
                    """.trimIndent(),
                    arrayOf<Any?>(
                        category.name,
                        category.nameResKey,
                        category.iconName,
                        category.color,
                        index
                    )
                )
            }
        }
    }

    companion object {
        val PRESET_CATEGORIES = listOf(
            CategoryEntity(
                name = "AI Tools", nameResKey = "category_ai",
                iconName = "SmartToy", color = 0xFFFF6B35, isPreset = true
            ),
            CategoryEntity(
                name = "Infrastructure", nameResKey = "category_infra",
                iconName = "Dns", color = 0xFF4ECDC4, isPreset = true
            ),
            CategoryEntity(
                name = "Entertainment", nameResKey = "category_entertainment",
                iconName = "SportsEsports", color = 0xFFFF6B6B, isPreset = true
            ),
            CategoryEntity(
                name = "Tools", nameResKey = "category_tools",
                iconName = "Build", color = 0xFF45B7D1, isPreset = true
            ),
            CategoryEntity(
                name = "Cloud", nameResKey = "category_cloud",
                iconName = "Cloud", color = 0xFF96CEB4, isPreset = true
            ),
            CategoryEntity(
                name = "Domains", nameResKey = "category_domain",
                iconName = "Language", color = 0xFFFFEAA7, isPreset = true
            ),
            CategoryEntity(
                name = "Storage", nameResKey = "category_storage",
                iconName = "Storage", color = 0xFFDDA0DD, isPreset = true
            ),
            CategoryEntity(
                name = "Other", nameResKey = "category_other",
                iconName = "MoreHoriz", color = 0xFF95A5A6, isPreset = true
            )
        )
    }
}
