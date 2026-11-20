package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import com.example.ui.util.tvFocusHighlight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.CatalogMovieItem
import com.example.model.CatalogSeries
import com.example.model.LanguageBadge
import com.example.ui.theme.NetflixRed
import com.example.ui.theme.NetflixRedBright
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.delay

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun MovieHeaderCarousel(
    featuredMovies: List<CatalogMovieItem>,
    onPlayMovie: (CatalogMovieItem) -> Unit,
    onDetailClick: (CatalogMovieItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (featuredMovies.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { featuredMovies.size })
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val currentMovie = featuredMovies[page]
            CardBanner(
                title = currentMovie.cleanTitle,
                posterUrl = currentMovie.posterUrl,
                categories = currentMovie.categories,
                language = currentMovie.language,
                totalItems = featuredMovies.size,
                currentIndex = pagerState.currentPage,
                onDotClick = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                onPlayClick = { onPlayMovie(currentMovie) },
                onDetailClick = { onDetailClick(currentMovie) }
            )
        }
    }
}

@Composable
fun SeriesHeaderCarousel(
    featuredSeries: List<CatalogSeries>,
    onPlaySeries: (CatalogSeries) -> Unit,
    onDetailClick: (CatalogSeries) -> Unit,
    modifier: Modifier = Modifier
) {
    if (featuredSeries.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { featuredSeries.size })
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val currentSeries = featuredSeries[page]
            CardBanner(
                title = currentSeries.seriesName,
                posterUrl = currentSeries.posterUrl,
                categories = listOf("${currentSeries.totalSeasonsCount} Sezon", "${currentSeries.totalEpisodesCount} Bölüm"),
                language = currentSeries.languages.firstOrNull() ?: LanguageBadge.ALL,
                totalItems = featuredSeries.size,
                currentIndex = pagerState.currentPage,
                onDotClick = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                onPlayClick = { onPlaySeries(currentSeries) },
                onDetailClick = { onDetailClick(currentSeries) }
            )
        }
    }
}

@Composable
private fun CardBanner(
    title: String,
    posterUrl: String?,
    categories: List<String>,
    language: LanguageBadge,
    totalItems: Int,
    currentIndex: Int,
    onDotClick: (Int) -> Unit,
    onPlayClick: () -> Unit,
    onDetailClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isTvOrWide = maxWidth > 600.dp
        val bannerHeight = if (isTvOrWide) 210.dp else 200.dp

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isTvOrWide) 24.dp else 16.dp, vertical = 6.dp)
                .height(bannerHeight)
                .testTag("header_carousel")
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
            val cleanUrl = remember(posterUrl) { com.example.data.CatalogRepository.sanitizePosterUrl(posterUrl) }

            // Placeholder Backdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF221118),
                                Color(0xFF141219),
                                Color(0xFF0F0E12)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    modifier = Modifier.size(56.dp)
                )
            }

            // Poster Backdrop
            if (!cleanUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(cleanUrl)
                        .crossfade(true)
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .build(),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Cinematic Gradient Overlay (Dark bottom gradient for high contrast)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            // Content Overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                // Language & Category Chips
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (language != LanguageBadge.ALL) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (language == LanguageBadge.DUBLAJ) SuccessGreen else NetflixRed
                        ) {
                            Text(
                                text = language.label,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (categories.isNotEmpty()) {
                        Text(
                            text = categories.take(2).joinToString(" • "),
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Title
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Actions: Hemen İzle + Detaylar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPlayClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NetflixRed,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(36.dp)
                            .tvFocusHighlight(
                                shape = RoundedCornerShape(10.dp),
                                focusedBorderColor = Color.White,
                                scaleWhenFocused = 1.08f,
                                onClick = onPlayClick
                            )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Hemen İzle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = onDetailClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .height(36.dp)
                            .tvFocusHighlight(
                                shape = RoundedCornerShape(10.dp),
                                focusedBorderColor = NetflixRedBright,
                                scaleWhenFocused = 1.08f,
                                onClick = onDetailClick
                            )
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Detay", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Dots Indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until totalItems) {
                            val isSelected = i == currentIndex
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) NetflixRedBright else Color.White.copy(alpha = 0.45f))
                                    .clickable { onDotClick(i) }
                            )
                        }
                    }
                }
            }
        }
    }
}
}
