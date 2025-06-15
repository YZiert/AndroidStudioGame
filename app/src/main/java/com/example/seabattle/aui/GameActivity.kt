package com.example.seabattle.aui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import com.example.seabattle.BaseActivity
import com.example.seabattle.R
import com.example.seabattle.data.CellState
import com.example.seabattle.data.Ship
import com.example.seabattle.databinding.ActivityGameBinding
import com.example.seabattle.game.*
import com.example.seabattle.utils.PreferencesManager
import com.example.seabattle.utils.SoundManager
import com.example.seabattle.utils.VibrationManager
import com.google.android.material.button.MaterialButton

class GameActivity : BaseActivity() {

    private lateinit var binding: ActivityGameBinding
    private lateinit var soundManager: SoundManager
    private lateinit var vibrationManager: VibrationManager
    private lateinit var prefsManager: PreferencesManager

    // Адаптеры для сеток
    private lateinit var playerGridAdapter: GameGridAdapter
    private lateinit var enemyGridAdapter: GameGridAdapter

    // Умный ИИ
    private lateinit var smartAI: SmartBattleshipAI
    private var aiDifficultyLevel: SmartBattleshipAI.Difficulty = SmartBattleshipAI.Difficulty.MEDIUM

    // Игровые данные
    private val playerBoard = GameBoard()
    private val aiBoard = GameBoard()
    private var playerTurn = true

    // Фаза игры
    private enum class GamePhase { SETUP, BATTLE }
    private var gamePhase = GamePhase.SETUP

    // Размещение кораблей
    private val shipsToPlace = mutableListOf(4, 3, 3, 2, 2, 2, 1, 1, 1, 1)
    private var currentShipIndex = 0
    private var isHorizontal = true

    // Отображение полей
    private var showingPlayerField = true

    // Cheat Mode
    private var cheatModeActive = false
    private val cheatPassword = "SKVORODA"

    // Handler для задержек
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            // Инициализация компонентов
            soundManager = SoundManager(this)
            vibrationManager = VibrationManager(this)
            prefsManager = PreferencesManager(this)

            // Получаем параметры игры
            val gameMode = intent.getStringExtra("GAME_MODE") ?: "VS_AI"
            val aiDifficulty = intent.getStringExtra("AI_DIFFICULTY") ?: "EASY"

            // Устанавливаем уровень сложности ИИ
            aiDifficultyLevel = when (aiDifficulty) {
                "EASY" -> SmartBattleshipAI.Difficulty.EASY
                "MEDIUM" -> SmartBattleshipAI.Difficulty.MEDIUM
                "HARD" -> SmartBattleshipAI.Difficulty.HARD
                "UNBEATABLE" -> SmartBattleshipAI.Difficulty.UNBEATABLE
                else -> SmartBattleshipAI.Difficulty.MEDIUM
            }

            smartAI = SmartBattleshipAI(GameBoard.BOARD_SIZE, aiDifficultyLevel)

            // Настройка UI
            setupUI()
            setupGrids()
            setupButtonListeners()

