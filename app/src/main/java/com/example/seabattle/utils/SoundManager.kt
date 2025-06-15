package com.example.seabattle.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.media.ToneGenerator
import android.util.Log

class SoundManager(private val context: Context) {

    private val prefs = PreferencesManager(context)
    private var backgroundMusic: MediaPlayer? = null
    private lateinit var soundPool: SoundPool
    private var toneGenerator: ToneGenerator? = null
    private var backgroundMusicHandler: android.os.Handler? = null
    private var backgroundMusicRunnable: Runnable? = null
    private var isBackgroundMusicPlaying = false

    // ID звуков
    private var buttonClickSound: Int = 0
    private var shotSound: Int = 0
    private var hitSound: Int = 0
    private var missSound: Int = 0
    private var explosionSound: Int = 0
    private var victorySound: Int = 0
    private var defeatSound: Int = 0

    // Флаги для проверки загрузки звуков
    private var soundsLoaded = false
    
    // Защита от одновременных воспроизведений
    private var isPlayingSound = false
    private var lastSoundTime = 0L
    private val soundCooldown = 100L // минимальный интервал между звуками в мс
    private val maxSoundDuration = 2000L // максимальная длительность звука для автосброса

    init {
        setupSoundPool()
        setupToneGenerator()
        loadSounds()
        setupBackgroundMusic()
    }

