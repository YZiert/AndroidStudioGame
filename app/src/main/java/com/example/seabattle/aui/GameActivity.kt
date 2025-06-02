package com.example.seabattle.aui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.seabattle.R
import com.example.seabattle.data.Cell
import com.example.seabattle.data.CellState
import com.example.seabattle.data.Ship
import com.example.seabattle.databinding.ActivityGameBinding
import com.example.seabattle.game.*
import com.example.seabattle.utils.PreferencesManager
import com.example.seabattle.utils.SoundManager
import com.example.seabattle.utils.VibrationManager

class GameActivity : AppCompatActivity() {

    enum class GameMode {
        TWO_PLAYERS,
        VS_AI
    }

    enum class GamePhase {
        SETUP_PLAYER1,
        SETUP_PLAYER2,
        BATTLE,
        GAME_OVER
    }

    private lateinit var binding: ActivityGameBinding
    private lateinit var soundManager: SoundManager
    private lateinit var vibrationManager: VibrationManager
    private lateinit var prefsManager: PreferencesManager

    private lateinit var gameMode: GameMode
    private var aiDifficulty: AIPlayer.Difficulty? = null
    private var currentPhase = GamePhase.SETUP_PLAYER1

    private val player1Board = GameBoard()
    private val player2Board = GameBoard()
    private var currentPlayerTurn = 1

    private var aiPlayer: AIPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    // Для размещения кораблей
    private val shipsToPlace = mutableListOf(4, 3, 3, 2, 2, 2, 1, 1, 1, 1)
    private var currentShipIndex = 0
    private var isPlacingHorizontal = true

    // Визуальные элементы
    private lateinit var player1GridCells: Array<Array<ImageView>>
    private lateinit var player2GridCells: Array<Array<ImageView>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        soundManager = SoundManager(this)
        vibrationManager = VibrationManager(this)
        prefsManager = PreferencesManager(this)

        // Получаем параметры игры
        gameMode = GameMode.valueOf(intent.getStringExtra("GAME_MODE") ?: GameMode.TWO_PLAYERS.name)

        if (gameMode == GameMode.VS_AI) {
            val difficultyName = intent.getStringExtra("AI_DIFFICULTY")
            aiDifficulty = difficultyName?.let { AIPlayer.Difficulty.valueOf(it) }
            initializeAI()
        }

