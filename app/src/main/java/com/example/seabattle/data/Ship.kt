package com.example.seabattle.data

data class Ship(
    val size: Int,
    val cells: MutableList<Cell> = mutableListOf(),
    var isHorizontal: Boolean = true
) {
    private val hits = mutableSetOf<Pair<Int, Int>>()

    fun hit(cell: Cell): Boolean {
        try {
            // Проверяем по координатам, а не по ссылке на объект
            val cellExists = cells.any { it.x == cell.x && it.y == cell.y }
            if (cellExists) {
                hits.add(Pair(cell.x, cell.y))
                return true
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }

    fun isDestroyed(): Boolean {
        return try {
            hits.size >= size
        } catch (e: Exception) {
            false
        }
    }

    fun getHitCount(): Int {
        return try {
            hits.size
        } catch (e: Exception) {
            0
        }
    }
}
