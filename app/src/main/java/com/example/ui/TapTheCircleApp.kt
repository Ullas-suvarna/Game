package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.GameViewModel
import com.example.game.ScreenState
import com.example.ui.screens.GameOverScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.StartScreen

@Composable
fun TapTheCircleApp(
    viewModel: GameViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Smooth animated gradient background shift
    val infiniteTransition = rememberInfiniteTransition(label = "GradientBackground")
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientShift"
    )

    val color1 = Color(0xFF1E1035) // Dark Purple
    val color2 = Color(0xFF2D1542) // Deep Violet
    val color3 = Color(0xFF0F172A) // Dark Slate Blue

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            color1,
            Color(
                red = color1.red + (color2.red - color1.red) * gradientShift,
                green = color1.green + (color2.green - color1.green) * gradientShift,
                blue = color1.blue + (color2.blue - color1.blue) * gradientShift
            ),
            color3
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Crossfade(
            targetState = uiState.screenState,
            animationSpec = tween(400),
            label = "ScreenTransition"
        ) { state ->
            when (state) {
                ScreenState.START -> {
                    StartScreen(
                        bestScore = uiState.bestScore,
                        isSoundEnabled = uiState.isSoundEnabled,
                        onStartGame = { viewModel.startGame() },
                        onToggleSound = { viewModel.toggleSound() }
                    )
                }

                ScreenState.PLAYING -> {
                    GameScreen(
                        uiState = uiState,
                        onTapCircle = { x, y -> viewModel.onTapCircle(x, y) },
                        onToggleSound = { viewModel.toggleSound() }
                    )
                }

                ScreenState.GAME_OVER -> {
                    GameOverScreen(
                        finalScore = uiState.currentScore,
                        bestScore = uiState.bestScore,
                        isNewHighScore = uiState.isNewHighScore,
                        onPlayAgain = { viewModel.startGame() },
                        onReturnHome = { viewModel.returnToMenu() }
                    )
                }
            }
        }
    }
}
