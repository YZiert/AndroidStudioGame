package com.example.seabattle

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.button.MaterialButton

/**
 * Базовый класс для всех активностей, который применяет шрифт Orbitron
 * ко всем текстовым элементам в иерархии представлений.
 */
open class BaseActivity : AppCompatActivity() {

    private lateinit var orbitronRegular: Typeface
    private lateinit var orbitronMedium: Typeface
    private lateinit var orbitronBold: Typeface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Инициализация шрифтов из ресурсов
        try {
            orbitronRegular = ResourcesCompat.getFont(this, R.font.exo2_regular)!!
            orbitronMedium = ResourcesCompat.getFont(this, R.font.exo2_medium)!!
            orbitronBold = ResourcesCompat.getFont(this, R.font.exo2_bold)!!
        } catch (e: Exception) {
            Log.e("BaseActivity", "Ошибка загрузки шрифтов: ${e.message}")
            // Используем системные шрифты в случае ошибки
            orbitronRegular = Typeface.DEFAULT
            orbitronMedium = Typeface.DEFAULT
            orbitronBold = Typeface.DEFAULT_BOLD
        }
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        applyFontToAllViews(window.decorView)
    }

    override fun setContentView(view: View) {
        super.setContentView(view)
        applyFontToAllViews(window.decorView)
    }

    /**
     * Рекурсивно применяет шрифт Orbitron ко всем текстовым элементам в иерархии представлений.
     */
    private fun applyFontToAllViews(view: View) {
        if (view is ViewGroup) {
            // Если это группа представлений, рекурсивно обрабатываем все дочерние элементы
            for (i in 0 until view.childCount) {
                applyFontToAllViews(view.getChildAt(i))
            }
        } else if (view is TextView) {
            // Применяем шрифт к TextView
            applyFontToTextView(view)
        } else if (view is Button) {
            // Применяем шрифт к Button
            applyFontToButton(view)
        } else if (view is EditText) {
            // Применяем шрифт к EditText
            applyFontToEditText(view)
        }
    }

    /**
     * Применяет шрифт Orbitron к TextView в зависимости от стиля текста.
     */
    private fun applyFontToTextView(textView: TextView) {
        when (textView.typeface?.style) {
            Typeface.BOLD -> textView.typeface = orbitronBold
            Typeface.ITALIC -> textView.typeface = orbitronMedium
            Typeface.BOLD_ITALIC -> textView.typeface = orbitronBold
            else -> textView.typeface = orbitronRegular
        }
    }

    /**
     * Применяет шрифт Orbitron к Button.
     */
    private fun applyFontToButton(button: Button) {
        if (button is MaterialButton) {
            button.typeface = orbitronBold
        } else {
            button.typeface = orbitronBold
        }
    }

    /**
     * Применяет шрифт Orbitron к EditText.
     */
    private fun applyFontToEditText(editText: EditText) {
        editText.typeface = orbitronRegular
    }
}