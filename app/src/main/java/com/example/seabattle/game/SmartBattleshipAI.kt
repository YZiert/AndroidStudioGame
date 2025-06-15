package com.example.seabattle.game

import android.util.Log
import com.example.seabattle.data.CellState
import kotlin.random.Random

/**
 * Умный ИИ для игры в морской бой с разными уровнями сложности
 */
class SmartBattleshipAI(
    private val boardSize: Int = 10,
    private val difficulty: Difficulty = Difficulty.MEDIUM
) {
    
    enum class Difficulty {
        EASY,       // Случайные выстрелы
        MEDIUM,     // Базовая охота и добивание
        HARD,       // Умная охота с приоритетами
        UNBEATABLE  // Почти идеальная игра
    }
    
    // Состояния ИИ
    private enum class AIState {
        HUNTING,    // Поиск кораблей
        TARGETING   // Добивание найденного корабля
    }
    
    private var currentState = AIState.HUNTING
    private val shotHistory = Array(boardSize) { Array(boardSize) { false } }
    private val hitCells = mutableListOf<Pair<Int, Int>>()
    private val targetQueue = mutableListOf<Pair<Int, Int>>()
    private val destroyedShips = mutableListOf<List<Pair<Int, Int>>>()
    private val random = Random.Default
    
    // Матрица вероятностей расположения кораблей
    private val densityMap = Array(boardSize) { Array(boardSize) { 0 } }
    
    /**
     * Делает выстрел по доске
     */
    fun makeShot(board: GameBoard): Pair<Int, Int> {
        return try {
            when (difficulty) {
                Difficulty.EASY -> easyShot()
                Difficulty.MEDIUM -> mediumShot()
                Difficulty.HARD -> hardShot(board)
                Difficulty.UNBEATABLE -> unbeatableShot(board)
            }
        } catch (e: Exception) {
            Log.e("SmartBattleshipAI", "Ошибка в makeShot: ${e.message}")
            findRandomAvailableCell()
        }
    }
    
    /**
     * Обновляет состояние ИИ после выстрела
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
                    updateDensityMap()
                }
                ShotResult.DESTROYED -> {
                    hitCells.add(Pair(x, y))
                    destroyedShips.add(hitCells.toList())
                    clearCurrentTargets()
                    currentState = if (targetQueue.isEmpty()) AIState.HUNTING else AIState.TARGETING
                    updateDensityMap()
                }
                ShotResult.MISS -> {
                    targetQueue.removeAll { it.first == x && it.second == y }
                    if (targetQueue.isEmpty()) {
                        currentState = AIState.HUNTING
                    }
                    updateDensityMap()
                }
                else -> {
                    targetQueue.removeAll { it.first == x && it.second == y }
                }
            }
        } catch (e: Exception) {
            Log.e("SmartBattleshipAI", "Ошибка в updateShotResult: ${e.message}")
        }
    }
    
    /**
     * Легкий уровень - почти случайные выстрелы
     */
    private fun easyShot(): Pair<Int, Int> {
        // Баланс между случайностью и стратегией
        return if (random.nextFloat() < 0.8f) {
            findRandomAvailableCell()
        } else {
            mediumShot()
        }
    }
    
    /**
     * Средний уровень - базовая охота и добивание
     */
    private fun mediumShot(): Pair<Int, Int> {
        return when (currentState) {
            AIState.HUNTING -> {
                // Оптимизация поиска через шахматный паттерн
                val checkerboardCells = getCheckerboardCells()
                val availableCheckerboard = checkerboardCells.filter { (x, y) -> !shotHistory[x][y] }
                
                if (availableCheckerboard.isNotEmpty()) {
                    availableCheckerboard.random(random)
                } else {
                    findRandomAvailableCell()
                }
            }
            AIState.TARGETING -> {
                targetingShot()
            }
        }
    }
    
    /**
     * Высокий уровень - умная охота с приоритетами
     */
    private fun hardShot(board: GameBoard): Pair<Int, Int> {
        return when (currentState) {
            AIState.HUNTING -> {
                // Применение вероятностной модели для оптимизации выстрелов
                val bestTargets = findHighDensityTargets()
                if (bestTargets.isNotEmpty()) {
                    bestTargets.random(random)
                } else {
                    mediumShot()
                }
            }
            AIState.TARGETING -> {
                smartTargetingShot()
            }
        }
    }
    
    /**
     * Непобедимый уровень - почти идеальная игра
     */
    private fun unbeatableShot(board: GameBoard): Pair<Int, Int> {
        return when (currentState) {
            AIState.HUNTING -> {
                // Комплексный анализ всех возможных конфигураций кораблей
                val optimalTargets = findOptimalHuntingTargets()
                if (optimalTargets.isNotEmpty()) {
                    optimalTargets.first()
                } else {
                    hardShot(board)
                }
            }
            AIState.TARGETING -> {
                perfectTargetingShot()
            }
        }
    }
    
    /**
     * Умное добивание для высокого уровня
     */
    private fun smartTargetingShot(): Pair<Int, Int> {
        targetQueue.removeAll { (x, y) -> shotHistory[x][y] }
        
        if (targetQueue.isEmpty()) {
            currentState = AIState.HUNTING
            return hardShot(GameBoard()) // Временная доска для расчетов
        }
        
        // Приоритизация целей по линейному расположению
        val lineTargets = findLineTargets()
        if (lineTargets.isNotEmpty()) {
            val target = lineTargets.first()
            targetQueue.remove(target)
            return target
        }
        
        // Выбор оптимальной цели на основе приоритетов
        val bestTarget = targetQueue.maxByOrNull { (x, y) -> 
            getTargetPriority(x, y)
        } ?: targetQueue.first()
        
        targetQueue.remove(bestTarget)
        return bestTarget
    }
    
    /**
     * Идеальное добивание для непобедимого уровня
     */
    private fun perfectTargetingShot(): Pair<Int, Int> {
        targetQueue.removeAll { (x, y) -> shotHistory[x][y] }
        
        if (targetQueue.isEmpty()) {
            currentState = AIState.HUNTING
            return unbeatableShot(GameBoard()) // Временная доска для расчетов
        }
        
        // Продвинутый анализ паттернов для выбора оптимальной цели
        val optimalTarget = analyzeAndSelectOptimalTarget()
        targetQueue.remove(optimalTarget)
        return optimalTarget
    }
    
    /**
     * Обычное добивание
     */
    private fun targetingShot(): Pair<Int, Int> {
        targetQueue.removeAll { (x, y) -> shotHistory[x][y] }
        
        if (targetQueue.isEmpty()) {
            currentState = AIState.HUNTING
            return mediumShot()
        }
        
        val lineTargets = findLineTargets()
        if (lineTargets.isNotEmpty()) {
            val target = lineTargets.first()
            targetQueue.remove(target)
            return target
        }
        
        val target = targetQueue.random(random)
        targetQueue.remove(target)
        return target
    }
    
    /**
     * Добавляет соседние клетки в очередь целей
     */
    private fun addAdjacentTargets(x: Int, y: Int) {
        val directions = listOf(
            Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0)
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
        targetQueue.clear()
        hitCells.clear()
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
        
        // Экстраполяция линии в обоих направлениях
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
     * По��учает клетки в шахматном порядке
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
     * Обновляет карту плотности для умного ИИ
     */
    private fun updateDensityMap() {
        // Инициализация карты вероятностей
        for (x in 0 until boardSize) {
            for (y in 0 until boardSize) {
                densityMap[x][y] = 0
            }
        }
        
        // Стандартный набор кораблей (4-палубный, 3-палубные, 2-палубные, 1-палубные)
        val shipSizes = listOf(4, 3, 3, 2, 2, 2, 1, 1, 1, 1)
        val remainingShips = shipSizes.toMutableList()
        
        // Исключение уже уничтоженных кораблей из расчетов
        for (destroyedShip in destroyedShips) {
            val shipSize = destroyedShip.size
            remainingShips.remove(shipSize)
        }
        
        // Расчет вероятностей для каждой клетки поля
        for (x in 0 until boardSize) {
            for (y in 0 until boardSize) {
                if (!shotHistory[x][y]) {
                    densityMap[x][y] = calculateCellDensity(x, y, remainingShips)
                }
            }
        }
    }
    
    /**
     * Вычисляет плотность для конкретной клетки
     */
    private fun calculateCellDensity(x: Int, y: Int, shipSizes: List<Int>): Int {
        var density = 0
        
        for (shipSize in shipSizes) {
            // Проверка возможности горизонтального размещения
            for (startX in maxOf(0, x - shipSize + 1)..minOf(boardSize - shipSize, x)) {
                if (canPlaceShipAt(startX, y, shipSize, true)) {
                    density++
                }
            }
            
            // Проверка возможности вертикального размещения
            for (startY in maxOf(0, y - shipSize + 1)..minOf(boardSize - shipSize, y)) {
                if (canPlaceShipAt(x, startY, shipSize, false)) {
                    density++
                }
            }
        }
        
        return density
    }
    
    /**
     * Проверяет, можно ли разместить корабль в указанной позиции
     */
    private fun canPlaceShipAt(startX: Int, startY: Int, size: Int, horizontal: Boolean): Boolean {
        for (i in 0 until size) {
            val x = if (horizontal) startX + i else startX
            val y = if (horizontal) startY else startY + i
            
            if (!isValidCoordinate(x, y) || shotHistory[x][y]) {
                return false
            }
        }
        return true
    }
    
    /**
     * Находит цели с высокой плотностью
     */
    private fun findHighDensityTargets(): List<Pair<Int, Int>> {
        val maxDensity = densityMap.flatten().maxOrNull() ?: 0
        if (maxDensity == 0) return emptyList()
        
        val highDensityTargets = mutableListOf<Pair<Int, Int>>()
        for (x in 0 until boardSize) {
            for (y in 0 until boardSize) {
                if (densityMap[x][y] == maxDensity && !shotHistory[x][y]) {
                    highDensityTargets.add(Pair(x, y))
                }
            }
        }
        
        return highDensityTargets
    }
    
    /**
     * Находит оптимальные цели для охоты (непобедимый уровень)
     */
    private fun findOptimalHuntingTargets(): List<Pair<Int, Int>> {
        updateDensityMap()
        val highDensityTargets = findHighDensityTargets()
        
        // Дополнительная фильтрация по стратегическим позициям
        return highDensityTargets.sortedByDescending { (x, y) ->
            getStrategicValue(x, y)
        }
    }
    
    /**
     * Получает стратегическую ценность позиции
     */
    private fun getStrategicValue(x: Int, y: Int): Int {
        var value = densityMap[x][y]
        
        // Бонус за центральные позиции
        val centerDistance = kotlin.math.abs(x - boardSize/2) + kotlin.math.abs(y - boardSize/2)
        value += (boardSize - centerDistance)
        
        // Бонус за позиции рядом с краями (для больших кораблей)
        if (x == 0 || x == boardSize-1 || y == 0 || y == boardSize-1) {
            value += 2
        }
        
        return value
    }
    
    /**
     * Получает приоритет цели для добивания
     */
    private fun getTargetPriority(x: Int, y: Int): Int {
        var priority = 0
        
        // Высокий приоритет для продолжения линии
        if (isOnLineWithHits(x, y)) {
            priority += 10
        }
        
        // Приоритет по количеству соседних попаданий
        val adjacentHits = countAdjacentHits(x, y)
        priority += adjacentHits * 5
        
        return priority
    }
    
    /**
     * Проверяет, находится ли цель на линии с попаданиями
     */
    private fun isOnLineWithHits(x: Int, y: Int): Boolean {
        if (hitCells.size < 2) return false
        
        for (i in 0 until hitCells.size - 1) {
            val hit1 = hitCells[i]
            val hit2 = hitCells[i + 1]
            
            if (isOnLine(hit1, hit2, Pair(x, y))) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Проверяет, находятся ли три точки на одной линии
     */
    private fun isOnLine(p1: Pair<Int, Int>, p2: Pair<Int, Int>, p3: Pair<Int, Int>): Boolean {
        val dx1 = p2.first - p1.first
        val dy1 = p2.second - p1.second
        val dx2 = p3.first - p2.first
        val dy2 = p3.second - p2.second
        
        return (dx1 == 0 && dx2 == 0) || (dy1 == 0 && dy2 == 0) ||
               (dx1 != 0 && dx2 != 0 && dx1 * dy2 == dx2 * dy1)
    }
    
    /**
     * Считает количество соседних попаданий
     */
    private fun countAdjacentHits(x: Int, y: Int): Int {
        var count = 0
        val directions = listOf(Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0))
        
        for ((dx, dy) in directions) {
            val newX = x + dx
            val newY = y + dy
            if (hitCells.contains(Pair(newX, newY))) {
                count++
            }
        }
        
        return count
    }
    
    /**
     * Анализирует и выбирает оптимальную цель (непобедимый уровень)
     */
    private fun analyzeAndSelectOptimalTarget(): Pair<Int, Int> {
        if (targetQueue.isEmpty()) {
            return findRandomAvailableCell()
        }
        
        // Сложный анализ паттернов для выбора лучшей цели
        return targetQueue.maxByOrNull { (x, y) ->
            var score = getTargetPriority(x, y)
            
            // Дополнительные факторы для непобедимого ИИ
            score += analyzeShipPatterns(x, y)
            score += calculateProbabilityBonus(x, y)
            
            score
        } ?: targetQueue.first()
    }
    
    /**
     * Анализирует паттерны кораблей
     */
    private fun analyzeShipPatterns(x: Int, y: Int): Int {
        // Сложная логика анализа возможных конфигураций кораблей
        var bonus = 0
        
        // Анализ возможных размеров кораблей в данной позиции
        val possibleShipSizes = mutableListOf<Int>()
        
        // Горизонтальный анализ
        var horizontalSpace = 1
        var leftX = x - 1
        while (leftX >= 0 && !shotHistory[leftX][y]) {
            horizontalSpace++
            leftX--
        }
        var rightX = x + 1
        while (rightX < boardSize && !shotHistory[rightX][y]) {
            horizontalSpace++
            rightX++
        }
        
        // Вертикальный анализ
        var verticalSpace = 1
        var topY = y - 1
        while (topY >= 0 && !shotHistory[x][topY]) {
            verticalSpace++
            topY--
        }
        var bottomY = y + 1
        while (bottomY < boardSize && !shotHistory[x][bottomY]) {
            verticalSpace++
            bottomY++
        }
        
        // Бонус за большие свободные пространства
        bonus += maxOf(horizontalSpace, verticalSpace)
        
        return bonus
    }
    
    /**
     * Вычисляет бонус вероятности
     */
    private fun calculateProbabilityBonus(x: Int, y: Int): Int {
        // Статистический анализ вероятности нахождения корабля
        var bonus = 0
        
        // Бонус за позиции, которые могут содержать большие корабли
        val maxShipSize = 4 // Предполагаем, что самый большой корабль - 4 клетки
        
        for (size in 2..maxShipSize) {
            if (canFitShip(x, y, size)) {
                bonus += size
            }
        }
        
        return bonus
    }
    
    /**
     * Проверяет, может ли корабль поместиться в данной позиции
     */
    private fun canFitShip(x: Int, y: Int, size: Int): Boolean {
        // Горизонтально
        var canFitHorizontally = true
        for (i in 0 until size) {
            if (x + i >= boardSize || shotHistory[x + i][y]) {
                canFitHorizontally = false
                break
            }
        }
        
        // Вертикально
        var canFitVertically = true
        for (i in 0 until size) {
            if (y + i >= boardSize || shotHistory[x][y + i]) {
                canFitVertically = false
                break
            }
        }
        
        return canFitHorizontally || canFitVertically
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
                densityMap[x][y] = 0
            }
        }
        hitCells.clear()
        targetQueue.clear()
        destroyedShips.clear()
    }
    
    /**
     * Получает информацию о состоянии ИИ для отладки
     */
    fun getDebugInfo(): String {
        return "Difficulty: $difficulty, State: $currentState, Hits: ${hitCells.size}, Targets: ${targetQueue.size}"
    }
}
