package com.spendlist.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.spendlist.app.domain.model.Category

@Composable
fun resolvedCategoryName(category: Category): String {
    if (category.nameResKey == null) return category.name

    val context = LocalContext.current
    val resId = context.resources.getIdentifier(
        category.nameResKey, "string", context.packageName
    )
    return if (resId != 0) context.getString(resId) else category.name
}
