package com.example.seabattle.game

import com.example.seabattle.data.Cell
import com.example.seabattle.data.CellState

abstract class AIPlayer(val difficulty: Difficulty) {

    enum class Difficulty {
        EASY,
        MEDIUM,
        HARD,
        UNBEATABLE
    }

    protected val shotHistory = mutableListOf<Pair<Int, Int>>()
    protected val hits = mutableListOf<Cell>()
    protected var lastHit: Cell? = null
    protected var huntingMode = false
    protected var targetCells = mutableListOf<Pair<Int, Int>>()

    abstract fun makeShot(enemyBoard: GameBoard): Pair<Int, Int>

    fun processShotResult(x: Int, y: Int, result: ShotResult, board: GameBoard) {
        shotHistory.add(x to y)

        when (result) {
            ShotResult.HIT -> {
                val cell = board.getCell(x, y)
                if (cell != null) {
                    hits.add(cell)
                    lastHit = cell
                    huntingMode = true
                    updateTargetCells(x, y, board)
                }
            }
            ShotResult.DESTROYED -> {
                huntingMode = false
                targetCells.clear()
                lastHit = null
                hits.removeAll { cell ->
                    cell.ship?.isDestroyed() == true
                }
            }
            else -> {}
        }
    }

    protected fun updateTargetCells(x: Int, y: Int, board: GameBoard) {
        val adjacentCells = listOf(
            x - 1 to y,
            x + 1 to y,
            x to y - 1,
            x to y + 1
        )

        for ((cx, cy) in adjacentCells) {
            if (cx in 0 until GameBoard.BOARD_SIZE &&
                cy in 0 until GameBoard.BOARD_SIZE &&
                !shotHistory.contains(cx to cy)) {
                targetCells.add(cx to cy)
            }
        }
    }

    protected fun getRandomUntriedCell(board: GameBoard): Pair<Int, Int> {
        val availableCells = mutableListOf<Pair<Int, Int>>()

        for (x in 0 until GameBoard.BOARD_SIZE) {
            for (y in 0 until GameBoard.BOARD_SIZE) {
                if (!shotHistory.contains(x to y)) {
                    availableCells.add(x to y)
                }
            }
        }

        return availableCells.random()
    }
}

// Лёгкий ИИ — случайные выстрелы
class EasyAI : AIPlayer(Difficulty.EASY) {
    override fun makeShot(enemyBoard: GameBoard): Pair<Int, Int> {
        return getRandomUntriedCell(enemyBoard)
    }
}

// Средний ИИ — добивает корабли
class MediumAI : AIPlayer(Difficulty.MEDIUM) {
    override fun makeShot(enemyBoard: GameBoard): Pair<Int, Int> {
        if (targetCells.isNotEmpty()) {
            return targetCells.removeAt(0)
        }
        return getRandomUntriedCell(enemyBoard)
    }
}

// Сложный ИИ — с паттерном шахматки
class HardAI : AIPlayer(Difficulty.HARD) {
    private var parity = 0

    override fun makeShot(enemyBoard: GameBoard): Pair<Int, Int> {
        if (targetCells.isNotEmpty()) {
            if (hits.size >= 2) {
                sortTargetsByDirection()
            }
            return targetCells.removeAt(0)
        }
        return getSmartRandomCell(enemyBoard)
    }

    private fun sortTargetsByDirection() {
        if (hits.size < 2) return

        val lastTwo = hits.takeLast(2)
        val isHorizontal = lastTwo[0].y == lastTwo[1].y

        targetCells.sortBy { (x, y) ->
            if (isHorizontal) {
                if (y == lastTwo[0].y) 0 else 1
            } else {
                if (x == lastTwo[0].x) 0 else 1
            }
        }
    }

    private fun getSmartRandomCell(board: GameBoard): Pair<Int, Int> {
        val availableCells = mutableListOf<Pair<Int, Int>>()

        for (x in 0 until GameBoard.BOARD_SIZE) {
            for (y in 0 until GameBoard.BOARD_SIZE) {
                if ((x + y + parity) % 2 == 0 && !shotHistory.contains(x to y)) {
                    availableCells.add(x to y)
                }
            }
        }

        if (availableCells.isEmpty()) {
            parity = 1 - parity
            return getRandomUntriedCell(board)
        }

        return availableCells.random()
    }
}

