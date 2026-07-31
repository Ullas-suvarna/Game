package com.example.data

import android.content.Context
import android.content.SharedPreferences

class ScoreRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var bestScore: Int
        get() = prefs.getInt(KEY_BEST_SCORE, 0)
        set(value) {
            prefs.edit().putInt(KEY_BEST_SCORE, value).apply()
        }

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()
        }

    /**
     * Checks if newScore is a high score and updates it if true.
     * Returns true if a new high score was set.
     */
    fun checkAndUpdateHighScore(newScore: Int): Boolean {
        if (newScore > bestScore) {
            bestScore = newScore
            return true
        }
        return false
    }

    companion object {
        private const val PREFS_NAME = "tap_the_circle_prefs"
        private const val KEY_BEST_SCORE = "key_best_score"
        private const val KEY_SOUND_ENABLED = "key_sound_enabled"
    }
}
