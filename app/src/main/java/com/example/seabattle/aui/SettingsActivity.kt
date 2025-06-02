package com.example.seabattle.aui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.seabattle.databinding.ActivitySettingsBinding
import com.example.seabattle.utils.PreferencesManager
import com.example.seabattle.utils.SoundManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefsManager: PreferencesManager
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferencesManager(this)
        soundManager = SoundManager(this)

        setupUI()
        loadSettings()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            soundManager.playButtonClick()
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            soundManager.playButtonClick()
            prefsManager.isSoundEnabled = isChecked

            if (isChecked) {
                soundManager.playBackgroundMusic()
            } else {
                soundManager.stopBackgroundMusic()
            }
        }

        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            soundManager.playButtonClick()
            prefsManager.isVibrationEnabled = isChecked
        }
    }

    private fun loadSettings() {
        binding.switchSound.isChecked = prefsManager.isSoundEnabled
        binding.switchVibration.isChecked = prefsManager.isVibrationEnabled
    }

    override fun onBackPressed() {
        soundManager.playButtonClick()
        super.onBackPressed()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}
