package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.MovieQuotesRepository
import com.example.ui.theme.NetflixRed
import com.example.ui.theme.NetflixRedBright

@Composable
fun SplashScreen(
    coverUrl: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "splash_progress_anim"
    )

    val currentQuote = remember { MovieQuotesRepository.getRandomMovieQuote() }

    val cleanCoverUrl = remember(coverUrl) { com.example.data.CatalogRepository.sanitizePosterUrl(coverUrl) ?: coverUrl }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("splash_screen_container")
    ) {
        // 1. Sinematik Arka Plan Gradyan Dolgusu (Cinematic dark background placeholder)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF2B141C),
                            Color(0xFF140D12),
                            Color.Black
                        )
                    )
                )
        )

        // 2. Tam Ekran Kapak Fotoğrafı (Full-screen Cover Image)
        if (cleanCoverUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(cleanCoverUrl)
                    .crossfade(true)
                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .build(),
                contentDescription = "MimuFlix Medya Kapağı",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("splash_cover_image")
            )
        }

        // 2. Üst ve Alt Sinematik Gölgelendirme (Top & Bottom Vignette Overlays)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.75f),
                            Color.Black.copy(alpha = 0.98f)
                        )
                    )
                )
        )

        // 3. Üstte Solda Büyük MimuFlix Logosu ve Yazısı (Top-Left Large Branding)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 24.dp, top = 20.dp)
                .testTag("splash_logo_header")
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(listOf(NetflixRedBright, NetflixRed))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "M",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp
                )
            }
            Text(
                text = "MimuFlix",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 38.sp,
                    letterSpacing = 1.5.sp,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.85f),
                        offset = Offset(2f, 4f),
                        blurRadius = 8f
                    )
                ),
                color = Color.White
            )
        }

        // 4. Alt Bölüm: Replik + Film Adı + Sola Dayalı 'Yükleniyor....' + En Altta Kırmızı Progress Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.Start
        ) {
            // Sola Dayalı İtalik Film Repliği ve İtalik Film Adı (Taşma korumalı)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
                    .testTag("splash_quote_container")
            ) {
                Text(
                    text = "“${currentQuote.quote}”",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.95f),
                            offset = Offset(1f, 2f),
                            blurRadius = 4f
                        )
                    ),
                    color = Color.White.copy(alpha = 0.95f),
                    softWrap = true,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("splash_quote_text")
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "— ${currentQuote.movieTitle}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.SemiBold,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.95f),
                            offset = Offset(1f, 2f),
                            blurRadius = 4f
                        )
                    ),
                    color = NetflixRedBright,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("splash_quote_movie")
                )
            }

            // Sola Dayalı 'Yükleniyor....' Metni
            Text(
                text = "Yükleniyor....",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.9f),
                        offset = Offset(1f, 2f),
                        blurRadius = 4f
                    )
                ),
                color = Color.White.copy(alpha = 0.95f),
                modifier = Modifier
                    .padding(start = 24.dp, bottom = 12.dp)
                    .testTag("splash_loading_text")
            )

            // En Alttaki Kırmızı İlerleme Çubuğu (Red Progress Bar at the absolute bottom)
            LinearProgressIndicator(
                progress = { animatedProgress },
                color = NetflixRed,
                trackColor = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .testTag("splash_progress_bar")
            )
        }
    }
}
