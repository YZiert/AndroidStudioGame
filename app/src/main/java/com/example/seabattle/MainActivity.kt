package com.example.seabattle

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import com.example.seabattle.aui.GameModeActivity
import com.example.seabattle.aui.SettingsActivity
import com.example.seabattle.aui.StatisticsActivity
import com.example.seabattle.databinding.ActivityMainBinding
import com.example.seabattle.utils.SoundManager

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализация звукового менеджера
        soundManager = SoundManager(this)

        // Анимация появления элементов
        animateViews()

        // Настройка кликов
        setupClickListeners()
    }

    private fun animateViews() {
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)

        binding.logoImage.startAnimation(fadeIn)
        binding.gameTitle.startAnimation(fadeIn)

        binding.buttonsContainer.startAnimation(slideUp)
    }

    private fun setupClickListeners() {
        binding.btnNewGame.setOnClickListener {
            soundManager.playButtonClick()
            startActivity(Intent(this, GameModeActivity::class.java))
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        binding.btnSettings.setOnClickListener {
            soundManager.playButtonClick()
            startActivity(Intent(this, SettingsActivity::class.java))
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        binding.btnStatistics.setOnClickListener {
            soundManager.playButtonClick()
            startActivity(Intent(this, StatisticsActivity::class.java))
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        binding.btnExit.setOnClickListener {
            soundManager.playButtonClick()
            showExitDialog()
        }
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this, R.style.DialogTheme)
            .setTitle("Выход из игры")
            .setMessage("Вы действительно хотите выйти?")
            .setPositiveButton("Да") { _, _ ->
                finish()
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        soundManager.playBackgroundMusic()
    }

    override fun onPause() {
        super.onPause()
        soundManager.pauseBackgroundMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}
