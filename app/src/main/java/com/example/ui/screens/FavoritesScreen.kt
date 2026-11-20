package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CatalogMovieItem
import com.example.model.CatalogSeries
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.theme.NetflixRed
import com.example.ui.theme.NetflixRedBright
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val favoriteMovies by viewModel.favoriteMovies.collectAsState()
    val favoriteSeries by viewModel.favoriteSeries.collectAsState()
    val dbTasks by viewModel.dbTasks.collectAsState()

    val queuedMovieIds = remember(dbTasks) { dbTasks.map { it.id }.toSet() }
    val queuedMovieUrls = remember(dbTasks) { dbTasks.map { it.streamUrl }.toSet() }
    val queuedEpisodeIds = remember(dbTasks) { dbTasks.map { it.id }.toSet() }
    val queuedEpisodeUrls = remember(dbTasks) { dbTasks.map { it.streamUrl }.toSet() }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }

    val filteredMovies = remember(favoriteMovies, searchQuery) {
        if (searchQuery.isBlank()) favoriteMovies
        else {
            val q = searchQuery.trim().lowercase()
            favoriteMovies.filter {
                it.cleanTitle.lowercase().contains(q) || it.title.lowercase().contains(q)
            }
        }
    }

    val filteredSeries = remember(favoriteSeries, searchQuery) {
        if (searchQuery.isBlank()) favoriteSeries
        else {
            val q = searchQuery.trim().lowercase()
            favoriteSeries.filter {
                it.seriesName.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = NetflixRedBright,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Favorilerim",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Search Input
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = if (pagerState.currentPage == 0) "Film ara…" else "Dizi ara…",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Temizle",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("input_search_favorites")
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openSettings() },
                        modifier = Modifier.testTag("btn_open_settings_favorites")
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
            // Tab Row: Filmler vs Diziler
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = NetflixRedBright
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    text = {
                        Text(
                            text = "Filmler (${favoriteMovies.size})",
                            fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    },
                    selectedContentColor = NetflixRedBright,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("tab_fav_movies")
                )

                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    text = {
                        Text(
                            text = "Diziler (${favoriteSeries.size})",
                            fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    },
                    selectedContentColor = NetflixRedBright,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("tab_fav_series")
                )
            }

            // Tab Content with Horizontal Pager
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val screenWidth = maxWidth
                val columns = when {
                    screenWidth >= 1200.dp -> GridCells.Fixed(8)
                    screenWidth >= 900.dp -> GridCells.Fixed(6)
                    screenWidth >= 600.dp -> GridCells.Fixed(4)
                    screenWidth >= 400.dp -> GridCells.Fixed(3)
                    else -> GridCells.Fixed(3)
                }
                val horizontalPadding = if (screenWidth >= 840.dp) 20.dp else 8.dp

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    if (page == 0) {
                        // Movies Tab
                        if (filteredMovies.isEmpty()) {
                            FavoritesEmptyState(
                                isMovies = true,
                                hasSearch = searchQuery.isNotBlank(),
                                onExplore = { viewModel.selectTab(MainTab.MOVIES) }
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = columns,
                                contentPadding = PaddingValues(
                                    start = horizontalPadding,
                                    end = horizontalPadding,
                                    top = 6.dp,
                                    bottom = 100.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredMovies, key = { it.id }) { movie ->
                                    val isQueued = queuedMovieIds.contains(movie.id) || queuedMovieUrls.contains(movie.streamUrl)
                                    MoviePosterCard(
                                        movie = movie,
                                        isQueued = isQueued,
                                        isFavorite = true,
                                        onCardClick = { viewModel.openMovieDetail(movie) },
                                        onToggleFavorite = { viewModel.toggleMovieFavorite(context, movie) }
                                    )
                                }
                            }
                        }
                    } else {
                        // Series Tab
                        if (filteredSeries.isEmpty()) {
                            FavoritesEmptyState(
                                isMovies = false,
                                hasSearch = searchQuery.isNotBlank(),
                                onExplore = { viewModel.selectTab(MainTab.SERIES) }
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = columns,
                                contentPadding = PaddingValues(
                                    start = horizontalPadding,
                                    end = horizontalPadding,
                                    top = 6.dp,
                                    bottom = 100.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredSeries, key = { it.id }) { series ->
                                    val isAllQueued = series.episodes.isNotEmpty() && series.episodes.all {
                                        queuedEpisodeIds.contains(it.id) || queuedEpisodeUrls.contains(it.streamUrl)
                                    }
                                    SeriesPosterCard(
                                        series = series,
                                        isQueued = isAllQueued,
                                        isFavorite = true,
                                        onCardClick = { viewModel.openSeriesDetail(series) },
                                        onToggleFavorite = { viewModel.toggleSeriesFavorite(context, series) }
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

@Composable
private fun FavoritesEmptyState(
    isMovies: Boolean,
    hasSearch: Boolean,
    onExplore: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.widthIn(max = 420.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(NetflixRed.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (hasSearch) Icons.Default.Search else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = NetflixRedBright,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (hasSearch) "Aramanıza uygun favori bulunamadı"
                else if (isMovies) "Favori Film Bulunmuyor"
                else "Favori Dizi Bulunmuyor",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (hasSearch) "Farklı bir arama terimi deneyebilir veya arama kutusunu temizleyebilirsiniz."
                else if (isMovies) "Filmler sayfasında afişlerin sol alt köşesinde bulunan kalp simgesine dokunarak filmleri favorilerinize ekleyebilirsiniz."
                else "Diziler sayfasında afişlerin sol alt köşesinde bulunan kalp simgesine dokunarak dizileri favorilerinize ekleyebilirsiniz.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            if (!hasSearch) {
                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onExplore,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NetflixRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(
                        imageVector = if (isMovies) Icons.Default.Movie else Icons.Default.Tv,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isMovies) "Filmleri Keşfet" else "Dizileri Keşfet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
