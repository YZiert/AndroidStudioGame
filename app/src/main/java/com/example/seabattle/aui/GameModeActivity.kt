package com.example.seabattle.aui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.seabattle.R
import com.example.seabattle.databinding.ActivityGameModeBinding
import com.example.seabattle.databinding.DialogDifficultyBinding
import com.example.seabattle.game.AIPlayer
import com.example.seabattle.utils.PreferencesManager
import com.example.seabattle.utils.SoundManager

class GameModeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameModeBinding
    private lateinit var soundManager: SoundManager
    private lateinit var prefsManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameModeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        soundManager = SoundManager(this)
        prefsManager = PreferencesManager(this)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            soundManager.playButtonClick()
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        binding.cardTwoPlayers.setOnClickListener {
            soundManager.playButtonClick()
            startTwoPlayerGame()
        }

        binding.cardVsAI.setOnClickListener {
            soundManager.playButtonClick()
            showDifficultyDialog()
        }
    }

    private fun startTwoPlayerGame() {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("GAME_MODE", GameActivity.GameMode.TWO_PLAYERS)
        startActivity(intent)
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    private fun showDifficultyDialog() {
        val dialogBinding = DialogDifficultyBinding.inflate(LayoutInflater.from(this))

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        // Проверяем, разблокирован ли Непобедимый уровень
        if (prefsManager.isUnbeatableUnlocked) {
            dialogBinding.btnUnbeatable.isEnabled = true
            dialogBinding.btnUnbeatable.setIconResource(R.drawable.ic_star)
            dialogBinding.tvUnbeatableHint.visibility = android.view.View.GONE
        }

        dialogBinding.btnEasy.setOnClickListener {
            soundManager.playButtonClick()
            startAIGame(AIPlayer.Difficulty.EASY)
            dialog.dismiss()
        }

        dialogBinding.btnMedium.setOnClickListener {
            soundManager.playButtonClick()
            startAIGame(AIPlayer.Difficulty.MEDIUM)
            dialog.dismiss()
        }

        dialogBinding.btnHard.setOnClickListener {
            soundManager.playButtonClick()
            startAIGame(AIPlayer.Difficulty.HARD)
            dialog.dismiss()
        }

        dialogBinding.btnUnbeatable.setOnClickListener {
            if (prefsManager.isUnbeatableUnlocked) {
                soundManager.playButtonClick()
                startAIGame(AIPlayer.Difficulty.UNBEATABLE)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun startAIGame(difficulty: AIPlayer.Difficulty) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("GAME_MODE", GameActivity.GameMode.VS_AI)
        intent.putExtra("AI_DIFFICULTY", difficulty.name)
        startActivity(intent)
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}
