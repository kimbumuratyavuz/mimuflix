package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NetflixRed
import com.example.ui.theme.NetflixRedBright
import kotlinx.coroutines.launch

data class OnboardingPageData(
    val badge: String,
    val icon: ImageVector,
    val title: String,
    val description: String,
    val highlightAccent: Color
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = listOf(
        OnboardingPageData(
            badge = "ZENGİN ARŞİV & KEŞİF",
            icon = Icons.Default.Movie,
            title = "Film & Dizi Dünyası",
            description = "Sürekli güncellenen zengin arşiviyle binlerce içeriği keşfedin. Kategori, tür, dublaj ve altyazı filtreleriyle aradığınız yapıma anında ulaşın.",
            highlightAccent = NetflixRedBright
        ),
        OnboardingPageData(
            badge = "YÜKSEK HIZLI MOTOR",
            icon = Icons.Default.Download,
            title = "Çok Parçalı İndirme",
            description = "M3U8 ve MP4 akışlarını birden çok bağlantı segmentiyle maksimum hızda indirin. İndirmeleri dilediğiniz an duraklatın ve kaldığı yerden devam ettirin.",
            highlightAccent = Color(0xFF38BDF8)
        ),
        OnboardingPageData(
            badge = "SEZON & BÖLÜM YÖNETİCİSİ",
            icon = Icons.Default.Tv,
            title = "Toplu Bölüm İndirme",
            description = "Dizilerin tüm sezonlarını tek ekranda görüntüleyin. İstediğiniz bölümleri işaretleyip tek bir dokunuşla indirme kuyruğuna topluca ekleyin.",
            highlightAccent = Color(0xFF4ADE80)
        ),
        OnboardingPageData(
            badge = "ÖZGÜRLÜK & ÇEVRİMDIŞI",
            icon = Icons.Default.PlayCircle,
            title = "İnternetsiz Kesintisiz Seyir",
            description = "İndirdiğiniz tüm videoları internet bağlantısına ihtiyaç duymadan cihazınızda izleyin. Sinematik kırmızı ve modern cam efektli tasarımın keyfini çıkarın.",
            highlightAccent = NetflixRedBright
        )
    )

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF140406),
                        Color(0xFF0A0B10),
                        Color(0xFF050608)
                    )
                )
            )
            .testTag("onboarding_screen")
    ) {
        // Decorative ambient glow behind content
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .size(340.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NetflixRed.copy(alpha = 0.22f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Bar: Skip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isLastPage) {
                    TextButton(
                        onClick = onFinish,
                        modifier = Modifier.testTag("btn_onboarding_skip")
                    ) {
                        Text(
                            text = "Atla",
                            color = Color.White.copy(alpha = 0.70f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            // Pager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIndex ->
                val page = pages[pageIndex]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Feature Visual Icon Card
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .shadow(
                                elevation = 24.dp,
                                shape = RoundedCornerShape(32.dp),
                                spotColor = page.highlightAccent.copy(alpha = 0.5f)
                            )
                            .clip(RoundedCornerShape(32.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xEE1E2433),
                                        Color(0xEE111520)
                                    )
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        page.highlightAccent.copy(alpha = 0.75f),
                                        Color.White.copy(alpha = 0.15f)
                                    )
                                ),
                                shape = RoundedCornerShape(32.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = page.title,
                            tint = page.highlightAccent,
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    // Badge pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = page.highlightAccent.copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = page.highlightAccent.copy(alpha = 0.45f)
                        )
                    ) {
                        Text(
                            text = page.badge,
                            color = page.highlightAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Title
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Description
                    Text(
                        text = page.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.72f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.fillMaxWidth(0.92f)
                    )
                }
            }

            // Bottom Navigation Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 28.dp)
                ) {
                    pages.indices.forEach { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 26.dp else 8.dp,
                            label = "dot_width"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) NetflixRedBright else Color.White.copy(alpha = 0.25f)
                                )
                        )
                    }
                }

                // Action Button (İleri or Hemen Başla)
                if (!isLastPage) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NetflixRed,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = NetflixRed)
                            .testTag("btn_onboarding_next")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "İleri",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onFinish,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NetflixRedBright,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = NetflixRedBright)
                            .testTag("btn_onboarding_start")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Hemen Başla",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
