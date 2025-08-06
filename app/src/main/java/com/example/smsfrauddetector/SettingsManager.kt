package com.example.smsfrauddetector

import android.content.Context
import android.content.SharedPreferences

object SettingsManager {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_DETECTION_ENABLED = "detection_enabled"
    private const val KEY_SELECTED_MODEL_NAME = "selected_model_name"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isDetectionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DETECTION_ENABLED, true)
    }

    fun setDetectionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DETECTION_ENABLED, enabled).apply()
    }

    fun getSelectedModelName(context: Context): String? {
        return getPrefs(context).getString(KEY_SELECTED_MODEL_NAME, null)
    }

    fun setSelectedModelName(context: Context, modelName: String?) {
        getPrefs(context).edit().putString(KEY_SELECTED_MODEL_NAME, modelName).apply()
    }
    
    fun isOnboardingCompleted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }
    
    fun setOnboardingCompleted(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
    }
}