package com.example.seabattle.game

import android.util.Log
import com.example.seabattle.data.CellState
import kotlin.random.Random

/**
 * Надежный ИИ для игры в морской бой
 * Использует стратегию охоты и добивания
 */
class BattleshipAI(private val boardSize: Int = 10) {
    
    // Состояния ИИ
    private enum class AIState {
        HUNTING,    // Поиск кораблей
        TARGETING   // Добивание найденного корабля
    }
    
    private var currentState = AIState.HUNTING
    private val shotHistory = Array(boardSize) { Array(boardSize) { false } }
    private val hitCells = mutableListOf<Pair<Int, Int>>()
    private val targetQueue = mutableListOf<Pair<Int, Int>>()
    private val random = Random.Default
    
    /**
     * Делает выстрел по доске
     * @param board игровая доска для анализа
     * @return координаты выстрела (x, y)
     */
    fun makeShot(board: GameBoard): Pair<Int, Int> {
        return try {
            when (currentState) {
                AIState.HUNTING -> huntingShot()
                AIState.TARGETING -> targetingShot(board)
            }
        } catch (e: Exception) {
            Log.e("BattleshipAI", "Ошибка в makeShot: ${e.message}")
            // Fallback - случайный выстрел
            findRandomAvailableCell()
        }
    }
    
    /**
     * Обновляет состояние ИИ после выстрела
     * @param x координата X
     * @param y координата Y
     * @param result результат выстрела
     */
    fun updateShotResult(x: Int, y: Int, result: ShotResult) {
        try {
            if (!isValidCoordinate(x, y)) return
            
            shotHistory[x][y] = true
            
            when (result) {
                ShotResult.HIT -> {
                    hitCells.add(Pair(x, y))
                    addAdjacentTargets(x, y)
                    currentState = AIState.TARGETING
                }
                ShotResult.DESTROYED -> {
                    hitCells.add(Pair(x, y))
                    clearCurrentTargets()
                    currentState = if (targetQueue.isEmpty()) AIState.HUNTING else AIState.TARGETING
                }
                ShotResult.MISS -> {
                    // Убираем неудачную цель из очереди если она там была
                    targetQueue.removeAll { it.first == x && it.second == y }
                    if (targetQueue.isEmpty()) {
                        currentState = AIState.HUNTING
                    }
                }
                else -> {
                    // Для ALREADY_SHOT и INVALID - просто убираем из очереди
                    targetQueue.removeAll { it.first == x && it.second == y }
                }
            }
        } catch (e: Exception) {
            Log.e("BattleshipAI", "Ошибка в updateShotResult: ${e.message}")
        }
    }
    
    /**
     * Режим охоты - поиск кораблей
     */
    private fun huntingShot(): Pair<Int, Int> {
        // Используем шахматную стратегию для более эффективного поиска
        val checkerboardCells = getCheckerboardCells()
        val availableCheckerboard = checkerboardCells.filter { (x, y) -> !shotHistory[x][y] }
        
        return if (availableCheckerboard.isNotEmpty()) {
            availableCheckerboard.random(random)
        } else {
            // Если шахматные клетки закончились, стреляем случайно
            findRandomAvailableCell()
        }
    }
    
    /**
     * Режим добивания - атака найденного корабля
     */
    private fun targetingShot(board: GameBoard): Pair<Int, Int> {
        // Убираем недоступные цели из очереди
        targetQueue.removeAll { (x, y) -> shotHistory[x][y] }
        
        if (targetQueue.isEmpty()) {
            currentState = AIState.HUNTING
            return huntingShot()
        }
        
        // Выбираем лучшую цель из очереди
        val bestTarget = selectBestTarget()
        targetQueue.remove(bestTarget)
        
        return bestTarget
    }
    
