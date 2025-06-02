package com.example.seabattle.game

import com.example.seabattle.data.Cell
import com.example.seabattle.data.CellState
import com.example.seabattle.data.Ship

class GameBoard {
    companion object {
        const val BOARD_SIZE = 10
    }

    private val board = Array(BOARD_SIZE) { x ->
        Array(BOARD_SIZE) { y ->
            Cell(x, y)
        }
    }

    val ships = mutableListOf<Ship>()

    fun getCell(x: Int, y: Int): Cell? {
        return if (x in 0 until BOARD_SIZE && y in 0 until BOARD_SIZE) {
            board[x][y]
        } else null
    }

    fun canPlaceShip(ship: Ship, startX: Int, startY: Int): Boolean {
        val cells = getShipCells(ship.size, startX, startY, ship.isHorizontal)
        if (cells.isEmpty()) return false

        // Проверяем, что клетки свободны и вокруг них нет других кораблей
        for (cell in cells) {
            if (!isCellAvailable(cell.x, cell.y)) return false
        }

        return true
    }

    fun placeShip(ship: Ship, startX: Int, startY: Int): Boolean {
        if (!canPlaceShip(ship, startX, startY)) return false

        val cells = getShipCells(ship.size, startX, startY, ship.isHorizontal)

        for (cell in cells) {
            cell.state = CellState.SHIP
            cell.ship = ship
            ship.cells.add(cell)
        }

        ships.add(ship)
        return true
    }
    
    fun shoot(x: Int, y: Int): ShotResult {
        val cell = getCell(x, y) ?: return ShotResult.INVALID

        when (cell.state) {
            CellState.EMPTY -> {
                cell.state = CellState.MISS
                return ShotResult.MISS
            }
            CellState.SHIP -> {
                cell.state = CellState.HIT
                cell.ship?.let { ship ->
                    ship.hit(cell)
                    if (ship.isDestroyed()) {
                        markShipAsDestroyed(ship)
                        return ShotResult.DESTROYED
                    }
                }
                return ShotResult.HIT
            }
            else -> return ShotResult.ALREADY_SHOT
        }
    }

    private fun markShipAsDestroyed(ship: Ship) {
        for (cell in ship.cells) {
            cell.state = CellState.DESTROYED
            // Отмечаем клетки вокруг корабля как промах
            markAroundAssMiss(cell.x, cell.y)
        }
    }

    private fun markAroundAssMiss(x: Int, y: Int) {
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue
                val cell = getCell(x + dx, y + dy)
                if (cell != null && cell.state == CellState.EMPTY) {
                    cell.state = CellState.MISS
                }
            }
        }
    }

    private fun isCellAvailable(x: Int, y: Int): Boolean {
        // Проверяем саму клетку и клетки вокруг неё
        for (dx in -1..1) {
            for (dy in -1..1) {
                val checkX = x + dx
                val checkY = y + dy
                val cell = getCell(checkX, checkY)
                if (cell != null && cell.state != CellState.EMPTY) {
                    return false
                }
            }
        }
        return true
    }

    private fun getShipCells(size: Int, startX: Int, startY: Int, isHorizontal: Boolean): List<Cell> {
        val cells = mutableListOf<Cell>()

        for (i in 0 until size) {
            val x = if (isHorizontal) startX + i else startX
            val y = if (isHorizontal) startY else startY + i
            val cell = getCell(x, y) ?: return emptyList()
            cells.add(cell)
        }

        return cells
    }

    fun areAllShipsDestroyed(): Boolean = ships.all { it.isDestroyed() }

    fun reset() {
        for (x in 0 until BOARD_SIZE) {
            for (y in 0 until BOARD_SIZE) {
                board[x][y] = Cell(x, y)
            }
        }
        ships.clear()
    }
}

enum class ShotResult {
    MISS,
    HIT,
    DESTROYED,
    ALREADY_SHOT,
    INVALID
}
