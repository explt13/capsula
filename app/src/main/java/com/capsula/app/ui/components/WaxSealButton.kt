package com.capsula.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A wax seal that has to be pressed and held to break.
 *
 * How it works:
 * - on press, a coroutine animates [progress] from its current value to 1f;
 * - if the finger is lifted early, that coroutine is cancelled and the ring rolls back;
 * - when the ring is full, a haptic tick plays and [onBroken] is called once.
 *
 * TalkBack users get a regular accessibility "click" action, because holding is hard for them.
 */
@Composable
fun WaxSealButton(
    onBroken: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 176.dp,
    holdDurationMillis: Int = 1_600,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val currentOnBroken by rememberUpdatedState(onBroken)
    var broken by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }

    val pressScale by animateFloatAsState(if (pressed && !broken) 0.93f else 1f, label = "sealPress")
    val waxColor = MaterialTheme.colorScheme.tertiary
    val goldColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .semantics {
                role = Role.Button
                contentDescription = "Wax seal. Press and hold to open the capsule"
                onClick(label = "Open capsule") {
                    if (!broken) {
                        broken = true
                        currentOnBroken()
                    }
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        if (broken) return@detectTapGestures
                        pressed = true
                        val fillJob = scope.launch {
                            val remaining = ((1f - progress.value) * holdDurationMillis).toInt()
                            progress.animateTo(1f, tween(remaining, easing = LinearEasing))
                            broken = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentOnBroken()
                        }
                        tryAwaitRelease()
                        pressed = false
                        if (!broken) {
                            fillJob.cancel()
                            scope.launch { progress.animateTo(0f, tween(durationMillis = 250)) }
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { progress.value },
            modifier = Modifier.fillMaxSize(),
            color = goldColor,
            strokeWidth = 7.dp,
            trackColor = goldColor.copy(alpha = 0.18f),
            strokeCap = StrokeCap.Round,
        )
        // The seal itself: wax disc with an embossed inner ring.
        Box(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxSize()
                .clip(CircleShape)
                .background(waxColor),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxSize()
                    .border(2.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(40.dp),
                )
            }
        }
    }
}
