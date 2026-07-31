package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameUiState

@Composable
fun GameScreen(
    uiState: GameUiState,
    onTapCircle: (Float, Float) -> Unit,
    onToggleSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // Dynamic shrinking circle size: starts at 85.dp and reduces as score increases down to 50.dp
    val circleDiameterDp = (85.dp - (uiState.currentScore * 1.2f).dp).coerceAtLeast(50.dp)
    val circleDiameterPx = with(density) { circleDiameterDp.toPx() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val containerWidthDp = maxWidth
        val containerHeightDp = maxHeight

        val containerWidthPx = with(density) { containerWidthDp.toPx() }
        val containerHeightPx = with(density) { containerHeightDp.toPx() }

        // Reserved top bar offset (so target circle never spawns underneath status bar/HUD)
        val topReservedPx = with(density) { 90.dp.toPx() }
        val bottomReservedPx = with(density) { 30.dp.toPx() }

        val playableWidthPx = (containerWidthPx - circleDiameterPx).coerceAtLeast(10f)
        val playableHeightPx = (containerHeightPx - circleDiameterPx - topReservedPx - bottomReservedPx).coerceAtLeast(10f)

        // Convert target fractional coordinates to pixel and Dp positions
        val targetPxX = (uiState.targetFractionX * playableWidthPx).coerceIn(0f, playableWidthPx)
        val targetPxY = topReservedPx + (uiState.targetFractionY * playableHeightPx).coerceIn(0f, playableHeightPx)

        val animatedDpX: Dp by animateDpAsState(
            targetValue = with(density) { targetPxX.toDp() },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessHigh
            ),
            label = "circleX"
        )

        val animatedDpY: Dp by animateDpAsState(
            targetValue = with(density) { targetPxY.toDp() },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessHigh
            ),
            label = "circleY"
        )

        // Smooth background color pulse for circle
        val animatedCircleColor by animateColorAsState(
            targetValue = uiState.currentColor,
            animationSpec = tween(durationMillis = 300),
            label = "circleColor"
        )

        // Scale pop effect on tap
        var isTapped by remember { mutableStateOf(false) }
        val circleScale by animateFloatAsState(
            targetValue = if (isTapped) 1.25f else 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioHighBouncy,
                stiffness = Spring.StiffnessHigh
            ),
            finishedListener = { isTapped = false },
            label = "circleScale"
        )

        // Time remaining in seconds
        val secondsRemaining = (uiState.timeRemainingMs / 1000L).toInt()
        val isTimeWarning = uiState.timeRemainingMs in 1L..5000L

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Status Bar (HUD)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score Display Chip
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "SCORE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${uiState.currentScore}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.testTag("current_score_text")
                        )
                    }
                }

                // Countdown Timer Chip
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (isTimeWarning) Color(0xFFFF2D55) else Color.White.copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isTimeWarning) Color.Yellow else Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Timer",
                            tint = if (isTimeWarning) Color.Yellow else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${secondsRemaining}s",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isTimeWarning) Color.Yellow else Color.White,
                            modifier = Modifier.testTag("timer_text")
                        )
                    }
                }

                // Best Score Pill + Mute Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Best",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${uiState.bestScore}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onToggleSound,
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (uiState.isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Toggle Sound",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Particle Bursts Layer
        Canvas(modifier = Modifier.fillMaxSize()) {
            uiState.particles.forEach { particle ->
                drawCircle(
                    color = particle.color.copy(alpha = particle.alpha),
                    radius = particle.size,
                    center = Offset(particle.startX + particle.vx, particle.startY + particle.vy)
                )
            }
        }

        // Floating Score Popups (+1, +2 COMBO!)
        uiState.popups.forEach { popup ->
            Text(
                text = popup.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = popup.color,
                modifier = Modifier
                    .offset(
                        x = with(density) { popup.x.toDp() },
                        y = with(density) { (popup.y - 40f).toDp() }
                    )
            )
        }

        val animatedCenterX = with(density) { animatedDpX.toPx() } + circleDiameterPx / 2f
        val animatedCenterY = with(density) { animatedDpY.toPx() } + circleDiameterPx / 2f

        // Target Circle with "Ullas" centered inside
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(x = animatedDpX, y = animatedDpY)
                .size(circleDiameterDp.coerceAtLeast(60.dp))
                .scale(circleScale)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    spotColor = animatedCircleColor,
                    ambientColor = animatedCircleColor
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            animatedCircleColor,
                            animatedCircleColor.copy(alpha = 0.85f)
                        )
                    )
                )
                .border(4.dp, Color.White, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isTapped = true
                    onTapCircle(animatedCenterX, animatedCenterY)
                }
                .testTag("tap_circle_button")
        ) {
            Text(
                text = "Ullas",
                color = Color.White,
                fontSize = (circleDiameterDp.value * 0.24f).coerceAtLeast(14f).sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp
            )
        }
    }
}
