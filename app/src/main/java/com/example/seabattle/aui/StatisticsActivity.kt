package com.example.seabattle.aui

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.seabattle.databinding.ActivityStatisticsBinding
import com.example.seabattle.utils.PreferencesManager
import com.example.seabattle.utils.SoundManager

class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding
    private lateinit var prefsManager: PreferencesManager
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferencesManager(this)
        soundManager = SoundManager(this)

        setupUI()
        loadStatistics()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            soundManager.playButtonClick()
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        binding.btnResetStats.setOnClickListener {
            soundManager.playButtonClick()
            showResetConfirmDialog()
        }
    }

    private fun loadStatistics() {
        val stats = prefsManager.loadStatistics()

        // Основная статистика
        binding.tvTotalGames.text = stats.totalGames.toString()
        binding.tvWins.text = stats.wins.toString()
        binding.tvLosses.text = stats.losses.toString()
        binding.tvWinRate.text = String.format("%.1f%%", stats.winRate)

        // Серии побед
        binding.tvCurrentStreak.text = stats.currentWinStreak.toString()
        binding.tvBestStreak.text = stats.bestWinStreak.toString()

        // Победы по уровням сложности
        binding.tvWinsEasy.text = stats.winsVsEasy.toString()
        binding.tvWinsMedium.text = stats.winsVsMedium.toString()
        binding.tvWinsHard.text = stats.winsVsHard.toString()
        binding.tvWinsUnbeatable.text = stats.winsVsUnbeatable.toString()
    }

    private fun showResetConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Сброс статистики")
            .setMessage("Вы уверены, что хотите сбросить всю статистику? Это действие нельзя отменить.")
            .setPositiveButton("Сбросить") { _, _ ->
                prefsManager.resetStatistics()
                loadStatistics()
                soundManager.playButtonClick()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onBackPressed() {
        soundManager.playButtonClick()
        super.onBackPressed()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}
