package com.finwise.finwise_app

import android.content.Context
object SessionManager {
    private const val PREF_NAME = "session_pref"
    private const val KEY_IS_LOGGED_IN = "isLoggedIn"

    fun setIsLoggedIn(context: Context, isLoggedIn: Boolean) {
        val editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
        editor.apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun clearSession(context: Context) {
        val editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
        editor.clear()
        editor.apply()
    }
}