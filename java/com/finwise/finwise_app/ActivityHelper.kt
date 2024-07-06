package com.finwise.finwise_app
import android.app.Activity
import android.content.Intent


object ActivityHelper {
    const val SELECTED_SUBCATEGORY_KEY = "selectedSubcategory"

    fun passSelectedSubcategory(sourceActivity: Activity, subcategory: String) {
        val intent = Intent(sourceActivity, expense_log::class.java)
        intent.putExtra(SELECTED_SUBCATEGORY_KEY, subcategory)
        sourceActivity.startActivity(intent)
    }

    fun extractSelectedSubcategory(targetActivity: Activity): String? {
        return targetActivity.intent.getStringExtra(SELECTED_SUBCATEGORY_KEY)
    }
}