// Непобедимый ИИ — вероятностный анализ
class UnbeatableAI : AIPlayer(Difficulty.UNBEATABLE) {
    private val probabilityMap = Array(GameBoard.BOARD_SIZE) { IntArray(GameBoard.BOARD_SIZE) }
    private val remainingShips = mutableListOf(4, 3, 3, 2, 2, 2, 1, 1, 1, 1)

    override fun makeShot(enemyBoard: GameBoard): Pair<Int, Int> {
        updateProbabilityMap(enemyBoard)

        if (targetCells.isNotEmpty() && huntingMode) {
            return getBestHuntingTarget(enemyBoard)
        }

        return getCellWithMaxProbability(enemyBoard)
    }

    private fun updateProbabilityMap(board: GameBoard) {
        for (x in 0 until GameBoard.BOARD_SIZE) {
            for (y in 0 until GameBoard.BOARD_SIZE) {
                probabilityMap[x][y] = 0
            }
        }

        for (shipSize in remainingShips) {
            for (x in 0..GameBoard.BOARD_SIZE - shipSize) {
                for (y in 0 until GameBoard.BOARD_SIZE) {
                    if (canPlaceShipAt(x, y, shipSize, true, board)) {
                        for (i in 0 until shipSize) {
                            if (!shotHistory.contains(x + i to y)) {
                                probabilityMap[x + i][y]++
                            }
                        }
                    }
                }
            }

            for (x in 0 until GameBoard.BOARD_SIZE) {
                for (y in 0..GameBoard.BOARD_SIZE - shipSize) {
                    if (canPlaceShipAt(x, y, shipSize, false, board)) {
                        for (i in 0 until shipSize) {
                            if (!shotHistory.contains(x to y + i)) {
                                probabilityMap[x][y + i]++
                            }
                        }
                    }
                }
            }
        }
    }

    private fun canPlaceShipAt(x: Int, y: Int, size: Int, horizontal: Boolean, board: GameBoard): Boolean {
        for (i in 0 until size) {
            val checkX = if (horizontal) x + i else x
            val checkY = if (horizontal) y else y + i

            if (checkX >= GameBoard.BOARD_SIZE || checkY >= GameBoard.BOARD_SIZE) return false

            val cell = board.getCell(checkX, checkY) ?: return false

            if (shotHistory.contains(checkX to checkY)) {
                if (cell.state == CellState.MISS || cell.state == CellState.DESTROYED) {
                    return false
                }
            }
        }
        return true
    }

    private fun getBestHuntingTarget(board: GameBoard): Pair<Int, Int> {
        if (hits.size >= 2) {
            val sorted = hits.sortedBy { it.x * 10 + it.y }
            val isHorizontal = sorted.all { it.y == sorted[0].y }

            targetCells.sortByDescending { (x, y) ->
                var priority = probabilityMap[x][y]
                if (isHorizontal && y == sorted[0].y) priority += 100
                if (!isHorizontal && x == sorted[0].x) priority += 100
                priority
            }
        }

        return if (targetCells.isNotEmpty()) targetCells.removeAt(0) else getCellWithMaxProbability(board)
    }

    private fun getCellWithMaxProbability(board: GameBoard): Pair<Int, Int> {
        var maxProb = -1
        val bestCells = mutableListOf<Pair<Int, Int>>()

        for (x in 0 until GameBoard.BOARD_SIZE) {
            for (y in 0 until GameBoard.BOARD_SIZE) {
                if (!shotHistory.contains(x to y)) {
                    val prob = probabilityMap[x][y]
                    if (prob > maxProb) {
                        maxProb = prob
                        bestCells.clear()
                        bestCells.add(x to y)
                    } else if (prob == maxProb) {
                        bestCells.add(x to y)
                    }
                }
            }
        }

        return bestCells.minByOrNull { (x, y) ->
            val centerX = GameBoard.BOARD_SIZE / 2
            val centerY = GameBoard.BOARD_SIZE / 2
            (x - centerX) * (x - centerX) + (y - centerY) * (y - centerY)
        } ?: getRandomUntriedCell(board)
    }
}