    /**
     * Добавляет соседние клетки в очередь целей
     */
    private fun addAdjacentTargets(x: Int, y: Int) {
        val directions = listOf(
            Pair(0, 1),   // вверх
            Pair(0, -1),  // вниз
            Pair(1, 0),   // вправо
            Pair(-1, 0)   // влево
        )
        
        for ((dx, dy) in directions) {
            val newX = x + dx
            val newY = y + dy
            
            if (isValidCoordinate(newX, newY) && !shotHistory[newX][newY]) {
                val target = Pair(newX, newY)
                if (!targetQueue.contains(target)) {
                    targetQueue.add(target)
                }
            }
        }
    }
    
    /**
     * Очищает текущие цели после уничтожения корабля
     */
    private fun clearCurrentTargets() {
        // Находим последний уничтоженный корабль и убираем связанные цели
        targetQueue.clear()
        hitCells.clear()
    }
    
    /**
     * Выбирает лучшую цель из очереди
     */
    private fun selectBestTarget(): Pair<Int, Int> {
        if (targetQueue.isEmpty()) {
            return findRandomAvailableCell()
        }
        
        // Если есть несколько попаданий подряд, продолжаем в том же направлении
        if (hitCells.size >= 2) {
            val lineTargets = findLineTargets()
            if (lineTargets.isNotEmpty()) {
                return lineTargets.first()
            }
        }
        
        // Иначе выбираем случайную цель из очереди
        return targetQueue.random(random)
    }
    
    /**
     * Находит цели на линии с последними попаданиями
     */
    private fun findLineTargets(): List<Pair<Int, Int>> {
        if (hitCells.size < 2) return emptyList()
        
        val lastHit = hitCells.last()
        val prevHit = hitCells[hitCells.size - 2]
        
        val dx = lastHit.first - prevHit.first
        val dy = lastHit.second - prevHit.second
        
        val lineTargets = mutableListOf<Pair<Int, Int>>()
        
        // Проверяем продолжение линии в обе стороны
        val nextX1 = lastHit.first + dx
        val nextY1 = lastHit.second + dy
        if (isValidCoordinate(nextX1, nextY1) && !shotHistory[nextX1][nextY1]) {
            lineTargets.add(Pair(nextX1, nextY1))
        }
        
        val nextX2 = prevHit.first - dx
        val nextY2 = prevHit.second - dy
        if (isValidCoordinate(nextX2, nextY2) && !shotHistory[nextX2][nextY2]) {
            lineTargets.add(Pair(nextX2, nextY2))
        }
        
        return lineTargets.filter { targetQueue.contains(it) }
    }
    
    /**
     * Получает клетки в шахматном порядке для эффективного поиска
     */
    private fun getCheckerboardCells(): List<Pair<Int, Int>> {
        val cells = mutableListOf<Pair<Int, Int>>()
        for (x in 0 until boardSize) {
            for (y in 0 until boardSize) {
                if ((x + y) % 2 == 0) {
                    cells.add(Pair(x, y))
                }
            }
        }
        return cells
    }
    
    /**
     * Находит случайную доступную клетку
     */
    private fun findRandomAvailableCell(): Pair<Int, Int> {
        val availableCells = mutableListOf<Pair<Int, Int>>()
        
        for (x in 0 until boardSize) {
            for (y in 0 until boardSize) {
                if (!shotHistory[x][y]) {
                    availableCells.add(Pair(x, y))
                }
            }
        }
        
        return if (availableCells.isNotEmpty()) {
            availableCells.random(random)
        } else {
            // Крайний случай - все клетки обстреляны
            Pair(0, 0)
        }
    }
    
    /**
     * Проверяет валидность координат
     */
    private fun isValidCoordinate(x: Int, y: Int): Boolean {
        return x in 0 until boardSize && y in 0 until boardSize
    }
    
    /**
     * Сбрасывает состояние ИИ для новой игры
     */
    fun reset() {
        currentState = AIState.HUNTING
        for (x in 0 until boardSize) {
            for (y in 0 until boardSize) {
                shotHistory[x][y] = false
            }
        }
        hitCells.clear()
        targetQueue.clear()
    }
    
    /**
     * Получает статистику ИИ для отладки
     */
    fun getDebugInfo(): String {
        return "State: $currentState, Hits: ${hitCells.size}, Targets: ${targetQueue.size}"
    }
}
