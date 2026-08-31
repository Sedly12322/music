package com.example.musicapp.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray

class FavoritesManager(context: Context) {
    private val prefs = context.getSharedPreferences("music_app_favorites", Context.MODE_PRIVATE)

    // Získá seznam všech oblíbených URI jako Stringy
    fun getFavorites(): Set<String> {
        val jsonString = prefs.getString("fav_uris", "[]")
        val jsonArray = JSONArray(jsonString)
        val list = mutableSetOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getString(i))
        }
        return list
    }

    // Přepne stav (přidá nebo odebere) a vrátí TRUE, pokud je nyní oblíbená
    fun toggleFavorite(uri: Uri): Boolean {
        val current = getFavorites().toMutableSet()
        val uriString = uri.toString()
        val isAdded: Boolean

        if (current.contains(uriString)) {
            current.remove(uriString)
            isAdded = false
        } else {
            current.add(uriString)
            isAdded = true
        }

        saveFavorites(current)
        return isAdded
    }

    // Zkontroluje, zda je konkrétní URI oblíbené
    fun isFavorite(uri: Uri): Boolean {
        return getFavorites().contains(uri.toString())
    }

    private fun saveFavorites(uris: Set<String>) {
        val jsonArray = JSONArray()
        uris.forEach { jsonArray.put(it) }
        prefs.edit().putString("fav_uris", jsonArray.toString()).apply()
    }
}