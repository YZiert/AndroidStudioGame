package com.example.seabattle.aui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.seabattle.BaseActivity
import com.example.seabattle.R
import com.example.seabattle.databinding.ActivityGameModeBinding
import com.example.seabattle.databinding.DialogDifficultyBinding
import com.example.seabattle.databinding.DialogPasswordBinding
import com.example.seabattle.game.AIPlayer
import com.example.seabattle.utils.PreferencesManager
import com.example.seabattle.utils.SoundManager

class GameModeActivity : BaseActivity() {

    private lateinit var binding: ActivityGameModeBinding
    private lateinit var soundManager: SoundManager
    private lateinit var prefsManager: PreferencesManager

    companion object {
        private const val DEVELOPER_PASSWORD = "DEVELOPER"
    }

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
            showPasswordDialog()
        }

        binding.cardVsAI.setOnClickListener {
            soundManager.playButtonClick()
            showDifficultyDialog()
        }
    }

    private fun showPasswordDialog() {
        val dialogBinding = DialogPasswordBinding.inflate(LayoutInflater.from(this))

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            soundManager.playButtonClick()
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            soundManager.playButtonClick()
            val enteredPassword = dialogBinding.etPassword.text.toString().trim()
            
            if (enteredPassword == DEVELOPER_PASSWORD) {
                dialog.dismiss()
                startTwoPlayerGame()
            } else {
                Toast.makeText(this, "Неверный пароль", Toast.LENGTH_SHORT).show()
                dialogBinding.etPassword.text?.clear()
            }
        }

        dialog.show()
    }

    private fun startTwoPlayerGame() {
        Toast.makeText(this, "Режим для 2 игроков находится в разработке", Toast.LENGTH_LONG).show()
        // Пока что просто показываем сообщение
        // В будущем здесь будет:
        // val intent = Intent(this, GameActivity::class.java)
        // intent.putExtra("GAME_MODE", "TWO_PLAYERS")
        // startActivity(intent)
        // overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
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
            startVsAIGame("EASY")
            dialog.dismiss()
        }

        dialogBinding.btnMedium.setOnClickListener {
            soundManager.playButtonClick()
            startVsAIGame("MEDIUM")
            dialog.dismiss()
        }

        dialogBinding.btnHard.setOnClickListener {
            soundManager.playButtonClick()
            startVsAIGame("HARD")
            dialog.dismiss()
        }

        dialogBinding.btnUnbeatable.setOnClickListener {
            if (prefsManager.isUnbeatableUnlocked) {
                soundManager.playButtonClick()
                startVsAIGame("UNBEATABLE")
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun startVsAIGame(difficulty: String) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("GAME_MODE", "VS_AI")
        intent.putExtra("AI_DIFFICULTY", difficulty)
        startActivity(intent)
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}
