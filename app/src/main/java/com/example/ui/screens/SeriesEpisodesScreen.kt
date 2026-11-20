package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.NetflixRed
import com.example.ui.theme.NetflixRedBright
import com.example.ui.theme.SuccessGreen

enum class EpisodeTabCategory(val title: String, val icon: String) {
    DUBLAJ("Dublaj", "🎙"),
    ALTYAZI("Altyazı", "💬"),
    OTHER("Tüm Bölümler", "📁")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesEpisodesScreen(
    series: CatalogSeries,
    queuedEpisodeIds: Set<String> = emptySet(),
    queuedEpisodeUrls: Set<String> = emptySet(),
    onBack: () -> Unit,
    onPlayEpisodeStream: (episode: CatalogEpisodeItem, group: List<CatalogEpisodeItem>) -> Unit,
    onDownloadEpisode: (CatalogEpisodeItem) -> Unit,
    onDownloadAllGroup: (List<CatalogEpisodeItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Handle Android system back gesture/button to return smoothly to Series Catalog
    BackHandler(onBack = onBack)

    // Group episodes: those ending with / containing "dublaj" under Dublaj, and "altyazı" under Altyazı
    val dublajEpisodes = remember(series) {
        series.episodes.filter { isDublajEpisode(it) }
    }
    val altyaziEpisodes = remember(series) {
        series.episodes.filter { isAltyaziEpisode(it) }
    }
    val otherEpisodes = remember(series, dublajEpisodes, altyaziEpisodes) {
        series.episodes.filter { !dublajEpisodes.contains(it) && !altyaziEpisodes.contains(it) }
    }

    // Determine available tabs
    val availableTabs = remember(dublajEpisodes, altyaziEpisodes, otherEpisodes) {
        buildList {
            if (dublajEpisodes.isNotEmpty()) add(EpisodeTabCategory.DUBLAJ)
            if (altyaziEpisodes.isNotEmpty()) add(EpisodeTabCategory.ALTYAZI)
            if (otherEpisodes.isNotEmpty() || (dublajEpisodes.isEmpty() && altyaziEpisodes.isEmpty())) {
                add(EpisodeTabCategory.OTHER)
            }
        }
    }

    var selectedTabIndex by remember(series.id) {
        mutableIntStateOf(0)
    }

    // Ensure index remains valid
    val safeTabIndex = selectedTabIndex.coerceIn(0, (availableTabs.size - 1).coerceAtLeast(0))
    val currentTab = availableTabs.getOrElse(safeTabIndex) { EpisodeTabCategory.OTHER }

    val currentEpisodes = remember(currentTab, dublajEpisodes, altyaziEpisodes, otherEpisodes) {
        when (currentTab) {
            EpisodeTabCategory.DUBLAJ -> dublajEpisodes
            EpisodeTabCategory.ALTYAZI -> altyaziEpisodes
            EpisodeTabCategory.OTHER -> if (otherEpisodes.isNotEmpty()) otherEpisodes else series.episodes
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = series.seriesName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${series.totalEpisodesCount} Bölüm • Bölüm Seçin ve İzleyin",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_back_to_series_catalog")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 860.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Hero Header Card
                item(key = "series_hero_header") {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Poster Thumbnail
                                Box(
                                    modifier = Modifier
                                        .size(width = 72.dp, height = 104.dp)
                                        .clip(RoundedCornerShape(12.dp))
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
                                        Icon(
                                            imageVector = Icons.Default.Tv,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            modifier = Modifier
                                                .size(36.dp)
                                                .align(Alignment.Center)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = series.seriesName,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Category / Language Badges
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (dublajEpisodes.isNotEmpty()) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = SuccessGreen.copy(alpha = 0.16f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f))
                                            ) {
                                                Text(
                                                    text = "${dublajEpisodes.size} Dublaj",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                                    color = SuccessGreen,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        if (altyaziEpisodes.isNotEmpty()) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                            ) {
                                                Text(
                                                    text = "${altyaziEpisodes.size} Altyazı",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Action buttons row: 1. Bölümü İzle & Tümünü İndir
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        currentEpisodes.firstOrNull()?.let { firstEp ->
                                            onPlayEpisodeStream(firstEp, currentEpisodes)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NetflixRed,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("btn_play_ep1")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "1. Bölümden İzle",
                                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }

                                OutlinedButton(
                                    onClick = { onDownloadAllGroup(currentEpisodes) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("btn_download_all_group")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${currentTab.title} Tümünü İndir",
                                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Dublaj vs Altyazı Tabs Row
                if (availableTabs.size > 1) {
                    item(key = "language_tabs_header") {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SecondaryTabRow(
                                selectedTabIndex = safeTabIndex,
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                indicator = {
                                    TabRowDefaults.SecondaryIndicator(
                                        color = if (currentTab == EpisodeTabCategory.DUBLAJ) SuccessGreen else MaterialTheme.colorScheme.primary,
                                        height = 3.dp,
                                        modifier = Modifier.tabIndicatorOffset(safeTabIndex)
                                    )
                                }
                            ) {
                                availableTabs.forEachIndexed { index, tab ->
                                    val count = when (tab) {
                                        EpisodeTabCategory.DUBLAJ -> dublajEpisodes.size
                                        EpisodeTabCategory.ALTYAZI -> altyaziEpisodes.size
                                        EpisodeTabCategory.OTHER -> otherEpisodes.size
                                    }
                                    val isSelected = safeTabIndex == index

                                    Tab(
                                        selected = isSelected,
                                        onClick = { selectedTabIndex = index },
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = tab.icon,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "${tab.title} ($count)",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontSize = if (isSelected) 12.5.sp else 11.5.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = when {
                                                        isSelected && tab == EpisodeTabCategory.DUBLAJ -> SuccessGreen
                                                        isSelected -> MaterialTheme.colorScheme.primary
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("tab_${tab.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }
                }

                // Section Title
                item(key = "section_episodes_title") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${currentTab.title} Bölümleri (${currentEpisodes.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "İzlemek için bölüme dokunun",
                            style = MaterialTheme.typography.labelSmall,
                            color = NetflixRedBright,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Episode Cards
                items(currentEpisodes, key = { it.id }) { ep ->
                    val isEpQueued = queuedEpisodeIds.contains(ep.id) || queuedEpisodeUrls.contains(ep.streamUrl)
                    val cleanTitle = cleanEpisodeDisplayTitle(ep.title, ep.episodeNumberText)
                    val isDub = isDublajEpisode(ep)

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDub) SuccessGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onPlayEpisodeStream(ep, currentEpisodes) }
                            .testTag("episode_item_${ep.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left: Play Icon Circle
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NetflixRed.copy(alpha = 0.15f))
                                    .border(1.dp, NetflixRed.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = NetflixRedBright,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Middle: Episode Title & Language Tag
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cleanTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isDub) SuccessGreen.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = if (isDub) "Dublaj" else "Altyazı",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = if (isDub) SuccessGreen else MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    if (isEpQueued) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = SuccessGreen.copy(alpha = 0.14f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = SuccessGreen,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Text(
                                                    text = "Kuyrukta",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                                    color = SuccessGreen,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Right: Action Buttons (Only İzle and İndir - NO M3U buttons!)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Watch Button
                                Button(
                                    onClick = { onPlayEpisodeStream(ep, currentEpisodes) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NetflixRed,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("btn_play_ep_${ep.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "İzle",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "İzle",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }

                                // Download Button
                                Button(
                                    onClick = { onDownloadEpisode(ep) },
                                    enabled = !isEpQueued,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = Color.White,
                                        disabledContainerColor = SuccessGreen.copy(alpha = 0.15f),
                                        disabledContentColor = SuccessGreen
                                    ),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("btn_download_ep_${ep.id}")
                                ) {
                                    Icon(
                                        imageVector = if (isEpQueued) Icons.Default.Check else Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isEpQueued) "İndirildi" else "İndir",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
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
    }
}

private fun isDublajEpisode(ep: CatalogEpisodeItem): Boolean {
    val fullText = "${ep.title} ${ep.episodeNumberText}".lowercase()
    val isDub = fullText.contains("dublaj") || ep.language == LanguageBadge.DUBLAJ
    val isSub = fullText.contains("altyazı") || fullText.contains("altyazi") || fullText.contains("alt yazı") || ep.language == LanguageBadge.ALTYAZI

    return when {
        isDub && !isSub -> true
        !isDub && isSub -> false
        isDub && isSub -> fullText.trim().endsWith("dublaj") || fullText.trim().endsWith("dublaj)")
        else -> false
    }
}

private fun isAltyaziEpisode(ep: CatalogEpisodeItem): Boolean {
    val fullText = "${ep.title} ${ep.episodeNumberText}".lowercase()
    val isDub = fullText.contains("dublaj") || ep.language == LanguageBadge.DUBLAJ
    val isSub = fullText.contains("altyazı") || fullText.contains("altyazi") || fullText.contains("alt yazı") || ep.language == LanguageBadge.ALTYAZI

    return when {
        isSub && !isDub -> true
        !isSub && isDub -> false
        isDub && isSub -> fullText.trim().endsWith("altyazı") || fullText.trim().endsWith("altyazi") || fullText.trim().endsWith("altyazı)") || fullText.trim().endsWith("altyazi)")
        else -> false
    }
}

private fun cleanEpisodeDisplayTitle(title: String, epText: String): String {
    var clean = if (epText.isNotBlank()) epText else title
    clean = clean
        .replace(Regex("(?i)[\\(\\[\\-]?\\s*dublaj\\s*[\\)\\]]?"), "")
        .replace(Regex("(?i)[\\(\\[\\-]?\\s*altyaz[ıi]\\s*[\\)\\]]?"), "")
        .replace(Regex("(?i)[\\(\\[\\-]?\\s*alt\\s*yaz[ıi]\\s*[\\)\\]]?"), "")
        .trim()
    return clean.ifEmpty { epText.ifEmpty { title } }
}
