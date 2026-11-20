package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CatalogEpisodeItem
import com.example.model.CatalogSeries
import com.example.model.LanguageBadge
import com.example.model.SeriesSortOrder
import com.example.ui.MainViewModel
import androidx.compose.material.icons.automirrored.filled.Sort
import com.example.ui.components.ContinueWatchingRow
import com.example.ui.components.SeriesHeaderCarousel
import com.example.ui.theme.NetflixRed
import com.example.ui.theme.NetflixRedBright
import com.example.ui.util.tvFocusHighlight
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesCatalogScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState()
) {
    val context = LocalContext.current
    val seriesList by viewModel.pagedSeries.collectAsState()
    val allSeries by viewModel.seriesList.collectAsState()
    val isLoading by viewModel.isLoadingSeries.collectAsState()
    val error by viewModel.seriesError.collectAsState()
    val selectedLanguage by viewModel.selectedSeriesLanguage.collectAsState()
    val sortOrder by viewModel.seriesSortOrder.collectAsState()
    val favoriteSeriesIds by viewModel.favoriteSeriesIds.collectAsState()
    val dbTasks by viewModel.dbTasks.collectAsState()
    val queuedEpisodeIds = remember(dbTasks) { dbTasks.map { it.id }.toSet() }
    val queuedEpisodeUrls = remember(dbTasks) { dbTasks.map { it.streamUrl }.toSet() }

    val featuredSeries by viewModel.featuredSeries.collectAsState()
    val seriesProgress by viewModel.seriesPlaybackProgress.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var showSortMenu by remember { mutableStateOf(false) }
    var seriesForDownloadSelection by remember { mutableStateOf<CatalogSeries?>(null) }

    // Auto-scroll to top when sorting or language filter changes
    androidx.compose.runtime.LaunchedEffect(sortOrder, selectedLanguage) {
        gridState.animateScrollToItem(0)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Diziler",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Search trigger button
                    IconButton(
                        onClick = { viewModel.openSeriesSearch() },
                        modifier = Modifier
                            .tvFocusHighlight(
                                shape = CircleShape,
                                focusedBorderColor = NetflixRedBright,
                                scaleWhenFocused = 1.15f,
                                onClick = { viewModel.openSeriesSearch() }
                            )
                            .testTag("btn_open_search_series")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Dizi Ara",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Sort order trigger button
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .tvFocusHighlight(
                                shape = CircleShape,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                scaleWhenFocused = 1.15f,
                                onClick = { showSortMenu = true }
                            )
                            .testTag("btn_sort_series")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sırala",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Sort menu
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Varsayılan Sıra") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null) },
                            onClick = {
                                viewModel.setSeriesSortOrder(SeriesSortOrder.DEFAULT)
                                showSortMenu = false
                                coroutineScope.launch { gridState.animateScrollToItem(0) }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Bölüm Sayısı: Azdan Çoğa") },
                            leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = SuccessGreen) },
                            onClick = {
                                viewModel.setSeriesSortOrder(SeriesSortOrder.EPISODES_ASC)
                                showSortMenu = false
                                coroutineScope.launch { gridState.animateScrollToItem(0) }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Bölüm Sayısı: Çoktan Aza") },
                            leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = WarningAmber) },
                            onClick = {
                                viewModel.setSeriesSortOrder(SeriesSortOrder.EPISODES_DESC)
                                showSortMenu = false
                                coroutineScope.launch { gridState.animateScrollToItem(0) }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("İsim (A-Z)") },
                            onClick = {
                                viewModel.setSeriesSortOrder(SeriesSortOrder.NAME_ASC)
                                showSortMenu = false
                                coroutineScope.launch { gridState.animateScrollToItem(0) }
                            }
                        )
                    }

                    // Settings button
                    IconButton(
                        onClick = { viewModel.openSettings() },
                        modifier = Modifier
                            .tvFocusHighlight(
                                shape = CircleShape,
                                focusedBorderColor = NetflixRedBright,
                                scaleWhenFocused = 1.15f,
                                onClick = { viewModel.openSettings() }
                            )
                            .testTag("btn_open_settings_series")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ayarlar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            val screenWidth = maxWidth
            val columns = when {
                screenWidth >= 1200.dp -> GridCells.Fixed(8)
                screenWidth >= 900.dp -> GridCells.Fixed(6)
                screenWidth >= 600.dp -> GridCells.Fixed(4)
                screenWidth >= 400.dp -> GridCells.Fixed(3)
                else -> GridCells.Fixed(3)
            }
            val horizontalPadding = if (screenWidth >= 840.dp) 20.dp else 8.dp

            when {
                isLoading && allSeries.isEmpty() -> {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }
                error != null && allSeries.isEmpty() -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Diziler yüklenemedi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = error ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )
                        Button(onClick = { viewModel.loadSeries(forceRefresh = true) }) {
                            Text("Tekrar Dene")
                        }
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = columns,
                        contentPadding = PaddingValues(start = horizontalPadding, end = horizontalPadding, top = 2.dp, bottom = 100.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 1. Featured Header Carousel (5 Random Recommended Series) - Scrolls with the page
                        if (featuredSeries.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }, key = "featured_series_carousel") {
                                SeriesHeaderCarousel(
                                    featuredSeries = featuredSeries,
                                    onPlaySeries = { viewModel.playStreamSeries(it) },
                                    onDetailClick = { viewModel.openSeriesDetail(it) }
                                )
                            }
                        }

                        // 2. Continue Watching (In-Progress Series Episodes) - Scrolls with the page
                        if (seriesProgress.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }, key = "series_progress_row") {
                                ContinueWatchingRow(
                                    items = seriesProgress,
                                    onResume = { viewModel.resumeFromProgress(it) },
                                    onRemove = { viewModel.clearPlaybackProgress(it.mediaId) }
                                )
                            }
                        }

                        // 3. Language & Sort Quick Chips - Scrolls with the page
                        item(span = { GridItemSpan(maxLineSpan) }, key = "series_filters_section") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Language chips
                                SeriesLanguageChip(
                                    label = "Tümü",
                                    selected = selectedLanguage == LanguageBadge.ALL,
                                    onClick = {
                                        viewModel.setSeriesLanguage(LanguageBadge.ALL)
                                        coroutineScope.launch { gridState.animateScrollToItem(0) }
                                    }
                                )
                                SeriesLanguageChip(
                                    label = "Dublaj",
                                    selected = selectedLanguage == LanguageBadge.DUBLAJ,
                                    badgeColor = SuccessGreen,
                                    onClick = {
                                        viewModel.setSeriesLanguage(LanguageBadge.DUBLAJ)
                                        coroutineScope.launch { gridState.animateScrollToItem(0) }
                                    }
                                )
                                SeriesLanguageChip(
                                    label = "Altyazı",
                                    selected = selectedLanguage == LanguageBadge.ALTYAZI,
                                    badgeColor = MaterialTheme.colorScheme.primary,
                                    onClick = {
                                        viewModel.setSeriesLanguage(LanguageBadge.ALTYAZI)
                                        coroutineScope.launch { gridState.animateScrollToItem(0) }
                                    }
                                )

                                // Sort Quick Chips
                                FilterChip(
                                    selected = sortOrder == SeriesSortOrder.EPISODES_ASC,
                                    onClick = {
                                        if (sortOrder == SeriesSortOrder.EPISODES_ASC) viewModel.setSeriesSortOrder(SeriesSortOrder.DEFAULT)
                                        else viewModel.setSeriesSortOrder(SeriesSortOrder.EPISODES_ASC)
                                        coroutineScope.launch { gridState.animateScrollToItem(0) }
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(14.dp))
                                    },
                                    label = { Text("Az Bölüm", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SuccessGreen.copy(alpha = 0.2f),
                                        selectedLabelColor = SuccessGreen
                                    )
                                )

                                FilterChip(
                                    selected = sortOrder == SeriesSortOrder.EPISODES_DESC,
                                    onClick = {
                                        if (sortOrder == SeriesSortOrder.EPISODES_DESC) viewModel.setSeriesSortOrder(SeriesSortOrder.DEFAULT)
                                        else viewModel.setSeriesSortOrder(SeriesSortOrder.EPISODES_DESC)
                                        coroutineScope.launch { gridState.animateScrollToItem(0) }
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(14.dp))
                                    },
                                    label = { Text("Çok Bölüm", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = WarningAmber.copy(alpha = 0.2f),
                                        selectedLabelColor = WarningAmber
                                    )
                                )
                            }
                        }

                        // 4. Series Items or Empty State
                        if (seriesList.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }, key = "series_empty_state") {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Kriterlere uygun dizi bulunamadı",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(seriesList, key = { it.id }) { series ->
                                val isAllQueued = series.episodes.isNotEmpty() && series.episodes.all {
                                    queuedEpisodeIds.contains(it.id) || queuedEpisodeUrls.contains(it.streamUrl)
                                }
                                val isFavorite = favoriteSeriesIds.contains(series.id)
                                SeriesPosterCard(
                                    series = series,
                                    isQueued = isAllQueued,
                                    isFavorite = isFavorite,
                                    onToggleFavorite = { viewModel.toggleSeriesFavorite(context, series) },
                                    onCardClick = { viewModel.openSeriesDetail(series) },
                                    onPlaySeries = { viewModel.openSeriesDetail(series) },
                                    onOpenDownloadSelection = { seriesForDownloadSelection = series }
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    // Series Episode Multi-selection Download Dialog (Clicking Download Button on Card)
    seriesForDownloadSelection?.let { series ->
        SeriesDownloadSelectionDialog(
            series = series,
            queuedEpisodeIds = queuedEpisodeIds,
            queuedEpisodeUrls = queuedEpisodeUrls,
            onDismiss = { seriesForDownloadSelection = null },
            onConfirmDownload = { selectedEpisodes ->
                viewModel.downloadSelectedEpisodes(context, series, selectedEpisodes)
                seriesForDownloadSelection = null
            }
        )
    }
}

@Composable
private fun SeriesLanguageChip(
    label: String,
    selected: Boolean,
    badgeColor: Color? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            badgeColor?.copy(alpha = 0.2f) ?: MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(
                1.5.dp,
                badgeColor ?: MaterialTheme.colorScheme.primary
            )
        } else {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (badgeColor != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) {
                    badgeColor ?: MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
fun SeriesPosterCard(
    series: CatalogSeries,
    isQueued: Boolean = false,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onCardClick: () -> Unit = {},
    onPlaySeries: () -> Unit = {},
    onOpenDownloadSelection: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusHighlight(
                shape = RoundedCornerShape(12.dp),
                scaleWhenFocused = 1.05f,
                onClick = onCardClick
            )
            .testTag("series_card_${series.id}")
    ) {
        Column {
            // Poster Box (2:3 aspect ratio)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!series.posterUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(series.posterUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = series.seriesName,
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
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Gradient overlay at bottom of poster
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )

                // Favorite Heart Button on Bottom-Left Corner of Poster
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(5.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.58f))
                        .clickable(onClick = onToggleFavorite)
                        .testTag("btn_fav_series_${series.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) "Favorilerden Çıkar" else "Favorilere Ekle",
                        tint = if (isFavorite) NetflixRedBright else Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Season & Episode count badge at bottom-right
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = "${series.totalSeasonsCount} Sezon ${series.totalEpisodesCount} Bölüm",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                // Language badges at top-right
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    series.languages.forEach { lang ->
                        if (lang != LanguageBadge.ALL) {
                            val badgeBg = when (lang) {
                                LanguageBadge.DUBLAJ -> SuccessGreen
                                LanguageBadge.ALTYAZI -> MaterialTheme.colorScheme.primary
                                else -> SecondaryCyan
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = badgeBg,
                                modifier = Modifier
                            ) {
                                Text(
                                    text = lang.label,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Series Name & Actions
            Column(
                modifier = Modifier.padding(6.dp)
            ) {
                Text(
                    text = series.seriesName,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        lineHeight = 14.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
fun SeriesDownloadSelectionDialog(
    series: CatalogSeries,
    queuedEpisodeIds: Set<String> = emptySet(),
    queuedEpisodeUrls: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onConfirmDownload: (List<CatalogEpisodeItem>) -> Unit
) {
    val unqueuedEpisodes = remember(series, queuedEpisodeIds, queuedEpisodeUrls) {
        series.episodes.filter { !queuedEpisodeIds.contains(it.id) && !queuedEpisodeUrls.contains(it.streamUrl) }
    }
    val unqueuedIds = remember(unqueuedEpisodes) {
        unqueuedEpisodes.map { it.id }
    }
    val selectedIds = remember {
        mutableStateListOf<String>().apply {
            addAll(unqueuedIds)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Header Row with Mini Poster and Close Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 46.dp, height = 64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (!series.posterUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = series.posterUrl,
                                    contentDescription = series.seriesName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bölüm Seçimi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = series.seriesName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Select all / deselect row
                    val unqueuedCount = unqueuedEpisodes.size
                    val queuedCount = series.episodes.size - unqueuedCount
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ) {
                            Text(
                                text = if (queuedCount > 0) "${selectedIds.size} / $unqueuedCount seçildi ($queuedCount kuyrukta)"
                                       else "${selectedIds.size} / ${series.episodes.size} bölüm seçildi",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    selectedIds.clear()
                                    selectedIds.addAll(unqueuedIds)
                                },
                                enabled = unqueuedIds.isNotEmpty(),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Tümü", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { selectedIds.clear() },
                                enabled = selectedIds.isNotEmpty(),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Temizle", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(series.episodes, key = { it.id }) { ep ->
                            val isAlreadyQueued = queuedEpisodeIds.contains(ep.id) || queuedEpisodeUrls.contains(ep.streamUrl)
                            val isChecked = selectedIds.contains(ep.id)

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    isAlreadyQueued -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                                    isChecked -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = when {
                                        isAlreadyQueued -> SuccessGreen.copy(alpha = 0.25f)
                                        isChecked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isAlreadyQueued) {
                                        if (isChecked) selectedIds.remove(ep.id)
                                        else selectedIds.add(ep.id)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isAlreadyQueued || isChecked,
                                        onCheckedChange = if (isAlreadyQueued) null else { checked ->
                                            if (checked) selectedIds.add(ep.id)
                                            else selectedIds.remove(ep.id)
                                        },
                                        enabled = !isAlreadyQueued,
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MaterialTheme.colorScheme.primary,
                                            disabledCheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                        ),
                                        modifier = Modifier.size(24.dp)
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ep.episodeNumberText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isChecked && !isAlreadyQueued) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isAlreadyQueued) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (ep.language != LanguageBadge.ALL) {
                                            Text(
                                                text = ep.language.label,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = if (isAlreadyQueued) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                                       else if (ep.language == LanguageBadge.DUBLAJ) SuccessGreen
                                                       else MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (isAlreadyQueued) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = SuccessGreen.copy(alpha = 0.15f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.35f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = SuccessGreen,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "Kuyrukta",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = SuccessGreen,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons
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
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = {
                                val selectedEps = series.episodes.filter {
                                    selectedIds.contains(it.id) &&
                                    !queuedEpisodeIds.contains(it.id) &&
                                    !queuedEpisodeUrls.contains(it.streamUrl)
                                }
                                onConfirmDownload(selectedEps)
                            },
                            enabled = selectedIds.isNotEmpty(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NetflixRed,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(44.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "İndir (${selectedIds.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

