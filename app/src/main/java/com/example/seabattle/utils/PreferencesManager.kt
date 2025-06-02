package com.example.seabattle.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.seabattle.data.GameStatistics
import com.example.seabattle.game.AIPlayer

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("SeaBattlePrefs", Context.MODE_PRIVATE)

    companion object {
        // Ключи для настроек
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_UNBEATABLE_UNLOCKED = "unbeatable_unlocked"

        // Ключи для статистики
        private const val KEY_TOTAL_GAMES = "total_games"
        private const val KEY_WINS = "wins"
        private const val KEY_LOSSES = "losses"
        private const val KEY_WINS_VS_EASY = "wins_vs_easy"
        private const val KEY_WINS_VS_MEDIUM = "wins_vs_medium"
        private const val KEY_WINS_VS_HARD = "wins_vs_hard"
        private const val KEY_WINS_VS_UNBEATABLE = "wins_vs_unbeatable"
        private const val KEY_CURRENT_WIN_STREAK = "current_win_streak"
        private const val KEY_BEST_WIN_STREAK = "best_win_streak"
    }

    // Настройки
    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var isVibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()

    var isUnbeatableUnlocked: Boolean
        get() = prefs.getBoolean(KEY_UNBEATABLE_UNLOCKED, false)
        set(value) = prefs.edit().putBoolean(KEY_UNBEATABLE_UNLOCKED, value).apply()

    // Статистика
    fun saveStatistics(stats: GameStatistics) {
        prefs.edit().apply {
            putInt(KEY_TOTAL_GAMES, stats.totalGames)
            putInt(KEY_WINS, stats.wins)
            putInt(KEY_LOSSES, stats.losses)
            putInt(KEY_WINS_VS_EASY, stats.winsVsEasy)
            putInt(KEY_WINS_VS_MEDIUM, stats.winsVsMedium)
            putInt(KEY_WINS_VS_HARD, stats.winsVsHard)
            putInt(KEY_WINS_VS_UNBEATABLE, stats.winsVsUnbeatable)
            putInt(KEY_CURRENT_WIN_STREAK, stats.currentWinStreak)
            putInt(KEY_BEST_WIN_STREAK, stats.bestWinStreak)
            apply()
        }
    }

    fun loadStatistics(): GameStatistics {
        return GameStatistics(
            totalGames = prefs.getInt(KEY_TOTAL_GAMES, 0),
            wins = prefs.getInt(KEY_WINS, 0),
            losses = prefs.getInt(KEY_LOSSES, 0),
            winsVsEasy = prefs.getInt(KEY_WINS_VS_EASY, 0),
            winsVsMedium = prefs.getInt(KEY_WINS_VS_MEDIUM, 0),
            winsVsHard = prefs.getInt(KEY_WINS_VS_HARD, 0),
            winsVsUnbeatable = prefs.getInt(KEY_WINS_VS_UNBEATABLE, 0),
            currentWinStreak = prefs.getInt(KEY_CURRENT_WIN_STREAK, 0),
            bestWinStreak = prefs.getInt(KEY_BEST_WIN_STREAK, 0)
        )
    }

    fun recordWin(difficulty: AIPlayer.Difficulty? = null) {
        val stats = loadStatistics()
        stats.totalGames++
        stats.wins++
        stats.currentWinStreak++

        if (stats.currentWinStreak > stats.bestWinStreak) {
            stats.bestWinStreak = stats.currentWinStreak
        }

        when (difficulty) {
            AIPlayer.Difficulty.EASY -> stats.winsVsEasy++
            AIPlayer.Difficulty.MEDIUM -> stats.winsVsMedium++
            AIPlayer.Difficulty.HARD -> {
                stats.winsVsHard++
                // Разблокируем Непобедимый уровень после победы над Высоким
                isUnbeatableUnlocked = true
            }
            AIPlayer.Difficulty.UNBEATABLE -> stats.winsVsUnbeatable++
            null -> {} // Игра против друга
        }

        saveStatistics(stats)
    }

    fun recordLoss() {
        val stats = loadStatistics()
        stats.totalGames++
        stats.losses++
        stats.currentWinStreak = 0
        saveStatistics(stats)
    }

    fun resetStatistics() {
        saveStatistics(GameStatistics())
    }
}
