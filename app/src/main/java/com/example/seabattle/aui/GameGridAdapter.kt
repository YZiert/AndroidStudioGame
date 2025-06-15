package com.example.seabattle.aui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.seabattle.R
import com.example.seabattle.data.CellState

class GameGridAdapter(
    private val gridSize: Int,
    private val onCellClick: (x: Int, y: Int) -> Unit
) : RecyclerView.Adapter<GameGridAdapter.CellViewHolder>() {

    private val cells = Array(gridSize * gridSize) { CellState.EMPTY }

    class CellViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cellView: View = itemView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_game_cell, parent, false)
        
        // Делаем клетки квадратными
        val screenWidth = parent.context.resources.displayMetrics.widthPixels
        val padding = parent.context.resources.getDimensionPixelSize(R.dimen.grid_padding)
        val cellSize = (screenWidth - padding * 2) / gridSize
        
        view.layoutParams = ViewGroup.LayoutParams(cellSize, cellSize)
        
        return CellViewHolder(view)
    }

    override fun onBindViewHolder(holder: CellViewHolder, position: Int) {
        val x = position % gridSize
        val y = position / gridSize
        val cellState = cells[position]

        // Создаем drawable с границами и нужным цветом
        val backgroundColor = when (cellState) {
            CellState.EMPTY -> ContextCompat.getColor(holder.itemView.context, R.color.cell_empty)
            CellState.SHIP -> ContextCompat.getColor(holder.itemView.context, R.color.cell_ship)
            CellState.HIT -> ContextCompat.getColor(holder.itemView.context, R.color.cell_hit)
            CellState.MISS -> ContextCompat.getColor(holder.itemView.context, R.color.cell_miss)
            CellState.DESTROYED -> ContextCompat.getColor(holder.itemView.context, R.color.cell_destroyed)
        }
        
        // Создаем drawable с границей
        val drawable = ContextCompat.getDrawable(holder.itemView.context, R.drawable.cell_border)?.mutate()
        drawable?.setTint(backgroundColor)
        holder.cellView.background = drawable
        
        // Устанавливаем обработчик клика
        holder.cellView.setOnClickListener {
            onCellClick(x, y)
        }
    }

    override fun getItemCount(): Int = gridSize * gridSize

    fun updateCell(x: Int, y: Int, state: CellState) {
        val position = y * gridSize + x
        if (position in 0 until cells.size) {
            cells[position] = state
            notifyItemChanged(position)
        }
    }

    fun updateAllCells(newCells: Array<Array<CellState>>) {
        for (y in 0 until gridSize) {
            for (x in 0 until gridSize) {
                val position = y * gridSize + x
                if (y < newCells.size && x < newCells[y].size) {
                    cells[position] = newCells[y][x]
                }
            }
        }
        notifyDataSetChanged()
    }
}
