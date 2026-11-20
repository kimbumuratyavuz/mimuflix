package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.DownloadedEpisode
import com.example.model.DownloadedMovie
import com.example.model.DownloadedSeriesFolder
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.theme.NetflixRed
import com.example.ui.theme.NetflixRedBright
import kotlinx.coroutines.launch

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.1f MB", mb)
        else -> String.format("%.0f KB", kb)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadedMediaScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val movies by viewModel.downloadedMovies.collectAsState()
    val seriesFolders by viewModel.downloadedSeries.collectAsState()

    // Refresh library when entering screen
    LaunchedEffect(Unit) {
        viewModel.refreshDownloadedMedia(context)
    }

    // Pager state: 0 = Filmler, 1 = Diziler
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    // Active series folder for episode list view
    var activeSeriesFolder by remember { mutableStateOf<DownloadedSeriesFolder?>(null) }

    // Synchronize active folder with current seriesFolders state
    val currentActiveFolder = activeSeriesFolder?.let { active ->
        seriesFolders.find { it.folder.absolutePath == active.folder.absolutePath }
    }
    if (activeSeriesFolder != null && (currentActiveFolder == null || currentActiveFolder.episodes.isEmpty())) {
        activeSeriesFolder = null
    }

    // Handle back button when viewing series episodes list
    if (currentActiveFolder != null) {
        BackHandler {
            activeSeriesFolder = null
        }
    }

    // Deletion confirmation states
    var movieToDelete by remember { mutableStateOf<DownloadedMovie?>(null) }
    var episodeToDelete by remember { mutableStateOf<Pair<DownloadedSeriesFolder, DownloadedEpisode>?>(null) }
    var folderToDelete by remember { mutableStateOf<DownloadedSeriesFolder?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (currentActiveFolder != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = currentActiveFolder.seriesName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${currentActiveFolder.episodes.size} İndirilen Bölüm • ${formatBytes(currentActiveFolder.totalSizeBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(NetflixRed.copy(alpha = 0.15f))
                                    .border(1.dp, NetflixRed.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    tint = NetflixRedBright,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "İndirilenler",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "${movies.size} Film • ${seriesFolders.size} Dizi (${seriesFolders.sumOf { it.episodes.size }} Bölüm)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (currentActiveFolder != null) {
                        IconButton(
                            onClick = { activeSeriesFolder = null },
                            modifier = Modifier.testTag("btn_back_to_series_folders")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Dizilere Dön"
                            )
                        }
                    }
                },
                actions = {
                    if (currentActiveFolder != null) {
                        IconButton(
                            onClick = { folderToDelete = currentActiveFolder },
                            modifier = Modifier.testTag("btn_delete_active_series_folder")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Diziyi Sil",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.refreshDownloadedMedia(context) },
                            modifier = Modifier.testTag("btn_refresh_downloads")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Yenile",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { viewModel.openFolder(context) },
                            modifier = Modifier.testTag("btn_open_folder")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Klasörü Aç",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.openSettings() },
                        modifier = Modifier.testTag("btn_open_settings_downloads")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ayarlar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Tabs: Filmler vs Diziler (hidden when inside a series' episode list)
            if (activeSeriesFolder == null) {
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = NetflixRedBright,
                    indicator = {
                        TabRowDefaults.PrimaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(pagerState.currentPage),
                            color = NetflixRedBright,
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Filmler (${movies.size})",
                                    fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        },
                        modifier = Modifier.testTag("tab_downloaded_movies")
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Diziler (${seriesFolders.size})",
                                    fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        },
                        modifier = Modifier.testTag("tab_downloaded_series")
                    )
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                val screenWidth = maxWidth
                val columns = when {
                    screenWidth >= 1200.dp -> GridCells.Fixed(8)
                    screenWidth >= 900.dp -> GridCells.Fixed(6)
                    screenWidth >= 600.dp -> GridCells.Fixed(4)
                    else -> GridCells.Fixed(3)
                }
                val gridHorizontalPadding = if (screenWidth >= 840.dp) 16.dp else 8.dp

                if (currentActiveFolder != null) {
                    // Bölümler Liste Görünümü (Episode List View)
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = 12.dp,
                            bottom = 120.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 840.dp)
                    ) {
                        item {
                            // Folder summary banner
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // Poster thumbnail
                                    Box(
                                        modifier = Modifier
                                            .width(52.dp)
                                            .aspectRatio(2f / 3f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        if (!currentActiveFolder.posterUrl.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(currentActiveFolder.posterUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = currentActiveFolder.seriesName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Tv,
                                                    contentDescription = null,
                                                    tint = NetflixRedBright,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = currentActiveFolder.seriesName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${currentActiveFolder.episodes.size} Bölüm • ${formatBytes(currentActiveFolder.totalSizeBytes)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "İndirilen Bölümler",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                        }

                        items(currentActiveFolder.episodes, key = { it.file.absolutePath }) { episode ->
                            DownloadedEpisodeRow(
                                episode = episode,
                                onPlay = { viewModel.playDownloadedEpisode(currentActiveFolder, episode) },
                                onDelete = { episodeToDelete = Pair(currentActiveFolder, episode) }
                            )
                        }
                    }
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        if (page == 0) {
                            // ============================================================
                            // 1. FİLMLER AFİŞ KART GÖRÜNÜMÜ (Movies Poster Grid)
                            // ============================================================
                            if (movies.isEmpty()) {
                                EmptyDownloadedView(
                                    title = "İndirilen Film Yok",
                                    subtitle = "Filmler kataloğundan dilediğiniz filmi indirip internetsiz izleyebilirsiniz.",
                                    buttonText = "Filmleri Keşfet",
                                    onAction = { viewModel.selectTab(MainTab.MOVIES) }
                                )
                            } else {
                                LazyVerticalGrid(
                                    columns = columns,
                                    contentPadding = PaddingValues(
                                        start = gridHorizontalPadding,
                                        end = gridHorizontalPadding,
                                        top = 8.dp,
                                        bottom = 120.dp
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(movies, key = { it.file.absolutePath }) { movie ->
                                        DownloadedMoviePosterCard(
                                            movie = movie,
                                            onPlay = { viewModel.playDownloadedMovie(movie) },
                                            onDelete = { movieToDelete = movie }
                                        )
                                    }
                                }
                            }
                        } else {
                            // ============================================================
                            // 2. DİZİLER: AFİŞ KART KLASÖRÜ VE BÖLÜM LİSTESİ
                            // ============================================================
                            if (seriesFolders.isEmpty()) {
                                EmptyDownloadedView(
                                    title = "İndirilen Dizi Yok",
                                    subtitle = "Diziler kataloğundan bölümleri indirip dilediğiniz zaman internetsiz izleyebilirsiniz.",
                                    buttonText = "Dizileri Keşfet",
                                    onAction = { viewModel.selectTab(MainTab.SERIES) }
                                )
                            } else {
                                LazyVerticalGrid(
                                    columns = columns,
                                    contentPadding = PaddingValues(
                                        start = gridHorizontalPadding,
                                        end = gridHorizontalPadding,
                                        top = 8.dp,
                                        bottom = 120.dp
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(seriesFolders, key = { it.folder.absolutePath }) { folder ->
                                        DownloadedSeriesFolderPosterCard(
                                            folder = folder,
                                            onClick = { activeSeriesFolder = folder },
                                            onDelete = { folderToDelete = folder }
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

    // Delete Movie Confirmation Dialog
    movieToDelete?.let { movie ->
        ModernDeleteConfirmDialog(
            title = "Filmi Sil",
            description = "\"${movie.title}\" cihazınızdan kalıcı olarak silinecektir.",
            onConfirm = {
                viewModel.deleteDownloadedMovie(context, movie)
                movieToDelete = null
            },
            onDismiss = { movieToDelete = null }
        )
    }

    // Delete Episode Confirmation Dialog
    episodeToDelete?.let { (folder, ep) ->
        val isLast = folder.episodes.size <= 1
        ModernDeleteConfirmDialog(
            title = "Bölümü Sil",
            description = "\"${ep.episodeName}\" silinecektir.${if (isLast) " Bu dizinin son bölümü olduğu için dizi klasörü de otomatik olarak silinecektir." else ""}",
            onConfirm = {
                viewModel.deleteDownloadedEpisode(context, folder, ep)
                episodeToDelete = null
                if (isLast) {
                    activeSeriesFolder = null
                }
            },
            onDismiss = { episodeToDelete = null }
        )
    }

    // Delete Entire Series Folder Confirmation Dialog
    folderToDelete?.let { folder ->
        ModernDeleteConfirmDialog(
            title = "Dizi Klasörünü Sil",
            description = "\"${folder.seriesName}\" klasörü ve içindeki ${folder.episodes.size} bölüm tamamen silinecektir.",
            onConfirm = {
                viewModel.deleteDownloadedSeriesFolder(context, folder)
                if (activeSeriesFolder?.folder?.absolutePath == folder.folder.absolutePath) {
                    activeSeriesFolder = null
                }
                folderToDelete = null
            },
            onDismiss = { folderToDelete = null }
        )
    }
}

/**
 * Filmler için Afiş Kart Görünümü (Poster Card)
 */
@Composable
private fun DownloadedMoviePosterCard(
    movie: DownloadedMovie,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .testTag("downloaded_movie_card_${movie.title}")
    ) {
        Column {
            // Poster Box (2:3 aspect ratio)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!movie.posterUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(movie.posterUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = NetflixRedBright.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Top format badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = movie.file.extension.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                // Top-Left size badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = formatBytes(movie.sizeBytes),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                // Gradient overlay at bottom of poster
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )

                // Play icon in center
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(NetflixRed.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Oynat",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Delete button in bottom-right corner
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable(onClick = onDelete)
                        .testTag("btn_delete_movie_${movie.title}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Title & Info below poster
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 7.dp, vertical = 6.dp)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 15.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Diziler için Afiş Kart Klasör Görünümü (Series Folder Poster Card)
 */
@Composable
private fun DownloadedSeriesFolderPosterCard(
    folder: DownloadedSeriesFolder,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("downloaded_series_folder_card_${folder.seriesName}")
    ) {
        Column {
            // Poster Box (2:3 aspect ratio)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!folder.posterUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(folder.posterUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = folder.seriesName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Top-Right episode count badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = NetflixRed.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "${folder.episodes.size} Bölüm",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Top-Left total size badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = formatBytes(folder.totalSizeBytes),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                // Gradient overlay at bottom of poster
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )

                // Folder badge in bottom-left
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = NetflixRedBright,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Dizi",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Delete button in bottom-right corner
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable(onClick = onDelete)
                        .testTag("btn_delete_series_${folder.seriesName}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Title & Info below poster
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 7.dp, vertical = 6.dp)
            ) {
                Text(
                    text = folder.seriesName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 15.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Dizi Bölümleri Liste Görünümü Öğesi
 */
@Composable
private fun DownloadedEpisodeRow(
    episode: DownloadedEpisode,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .testTag("downloaded_ep_${episode.episodeName}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NetflixRed.copy(alpha = 0.15f))
                    .border(1.dp, NetflixRed.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = NetflixRedBright,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.episodeName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = episode.file.extension.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                    Text(
                        text = formatBytes(episode.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Play & Delete Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onPlay,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NetflixRed,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Oynat",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "İzle",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                        .testTag("btn_delete_episode")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Bölümü Sil",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDownloadedView(
    title: String,
    subtitle: String,
    buttonText: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 520.dp)
            .padding(horizontal = 24.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(NetflixRed.copy(alpha = 0.12f))
                .border(1.5.dp, NetflixRed.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = NetflixRedBright,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAction,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NetflixRed,
                contentColor = Color.White
            ),
            modifier = Modifier
                .height(44.dp)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = buttonText,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ModernDeleteConfirmDialog(
    title: String,
    description: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 18.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 440.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(26.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                        .border(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = "Vazgeç",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_confirm_delete")
                    ) {
                        Text(
                            text = "Sil",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
