package com.example.seabattle.data

data class Ship(
    val size: Int,
    val cells: MutableList<Cell> = mutableListOf(),
    var isHorizontal: Boolean = true
) {
    private val hits = mutableSetOf<Cell>()

    fun hit(cell: Cell): Boolean {
        if (cells.contains(cell)) {
            hits.add(cell)
            return true
        }
        return false
    }

    fun isDestroyed(): Boolean = hits.size == size

    fun getHitCount(): Int = hits.size
}
