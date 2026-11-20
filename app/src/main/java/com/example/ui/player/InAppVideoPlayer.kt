package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import com.example.ui.util.tvFocusHighlight
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.model.PlayerMediaItem
import com.example.ui.theme.NetflixRed
import com.example.ui.theme.NetflixRedBright
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun Context.findActivity(): Activity? {
    var cur = this
    while (cur is ContextWrapper) {
        if (cur is Activity) return cur
        cur = cur.baseContext
    }
    return null
}

private fun formatDuration(millis: Long): String {
    if (millis <= 0) return "00:00"
    val totalSeconds = millis / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@OptIn(UnstableApi::class)
@Composable
fun InAppVideoPlayer(
    mediaItem: PlayerMediaItem,
    onDismiss: () -> Unit,
    onPlayNextEpisode: (PlayerMediaItem) -> Unit,
    onSaveProgress: ((positionMs: Long, durationMs: Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = remember { context.findActivity() }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(350)
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            // ignore
        }
    }

    // Automatic Landscape & Immersive Mode lifecycle
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val window = activity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            activity?.requestedOrientation = originalOrientation
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Player state
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var showResumePill by remember { mutableStateOf(mediaItem.initialPositionMs > 1500L) }

    LaunchedEffect(showResumePill) {
        if (showResumePill) {
            delay(5000)
            showResumePill = false
        }
    }

    // Controls overlay visibility & auto-hide
    var showControls by remember { mutableStateOf(true) }
    var controlsTimeoutJob by remember { mutableStateOf<Job?>(null) }

    fun scheduleControlsAutoHide() {
        controlsTimeoutJob?.cancel()
        controlsTimeoutJob = coroutineScope.launch {
            delay(3500)
            if (isPlaying) {
                showControls = false
            }
        }
    }

    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            scheduleControlsAutoHide()
        }
    }

    // 2x Hold Speed State
    var is2xActive by remember { mutableStateOf(false) }

    // Double Tap Seek feedback indicator
    var seekFeedbackSide by remember { mutableStateOf<String?>(null) } // "LEFT" or "RIGHT"
    var seekFeedbackJob by remember { mutableStateOf<Job?>(null) }

    fun triggerSeekFeedback(side: String) {
        seekFeedbackSide = side
        seekFeedbackJob?.cancel()
        seekFeedbackJob = coroutineScope.launch {
            delay(800)
            seekFeedbackSide = null
        }
    }

    // Aspect Ratio / Resize mode: 0=FIT, 1=FILL, 2=ZOOM
    var resizeModeIndex by remember { mutableIntStateOf(0) }
    val resizeModes = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT to "FIT (Orijinal)",
        AspectRatioFrameLayout.RESIZE_MODE_FILL to "FILL (Tam Ekran)",
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "ZOOM (Kırp & Büyüt)"
    )

    // Build ExoPlayer
    val exoPlayer = remember(mediaItem.mediaUri) {
        try {
            ExoPlayer.Builder(context).build().apply {
                val item = MediaItem.Builder()
                    .setUri(mediaItem.mediaUri)
                    .apply {
                        if (mediaItem.mediaUri.contains(".m3u8", ignoreCase = true)) {
                            setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
                        } else if (mediaItem.mediaUri.contains(".mpd", ignoreCase = true)) {
                            setMimeType(androidx.media3.common.MimeTypes.APPLICATION_MPD)
                        }
                    }
                    .build()
                setMediaItem(item)
                prepare()
                if (mediaItem.initialPositionMs > 1000L) {
                    seekTo(mediaItem.initialPositionMs)
                }
                playWhenReady = true
            }
        } catch (e: Exception) {
            playbackError = e.localizedMessage ?: "Oynatıcı başlatılamadı"
            ExoPlayer.Builder(context).build()
        }
    }

    // Lifecycle Observer: Stop and pause playback immediately when app enters background or is paused
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    try {
                        exoPlayer.pause()
                        exoPlayer.playWhenReady = false
                        isPlaying = false
                    } catch (e: Exception) {
                        // ignore
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Intercept hardware and gesture back navigation: stop playback before dismissing
    BackHandler {
        try {
            exoPlayer.pause()
            exoPlayer.playWhenReady = false
            exoPlayer.stop()
        } catch (e: Exception) {
            // ignore
        }
        onDismiss()
    }

    // Player event listener & release cleanup
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    totalDuration = exoPlayer.duration.coerceAtLeast(0L)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = error.localizedMessage ?: "Oynatma hatası oluştu"
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            try {
                val lastPos = exoPlayer.currentPosition
                val lastDur = exoPlayer.duration
                if (lastPos > 1000L && lastDur > 0L) {
                    onSaveProgress?.invoke(lastPos, lastDur)
                }
                exoPlayer.pause()
                exoPlayer.playWhenReady = false
                exoPlayer.stop()
                exoPlayer.removeListener(listener)
                exoPlayer.release()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    // Periodic time update ticker & progress saver
    LaunchedEffect(exoPlayer) {
        var tickCount = 0
        while (true) {
            if (exoPlayer.isPlaying) {
                val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                val dur = exoPlayer.duration.coerceAtLeast(0L)
                currentPosition = pos
                totalDuration = dur
                tickCount++
                if (tickCount % 4 == 0 && pos > 1000L && dur > 0L) {
                    onSaveProgress?.invoke(pos, dur)
                }
            }
            delay(500)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp) {
                    when (keyEvent.key) {
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            if (!showControls) {
                                showControls = true
                                scheduleControlsAutoHide()
                            } else {
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    exoPlayer.play()
                                    scheduleControlsAutoHide()
                                }
                            }
                            true
                        }
                        Key.DirectionLeft, Key.MediaRewind -> {
                            val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(newPos)
                            currentPosition = newPos
                            triggerSeekFeedback("LEFT")
                            showControls = true
                            scheduleControlsAutoHide()
                            true
                        }
                        Key.DirectionRight, Key.MediaFastForward -> {
                            val target = if (exoPlayer.duration > 0) exoPlayer.duration else Long.MAX_VALUE
                            val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(target)
                            exoPlayer.seekTo(newPos)
                            currentPosition = newPos
                            triggerSeekFeedback("RIGHT")
                            showControls = true
                            scheduleControlsAutoHide()
                            true
                        }
                        Key.DirectionUp, Key.DirectionDown -> {
                            showControls = true
                            scheduleControlsAutoHide()
                            true
                        }
                        Key.MediaPlayPause -> {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                                scheduleControlsAutoHide()
                            }
                            true
                        }
                        Key.MediaPlay -> {
                            exoPlayer.play()
                            scheduleControlsAutoHide()
                            true
                        }
                        Key.MediaPause -> {
                            exoPlayer.pause()
                            showControls = true
                            true
                        }
                        Key.MediaNext -> {
                            if (mediaItem.hasNextEpisode) {
                                mediaItem.getNextEpisode()?.let { onPlayNextEpisode(it) }
                                true
                            } else false
                        }
                        Key.MediaPrevious -> {
                            if (mediaItem.hasPreviousEpisode) {
                                mediaItem.getPreviousEpisode()?.let { onPlayNextEpisode(it) }
                                true
                            } else {
                                exoPlayer.seekTo(0L)
                                true
                            }
                        }
                        Key.Back, Key.Escape -> {
                            try {
                                exoPlayer.pause()
                                exoPlayer.playWhenReady = false
                                exoPlayer.stop()
                            } catch (e: Exception) {
                                // ignore
                            }
                            onDismiss()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .testTag("in_app_video_player")
    ) {
        // 1. AndroidView wrapping Media3 PlayerView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // We render custom M3 controls overlay
                    resizeMode = resizeModes[resizeModeIndex].first
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
                playerView.resizeMode = resizeModes[resizeModeIndex].first
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Gesture Detection Layer (Double Tap +/- 5s & Press-and-Hold 2X)
        Row(modifier = Modifier.fillMaxSize()) {
            // Left half (Double tap: -5s, Hold: 2x)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(exoPlayer) {
                        detectTapGestures(
                            onTap = {
                                showControls = !showControls
                                if (showControls) scheduleControlsAutoHide()
                            },
                            onDoubleTap = {
                                val newPos = (exoPlayer.currentPosition - 5000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(newPos)
                                currentPosition = newPos
                                triggerSeekFeedback("LEFT")
                            },
                            onPress = {
                                var holdJob: Job? = null
                                coroutineScope.launch {
                                    holdJob = launch {
                                        delay(320)
                                        is2xActive = true
                                        exoPlayer.playbackParameters = PlaybackParameters(2.0f)
                                    }
                                }
                                tryAwaitRelease()
                                holdJob?.cancel()
                                if (is2xActive) {
                                    is2xActive = false
                                    exoPlayer.playbackParameters = PlaybackParameters(1.0f)
                                }
                            }
                        )
                    }
            )

            // Right half (Double tap: +5s, Hold: 2x)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(exoPlayer) {
                        detectTapGestures(
                            onTap = {
                                showControls = !showControls
                                if (showControls) scheduleControlsAutoHide()
                            },
                            onDoubleTap = {
                                val target = if (exoPlayer.duration > 0) exoPlayer.duration else Long.MAX_VALUE
                                val newPos = (exoPlayer.currentPosition + 5000L).coerceAtMost(target)
                                exoPlayer.seekTo(newPos)
                                currentPosition = newPos
                                triggerSeekFeedback("RIGHT")
                            },
                            onPress = {
                                var holdJob: Job? = null
                                coroutineScope.launch {
                                    holdJob = launch {
                                        delay(320)
                                        is2xActive = true
                                        exoPlayer.playbackParameters = PlaybackParameters(2.0f)
                                    }
                                }
                                tryAwaitRelease()
                                holdJob?.cancel()
                                if (is2xActive) {
                                    is2xActive = false
                                    exoPlayer.playbackParameters = PlaybackParameters(1.0f)
                                }
                            }
                        )
                    }
            )
        }

        // 3. 2X Speed Active Indicator Badge (Top Center)
        AnimatedVisibility(
            visible = is2xActive,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = NetflixRed.copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.8f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "2X Hızlı Oynatılıyor",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 4. Double Tap Feedback Overlays (-5s on Left, +5s on Right)
        AnimatedVisibility(
            visible = seekFeedbackSide == "LEFT",
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "-5 sn",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = seekFeedbackSide == "RIGHT",
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "+5 sn",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 5. Buffering Spinner
        if (isBuffering && playbackError == null) {
            Box(
                modifier = Modifier.align(Alignment.Center)
            ) {
                CircularProgressIndicator(
                    color = NetflixRedBright,
                    strokeWidth = 3.5.dp,
                    modifier = Modifier.size(54.dp)
                )
            }
        }

        // 6. Playback Error Dialog / Overlay
        if (playbackError != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xDD181822),
                border = androidx.compose.foundation.BorderStroke(1.dp, NetflixRed.copy(alpha = 0.6f)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Oynatma Hatası",
                        color = NetflixRedBright,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = playbackError ?: "",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    IconButton(
                        onClick = {
                            try {
                                exoPlayer.pause()
                                exoPlayer.playWhenReady = false
                                exoPlayer.stop()
                            } catch (e: Exception) {
                                // ignore
                            }
                            onDismiss()
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NetflixRed)
                    ) {
                        Text("Kapat", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }

        // 6.5 Resume from Last Position Pill
        AnimatedVisibility(
            visible = showResumePill,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xEE1E1E28),
                border = androidx.compose.foundation.BorderStroke(1.dp, NetflixRed.copy(alpha = 0.7f)),
                shadowElevation = 8.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = NetflixRedBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Kaldığın yerden devam ediliyor (${formatDuration(mediaItem.initialPositionMs)})",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NetflixRed,
                        modifier = Modifier.clickable {
                            exoPlayer.seekTo(0L)
                            showResumePill = false
                        }
                    ) {
                        Text(
                            text = "Baştan Başlat",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 7. Fullscreen Controls Overlay (Top Bar, Center Controls, Bottom Bar)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                // Top Bar: Back button, Title, Resize button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = {
                                try {
                                    exoPlayer.pause()
                                    exoPlayer.playWhenReady = false
                                    exoPlayer.stop()
                                } catch (e: Exception) {
                                    // ignore
                                }
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .testTag("btn_player_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Geri",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = mediaItem.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (mediaItem.isStream) {
                                Text(
                                    text = "Canlı / Çevrimiçi Yayın",
                                    color = NetflixRedBright,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Screen Resize Mode Toggle Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .tvFocusHighlight(
                                shape = RoundedCornerShape(12.dp),
                                focusedBorderColor = Color.White,
                                scaleWhenFocused = 1.08f,
                                onClick = {
                                    resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size
                                    scheduleControlsAutoHide()
                                }
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Boyut",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = resizeModes[resizeModeIndex].second.substringBefore(" "),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Center Controls: Rewind 5s, Play/Pause, Forward 5s
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    // -5s button
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition - 5000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(newPos)
                            currentPosition = newPos
                            scheduleControlsAutoHide()
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .tvFocusHighlight(
                                shape = CircleShape,
                                focusedBorderColor = Color.White,
                                scaleWhenFocused = 1.15f,
                                onClick = {
                                    val newPos = (exoPlayer.currentPosition - 5000L).coerceAtLeast(0L)
                                    exoPlayer.seekTo(newPos)
                                    currentPosition = newPos
                                    scheduleControlsAutoHide()
                                }
                            )
                            .testTag("btn_player_rewind_5s")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "5sn Geri",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Play/Pause button
                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                                scheduleControlsAutoHide()
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(NetflixRed)
                            .tvFocusHighlight(
                                shape = CircleShape,
                                focusedBorderColor = Color.White,
                                scaleWhenFocused = 1.15f,
                                onClick = {
                                    if (exoPlayer.isPlaying) {
                                        exoPlayer.pause()
                                    } else {
                                        exoPlayer.play()
                                        scheduleControlsAutoHide()
                                    }
                                }
                            )
                            .testTag("btn_player_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Duraklat" else "Oynat",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // +5s button
                    IconButton(
                        onClick = {
                            val target = if (exoPlayer.duration > 0) exoPlayer.duration else Long.MAX_VALUE
                            val newPos = (exoPlayer.currentPosition + 5000L).coerceAtMost(target)
                            exoPlayer.seekTo(newPos)
                            currentPosition = newPos
                            scheduleControlsAutoHide()
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .tvFocusHighlight(
                                shape = CircleShape,
                                focusedBorderColor = Color.White,
                                scaleWhenFocused = 1.15f,
                                onClick = {
                                    val target = if (exoPlayer.duration > 0) exoPlayer.duration else Long.MAX_VALUE
                                    val newPos = (exoPlayer.currentPosition + 5000L).coerceAtMost(target)
                                    exoPlayer.seekTo(newPos)
                                    currentPosition = newPos
                                    scheduleControlsAutoHide()
                                }
                            )
                            .testTag("btn_player_forward_5s")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "5sn İleri",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Bottom Bar: Scrubber Slider, Time text, and "Sonraki Bölüm" (Next Episode) if available
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    var isScrubbing by remember { mutableStateOf(false) }
                    var scrubPosition by remember { mutableFloatStateOf(0f) }

                    // Scrubber Slider
                    Slider(
                        value = if (isScrubbing) scrubPosition else if (totalDuration > 0) currentPosition.toFloat() / totalDuration else 0f,
                        onValueChange = { frac ->
                            isScrubbing = true
                            scrubPosition = frac
                        },
                        onValueChangeFinished = {
                            if (totalDuration > 0) {
                                val target = (scrubPosition * totalDuration).toLong()
                                exoPlayer.seekTo(target)
                                currentPosition = target
                            }
                            isScrubbing = false
                            scheduleControlsAutoHide()
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = NetflixRedBright,
                            activeTrackColor = NetflixRed,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current & Total Time
                        Text(
                            text = "${formatDuration(currentPosition)} / ${formatDuration(totalDuration)}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )

                        // Next Episode Button: Automatically hidden if there is no next episode!
                        if (mediaItem.hasNextEpisode) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = NetflixRed,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .tvFocusHighlight(
                                        shape = RoundedCornerShape(10.dp),
                                        focusedBorderColor = Color.White,
                                        scaleWhenFocused = 1.08f,
                                        onClick = {
                                            mediaItem.getNextEpisode()?.let { next ->
                                                onPlayNextEpisode(next)
                                            }
                                        }
                                    )
                                    .testTag("btn_player_next_episode")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Sonraki Bölüm",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = "Sonraki Bölüm",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
