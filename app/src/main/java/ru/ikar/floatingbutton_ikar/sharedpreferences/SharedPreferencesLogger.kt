package ru.ikar.floatingbutton_ikar.sharedpreferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class SharedPreferencesLogger(context: Context, prefName: String) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
    private var sharedPreferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener
    init {
        sharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            // Здесь мы логируем только измененный ключ и его новое значение
            val value = sharedPreferences.all[key]
            Log.d("SharedPreferences", "Changed key: $key, New value: $value")

            // Если хотите логировать все значения после изменения, можно вызвать:
            logAllSharedPreferences()
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }

    private fun logAllSharedPreferences() {
        val allEntries = sharedPreferences.all
        for ((key, value) in allEntries) {
            Log.d("SharedPreferences", "$key: $value")
        }
    }

    fun unregisterListener() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }
}