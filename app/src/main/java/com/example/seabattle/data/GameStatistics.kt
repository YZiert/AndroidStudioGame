package com.example.seabattle.data

data class GameStatistics(
    var totalGames: Int = 0,
    var wins: Int = 0,
    var losses: Int = 0,
    var winsVsEasy: Int = 0,
    var winsVsMedium: Int = 0,
    var winsVsHard: Int = 0,
    var winsVsUnbeatable: Int = 0,
    var currentWinStreak: Int = 0,
    var bestWinStreak: Int = 0
) {
    val winRate: Float
        get() = if (totalGames > 0) (wins.toFloat() / totalGames) * 100 else 0f
}
