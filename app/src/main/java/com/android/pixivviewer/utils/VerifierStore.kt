package com.android.pixivviewer.utils

import android.content.Context
import android.content.SharedPreferences

object VerifierStore {
    private const val PREF_NAME = "auth_prefs"
    private const val KEY_VERIFIER = "code_verifier"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // 儲存 Verifier
    fun saveVerifier(context: Context, verifier: String) {
        getPrefs(context).edit().putString(KEY_VERIFIER, verifier).apply()
    }

    // 讀取 Verifier
    fun getVerifier(context: Context): String {
        return getPrefs(context).getString(KEY_VERIFIER, "") ?: ""
    }
}