            // Показываем уведомление о режиме игры
            if (gameMode == "TWO_PLAYERS") {
                Toast.makeText(this, "Режим: 2 игрока (�� разработке)", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Режим: Против ИИ ($aiDifficulty)", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e("GameActivity", "Ошибка в onCreate: ${e.message}")
            Toast.makeText(this, "Ошибка инициализации игры", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupUI() {
        // Начальная фаза - расстановка кораблей
        binding.setupContainer.visibility = View.VISIBLE
        binding.battleContainer.visibility = View.GONE
        
        // Показываем поле игрока
        showPlayerField()
        
        // Установка текста заголовка
        binding.tvCurrentPlayer.text = "Разместите ваши корабли"
        
        // Обновляем информацию о текущем корабле
        updateShipInfo()
    }

    private fun setupGrids() {
        // Настройка сетки игрока
        playerGridAdapter = GameGridAdapter(GameBoard.BOARD_SIZE) { x, y ->
            handleCellClick(x, y, true)
        }
        
        binding.playerGrid.apply {
            layoutManager = GridLayoutManager(this@GameActivity, GameBoard.BOARD_SIZE)
            adapter = playerGridAdapter
        }

        // Настройка сетки противника
        enemyGridAdapter = GameGridAdapter(GameBoard.BOARD_SIZE) { x, y ->
            handleCellClick(x, y, false)
        }
        
        binding.enemyGrid.apply {
            layoutManager = GridLayoutManager(this@GameActivity, GameBoard.BOARD_SIZE)
            adapter = enemyGridAdapter
        }
    }

    private fun setupButtonListeners() {
        // Кнопка поворота корабля
        binding.btnRotate.setOnClickListener {
            soundManager.playButtonClick()
            isHorizontal = !isHorizontal
            binding.btnRotate.text = if (isHorizontal) "Горизонтально" else "Вертикально"
        }

        // Кнопка случайного размещения
        binding.btnRandomPlace.setOnClickListener {
            try {
                soundManager.playButtonClick()
                placeShipsRandomly()
            } catch (e: Exception) {
                Log.e("GameActivity", "Ошибка при случайном размещении: ${e.message}")
                // Сбрасываем звуковую систему при ошибке
                soundManager.resetSoundSystem()
                placeShipsRandomly()
            }
        }

        // Кнопка готовности
        binding.btnReady.setOnClickListener {
            soundManager.playButtonClick()
            if (currentShipIndex >= shipsToPlace.size) {
                startBattle()
            } else {
                Toast.makeText(this, "Разместите все корабли", Toast.LENGTH_SHORT).show()
            }
        }

        // Кнопка переключения полей
        binding.btnSwitchBoard.setOnClickListener {
            soundManager.playButtonClick()
            switchField()
        }

        // Кнопка возврата
        binding.btnBack.setOnClickListener {
            soundManager.playButtonClick()
            showExitDialog()
        }

        // Долгое нажатие на заголовок для активации Cheat Mode
        binding.tvCurrentPlayer.setOnLongClickListener {
            if (gamePhase == GamePhase.BATTLE) {
                showCheatDialog()
            }
            true
        }
    }

    private fun showCheatDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cheat, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val etCheatPassword = dialogView.findViewById<EditText>(R.id.etCheatPassword)
        val btnCheatCancel = dialogView.findViewById<MaterialButton>(R.id.btnCheatCancel)
        val btnCheatConfirm = dialogView.findViewById<MaterialButton>(R.id.btnCheatConfirm)

        btnCheatCancel.setOnClickListener {
            soundManager.playButtonClick()
            dialog.dismiss()
        }

        btnCheatConfirm.setOnClickListener {
            soundManager.playButtonClick()
            val enteredPassword = etCheatPassword.text.toString().trim()
            
            if (enteredPassword == cheatPassword) {
                dialog.dismiss()
                activateCheatMode()
            } else {
                Toast.makeText(this, "Неверный пароль", Toast.LENGTH_SHORT).show()
                etCheatPassword.text?.clear()
            }
        }

        dialog.show()
    }

    private fun activateCheatMode() {
        cheatModeActive = true
        Toast.makeText(this, "Cheat Mode активирован! Корабли противника видны.", Toast.LENGTH_LONG).show()
        updateEnemyGrid() // Обновляем отображение с показом кораблей
    }

    private fun handleCellClick(x: Int, y: Int, isPlayerGrid: Boolean) {
        try {
            if (gamePhase == GamePhase.SETUP) {
                // В фазе подготовки обрабатываем только клики по своей доске
                if (isPlayerGrid && showingPlayerField) {
                    tryPlaceShip(x, y)
                }
            } else {
                // В фазе боя обрабатываем только клики по доске противника и только в свой ход
                if (!isPlayerGrid && !showingPlayerField && playerTurn) {
                    processPlayerShot(x, y)
                }
            }
        } catch (e: Exception) {
            Log.e("GameActivity", "Ошибка в handleCellClick: ${e.message}")
        }
    }

    private fun tryPlaceShip(x: Int, y: Int) {
        try {
            if (currentShipIndex >= shipsToPlace.size) {
                Toast.makeText(this, "Все корабли размещены", Toast.LENGTH_SHORT).show()
                return
            }

            val shipSize = shipsToPlace[currentShipIndex]
            val ship = Ship(shipSize, isHorizontal = isHorizontal)

            if (playerBoard.canPlaceShip(ship, x, y)) {
                playerBoard.placeShip(ship, x, y)
                vibrationManager.vibrateSuccess()
                updatePlayerGrid()

                currentShipIndex++
                updateShipInfo()

                if (currentShipIndex >= shipsToPlace.size) {
                    binding.btnReady.visibility = View.VISIBLE
                    Toast.makeText(this, "Все корабли размещены! Нажмите ГОТОВО", Toast.LENGTH_SHORT).show()
                }
            } else {
                vibrationManager.vibrateError()
                Toast.makeText(this, "Нельзя разместить здесь корабль", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("GameActivity", "Ошибка в tryPlaceShip: ${e.message}")
        }
    }

    private fun placeShipsRandomly() {
        try {
            playerBoard.reset()
            currentShipIndex = 0

            for (shipSize in shipsToPlace) {
                var placed = false
                var attempts = 0

                while (!placed && attempts < 100) {
                    val x = (0 until GameBoard.BOARD_SIZE).random()
                    val y = (0 until GameBoard.BOARD_SIZE).random()
                    val horizontal = listOf(true, false).random()

                    val ship = Ship(shipSize, isHorizontal = horizontal)
                    if (playerBoard.canPlaceShip(ship, x, y)) {
                        playerBoard.placeShip(ship, x, y)
                        placed = true
                    }
                    attempts++
                }
            }

            currentShipIndex = shipsToPlace.size
            updatePlayerGrid()
            binding.btnReady.visibility = View.VISIBLE

            Toast.makeText(this, "Корабли размещены случайным образом!", Toast.LENGTH_SHORT).show()
            updateShipInfo()
        } catch (e: Exception) {
            Log.e("GameActivity", "Ошибка в placeShipsRandomly: ${e.message}")
        }
    }

    private fun updateShipInfo() {
        val remainingText = if (currentShipIndex < shipsToPlace.size) {
            val shipSize = shipsToPlace[currentShipIndex]
            "Разместите корабль: $shipSize клеток (осталось: ${shipsToPlace.size - currentShipIndex})"
        } else {
            "Все корабли размещены! Нажмите ГОТОВО"
        }
        binding.tvShipInfo.text = remainingText
    }

    private fun startBattle() {
        try {
            // Размещаем корабли компьютера
            placeAIShips()

            // Переключаемся в боевую фазу
            gamePhase = GamePhase.BATTLE

            // Обновляем UI - заменяем setupContainer на battleContainer
            binding.setupContainer.visibility = View.GONE
            binding.battleContainer.visibility = View.VISIBLE
            
            // Обновляем constraint для gameFieldContainer
            val params = binding.gameFieldContainer.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.topToBottom = binding.battleContainer.id
            binding.gameFieldContainer.layoutParams = params

            // Показываем поле противника для атаки
            showEnemyField()

            // Обновляем доски
            updatePlayerGrid()
            updateEnemyGrid()

            // Ход игрока
            playerTurn = true

            // Обновляем текст
            binding.tvStatus.text = "Ваш ход. Стреляйте по полю противника."
            binding.tvCurrentPlayer.text = "Битва! (Долгое нажатие для читов)"

            // Запускаем фоновую музыку
            soundManager.playBackgroundMusic()
            
            // Уведомление
            Toast.makeText(this, "Битва началась! Стреляйте по полю противника.", Toast.LENGTH_SHORT).show()
            
            // Отладочная информация
            Log.d("GameActivity", "AI ships placed: ${aiBoard.ships.size}")
            Log.d("GameActivity", "AI difficulty: $aiDifficultyLevel")
        } catch (e: Exception) {
            Log.e("GameActivity", "Ошибка �� startBattle: ${e.message}")
        }
    }

    private fun placeAIShips() {
        try {
            aiBoard.reset()
            Log.d("GameActivity", "Начинаем размещение кораблей ИИ")

            for (shipSize in shipsToPlace) {
                var placed = false
                var attempts = 0

                while (!placed && attempts < 100) {
                    val x = (0 until GameBoard.BOARD_SIZE).random()
                    val y = (0 until GameBoard.BOARD_SIZE).random()
                    val horizontal = listOf(true, false).random()

                    val ship = Ship(shipSize, isHorizontal = horizontal)
                    if (aiBoard.canPlaceShip(ship, x, y)) {
                        val success = aiBoard.placeShip(ship, x, y)
                        if (success) {
                            placed = true
                            Log.d("GameActivity", "Размещен корабль размера $shipSize в ($x, $y), горизонтально: $horizontal")
                        }
                    }
                    attempts++
                }
                
                if (!placed) {
                    Log.e("GameActivity", "Не удалось разместить корабль размера $shipSize")
                }
            }
            
            Log.d("GameActivity", "Размещение кораблей ИИ завершено. Всего кораблей: ${aiBoard.ships.size}")
        } catch (e: Exception) {
            Log.e("GameActivity", "Ошибка в placeAIShips: ${e.message}")
        }
    }

    private fun processPlayerShot(x: Int, y: Int) {
        try {
            if (!playerTurn) return

            // Воспроизводим звук выстрела
            soundManager.playShot()

            val result = aiBoard.shoot(x, y)

            when (result) {
                ShotResult.MISS -> {
                    soundManager.playMiss()
                    vibrationManager.vibrateShort()
                    updateEnemyGrid()
                    binding.tvStatus.text = "Промах! Ход компьютера."

                    // Переход хода к компьютеру
                    playerTurn = false

                    // Ход компьютера с задержкой
                    handler.postDelayed({
                        makeAIMove()
                    }, 1000)
                }
                ShotResult.HIT -> {
                    soundManager.playHit()
                    vibrationManager.vibrateMedium()
                    updateEnemyGrid()
                    binding.tvStatus.text = "Попадание! Стреляйте еще раз."
                }
                ShotResult.DESTROYED -> {
                    soundManager.playExplosion()
                    vibrationManager.vibrateLong()
                    updateEnemyGrid()

                    if (aiBoard.areAllShipsDestroyed()) {
                        endGame(true)
                    } else {
                        binding.tvStatus.text = "Корабль уничтожен! Стреляйте еще раз."
                    }
                }
                ShotResult.ALREADY_SHOT -> {
                    vibrationManager.vibrateError()
                    Toast.makeText(this, "Вы уже стреляли в эту клетку", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        } catch (e: Exception) {
            Log.e("GameActivity", "Ошибка в processPlayerShot: ${e.message}")
        }
    }

    private fun makeAIMove() {
        try {
            // Получаем координаты выстрела от ИИ
            val (x, y) = smartAI.makeShot(playerBoard)
            
            // Делаем выстрел
            val result = playerBoard.shoot(x, y)
            
            // Обновляем ИИ о результате выстрела
            smartAI.updateShotResult(x, y, result)

            when (result) {
                ShotResult.MISS -> {
                    soundManager.playMiss()
                    updatePlayerGrid()
                    binding.tvStatus.text = "Компьютер промахнулся! Ваш ход."
                    playerTurn = true
                }
                ShotResult.HIT, ShotResult.DESTROYED -> {
                    soundManager.playHit()
                    updatePlayerGrid()

                    if (result == ShotResult.DESTROYED && playerBoard.areAllShipsDestroyed()) {
                        endGame(false)
                    } else {
                        binding.tvStatus.text = "Компьютер попал! Компьютер ходит снова."
                        
                        // Компьютер ходит снова через 1 секунду
                        handler.postDelayed({
                            makeAIMove()
                        }, 1000)
                    }
                }
                else -> {
                    playerTurn = true
                    binding.tvStatus.text = "Ваш ход."
                }
            }
        } catch (e: Exception) {
            Log.e("GameActivity", "Ошибка в makeAIMove: ${e.message}")
            playerTurn = true
            binding.tvStatus.text = "Ваш ход."
        }
    }

    private fun showPlayerField() {
        showingPlayerField = true
        binding.playerFieldLayout.visibility = View.VISIBLE
        binding.enemyFieldLayout.visibility = View.GONE
        binding.btnSwitchBoard.text = "Показать поле противника"
    }

    private fun showEnemyField() {
        showingPlayerField = false
        binding.playerFieldLayout.visibility = View.GONE
        binding.enemyFieldLayout.visibility = View.VISIBLE
        binding.btnSwitchBoard.text = "Показать мое поле"
    }

    private fun switchField() {
        if (gamePhase == GamePhase.BATTLE) {
            if (showingPlayerField) {
                showEnemyField()
            } else {
                showPlayerField()
            }
        }
    }

    private fun updatePlayerGrid() {
        try {
            val cells = Array(GameBoard.BOARD_SIZE) { Array(GameBoard.BOARD_SIZE) { CellState.EMPTY } }
            
            for (x in 0 until GameBoard.BOARD_SIZE) {
                for (y in 0 until GameBoard.BOARD_SIZE) {
                    val cell = playerBoard.getCell(x, y)
                    cells[y][x] = cell?.state ?: CellState.EMPTY
                }
            }
            
            playerGridAdapter.updateAllCells(cells)
        } catch (e: Exception) {
            Log.e("GameActivity", "Ошибка в updatePlayerGrid: ${e.message}")
        }
    }

    private fun updateEnemyGrid() {
        try {
            val cells = Array(GameBoard.BOARD_SIZE) { Array(GameBoard.BOARD_SIZE) { CellState.EMPTY } }
            
            for (x in 0 until GameBoard.BOARD_SIZE) {
                for (y in 0 until GameBoard.BOARD_SIZE) {
                    val cell = aiBoard.getCell(x, y)
                    
                    if (cheatModeActive) {
                        // В режиме читов показываем все корабли
                        cells[y][x] = cell?.state ?: CellState.EMPTY
                    } else {
                        // Обычный режим - скрываем неповрежденные корабли
                        cells[y][x] = when (cell?.state) {
                            CellState.HIT, CellState.MISS, CellState.DESTROYED -> cell.state
                            else -> CellState.EMPTY
                        }
                    }
                }
            }
            
            enemyGridAdapter.updateAllCells(cells)
        } catch (e: Exception) {
            Log.e("GameActivity", "Ошибка в updateEnemyGrid: ${e.message}")
        }
    }

    private fun endGame(playerWon: Boolean) {
        try {
            // Записываем статистику
            if (playerWon) {
                val difficulty = when (aiDifficultyLevel) {
                    SmartBattleshipAI.Difficulty.EASY -> AIPlayer.Difficulty.EASY
                    SmartBattleshipAI.Difficulty.MEDIUM -> AIPlayer.Difficulty.MEDIUM
                    SmartBattleshipAI.Difficulty.HARD -> AIPlayer.Difficulty.HARD
                    SmartBattleshipAI.Difficulty.UNBEATABLE -> AIPlayer.Difficulty.UNBEATABLE
                }
                prefsManager.recordWin(difficulty)
            } else {
                prefsManager.recordLoss()
            }

            val message = if (playerWon) {
                soundManager.playVictory()
                "Поздравляем! Вы победили!"
            } else {
                soundManager.playDefeat()
                "Компьютер победил!"
            }

            AlertDialog.Builder(this)
                .setTitle("Игра окончена")
                .setMessage(message)
                .setPositiveButton("Новая игра") { _, _ ->
                    recreate()
                }
                .setNegativeButton("В меню") { _, _ ->
                    finish()
                }
                .setCancelable(false)
                .show()
        } catch (e: Exception) {
            Log.e("GameActivity", "Ошибка в endGame: ${e.message}")
        }
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setTitle("Выход из игры")
            .setMessage("Вы уверены, что хотите выйти? Прогресс будет потерян.")
            .setPositiveButton("Да") { _, _ ->
                finish()
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            soundManager.release()
        } catch (e: Exception) {
            Log.e("GameActivity", "Ошибка при освобождении ресурсов SoundManager: ${e.message}")
        }
    }
}
