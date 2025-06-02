package com.example.seabattle.utils

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresPermission

class VibrationManager(context: Context) {

    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val prefs = PreferencesManager(context)

    @RequiresPermission(Manifest.permission.VIBRATE)
    fun vibrateShort() {
        if (!prefs.isVibrationEnabled) return
        vibrate(50)
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    fun vibrateMedium() {
        if (!prefs.isVibrationEnabled) return
        vibrate(100)
    }

    fun vibrateLong() {
        if (!prefs.isVibrationEnabled) return
        vibrate(200)
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    fun vibrateSuccess() {
        if (!prefs.isVibrationEnabled) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 50, 50, 100)
            val amplitudes = intArrayOf(0, 128, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 50, 50, 100), -1)
        }
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    fun vibrateError() {
        if (!prefs.isVibrationEnabled) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 100, 100, 100, 100, 100)
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 100, 100, 100, 100, 100), -1)
        }
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun vibrate(milliseconds: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(milliseconds)
        }
    }
}
