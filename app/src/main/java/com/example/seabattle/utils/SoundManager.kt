package com.example.seabattle.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

class SoundManager(private val context: Context) {

    private val prefs = PreferencesManager(context)
    private var backgroundMusic: MediaPlayer? = null
    private lateinit var soundPool: SoundPool

    // ID звуков
    private var buttonClickSound: Int = 0
    private var shotSound: Int = 0
    private var hitSound: Int = 0
    private var missSound: Int = 0
    private var explosionSound: Int = 0
    private var victorySound: Int = 0
    private var defeatSound: Int = 0

    init {
        setupSoundPool()
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
    }

    private fun loadSounds() {
        // Загрузка звуков (создайте эти файлы в папке res/raw)
        // buttonClickSound = soundPool.load(context, R.raw.button_click, 1)
        // shotSound = soundPool.load(context, R.raw.shot, 1)
        // hitSound = soundPool.load(context, R.raw.hit, 1)
        // missSound = soundPool.load(context, R.raw.miss, 1)
        // explosionSound = soundPool.load(context, R.raw.explosion, 1)
        // victorySound = soundPool.load(context, R.raw.victory, 1)
        // defeatSound = soundPool.load(context, R.raw.defeat, 1)
    }

    private fun setupBackgroundMusic() {
        // Создайте файл background_music.mp3 в папке res/raw
        // backgroundMusic = MediaPlayer.create(context, R.raw.background_music)
        backgroundMusic?.apply {
            isLooping = true
            setVolume(0.3f, 0.3f)
        }
    }

    fun playButtonClick() {
        if (prefs.isSoundEnabled && buttonClickSound != 0) {
            soundPool.play(buttonClickSound, 1f, 1f, 1, 0, 1f)
        }
    }

    fun playShot() {
        if (prefs.isSoundEnabled && shotSound != 0) {
            soundPool.play(shotSound, 1f, 1f, 1, 0, 1f)
        }
    }

    fun playHit() {
        if (prefs.isSoundEnabled && hitSound != 0) {
            soundPool.play(hitSound, 1f, 1f, 1, 0, 1f)
        }
    }

    fun playMiss() {
        if (prefs.isSoundEnabled && missSound != 0) {
            soundPool.play(missSound, 1f, 1f, 1, 0, 1f)
        }
    }

    fun playExplosion() {
        if (prefs.isSoundEnabled && explosionSound != 0) {
            soundPool.play(explosionSound, 1f, 1f, 1, 0, 1f)
        }
    }

    fun playVictory() {
        if (prefs.isSoundEnabled && victorySound != 0) {
            soundPool.play(victorySound, 1f, 1f, 1, 0, 1f)
        }
    }

    fun playDefeat() {
        if (prefs.isSoundEnabled && defeatSound != 0) {
            soundPool.play(defeatSound, 1f, 1f, 1, 0, 1f)
        }
    }

    fun playBackgroundMusic() {
        if (prefs.isSoundEnabled) {
            backgroundMusic?.start()
        }
    }

    fun pauseBackgroundMusic() {
        backgroundMusic?.pause()
    }

    fun stopBackgroundMusic() {
        backgroundMusic?.stop()
    }

    fun release() {
        soundPool.release()
        backgroundMusic?.release()
    }
}
