package com.coderkillers.guiafacil.data.local

import android.content.Context
import android.content.SharedPreferences
import com.coderkillers.guiafacil.data.model.SavedProcedure
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LocalStorageManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("guiafacil_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_SAVED_PROCEDURES = "saved_procedures"
        private const val KEY_SELECTED_LANGUAGE = "selected_language"
        private const val KEY_DICTIONARY_JSON = "dictionary_json"
    }

    fun saveProcedure(procedure: SavedProcedure) {
        val currentList = getSavedProcedures().toMutableList()
        // Remove duplicate ID if exists, then add at front
        currentList.removeAll { it.id == procedure.id }
        currentList.add(0, procedure)
        saveList(currentList)
    }

    fun getSavedProcedures(): List<SavedProcedure> {
        val json = prefs.getString(KEY_SAVED_PROCEDURES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SavedProcedure>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun updateProcedure(updated: SavedProcedure) {
        val list = getSavedProcedures().toMutableList()
        val index = list.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            list[index] = updated
            saveList(list)
        }
    }

    fun deleteProcedure(id: String) {
        val list = getSavedProcedures().toMutableList()
        list.removeAll { it.id == id }
        saveList(list)
    }

    private fun saveList(list: List<SavedProcedure>) {
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_SAVED_PROCEDURES, json).apply()
    }

    fun saveSelectedLanguage(lang: String) {
        prefs.edit().putString(KEY_SELECTED_LANGUAGE, lang).apply()
    }

    fun getSelectedLanguage(): String {
        return prefs.getString(KEY_SELECTED_LANGUAGE, "Español") ?: "Español"
    }

    fun saveDictionary(dict: Map<String, String>) {
        val json = gson.toJson(dict)
        prefs.edit().putString(KEY_DICTIONARY_JSON, json).apply()
    }

    fun getDictionary(): Map<String, String>? {
        val json = prefs.getString(KEY_DICTIONARY_JSON, null) ?: return null
        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }
}
