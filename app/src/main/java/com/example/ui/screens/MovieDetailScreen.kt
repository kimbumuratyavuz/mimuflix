package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.CatalogMovieItem
import com.example.model.LanguageBadge
import com.example.ui.MainViewModel
import com.example.ui.theme.NetflixRed
import com.example.ui.theme.NetflixRedBright
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.util.tvFocusHighlight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    movie: CatalogMovieItem,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler { onBack() }

    val favoriteMovieIds by viewModel.favoriteMovieIds.collectAsState()
    val isFavorite = favoriteMovieIds.contains(movie.id)

    val dbTasks by viewModel.dbTasks.collectAsState()
    val isQueued = remember(dbTasks) {
        dbTasks.any { it.id == movie.id || it.streamUrl == movie.streamUrl }
    }

    val allProgress by viewModel.allPlaybackProgress.collectAsState()
    val savedProgress = remember(allProgress, movie.id) {
        allProgress.find { it.mediaId == movie.id }
    }

    val tmdbState by viewModel.currentMovieTmdbState.collectAsState()

    LaunchedEffect(movie.id) {
        viewModel.loadTmdbForMovie(movie.title)
    }

    val tmdbDetail = (tmdbState as? TmdbUiState.Success)?.detail
    val displayBackdropUrl = tmdbDetail?.backdropUrl ?: movie.posterUrl
    val displayPosterUrl = tmdbDetail?.posterUrl ?: movie.posterUrl

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = movie.cleanTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_back_movie_detail")
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
                        onClick = { viewModel.toggleMovieFavorite(context, movie) },
                        modifier = Modifier.testTag("btn_fav_movie_detail")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero Backdrop Image with Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!displayBackdropUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(displayBackdropUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = movie.cleanTitle,
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

                // Poster + Floating Language Badges
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
                            .width(100.dp)
                            .aspectRatio(2f / 3f)
                    ) {
                        if (!displayPosterUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(displayPosterUrl)
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
                                Icon(Icons.Default.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.padding(bottom = 6.dp)) {
                        Text(
                            text = movie.cleanTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (movie.language != LanguageBadge.ALL) {
                                val badgeColor = when (movie.language) {
                                    LanguageBadge.DUBLAJ -> SuccessGreen
                                    LanguageBadge.ALTYAZI -> MaterialTheme.colorScheme.primary
                                    LanguageBadge.BOTH -> SecondaryCyan
                                    else -> MaterialTheme.colorScheme.primary
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = badgeColor
                                ) {
                                    Text(
                                        text = movie.language.label,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // In-Progress Bar (if movie has saved progress)
            if (savedProgress != null && savedProgress.progressPercent in 0.02f..0.96f) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NetflixRed.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kaldığın Yer: ${savedProgress.formattedProgressTime}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = savedProgress.formattedRemainingTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = NetflixRedBright
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { savedProgress.progressPercent },
                            color = NetflixRedBright,
                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Play / Resume Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val hasProgress = savedProgress != null && savedProgress.progressPercent in 0.02f..0.96f

                // Large Main Play Button
                Button(
                    onClick = { viewModel.playStreamMovie(movie, resume = true) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NetflixRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .tvFocusHighlight(
                            shape = RoundedCornerShape(12.dp),
                            focusedBorderColor = Color.White,
                            scaleWhenFocused = 1.04f,
                            onClick = { viewModel.playStreamMovie(movie, resume = true) }
                        )
                        .testTag("btn_play_movie_detail")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasProgress) "Kaldığın Yerden Devam Et" else "Filmi İzle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // If in-progress, allow starting from beginning
                if (hasProgress) {
                    OutlinedButton(
                        onClick = { viewModel.playStreamMovie(movie, resume = false) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .tvFocusHighlight(
                                shape = RoundedCornerShape(12.dp),
                                focusedBorderColor = NetflixRedBright,
                                scaleWhenFocused = 1.04f,
                                onClick = { viewModel.playStreamMovie(movie, resume = false) }
                            )
                            .testTag("btn_restart_movie_detail")
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Baştan Başlat", fontWeight = FontWeight.SemiBold)
                    }
                }

                // Download Button
                Button(
                    onClick = {
                        if (!isQueued) {
                            viewModel.directDownloadMovie(context, movie)
                        }
                    },
                    enabled = !isQueued,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .tvFocusHighlight(
                            shape = RoundedCornerShape(12.dp),
                            focusedBorderColor = NetflixRedBright,
                            scaleWhenFocused = 1.04f,
                            onClick = {
                                if (!isQueued) {
                                    viewModel.directDownloadMovie(context, movie)
                                }
                            }
                        )
                        .testTag("btn_download_movie_detail")
                ) {
                    Icon(
                        imageVector = if (isQueued) Icons.Default.Check else Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isQueued) "İndirme Kuyruğunda" else "Filmi İndir",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // TMDB Details Section: Puan, Yıl, Süre, Yönetmen, Konusu, Oyuncular
            TmdbMetadataSection(
                state = tmdbState,
                isTvSeries = false
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
