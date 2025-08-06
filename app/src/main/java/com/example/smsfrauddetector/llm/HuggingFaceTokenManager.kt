package com.example.smsfrauddetector.llm

import android.content.Context
import android.content.SharedPreferences

class HuggingFaceTokenManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("hugging_face_token", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        sharedPreferences.edit().putString("token", token).apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString("token", null)
    }
}