package com.example.seabattle.data

data class Cell(
    val x: Int,
    val y: Int,
    var state: CellState = CellState.EMPTY,
    var ship: Ship? = null
)

enum class CellState {
    EMPTY,      // Пустая клетка
    SHIP,       // Клетка с кораблем
    HIT,        // Попадание
    MISS,       // Промах
    DESTROYED   // Уничтоженный корабль
}
