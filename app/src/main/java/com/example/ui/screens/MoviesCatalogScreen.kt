package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CatalogMovieItem
import com.example.model.LanguageBadge
import com.example.ui.MainViewModel
import com.example.ui.components.ContinueWatchingRow
import com.example.ui.components.MovieHeaderCarousel
import com.example.ui.theme.NetflixRed
import com.example.ui.theme.NetflixRedBright
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.util.tvFocusHighlight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesCatalogScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState()
) {
    val context = LocalContext.current
    val movies by viewModel.pagedMovies.collectAsState()
    val allMovies by viewModel.moviesList.collectAsState()
    val isLoading by viewModel.isLoadingMovies.collectAsState()
    val error by viewModel.moviesError.collectAsState()
    val selectedCategory by viewModel.selectedMovieCategory.collectAsState()
    val selectedLanguage by viewModel.selectedMovieLanguage.collectAsState()
    val categories by viewModel.movieCategories.collectAsState()
    val favoriteMovieIds by viewModel.favoriteMovieIds.collectAsState()
    val dbTasks by viewModel.dbTasks.collectAsState()
    val queuedMovieIds = remember(dbTasks) { dbTasks.map { it.id }.toSet() }
    val queuedMovieUrls = remember(dbTasks) { dbTasks.map { it.streamUrl }.toSet() }

    val featuredMovies by viewModel.featuredMovies.collectAsState()
    val movieProgress by viewModel.moviePlaybackProgress.collectAsState()
    val allProgress by viewModel.allPlaybackProgress.collectAsState()
    val progressMap = remember(allProgress) { allProgress.associateBy { it.mediaId } }

    var isCategoryDropdownOpen by remember { mutableStateOf(false) }
    var movieToConfirmDownload by remember { mutableStateOf<CatalogMovieItem?>(null) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    // Auto-scroll to top when category or language filter changes
    androidx.compose.runtime.LaunchedEffect(selectedCategory, selectedLanguage) {
        gridState.animateScrollToItem(0)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Filmler",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openMovieSearch() },
                        modifier = Modifier
                            .tvFocusHighlight(
                                shape = CircleShape,
                                focusedBorderColor = NetflixRedBright,
                                scaleWhenFocused = 1.15f,
                                onClick = { viewModel.openMovieSearch() }
                            )
                            .testTag("btn_open_search_movies")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Film Ara",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { viewModel.openSettings() },
                        modifier = Modifier
                            .tvFocusHighlight(
                                shape = CircleShape,
                                focusedBorderColor = NetflixRedBright,
                                scaleWhenFocused = 1.15f,
                                onClick = { viewModel.openSettings() }
                            )
                            .testTag("btn_open_settings_movies")
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
                isLoading && allMovies.isEmpty() -> {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
                error != null && allMovies.isEmpty() -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "Filmler yüklenemedi",
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
                        Button(onClick = { viewModel.loadMovies(forceRefresh = true) }) {
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
                        // 1. Featured Header Carousel (5 Random Recommended Movies) - Scrolls with the page
                        if (featuredMovies.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }, key = "featured_movies_carousel") {
                                MovieHeaderCarousel(
                                    featuredMovies = featuredMovies,
                                    onPlayMovie = { viewModel.playStreamMovie(it) },
                                    onDetailClick = { viewModel.openMovieDetail(it) }
                                )
                            }
                        }

                        // 2. Continue Watching (In-Progress Movies) - Scrolls with the page
                        if (movieProgress.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }, key = "movie_progress_row") {
                                ContinueWatchingRow(
                                    items = movieProgress,
                                    onResume = { viewModel.resumeFromProgress(it) },
                                    onRemove = { viewModel.clearPlaybackProgress(it.mediaId) }
                                )
                            }
                        }

                        // 3. Language Filter Chips & Categories Dropdown - Scrolls with the page
                        item(span = { GridItemSpan(maxLineSpan) }, key = "movie_filters_section") {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Language Filter Chips
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LanguageChip(
                                        label = "Tümü",
                                        selected = selectedLanguage == LanguageBadge.ALL && selectedCategory == null,
                                        onClick = {
                                            viewModel.resetMovieFiltersToAll()
                                            coroutineScope.launch { gridState.animateScrollToItem(0) }
                                        }
                                    )
                                    LanguageChip(
                                        label = "Dublaj",
                                        selected = selectedLanguage == LanguageBadge.DUBLAJ,
                                        badgeColor = SuccessGreen,
                                        onClick = {
                                            viewModel.setMovieLanguage(LanguageBadge.DUBLAJ)
                                            coroutineScope.launch { gridState.animateScrollToItem(0) }
                                        }
                                    )
                                    LanguageChip(
                                        label = "Altyazı",
                                        selected = selectedLanguage == LanguageBadge.ALTYAZI,
                                        badgeColor = MaterialTheme.colorScheme.primary,
                                        onClick = {
                                            viewModel.setMovieLanguage(LanguageBadge.ALTYAZI)
                                            coroutineScope.launch { gridState.animateScrollToItem(0) }
                                        }
                                    )
                                }

                                // Categories: Dropdown List Selector
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selectedCategory != null) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (selectedCategory != null) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .tvFocusHighlight(
                                                shape = RoundedCornerShape(12.dp),
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                scaleWhenFocused = 1.03f,
                                                onClick = { isCategoryDropdownOpen = true }
                                            )
                                            .testTag("dropdown_categories_trigger")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Category,
                                                    contentDescription = null,
                                                    tint = if (selectedCategory != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = "Kategori:",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = selectedCategory ?: "Tüm Kategoriler",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (selectedCategory != null) FontWeight.Bold else FontWeight.SemiBold,
                                                        color = if (selectedCategory != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Açılır Liste",
                                                tint = if (selectedCategory != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    // Dropdown Menu for Categories
                                    DropdownMenu(
                                        expanded = isCategoryDropdownOpen,
                                        onDismissRequest = { isCategoryDropdownOpen = false },
                                        modifier = Modifier
                                            .fillMaxWidth(0.9f)
                                            .heightIn(max = 380.dp)
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "Tüm Kategoriler (Hepsi)",
                                                    fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (selectedCategory == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            leadingIcon = {
                                                if (selectedCategory == null) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            },
                                            onClick = {
                                                viewModel.setMovieCategory(null)
                                                isCategoryDropdownOpen = false
                                                coroutineScope.launch { gridState.animateScrollToItem(0) }
                                            }
                                        )

                                        if (categories.isNotEmpty()) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                            categories.forEach { category ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = category,
                                                            fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (selectedCategory == category) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    },
                                                    leadingIcon = {
                                                        if (selectedCategory == category) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    },
                                                    onClick = {
                                                        viewModel.setMovieCategory(category)
                                                        isCategoryDropdownOpen = false
                                                        coroutineScope.launch { gridState.animateScrollToItem(0) }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Movie Cards or Empty State
                        if (movies.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }, key = "movies_empty_state") {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Kriterlere uygun film bulunamadı",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(movies, key = { it.id }) { movie ->
                                val isQueued = queuedMovieIds.contains(movie.id) || queuedMovieUrls.contains(movie.streamUrl)
                                val isFavorite = favoriteMovieIds.contains(movie.id)
                                val itemProgress = progressMap[movie.id]?.progressPercent ?: 0f
                                MoviePosterCard(
                                    movie = movie,
                                    isQueued = isQueued,
                                    isFavorite = isFavorite,
                                    progressPercent = itemProgress,
                                    onCardClick = { viewModel.openMovieDetail(movie) },
                                    onToggleFavorite = { viewModel.toggleMovieFavorite(context, movie) },
                                    onPlayStream = { viewModel.playStreamMovie(movie) },
                                    onDirectDownload = { movieToConfirmDownload = movie }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modernized Movie Download Confirmation Dialog
    movieToConfirmDownload?.let { movie ->
        Dialog(
            onDismissRequest = { movieToConfirmDownload = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .widthIn(max = 440.dp)
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
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header Icon
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(NetflixRed.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = NetflixRedBright,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Kuyruğa Ekle",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "İçerik indirme kuyruğuna alınacak",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Movie Preview Card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 54.dp, height = 78.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    if (!movie.posterUrl.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = movie.posterUrl,
                                            contentDescription = movie.cleanTitle,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Movie,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = movie.cleanTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                       overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (movie.language != LanguageBadge.ALL) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = (if (movie.language == LanguageBadge.DUBLAJ) SuccessGreen else MaterialTheme.colorScheme.primary).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = movie.language.label,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = if (movie.language == LanguageBadge.DUBLAJ) SuccessGreen else MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        if (movie.categories.isNotEmpty()) {
                                            Text(
                                                text = movie.categories.first(),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Info banner
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "İndirmeler sekmesinden dilediğiniz zaman sırayla başlatabilir ve duraklatabilirsiniz.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { movieToConfirmDownload = null },
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
                                onClick = {
                                    viewModel.directDownloadMovie(context, movie)
                                    movieToConfirmDownload = null
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NetflixRed,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "İndir",
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
    }
}

@Composable
private fun LanguageChip(
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
        modifier = Modifier.tvFocusHighlight(
            shape = RoundedCornerShape(16.dp),
            focusedBorderColor = badgeColor ?: MaterialTheme.colorScheme.primary,
            scaleWhenFocused = 1.08f,
            onClick = onClick
        )
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
fun MoviePosterCard(
    movie: CatalogMovieItem,
    isQueued: Boolean = false,
    isFavorite: Boolean = false,
    progressPercent: Float = 0f,
    onCardClick: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onPlayStream: () -> Unit = {},
    onDirectDownload: () -> Unit = {}
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
            .testTag("movie_card_${movie.id}")
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
                        contentDescription = movie.cleanTitle,
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
                        .testTag("btn_fav_movie_${movie.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) "Favorilerden Çıkar" else "Favorilere Ekle",
                        tint = if (isFavorite) NetflixRedBright else Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Language Badge on poster
                if (movie.language != LanguageBadge.ALL) {
                    val badgeBg = when (movie.language) {
                        LanguageBadge.DUBLAJ -> SuccessGreen
                        LanguageBadge.ALTYAZI -> MaterialTheme.colorScheme.primary
                        LanguageBadge.BOTH -> SecondaryCyan
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeBg,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = movie.language.label,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                // In-progress indicator bar
                if (progressPercent in 0.02f..0.98f) {
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { progressPercent },
                        color = NetflixRedBright,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
            }

            // Info & Action Section
            Column(
                modifier = Modifier.padding(6.dp)
            ) {
                Text(
                    text = movie.cleanTitle,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        lineHeight = 14.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = if (movie.categories.isNotEmpty()) movie.categories.first() else " ",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp, bottom = 2.dp)
                )
            }
        }
    }
}
