package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Synthesizes crisp game audio pops and chimes offline using AudioTrack
 * to ensure 100% offline reliability without missing asset dependencies.
 */
class SoundManager(private val context: Context) {

    private val sampleRate = 44100
    private var isSoundEnabled = true

    fun setSoundEnabled(enabled: Boolean) {
        isSoundEnabled = enabled
    }

    /**
     * Plays a quick, pleasant bubble tap pop sound when the circle is clicked.
     */
    fun playTapSound(combo: Int = 1) {
        if (!isSoundEnabled) return

        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Pitch increases slightly with combo
                val startFreq = 440.0 + (combo * 30).coerceAtMost(400)
                val endFreq = startFreq * 1.6
                val durationMs = 60
                val numSamples = (sampleRate * durationMs) / 1000
                val buffer = ByteArray(numSamples * 2)

                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val freq = startFreq + (endFreq - startFreq) * progress
                    val angle = 2.0 * Math.PI * freq * i / sampleRate
                    // Apply exponential envelope decay for clean pop effect
                    val envelope = Math.exp(-progress * 5.0)
                    val sample = (sin(angle) * envelope * 28000).toInt().coerceIn(-32768, 32767)

                    val shortVal = sample.toShort()
                    buffer[i * 2] = (shortVal.toInt() and 0xFF).toByte()
                    buffer[i * 2 + 1] = ((shortVal.toInt() shr 8) and 0xFF).toByte()
                }

                playBuffer(buffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Plays a high-pitched celebration chime for high scores or combo streaks.
     */
    fun playComboSound() {
        if (!isSoundEnabled) return

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val frequencies = doubleArrayOf(523.25, 659.25, 783.99, 1046.50) // C5, E5, G5, C6
                val durationMsPerNote = 50
                val numSamplesPerNote = (sampleRate * durationMsPerNote) / 1000
                val totalSamples = numSamplesPerNote * frequencies.size
                val buffer = ByteArray(totalSamples * 2)

                var byteIdx = 0
                for (freq in frequencies) {
                    for (i in 0 until numSamplesPerNote) {
                        val progress = i.toDouble() / numSamplesPerNote
                        val angle = 2.0 * Math.PI * freq * i / sampleRate
                        val envelope = 1.0 - progress
                        val sample = (sin(angle) * envelope * 24000).toInt().coerceIn(-32768, 32767)

                        val shortVal = sample.toShort()
                        buffer[byteIdx++] = (shortVal.toInt() and 0xFF).toByte()
                        buffer[byteIdx++] = ((shortVal.toInt() shr 8) and 0xFF).toByte()
                    }
                }

                playBuffer(buffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Plays game over fanfare sound.
     */
    fun playGameOverSound() {
        if (!isSoundEnabled) return

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val frequencies = doubleArrayOf(440.0, 392.0, 349.23, 293.66) // A4, G4, F4, D4
                val durationMsPerNote = 120
                val numSamplesPerNote = (sampleRate * durationMsPerNote) / 1000
                val totalSamples = numSamplesPerNote * frequencies.size
                val buffer = ByteArray(totalSamples * 2)

                var byteIdx = 0
                for (freq in frequencies) {
                    for (i in 0 until numSamplesPerNote) {
                        val progress = i.toDouble() / numSamplesPerNote
                        val angle = 2.0 * Math.PI * freq * i / sampleRate
                        val envelope = Math.exp(-progress * 2.5)
                        val sample = (sin(angle) * envelope * 24000).toInt().coerceIn(-32768, 32767)

                        val shortVal = sample.toShort()
                        buffer[byteIdx++] = (shortVal.toInt() and 0xFF).toByte()
                        buffer[byteIdx++] = ((shortVal.toInt() shr 8) and 0xFF).toByte()
                    }
                }

                playBuffer(buffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playBuffer(buffer: ByteArray) {
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()

        // Release AudioTrack after playback finishes
        CoroutineScope(Dispatchers.Default).launch {
            val durationMs = (buffer.size * 1000L) / (sampleRate * 2)
            kotlinx.coroutines.delay(durationMs + 100)
            audioTrack.stop()
            audioTrack.release()
        }
    }
}