        setupUI()
        startGame()
    }

    private fun initializeAI() {
        aiPlayer = when (aiDifficulty) {
            AIPlayer.Difficulty.EASY -> EasyAI()
            AIPlayer.Difficulty.MEDIUM -> MediumAI()
            AIPlayer.Difficulty.HARD -> HardAI()
            AIPlayer.Difficulty.UNBEATABLE -> UnbeatableAI()
            null -> EasyAI()
        }
    }

    private fun setupUI() {
        // Настройка сеток
        setupGameGrid(binding.player1Grid, true)
        setupGameGrid(binding.player2Grid, false)

        // Кнопки
        binding.btnRotate.setOnClickListener {
            soundManager.playButtonClick()
            isPlacingHorizontal = !isPlacingHorizontal
            updateRotateButton()
        }

        binding.btnRandomPlace.setOnClickListener {
            soundManager.playButtonClick()
            placeShipsRandomly(if (currentPhase == GamePhase.SETUP_PLAYER1) player1Board else player2Board)
        }

        binding.btnReady.setOnClickListener {
            soundManager.playButtonClick()
            onReadyButtonClick()
        }

        binding.btnBack.setOnClickListener {
            soundManager.playButtonClick()
            showExitDialog()
        }
    }

    private fun setupGameGrid(gridLayout: GridLayout, isPlayer1: Boolean) {
        gridLayout.removeAllViews()
        gridLayout.columnCount = GameBoard.BOARD_SIZE
        gridLayout.rowCount = GameBoard.BOARD_SIZE

        val cells = Array(GameBoard.BOARD_SIZE) { Array(GameBoard.BOARD_SIZE) { ImageView(this) } }

        for (y in 0 until GameBoard.BOARD_SIZE) {
            for (x in 0 until GameBoard.BOARD_SIZE) {
                val cell = ImageView(this)
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 0
                    columnSpec = GridLayout.spec(x, 1f)
                    rowSpec = GridLayout.spec(y, 1f)
                    setMargins(1, 1, 1, 1)
                }

                cell.layoutParams = params
                cell.setBackgroundColor(ContextCompat.getColor(this, R.color.cell_empty))
                cell.scaleType = ImageView.ScaleType.CENTER_CROP

                // Обработка кликов
                cell.setOnClickListener {
                    onCellClick(x, y, isPlayer1)
                }

                cells[x][y] = cell
                gridLayout.addView(cell)
            }
        }

        if (isPlayer1) {
            player1GridCells = cells
        } else {
            player2GridCells = cells
        }
    }

    private fun onCellClick(x: Int, y: Int, isPlayer1Grid: Boolean) {
        when (currentPhase) {
            GamePhase.SETUP_PLAYER1 -> {
                if (isPlayer1Grid) {
                    tryPlaceShip(x, y, player1Board)
                }
            }
            GamePhase.SETUP_PLAYER2 -> {
                if (!isPlayer1Grid && gameMode == GameMode.TWO_PLAYERS) {
                    tryPlaceShip(x, y, player2Board)
                }
            }
            GamePhase.BATTLE -> {
                if (gameMode == GameMode.TWO_PLAYERS) {
                    handleBattleClick(x, y, isPlayer1Grid)
                } else {
                    // В режиме против ИИ игрок стреляет только по доске ИИ
                    if (!isPlayer1Grid && currentPlayerTurn == 1) {
                        handleBattleClick(x, y, false)
                    }
                }
            }
            GamePhase.GAME_OVER -> {
                // Игра окончена, клики игнорируются
            }
        }
    }

    private fun tryPlaceShip(x: Int, y: Int, board: GameBoard) {
        if (currentShipIndex >= shipsToPlace.size) return

        val shipSize = shipsToPlace[currentShipIndex]
        val ship = Ship(shipSize, isHorizontal = isPlacingHorizontal)

        if (board.canPlaceShip(ship, x, y)) {
            board.placeShip(ship, x, y)
            vibrationManager.vibrateSuccess()
            updateBoardVisual(board, currentPhase == GamePhase.SETUP_PLAYER1)

            currentShipIndex++
            if (currentShipIndex >= shipsToPlace.size) {
                // Все корабли размещены
                binding.btnReady.visibility = View.VISIBLE
            } else {
                updateShipPlacementInfo()
            }
        } else {
            vibrationManager.vibrateError()
        }
    }

    private fun placeShipsRandomly(board: GameBoard) {
        board.reset()
        currentShipIndex = 0

        for (shipSize in shipsToPlace) {
            var placed = false
            var attempts = 0

            while (!placed && attempts < 100) {
                val x = (0 until GameBoard.BOARD_SIZE).random()
                val y = (0 until GameBoard.BOARD_SIZE).random()
                val horizontal = listOf(true, false).random()

                val ship = Ship(shipSize, isHorizontal = horizontal)
                if (board.canPlaceShip(ship, x, y)) {
                    board.placeShip(ship, x, y)
                    placed = true
                }
                attempts++
            }
        }

        currentShipIndex = shipsToPlace.size
        updateBoardVisual(board, currentPhase == GamePhase.SETUP_PLAYER1)
        binding.btnReady.visibility = View.VISIBLE
    }

    private fun updateBoardVisual(board: GameBoard, isPlayer1: Boolean) {
        val cells = if (isPlayer1) player1GridCells else player2GridCells

        for (x in 0 until GameBoard.BOARD_SIZE) {
            for (y in 0 until GameBoard.BOARD_SIZE) {
                val cell = board.getCell(x, y)
                val imageView = cells[x][y]

                when (cell?.state) {
                    CellState.EMPTY -> {
                        imageView.setBackgroundColor(ContextCompat.getColor(this, R.color.cell_empty))
                        imageView.setImageDrawable(null)
                    }
                    CellState.SHIP -> {
                        // Показываем корабли только в фазе расстановки или на своей доске в режиме ИИ
                        if (currentPhase != GamePhase.BATTLE ||
                            (isPlayer1 && gameMode == GameMode.VS_AI)) {
                            imageView.setBackgroundColor(ContextCompat.getColor(this, R.color.cell_ship))
                        } else {
                            imageView.setBackgroundColor(ContextCompat.getColor(this, R.color.cell_empty))
                        }
                    }
                    CellState.HIT -> {
                        imageView.setBackgroundColor(ContextCompat.getColor(this, R.color.cell_hit))
                        imageView.setImageResource(R.drawable.ic_hit)
                    }
                    CellState.MISS -> {
                        imageView.setBackgroundColor(ContextCompat.getColor(this, R.color.cell_miss))
                        imageView.setImageResource(R.drawable.ic_miss)
                    }
                    CellState.DESTROYED -> {
                        imageView.setBackgroundColor(ContextCompat.getColor(this, R.color.cell_destroyed))
                        imageView.setImageResource(R.drawable.ic_explosion)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun handleBattleClick(x: Int, y: Int, isPlayer1Grid: Boolean) {
        // Определяем, по какой доске стреляем
        val targetBoard = if (isPlayer1Grid) player1Board else player2Board
        val isValidShot = (currentPlayerTurn == 1 && !isPlayer1Grid) ||
                (currentPlayerTurn == 2 && isPlayer1Grid)

        if (!isValidShot) return

        val result = targetBoard.shoot(x, y)

        when (result) {
            ShotResult.MISS -> {
                soundManager.playMiss()
                vibrationManager.vibrateShort()
                updateBoardVisual(targetBoard, isPlayer1Grid)
                switchTurn()
            }
            ShotResult.HIT -> {
                soundManager.playHit()
                vibrationManager.vibrateMedium()
                updateBoardVisual(targetBoard, isPlayer1Grid)
                updateStatus("Попадание! Стреляйте еще раз")
            }
            ShotResult.DESTROYED -> {
                soundManager.playExplosion()
                vibrationManager.vibrateLong()
                updateBoardVisual(targetBoard, isPlayer1Grid)

                if (targetBoard.areAllShipsDestroyed()) {
                    endGame(currentPlayerTurn)
                } else {
                    updateStatus("Корабль уничтожен! Стреляйте еще раз")
                }
            }
            ShotResult.ALREADY_SHOT -> {
                vibrationManager.vibrateError()
            }
            else -> {}
        }

        // Если играем против ИИ и ход ИИ
        if (gameMode == GameMode.VS_AI && currentPlayerTurn == 2 && result == ShotResult.MISS) {
            makeAIMove()
        }
    }

    private fun makeAIMove() {
        binding.player2Grid.isEnabled = false
        updateStatus("Ход компьютера...")

        handler.postDelayed({
            val (x, y) = aiPlayer!!.makeShot(player1Board)
            val result = player1Board.shoot(x, y)
            aiPlayer!!.processShotResult(x, y, result, player1Board)

            when (result) {
                ShotResult.MISS -> {
                    soundManager.playMiss()
                    updateBoardVisual(player1Board, true)
                    switchTurn()
                    binding.player2Grid.isEnabled = true
                }
                ShotResult.HIT, ShotResult.DESTROYED -> {
                    soundManager.playHit()
                    updateBoardVisual(player1Board, true)

                    if (result == ShotResult.DESTROYED && player1Board.areAllShipsDestroyed()) {
                        endGame(2)
                    } else {
                        // ИИ продолжает ход после попадания
                        makeAIMove()
                    }
                }
                else -> {
                    // Повторяем ход при невалидном выстреле
                    makeAIMove()
                }
            }
        }, 1000)
    }

    private fun switchTurn() {
        currentPlayerTurn = if (currentPlayerTurn == 1) 2 else 1
        updateTurnIndicator()
    }

    private fun updateTurnIndicator() {
        val turnText = if (gameMode == GameMode.VS_AI) {
            if (currentPlayerTurn == 1) "Ваш ход" else "Ход компьютера"
        } else {
            "Ход игрока $currentPlayerTurn"
        }
        binding.tvCurrentTurn.text = turnText
    }

    private fun updateStatus(message: String) {
        binding.tvStatus.text = message
    }

    private fun updateShipPlacementInfo() {
        val shipSize = shipsToPlace[currentShipIndex]
        binding.tvShipInfo.text = "Разместите корабль: $shipSize клеток"
    }

    private fun updateRotateButton() {
        binding.btnRotate.text = if (isPlacingHorizontal) "Горизонтально" else "Вертикально"
    }

    private fun onReadyButtonClick() {
        when (currentPhase) {
            GamePhase.SETUP_PLAYER1 -> {
                if (gameMode == GameMode.TWO_PLAYERS) {
                    // Переход к размещению кораблей второго игрока
                    currentPhase = GamePhase.SETUP_PLAYER2
                    currentShipIndex = 0
                    binding.btnReady.visibility = View.GONE
                    binding.tvCurrentPlayer.text = "Игрок 2: Разместите корабли"
                    binding.player1Grid.visibility = View.GONE
                    binding.player2Grid.visibility = View.VISIBLE
                    updateShipPlacementInfo()
                } else {
                    // В режиме против ИИ сразу начинаем бой
                    setupAIShips()
                    startBattle()
                }
            }
            GamePhase.SETUP_PLAYER2 -> {
                startBattle()
            }
            else -> {}
        }
    }

    private fun setupAIShips() {
        placeShipsRandomly(player2Board)
    }

    private fun startBattle() {
        currentPhase = GamePhase.BATTLE
        binding.setupContainer.visibility = View.GONE
        binding.battleContainer.visibility = View.VISIBLE

        binding.player1Grid.visibility = View.VISIBLE
        binding.player2Grid.visibility = View.VISIBLE

        // Скрываем кнопки, не нужные в фазе боя
        binding.btnRotate.visibility = View.GONE
        binding.btnRandomPlace.visibility = View.GONE
        binding.btnReady.visibility = View.GONE

        updateBoardVisual(player1Board, true)
        updateBoardVisual(player2Board, false)

        // В режиме ИИ скрываем корабли противника
        if (gameMode == GameMode.VS_AI) {
            hideShipsOnBoard(player2Board, false)
        }

        updateTurnIndicator()
        updateStatus("Битва началась!")
    }

    private fun hideShipsOnBoard(board: GameBoard, isPlayer1: Boolean) {
        val cells = if (isPlayer1) player1GridCells else player2GridCells

        for (x in 0 until GameBoard.BOARD_SIZE) {
            for (y in 0 until GameBoard.BOARD_SIZE) {
                val cell = board.getCell(x, y)
                val imageView = cells[x][y]

                if (cell?.state == CellState.SHIP) {
                    imageView.setBackgroundColor(ContextCompat.getColor(this, R.color.cell_empty))
                }
            }
        }
    }

    private fun startGame() {
        when (gameMode) {
            GameMode.TWO_PLAYERS -> {
                binding.tvCurrentPlayer.text = "Игрок 1: Разместите корабли"
                binding.player2Grid.visibility = View.GONE
            }
            GameMode.VS_AI -> {
                binding.tvCurrentPlayer.text = "Разместите ваши корабли"
                binding.player2Grid.visibility = View.GONE
            }
        }

        updateShipPlacementInfo()
        updateRotateButton()
    }

    private fun endGame(winner: Int) {
        currentPhase = GamePhase.GAME_OVER

        val winnerText = if (gameMode == GameMode.VS_AI) {
            if (winner == 1) {
                soundManager.playVictory()
                prefsManager.recordWin(aiDifficulty)
                "Поздравляем! Вы победили!"
            } else {
                soundManager.playDefeat()
                prefsManager.recordLoss()
                "Компьютер победил!"
            }
        } else {
            soundManager.playVictory()
            prefsManager.recordWin()
            "Игрок $winner победил!"
        }

        AlertDialog.Builder(this)
            .setTitle("Игра окончена")
            .setMessage(winnerText)
            .setPositiveButton("Новая игра") { _, _ ->
                recreate()
            }
            .setNegativeButton("В меню") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
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
}
