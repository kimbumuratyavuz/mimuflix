package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.model.TmdbUiState
import com.example.ui.components.TmdbMetadataSection
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.CatalogEpisodeItem
import com.example.model.CatalogSeries
import com.example.model.LanguageBadge
import com.example.ui.MainViewModel
import com.example.ui.theme.NetflixRed
import com.example.ui.theme.NetflixRedBright
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.util.tvFocusHighlight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesDetailScreen(
    series: CatalogSeries,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler { onBack() }

    val favoriteSeriesIds by viewModel.favoriteSeriesIds.collectAsState()
    val isFavorite = favoriteSeriesIds.contains(series.id)

    val dbTasks by viewModel.dbTasks.collectAsState()
    val queuedEpisodeIds = remember(dbTasks) { dbTasks.map { it.id }.toSet() }
    val queuedEpisodeUrls = remember(dbTasks) { dbTasks.map { it.streamUrl }.toSet() }

    val allProgress by viewModel.allPlaybackProgress.collectAsState()

    // Available seasons (e.g. 1, 2, 3...)
    val seasons = remember(series.episodes) {
        series.seasons.ifEmpty { listOf(1) }
    }
    var selectedSeason by remember { mutableIntStateOf(seasons.first()) }

    // Languages available in the series (Dublaj / Altyazı)
    val availableLanguages = remember(series.episodes) {
        val list = mutableListOf<LanguageBadge>()
        val hasDub = series.episodes.any { it.language == LanguageBadge.DUBLAJ || it.language == LanguageBadge.BOTH }
        val hasAlt = series.episodes.any { it.language == LanguageBadge.ALTYAZI || it.language == LanguageBadge.BOTH }
        if (hasDub) list.add(LanguageBadge.DUBLAJ)
        if (hasAlt) list.add(LanguageBadge.ALTYAZI)
        list
    }

    // Default to first available language: Dublaj if present, otherwise Altyazı
    val initialLanguage = remember(availableLanguages) {
        availableLanguages.firstOrNull()
    }
    var selectedLanguage by remember { mutableStateOf<LanguageBadge?>(initialLanguage) }

    var isSeasonDropdownOpen by remember { mutableStateOf(false) }
    var isLanguageDropdownOpen by remember { mutableStateOf(false) }

    // Ensure selectedLanguage stays valid for the chosen season
    LaunchedEffect(selectedSeason) {
        val seasonEps = series.episodes.filter { it.seasonNumber == selectedSeason || seasons.size == 1 }
        val currentHasEps = seasonEps.any { selectedLanguage == null || it.language == selectedLanguage || it.language == LanguageBadge.BOTH }
        if (!currentHasEps && availableLanguages.isNotEmpty()) {
            val hasDub = seasonEps.any { it.language == LanguageBadge.DUBLAJ || it.language == LanguageBadge.BOTH }
            val hasAlt = seasonEps.any { it.language == LanguageBadge.ALTYAZI || it.language == LanguageBadge.BOTH }
            when {
                hasDub && availableLanguages.contains(LanguageBadge.DUBLAJ) -> selectedLanguage = LanguageBadge.DUBLAJ
                hasAlt && availableLanguages.contains(LanguageBadge.ALTYAZI) -> selectedLanguage = LanguageBadge.ALTYAZI
                else -> selectedLanguage = availableLanguages.firstOrNull()
            }
        }
    }

    // Filtered episodes for the active season and language
    val displayedEpisodes = remember(series.episodes, selectedSeason, selectedLanguage) {
        series.episodes.filter { ep ->
            (ep.seasonNumber == selectedSeason || seasons.size == 1) &&
            (selectedLanguage == null || ep.language == selectedLanguage || ep.language == LanguageBadge.BOTH)
        }
    }

    val tmdbState by viewModel.currentSeriesTmdbState.collectAsState()

    LaunchedEffect(series.id) {
        viewModel.loadTmdbForSeries(series.seriesName)
    }

    val tmdbDetail = (tmdbState as? TmdbUiState.Success)?.detail
    val displayBackdropUrl = tmdbDetail?.backdropUrl ?: series.posterUrl
    val displayPosterUrl = tmdbDetail?.posterUrl ?: series.posterUrl

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = series.seriesName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_back_series_detail")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleSeriesFavorite(context, series) },
                        modifier = Modifier.testTag("btn_fav_series_detail")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favori",
                            tint = if (isFavorite) NetflixRedBright else MaterialTheme.colorScheme.onBackground
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Hero Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (!displayBackdropUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(displayBackdropUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = series.seriesName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Vignette gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.35f),
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )

                    // Poster + Series Title Info
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            modifier = Modifier
                                .width(94.dp)
                                .aspectRatio(2f / 3f)
                        ) {
                            if (!displayPosterUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(displayPosterUrl)
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
                                    Icon(Icons.Default.Tv, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.padding(bottom = 6.dp)) {
                            Text(
                                text = series.seriesName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${series.totalSeasonsCount} Sezon • ${series.totalEpisodesCount} Bölüm",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Batch download & M3U copy actions
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.playStreamSeries(series) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NetflixRed,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .tvFocusHighlight(
                                            shape = RoundedCornerShape(8.dp),
                                            focusedBorderColor = Color.White,
                                            scaleWhenFocused = 1.05f,
                                            onClick = { viewModel.playStreamSeries(series) }
                                        )
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "İlk Bölümü İzle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.directDownloadSeries(context, series) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .tvFocusHighlight(
                                            shape = RoundedCornerShape(8.dp),
                                            focusedBorderColor = NetflixRedBright,
                                            scaleWhenFocused = 1.05f,
                                            onClick = { viewModel.directDownloadSeries(context, series) }
                                        )
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Tümünü İndir", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // TMDB Series Details: Puan, Yıl, Yaratıcı/Yönetmen, Dizi Konusu, Oyuncular
            item {
                Spacer(modifier = Modifier.height(10.dp))
                TmdbMetadataSection(
                    state = tmdbState,
                    isTvSeries = true
                )
            }

            // Side-by-Side Dropdowns: Sezonlar & Dil Filtresi
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Sezon Açılır Liste (Seasons Dropdown)
                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .tvFocusHighlight(
                                    shape = RoundedCornerShape(12.dp),
                                    focusedBorderColor = NetflixRedBright,
                                    scaleWhenFocused = 1.04f,
                                    onClick = { isSeasonDropdownOpen = true }
                                )
                                .testTag("dropdown_season_trigger")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = null,
                                        tint = NetflixRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Sezon $selectedSeason",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Sezon Seç",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isSeasonDropdownOpen,
                            onDismissRequest = { isSeasonDropdownOpen = false },
                            modifier = Modifier.fillMaxWidth(0.44f)
                        ) {
                            for (season in seasons) {
                                val isSelected = season == selectedSeason
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Sezon $season",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) NetflixRed else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    leadingIcon = {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = NetflixRed
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedSeason = season
                                        isSeasonDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }

                    // 2. Dublaj / Altyazı Açılır Liste (Language Filter Dropdown)
                    // "İkisinden biri yoksa filtrede görünmesin"
                    if (availableLanguages.isNotEmpty()) {
                        Box(modifier = Modifier.weight(1f)) {
                            val activeLang = selectedLanguage
                            val activeColor = when (activeLang) {
                                LanguageBadge.DUBLAJ -> SuccessGreen
                                LanguageBadge.ALTYAZI -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.primary
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (activeLang != null) activeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (activeLang != null) activeColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tvFocusHighlight(
                                        shape = RoundedCornerShape(12.dp),
                                        focusedBorderColor = activeColor,
                                        scaleWhenFocused = 1.04f,
                                        onClick = { isLanguageDropdownOpen = true }
                                    )
                                    .testTag("dropdown_language_trigger")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (activeLang == LanguageBadge.DUBLAJ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = null,
                                                tint = SuccessGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else if (activeLang == LanguageBadge.ALTYAZI) {
                                            Icon(
                                                imageVector = Icons.Default.ClosedCaption,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = when (activeLang) {
                                                LanguageBadge.DUBLAJ -> "Dublaj"
                                                LanguageBadge.ALTYAZI -> "Altyazı"
                                                else -> "Dil Seçin"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (activeLang != null) activeColor else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dil Seç",
                                        tint = if (activeLang != null) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isLanguageDropdownOpen,
                                onDismissRequest = { isLanguageDropdownOpen = false },
                                modifier = Modifier.fillMaxWidth(0.44f)
                            ) {
                                // "İkisinden biri yoksa filtrede görünmesin"
                                if (availableLanguages.contains(LanguageBadge.DUBLAJ)) {
                                    val isDubSelected = selectedLanguage == LanguageBadge.DUBLAJ
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Dublaj",
                                                fontWeight = if (isDubSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isDubSelected) SuccessGreen else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = null,
                                                tint = if (isDubSelected) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            if (isDubSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = SuccessGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedLanguage = LanguageBadge.DUBLAJ
                                            isLanguageDropdownOpen = false
                                        }
                                    )
                                }

                                if (availableLanguages.contains(LanguageBadge.ALTYAZI)) {
                                    val isAltSelected = selectedLanguage == LanguageBadge.ALTYAZI
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Altyazı",
                                                fontWeight = if (isAltSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isAltSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.ClosedCaption,
                                                contentDescription = null,
                                                tint = if (isAltSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            if (isAltSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedLanguage = LanguageBadge.ALTYAZI
                                            isLanguageDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )
            }

            // Episode List Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bölümler (${displayedEpisodes.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Episode List Items
            if (displayedEpisodes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Bu sezonda seçilen filtreye uygun bölüm bulunamadı.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(displayedEpisodes, key = { it.id }) { ep ->
                    val isQueued = queuedEpisodeIds.contains(ep.id) || queuedEpisodeUrls.contains(ep.streamUrl)
                    val epProgress = allProgress.find { it.mediaId == ep.id }

                    EpisodeRowItem(
                        series = series,
                        episode = ep,
                        isQueued = isQueued,
                        progress = epProgress,
                        onPlay = {
                            viewModel.playStreamEpisode(series, ep, displayedEpisodes, resume = true)
                        },
                        onDownload = {
                            viewModel.directDownloadEpisode(context, ep, series.seriesName)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EpisodeRowItem(
    series: CatalogSeries,
    episode: CatalogEpisodeItem,
    isQueued: Boolean,
    progress: com.example.model.PlaybackProgressEntity?,
    onPlay: () -> Unit,
    onDownload: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .tvFocusHighlight(
                shape = RoundedCornerShape(12.dp),
                scaleWhenFocused = 1.03f,
                onClick = onPlay
            )
            .testTag("episode_item_${episode.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play Icon Box
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(NetflixRed.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "İzle",
                        tint = NetflixRedBright,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Episode Title & Badges
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = episode.episodeNumberText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (episode.language != LanguageBadge.ALL) {
                            Text(
                                text = episode.language.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (episode.language == LanguageBadge.DUBLAJ) SuccessGreen else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (progress != null && progress.progressPercent in 0.02f..0.96f) {
                            Text(
                                text = "• ${progress.formattedRemainingTime}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = NetflixRedBright
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Download Button
                IconButton(
                    onClick = onDownload,
                    enabled = !isQueued,
                    modifier = Modifier.testTag("btn_dl_ep_${episode.id}")
                ) {
                    Icon(
                        imageVector = if (isQueued) Icons.Default.Check else Icons.Default.Download,
                        contentDescription = if (isQueued) "Kuyrukta" else "İndir",
                        tint = if (isQueued) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // In-Progress Bar if episode partially watched
            if (progress != null && progress.progressPercent in 0.02f..0.96f) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.progressPercent },
                    color = NetflixRedBright,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                )
            }
        }
    }
}
