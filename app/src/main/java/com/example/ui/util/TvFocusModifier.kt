package com.example.ui.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NetflixRedBright

/**
 * Adds Android TV D-pad focusability, smooth focus scaling, high-contrast glow border,
 * and remote control (DPAD_CENTER / ENTER) click trigger.
 */
fun Modifier.tvFocusHighlight(
    shape: Shape = RoundedCornerShape(12.dp),
    focusedBorderColor: Color = NetflixRedBright,
    borderWidth: Dp = 3.dp,
    scaleWhenFocused: Float = 1.05f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) scaleWhenFocused else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "tv_focus_scale"
    )

    this
        .scale(scale)
        .onFocusChanged { isFocused = it.isFocused }
        .then(
            if (isFocused) {
                Modifier.border(BorderStroke(borderWidth, focusedBorderColor), shape)
            } else {
                Modifier
            }
        )
        .then(
            if (onClick != null) {
                Modifier
                    .clickable(onClick = onClick)
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyUp &&
                            (keyEvent.key == Key.DirectionCenter ||
                             keyEvent.key == Key.Enter ||
                             keyEvent.key == Key.NumPadEnter)
                        ) {
                            onClick.invoke()
                            true
                        } else {
                            false
                        }
                    }
            } else {
                Modifier.focusable()
            }
        )
}
