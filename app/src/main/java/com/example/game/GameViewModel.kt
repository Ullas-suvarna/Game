package com.example.game

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundManager
import com.example.data.ScoreRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class ScreenState {
    START,
    PLAYING,
    GAME_OVER
}

data class Particle(
    val id: Long,
    val startX: Float,
    val startY: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val alpha: Float = 1.0f
)

data class TapPopup(
    val id: Long,
    val text: String,
    val x: Float,
    val y: Float,
    val color: Color,
    val createdAt: Long = System.currentTimeMillis()
)

data class GameUiState(
    val screenState: ScreenState = ScreenState.START,
    val currentScore: Int = 0,
    val bestScore: Int = 0,
    val isNewHighScore: Boolean = false,
    val timeRemainingMs: Long = 30_000L,
    val targetFractionX: Float = 0.5f,
    val targetFractionY: Float = 0.5f,
    val currentColor: Color = Color(0xFF673AB7), // Deep Purple
    val nextColor: Color = Color(0xFFE91E63),   // Pink
    val comboStreak: Int = 0,
    val particles: List<Particle> = emptyList(),
    val popups: List<TapPopup> = emptyList(),
    val isSoundEnabled: Boolean = true
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val scoreRepository = ScoreRepository(application)
    private val soundManager = SoundManager(application)

    private val _uiState = MutableStateFlow(
        GameUiState(
            bestScore = scoreRepository.bestScore,
            isSoundEnabled = scoreRepository.isSoundEnabled
        )
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var lastTapTimeMs: Long = 0L
    private var lastMoveTimeMs: Long = 0L

    // Vibrant Material colors for circle
    val circleColors = listOf(
        Color(0xFFE91E63), // Pink
        Color(0xFF9C27B0), // Purple
        Color(0xFF673AB7), // Deep Purple
        Color(0xFF3F51B5), // Indigo
        Color(0xFF2196F3), // Blue
        Color(0xFF00BCD4), // Cyan
        Color(0xFF009688), // Teal
        Color(0xFF4CAF50), // Green
        Color(0xFFFF9800), // Orange
        Color(0xFFFF5722), // Deep Orange
        Color(0xFFFF2D55), // Bright Neon Red
        Color(0xFF5856D6), // Electric Iris
        Color(0xFF00E676)  // Neon Green
    )

    init {
        soundManager.setSoundEnabled(scoreRepository.isSoundEnabled)
    }

    fun toggleSound() {
        val newSoundState = !_uiState.value.isSoundEnabled
        scoreRepository.isSoundEnabled = newSoundState
        soundManager.setSoundEnabled(newSoundState)
        _uiState.update { it.copy(isSoundEnabled = newSoundState) }
    }

    fun startGame() {
        timerJob?.cancel()

        val initialColor = circleColors.random()
        var nextCol = circleColors.random()
        while (nextCol == initialColor) {
            nextCol = circleColors.random()
        }

        _uiState.update {
            GameUiState(
                screenState = ScreenState.PLAYING,
                currentScore = 0,
                bestScore = scoreRepository.bestScore,
                isNewHighScore = false,
                timeRemainingMs = 30_000L,
                targetFractionX = 0.2f + Random.nextFloat() * 0.6f,
                targetFractionY = 0.25f + Random.nextFloat() * 0.5f,
                currentColor = initialColor,
                nextColor = nextCol,
                comboStreak = 0,
                particles = emptyList(),
                popups = emptyList(),
                isSoundEnabled = scoreRepository.isSoundEnabled
            )
        }

        lastTapTimeMs = System.currentTimeMillis()
        lastMoveTimeMs = System.currentTimeMillis()

        // 30-second countdown timer ticking every 50ms for smooth UI updates & 0.5s circle moves
        timerJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val totalTime = 30_000L

            while (true) {
                val now = System.currentTimeMillis()
                val elapsed = now - startTime
                val remaining = (totalTime - elapsed).coerceAtLeast(0L)

                // Auto-relocate circle dynamically: starts at 2000ms (2.0s) at score 0, decreasing to 500ms (0.5s) as score increases
                val currentScore = _uiState.value.currentScore
                val dynamicMoveIntervalMs = (2000L - (currentScore * 50L)).coerceAtLeast(500L)

                if (now - lastMoveTimeMs >= dynamicMoveIntervalMs) {
                    lastMoveTimeMs = now
                    autoRelocateCircle()
                }

                _uiState.update { currentState ->
                    // Clean up expired popups (older than 800ms)
                    val validPopups = currentState.popups.filter { now - it.createdAt < 800 }
                    currentState.copy(
                        timeRemainingMs = remaining,
                        popups = validPopups
                    )
                }

                if (remaining <= 0L) {
                    onGameOver()
                    break
                }
                delay(50L)
            }
        }
    }

    private fun autoRelocateCircle() {
        if (_uiState.value.screenState != ScreenState.PLAYING) return

        val oldX = _uiState.value.targetFractionX
        val oldY = _uiState.value.targetFractionY
        var newX: Float
        var newY: Float
        do {
            newX = 0.15f + Random.nextFloat() * 0.7f
            newY = 0.18f + Random.nextFloat() * 0.62f
        } while (kotlin.math.hypot(newX - oldX, newY - oldY) < 0.25f)

        val currentColor = _uiState.value.nextColor
        var nextColor = circleColors.random()
        while (nextColor == currentColor) {
            nextColor = circleColors.random()
        }

        _uiState.update { state ->
            state.copy(
                targetFractionX = newX,
                targetFractionY = newY,
                currentColor = currentColor,
                nextColor = nextColor,
                comboStreak = 0
            )
        }
    }

    fun onTapCircle(tapX: Float, tapY: Float) {
        if (_uiState.value.screenState != ScreenState.PLAYING) return

        val now = System.currentTimeMillis()
        val timeDiff = now - lastTapTimeMs
        lastTapTimeMs = now
        lastMoveTimeMs = now // Reset the 0.5s auto-move timer when player taps target

        // Calculate combo
        val currentCombo = if (timeDiff < 500) {
            (_uiState.value.comboStreak + 1).coerceAtMost(5)
        } else {
            1
        }

        // Always add exactly 1 point per circle click
        val pointsToAdd = 1

        // Play audio pop effect
        soundManager.playTapSound(currentCombo)

        // Generate new position far enough from current
        val oldX = _uiState.value.targetFractionX
        val oldY = _uiState.value.targetFractionY
        var newX: Float
        var newY: Float
        do {
            newX = 0.15f + Random.nextFloat() * 0.7f
            newY = 0.18f + Random.nextFloat() * 0.62f
        } while (kotlin.math.hypot(newX - oldX, newY - oldY) < 0.25f)

        // Select new random color
        val currentColor = _uiState.value.nextColor
        var nextColor = circleColors.random()
        while (nextColor == currentColor) {
            nextColor = circleColors.random()
        }

        // Spawn particle burst effect
        val newParticles = generateParticles(tapX, tapY, currentColor)

        // Spawn floating text popup showing +1 point
        val popupText = "+1"
        val newPopup = TapPopup(
            id = Random.nextLong(),
            text = popupText,
            x = tapX,
            y = tapY,
            color = currentColor
        )

        _uiState.update { state ->
            state.copy(
                currentScore = state.currentScore + pointsToAdd,
                targetFractionX = newX,
                targetFractionY = newY,
                currentColor = currentColor,
                nextColor = nextColor,
                comboStreak = currentCombo,
                particles = (state.particles + newParticles).takeLast(40),
                popups = state.popups + newPopup
            )
        }
    }

    private fun generateParticles(x: Float, y: Float, color: Color): List<Particle> {
        val count = 10
        return List(count) {
            val angle = Random.nextDouble(0.0, 2 * Math.PI)
            val speed = Random.nextFloat() * 18f + 6f
            Particle(
                id = Random.nextLong(),
                startX = x,
                startY = y,
                vx = (Math.cos(angle) * speed).toFloat(),
                vy = (Math.sin(angle) * speed).toFloat(),
                color = color,
                size = Random.nextFloat() * 12f + 8f
            )
        }
    }

    private fun onGameOver() {
        val finalScore = _uiState.value.currentScore
        val isNewHigh = scoreRepository.checkAndUpdateHighScore(finalScore)
        val bestScore = scoreRepository.bestScore

        if (isNewHigh) {
            soundManager.playComboSound()
        } else {
            soundManager.playGameOverSound()
        }

        _uiState.update {
            it.copy(
                screenState = ScreenState.GAME_OVER,
                bestScore = bestScore,
                isNewHighScore = isNewHigh,
                timeRemainingMs = 0L
            )
        }
    }

    fun returnToMenu() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                screenState = ScreenState.START,
                bestScore = scoreRepository.bestScore
            )
        }
    }
}
