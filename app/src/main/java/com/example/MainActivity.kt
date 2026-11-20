package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NetflixRed
import com.example.ui.theme.NetflixRedBright
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.VideoLibrary
import com.example.ui.screens.DownloadedMediaScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.player.InAppVideoPlayer
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import com.example.ui.AppScreen
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.ThemeMode
import com.example.ui.screens.DownloadsQueueScreen
import com.example.ui.screens.MoviesCatalogScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SeriesCatalogScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.MovieDetailScreen
import com.example.ui.screens.MovieSearchScreen
import com.example.ui.screens.SeriesDetailScreen
import com.example.ui.screens.SeriesSearchScreen
import com.example.ui.screens.ManualInputScreen
import com.example.ui.screens.M3uListScreen
import com.example.ui.theme.MimuDownloaderTheme
import com.example.ui.util.tvFocusHighlight

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
            val originTab by viewModel.originTab.collectAsState()
            val themeMode by viewModel.themeMode.collectAsState()
            val keepScreenOn by viewModel.keepScreenOn.collectAsState()
            val currentTab by viewModel.currentTab.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()
            val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()
            val isDownloading by viewModel.isDownloading.collectAsState()
            val isSplashVisible by viewModel.isSplashVisible.collectAsState()
            val splashCoverUrl by viewModel.splashMovieCoverUrl.collectAsState()
            val splashProgress by viewModel.splashProgress.collectAsState()

            // Preserved scroll states across all tabs
            val movieGridState = rememberLazyGridState()
            val seriesGridState = rememberLazyGridState()

            // Back navigation:
            // 1. If in a secondary screen (Detail, Search, List), navigate back through backstack
            // 2. If in a tab other than Movies, return to Movies tab
            BackHandler(enabled = !isOnboardingCompleted || currentScreen != AppScreen.HOME || currentTab != MainTab.MOVIES) {
                if (!isOnboardingCompleted) {
                    return@BackHandler
                }
                if (currentScreen != AppScreen.HOME) {
                    viewModel.navigateBack()
                    return@BackHandler
                }
                if (currentTab != MainTab.MOVIES) {
                    viewModel.selectTab(MainTab.MOVIES)
                }
            }

            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        androidx.activity.SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (darkTheme) {
                        androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        androidx.activity.SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
                onDispose {}
            }

            // Handle Keep Screen On
            DisposableEffect(keepScreenOn) {
                if (keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                onDispose {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            // Permission Launcher for Android 13+ Notifications & Android <= 9 Storage
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { _ -> }

            LaunchedEffect(Unit) {
                val permissionsToRequest = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }
                if (permissionsToRequest.isNotEmpty()) {
                    permissionLauncher.launch(permissionsToRequest.toTypedArray())
                }
            }

            MimuDownloaderTheme(darkTheme = darkTheme) {
                if (!isOnboardingCompleted) {
                    OnboardingScreen(
                        onFinish = { viewModel.completeOnboarding() },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        // Full-screen content layer extending all the way down behind the floating menu
                        // Movies and Series catalog layers stay composed with their LazyGridState to preserve exact scroll position!
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val isMovies = currentTab == MainTab.MOVIES
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        alpha = if (isMovies) 1f else 0f
                                        translationX = if (isMovies) 0f else 10000f
                                    }
                            ) {
                                MoviesCatalogScreen(
                                    viewModel = viewModel,
                                    gridState = movieGridState,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            val isSeries = currentTab == MainTab.SERIES
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        alpha = if (isSeries) 1f else 0f
                                        translationX = if (isSeries) 0f else 10000f
                                    }
                            ) {
                                SeriesCatalogScreen(
                                    viewModel = viewModel,
                                    gridState = seriesGridState,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            if (currentTab == MainTab.FAVORITES) {
                                FavoritesScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            if (currentTab == MainTab.DOWNLOADED) {
                                DownloadedMediaScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            if (currentTab == MainTab.DOWNLOADER) {
                                DownloadsQueueScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                    // Soft gradient scrim underlay along the bottom edge:
                    // Gently dims out the busy scrolling movie/series poster content behind the navigation area
                    // so the items never visually clash with the floating glass bar.
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = if (darkTheme) {
                                        listOf(
                                            Color.Transparent,
                                            Color(0x66080A10),
                                            Color(0xB3080A10),
                                            Color(0xF0080A10)
                                        )
                                    } else {
                                        listOf(
                                            Color.Transparent,
                                            Color(0x55F5F7FA),
                                            Color(0xB0F5F7FA),
                                            Color(0xEBF5F7FA)
                                        )
                                    }
                                )
                            )
                    )

                    // Floating Glass Bottom Bar sitting directly over the bottom of the screen
                    val isMovies = currentTab == MainTab.MOVIES
                    val isSeries = currentTab == MainTab.SERIES
                    val isFavorites = currentTab == MainTab.FAVORITES
                    val isDownloaded = currentTab == MainTab.DOWNLOADED
                    val isDl = currentTab == MainTab.DOWNLOADER

                    val movieIconSize by animateDpAsState(targetValue = if (isMovies) 24.dp else 19.dp, label = "movie_icon_size")
                    val seriesIconSize by animateDpAsState(targetValue = if (isSeries) 24.dp else 19.dp, label = "series_icon_size")
                    val favoritesIconSize by animateDpAsState(targetValue = if (isFavorites) 24.dp else 19.dp, label = "favorites_icon_size")
                    val downloadedIconSize by animateDpAsState(targetValue = if (isDownloaded) 24.dp else 19.dp, label = "downloaded_icon_size")
                    val dlIconSize by animateDpAsState(targetValue = if (isDl) 24.dp else 19.dp, label = "dl_icon_size")

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .widthIn(max = 560.dp)
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    ) {
                        // Ambient backlight glow for dark theme so the glass stands out vividly
                        if (darkTheme) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                NetflixRed.copy(alpha = 0.28f),
                                                Color.Transparent
                                            ),
                                            radius = 420f
                                        ),
                                        shape = RoundedCornerShape(28.dp)
                                    )
                            )
                        }

                        // Frosted Glass Bar Container with multi-layered specular gradient & beveled rim
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = if (darkTheme) 22.dp else 14.dp,
                                    shape = RoundedCornerShape(26.dp),
                                    spotColor = if (darkTheme) Color.Black.copy(alpha = 0.95f) else Color(0x35000000),
                                    ambientColor = if (darkTheme) Color.Black.copy(alpha = 0.75f) else Color(0x20000000)
                                )
                                .clip(RoundedCornerShape(26.dp))
                                .background(
                                    brush = if (darkTheme) {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xEE2A3546), // Luminous frosted top sheen (~93% opaque)
                                                Color(0xE8161F2C), // High-density smoky frost body (~91% opaque)
                                                Color(0xF0101520), // Deep rich glass body (~94% opaque)
                                                Color(0xF80B0E14)  // Weighted bottom glass (~97% opaque)
                                            )
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFBFFFFFF),
                                                Color(0xF2F1F5F9),
                                                Color(0xF6E2E8F0)
                                            )
                                        )
                                    }
                                )
                                .border(
                                    border = BorderStroke(
                                        width = 1.3.dp,
                                        brush = Brush.verticalGradient(
                                            colors = if (darkTheme) {
                                                listOf(
                                                    Color.White.copy(alpha = 0.85f), // Crisp bright specular top reflection
                                                    Color.White.copy(alpha = 0.30f), // Translucent side rim
                                                    NetflixRed.copy(alpha = 0.40f),  // Subtle Netflix red refraction
                                                    Color.White.copy(alpha = 0.20f)  // Soft bottom edge
                                                )
                                            } else {
                                                listOf(
                                                    Color.White.copy(alpha = 0.95f),
                                                    Color.White.copy(alpha = 0.50f),
                                                    NetflixRed.copy(alpha = 0.30f),
                                                    Color.White.copy(alpha = 0.75f)
                                                )
                                            }
                                        )
                                    ),
                                    shape = RoundedCornerShape(26.dp)
                                )
                                .testTag("bottom_nav_bar")
                        ) {
                            // Specular gloss reflection line right below the top curved rim
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.2.dp)
                                    .align(Alignment.TopCenter)
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.White.copy(alpha = if (darkTheme) 0.70f else 0.85f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )

                            val navColors = NavigationBarItemDefaults.colors(
                                selectedIconColor = if (darkTheme) NetflixRedBright else NetflixRed,
                                selectedTextColor = if (darkTheme) NetflixRedBright else NetflixRed,
                                unselectedIconColor = if (darkTheme) Color.White.copy(alpha = 0.75f) else Color(0xFF64748B),
                                unselectedTextColor = if (darkTheme) Color.White.copy(alpha = 0.75f) else Color(0xFF64748B),
                                indicatorColor = if (darkTheme) NetflixRed.copy(alpha = 0.32f) else NetflixRed.copy(alpha = 0.18f)
                            )

                            NavigationBar(
                                containerColor = Color.Transparent,
                                tonalElevation = 0.dp,
                                windowInsets = WindowInsets(0, 0, 0, 0),
                                modifier = Modifier.height(68.dp)
                            ) {
                                // 1. Filmler Tab
                                NavigationBarItem(
                                    selected = isMovies,
                                    onClick = { viewModel.selectTab(MainTab.MOVIES) },
                                    icon = {
                                        Icon(
                                            imageVector = if (isMovies) Icons.Filled.Movie else Icons.Outlined.Movie,
                                            contentDescription = stringResource(R.string.tab_movies),
                                            modifier = Modifier.size(movieIconSize)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(R.string.tab_movies),
                                            fontSize = if (isMovies) 11.5.sp else 9.5.sp,
                                            fontWeight = if (isMovies) FontWeight.Black else FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    },
                                    colors = navColors,
                                    modifier = Modifier
                                        .testTag("tab_movies")
                                        .tvFocusHighlight(
                                            shape = RoundedCornerShape(16.dp),
                                            focusedBorderColor = NetflixRedBright,
                                            scaleWhenFocused = 1.08f,
                                            onClick = { viewModel.selectTab(MainTab.MOVIES) }
                                        )
                                 )

                                // 2. Diziler Tab
                                NavigationBarItem(
                                    selected = isSeries,
                                    onClick = { viewModel.selectTab(MainTab.SERIES) },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSeries) Icons.Filled.Tv else Icons.Outlined.Tv,
                                            contentDescription = stringResource(R.string.tab_series),
                                            modifier = Modifier.size(seriesIconSize)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(R.string.tab_series),
                                            fontSize = if (isSeries) 11.5.sp else 9.5.sp,
                                            fontWeight = if (isSeries) FontWeight.Black else FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    },
                                    colors = navColors,
                                    modifier = Modifier
                                        .testTag("tab_series")
                                        .tvFocusHighlight(
                                            shape = RoundedCornerShape(16.dp),
                                            focusedBorderColor = NetflixRedBright,
                                            scaleWhenFocused = 1.08f,
                                            onClick = { viewModel.selectTab(MainTab.SERIES) }
                                        )
                                )

                                // 3. Favoriler Tab
                                NavigationBarItem(
                                    selected = isFavorites,
                                    onClick = { viewModel.selectTab(MainTab.FAVORITES) },
                                    icon = {
                                        Icon(
                                            imageVector = if (isFavorites) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                            contentDescription = stringResource(R.string.tab_favorites),
                                            modifier = Modifier.size(favoritesIconSize)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(R.string.tab_favorites),
                                            fontSize = if (isFavorites) 11.5.sp else 9.5.sp,
                                            fontWeight = if (isFavorites) FontWeight.Black else FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    },
                                    colors = navColors,
                                    modifier = Modifier
                                        .testTag("tab_favorites")
                                        .tvFocusHighlight(
                                            shape = RoundedCornerShape(16.dp),
                                            focusedBorderColor = NetflixRedBright,
                                            scaleWhenFocused = 1.08f,
                                            onClick = { viewModel.selectTab(MainTab.FAVORITES) }
                                        )
                                )

                                // 4. İndirilenler Tab
                                NavigationBarItem(
                                    selected = isDownloaded,
                                    onClick = { viewModel.selectTab(MainTab.DOWNLOADED) },
                                    icon = {
                                        Icon(
                                            imageVector = if (isDownloaded) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary,
                                            contentDescription = stringResource(R.string.tab_downloaded),
                                            modifier = Modifier.size(downloadedIconSize)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(R.string.tab_downloaded),
                                            fontSize = if (isDownloaded) 11.sp else 9.5.sp,
                                            fontWeight = if (isDownloaded) FontWeight.Black else FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    },
                                    colors = navColors,
                                    modifier = Modifier
                                        .testTag("tab_downloaded")
                                        .tvFocusHighlight(
                                            shape = RoundedCornerShape(16.dp),
                                            focusedBorderColor = NetflixRedBright,
                                            scaleWhenFocused = 1.08f,
                                            onClick = { viewModel.selectTab(MainTab.DOWNLOADED) }
                                        )
                                )

                                // 5. Kuyruk / İndirme Tab
                                NavigationBarItem(
                                    selected = isDl,
                                    onClick = { viewModel.selectTab(MainTab.DOWNLOADER) },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (isDownloading) {
                                                    Badge(
                                                        containerColor = if (darkTheme) NetflixRedBright else NetflixRed
                                                    ) {
                                                        Text("●", color = Color.White)
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isDl) Icons.Filled.Download else Icons.Outlined.Download,
                                                contentDescription = stringResource(R.string.tab_downloader),
                                                modifier = Modifier.size(dlIconSize)
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(R.string.tab_downloader),
                                            fontSize = if (isDl) 11.sp else 9.5.sp,
                                            fontWeight = if (isDl) FontWeight.Black else FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    },
                                    colors = navColors,
                                    modifier = Modifier
                                        .testTag("tab_downloader")
                                        .tvFocusHighlight(
                                            shape = RoundedCornerShape(16.dp),
                                            focusedBorderColor = NetflixRedBright,
                                            scaleWhenFocused = 1.08f,
                                            onClick = { viewModel.selectTab(MainTab.DOWNLOADER) }
                                        )
                                )
                            }
                        }
                    }

                    // Fullscreen Secondary App Screens
                    when (currentScreen) {
                        AppScreen.MOVIE_DETAIL -> {
                            val movie by viewModel.selectedMovieForDetail.collectAsState()
                            movie?.let {
                                MovieDetailScreen(
                                    movie = it,
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateBack() },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        AppScreen.SERIES_DETAIL -> {
                            val series by viewModel.selectedSeriesForDetail.collectAsState()
                            series?.let {
                                SeriesDetailScreen(
                                    series = it,
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateBack() },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        AppScreen.MOVIE_SEARCH -> {
                            MovieSearchScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        AppScreen.SERIES_SEARCH -> {
                            SeriesSearchScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        AppScreen.MANUAL_INPUT -> {
                            ManualInputScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        AppScreen.LIST -> {
                            M3uListScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        AppScreen.HOME -> {
                            // Home is rendered underneath
                        }
                    }

                    // Fullscreen Settings Page Overlay
                    if (showSettingsDialog) {
                        SettingsScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.closeSettings() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Fullscreen In-App Video Player Overlay
                    val activePlayerMedia by viewModel.activePlayerMedia.collectAsState()
                    activePlayerMedia?.let { media ->
                        InAppVideoPlayer(
                            mediaItem = media,
                            onDismiss = { viewModel.closePlayer() },
                            onPlayNextEpisode = { viewModel.playNextEpisode(it) },
                            onSaveProgress = { pos, dur ->
                                viewModel.savePlaybackProgress(
                                    mediaId = media.mediaId.ifEmpty { media.mediaUri },
                                    title = media.title,
                                    posterUrl = media.posterUrl,
                                    streamUrl = media.mediaUri,
                                    isSeries = media.isSeries,
                                    seriesName = media.seriesName,
                                    episodeName = media.episodeName,
                                    positionMs = pos,
                                    durationMs = dur
                                )
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Splash Screen Overlay (M3U listeleri yüklenene kadar tam ekran film kapağı & altta kırmızı progress bar)
                    AnimatedVisibility(
                        visible = isSplashVisible,
                        enter = fadeIn(),
                        exit = fadeOut(animationSpec = tween(400))
                    ) {
                        SplashScreen(
                            coverUrl = splashCoverUrl,
                            progress = splashProgress,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
}