    private fun setupSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                soundsLoaded = true
                Log.d("SoundManager", "Звуки загружены успешно")
            } else {
                Log.e("SoundManager", "Ошибка загрузки звуков: $status")
            }
        }
    }

    private fun setupToneGenerator() {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 50)
        } catch (e: Exception) {
            Log.e("SoundManager", "Ошибка создания ToneGenerator: ${e.message}")
        }
    }

    private fun loadSounds() {
        try {
            // Попытка загрузки звуков из ресурсов
            // Если файлы отсутствуют, будем использовать системные звуки
            
            // Для демонстрации создадим простые звуковые эффекты
            // В реальном проекте здесь должны быть загружены файлы из res/raw
            
            Log.d("SoundManager", "Инициализация звуковой системы")
            soundsLoaded = true
            
        } catch (e: Exception) {
            Log.e("SoundManager", "Ошибка загрузки звуков: ${e.message}")
            soundsLoaded = false
        }
    }

    private fun setupBackgroundMusic() {
        try {
            backgroundMusicHandler = android.os.Handler(android.os.Looper.getMainLooper())
            Log.d("SoundManager", "Система фоновой музыки инициализирована")
        } catch (e: Exception) {
            Log.e("SoundManager", "Ошибка настройки фоновой музыки: ${e.message}")
        }
    }

    private fun createBackgroundMusicLoop() {
        backgroundMusicRunnable = object : Runnable {
            private var noteIndex = 0
            private val melody = arrayOf(
                ToneGenerator.TONE_DTMF_1 to 600,
                ToneGenerator.TONE_DTMF_3 to 600,
                ToneGenerator.TONE_DTMF_5 to 600,
                ToneGenerator.TONE_DTMF_3 to 600,
                ToneGenerator.TONE_DTMF_1 to 600,
                ToneGenerator.TONE_DTMF_5 to 600,
                ToneGenerator.TONE_DTMF_7 to 600,
                ToneGenerator.TONE_DTMF_5 to 600
            )

            override fun run() {
                if (isBackgroundMusicPlaying && prefs.isSoundEnabled && !isPlayingSound) {
                    try {
                        val (tone, duration) = melody[noteIndex]
                        // Используем отдельный ToneGenerator для фоновой музыки с меньшей громкостью
                        val backgroundTone = ToneGenerator(AudioManager.STREAM_MUSIC, 30)
                        backgroundTone.startTone(tone, duration)
                        backgroundTone.release()
                        
                        noteIndex = (noteIndex + 1) % melody.size
                        
                        backgroundMusicHandler?.postDelayed(this, (duration + 400).toLong())
                    } catch (e: Exception) {
                        Log.e("SoundManager", "Ошибка воспроизведения фоновой мелодии: ${e.message}")
                        // При ошибке останавливаем фоновую музыку
                        isBackgroundMusicPlaying = false
                    }
                }
            }
        }
    }

    // Безопасное воспроизведение звука с защитой от конфликтов
    private fun safePlayTone(tone: Int, duration: Int, soundName: String) {
        if (!prefs.isSoundEnabled) return
        
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSoundTime < soundCooldown) {
            Log.d("SoundManager", "Пропуск звука $soundName - слишком частые вызовы")
            return
        }
        
        if (isPlayingSound) {
            Log.d("SoundManager", "Пропуск звука $soundName - уже воспроизводится другой звук")
            return
        }
        
        try {
            // Проверяем, что ToneGenerator существует
            if (toneGenerator == null) {
                Log.w("SoundManager", "ToneGenerator is null, recreating...")
                recreateToneGenerator()
                if (toneGenerator == null) {
                    Log.e("SoundManager", "Failed to recreate ToneGenerator")
                    return
                }
            }
            
            isPlayingSound = true
            lastSoundTime = currentTime
            
            // Воспроизводим звук с дополнительной проверкой
            val success = toneGenerator?.startTone(tone, duration) ?: false
            if (!success) {
                Log.w("SoundManager", "Failed to start tone $soundName, recreating ToneGenerator")
                isPlayingSound = false
                recreateToneGenerator()
                return
            }
            
            // Сбрасываем флаг через время воспроизведения с дополнительной защитой
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    isPlayingSound = false
                } catch (e: Exception) {
                    Log.e("SoundManager", "Ошибка сброса флага звука: ${e.message}")
                    isPlayingSound = false
                }
            }, duration.toLong() + 50)
            
            // Дополнительная защита от зависания - принудительный сброс через максимальное время
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (isPlayingSound && (System.currentTimeMillis() - lastSoundTime) > maxSoundDuration) {
                    Log.w("SoundManager", "Принудительный сброс зависшего флага звука")
                    isPlayingSound = false
                    recreateToneGenerator()
                }
            }, maxSoundDuration)
            
        } catch (e: Exception) {
            Log.e("SoundManager", "Ошибка воспроизведения звука $soundName: ${e.message}")
            isPlayingSound = false
            // Пересоздаем ToneGenerator если он сломался
            recreateToneGenerator()
        }
    }

    // Пересоздание ToneGenerator в случае ошибки
    private fun recreateToneGenerator() {
        try {
            toneGenerator?.release()
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 50)
            Log.d("SoundManager", "ToneGenerator пересоздан")
        } catch (e: Exception) {
            Log.e("SoundManager", "Ошибка пересоздания ToneGenerator: ${e.message}")
            toneGenerator = null
        }
    }

    fun playButtonClick() {
        safePlayTone(ToneGenerator.TONE_PROP_BEEP, 100, "button_click")
    }

    fun playShot() {
        safePlayTone(ToneGenerator.TONE_DTMF_1, 200, "shot")
    }

    fun playHit() {
        safePlayTone(ToneGenerator.TONE_DTMF_5, 300, "hit")
    }

    fun playMiss() {
        safePlayTone(ToneGenerator.TONE_DTMF_0, 150, "miss")
    }

    fun playExplosion() {
        safePlayTone(ToneGenerator.TONE_DTMF_9, 500, "explosion")
    }

    fun playVictory() {
        if (prefs.isSoundEnabled) {
            try {
                // Звук победы - восходящая последовательность
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_1, 200)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    toneGenerator?.startTone(ToneGenerator.TONE_DTMF_5, 200)
                }, 250)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    toneGenerator?.startTone(ToneGenerator.TONE_DTMF_9, 400)
                }, 500)
            } catch (e: Exception) {
                Log.e("SoundManager", "Ошибка воспроизведения звука победы: ${e.message}")
            }
        }
    }

    fun playDefeat() {
        if (prefs.isSoundEnabled) {
            try {
                // Звук поражения - нисходящая последовательность
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_9, 200)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    toneGenerator?.startTone(ToneGenerator.TONE_DTMF_5, 200)
                }, 250)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    toneGenerator?.startTone(ToneGenerator.TONE_DTMF_1, 400)
                }, 500)
            } catch (e: Exception) {
                Log.e("SoundManager", "Ошибка воспроизведения звука поражения: ${e.message}")
            }
        }
    }

    fun playBackgroundMusic() {
        if (prefs.isSoundEnabled && !isBackgroundMusicPlaying) {
            try {
                isBackgroundMusicPlaying = true
                createBackgroundMusicLoop()
                backgroundMusicRunnable?.let { 
                    backgroundMusicHandler?.post(it)
                }
                Log.d("SoundManager", "Фоновая музыка запущена")
            } catch (e: Exception) {
                Log.e("SoundManager", "Ошибка воспроизведения фоновой музыки: ${e.message}")
                isBackgroundMusicPlaying = false
            }
        }
    }

    fun pauseBackgroundMusic() {
        try {
            isBackgroundMusicPlaying = false
            backgroundMusicRunnable?.let { 
                backgroundMusicHandler?.removeCallbacks(it)
            }
            Log.d("SoundManager", "Фоновая музыка приостановлена")
        } catch (e: Exception) {
            Log.e("SoundManager", "Ошибка паузы фоновой музыки: ${e.message}")
        }
    }

    fun stopBackgroundMusic() {
        try {
            isBackgroundMusicPlaying = false
            backgroundMusicRunnable?.let { 
                backgroundMusicHandler?.removeCallbacks(it)
            }
            Log.d("SoundManager", "Фоновая музыка остановлена")
        } catch (e: Exception) {
            Log.e("SoundManager", "Ошибка остановки фоновой музыки: ${e.message}")
        }
    }

    // Сброс звуковой системы при возникновении проблем
    fun resetSoundSystem() {
        try {
            Log.d("SoundManager", "Сброс звуковой системы")
            
            // Останавливаем фоновую музыку
            stopBackgroundMusic()
            
            // Сбрасываем флаги
            isPlayingSound = false
            lastSoundTime = 0L
            
            // Пересоздаем ToneGenerator
            recreateToneGenerator()
            
            Log.d("SoundManager", "Звуковая система сброшена")
        } catch (e: Exception) {
            Log.e("SoundManager", "Ошибка сброса звуковой системы: ${e.message}")
        }
    }

    fun release() {
        try {
            stopBackgroundMusic()
            isPlayingSound = false
            soundPool.release()
            backgroundMusic?.release()
            toneGenerator?.release()
            backgroundMusicHandler = null
            backgroundMusicRunnable = null
            Log.d("SoundManager", "Ресурсы SoundManager освобождены")
        } catch (e: Exception) {
            Log.e("SoundManager", "Ошибка освобождения ресурсов: ${e.message}")
        }
    }
}
