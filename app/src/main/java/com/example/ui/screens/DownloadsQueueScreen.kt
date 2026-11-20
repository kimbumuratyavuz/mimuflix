package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.DownloadStatus
import com.example.model.DownloadTask
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NetflixRed
import com.example.ui.theme.NetflixRedBright
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsQueueScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dbTasks by viewModel.dbTasks.collectAsState()
    val activeTask by viewModel.activeServiceTask.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()

    var selectedTaskIds by remember { mutableStateOf(setOf<String>()) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Map tasks for real-time progress lookup from active service task
    val currentTaskList = remember(dbTasks, activeTask) {
        val map = dbTasks.associateBy { it.id }.toMutableMap()
        activeTask?.let { map[it.id] = it }
        dbTasks.map { map[it.id] ?: it }
    }

    val pendingCount = remember(currentTaskList) {
        currentTaskList.count { it.status == DownloadStatus.PENDING || it.status == DownloadStatus.PAUSED }
    }
    val completedCount = remember(currentTaskList) {
        currentTaskList.count { it.status == DownloadStatus.COMPLETED }
    }
    val isAllSelected = currentTaskList.isNotEmpty() && selectedTaskIds.size == currentTaskList.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "İndirme & Kuyruk",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (currentTaskList.isEmpty()) "Kuyruk boş" else "$pendingCount bekleyen • $completedCount tamamlandı",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Open Downloads folder
                    IconButton(
                        onClick = { viewModel.openFolder(context) },
                        modifier = Modifier.testTag("btn_open_folder")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = stringResource(R.string.btn_go_to_files),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Clear all button (if any tasks exist)
                    if (currentTaskList.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier.testTag("btn_clear_all_tasks")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Tümünü Temizle",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    // Open Settings
                    IconButton(
                        onClick = { viewModel.openSettings() },
                        modifier = Modifier.testTag("btn_open_settings_queue")
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (currentTaskList.isEmpty()) {
                // Empty State: clean, informative, no M3U/fast paste buttons
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 560.dp)
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(NetflixRed.copy(alpha = 0.12f))
                            .border(1.5.dp, NetflixRed.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = NetflixRedBright,
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "İndirme Kuyruğu Boş",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Film ve Dizi sayfalarındaki indirme butonuna dokunarak dilediğiniz içerikleri buraya ekleyebilirsiniz.\nEkledikten sonra tümünü sırayla veya aralarından seçerek indirebilirsiniz.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = { viewModel.selectTab(MainTab.MOVIES) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NetflixRed,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("btn_browse_movies")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Filmleri Keşfet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                // Active Queue & Task List
                LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 880.dp)
                ) {
                    // Sticky Control Card: Bulk operations
                    item(key = "header_controls") {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("queue_control_card")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                // Primary Action Row: Start All or Stop
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isDownloading) {
                                        if (isPaused) {
                                            Button(
                                                onClick = { viewModel.resumeDownloads(context) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = SuccessGreen,
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(46.dp)
                                                    .testTag("btn_resume_queue")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Devam Et",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }
                                        } else {
                                            Button(
                                                onClick = { viewModel.pauseDownloads(context) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = WarningAmber,
                                                    contentColor = Color.Black
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(46.dp)
                                                    .testTag("btn_pause_queue")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Pause,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Duraklat",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.stopDownloads(context) },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .height(44.dp)
                                        ) {
                                            Icon(
                                                    imageVector = Icons.Default.Stop,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "Durdur",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }
                                        } else {
                                            Button(
                                                onClick = { viewModel.startAllQueue(context) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = NetflixRed,
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .testTag("btn_start_all_queue")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = "Tümünü İndir",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        // Select/Deselect All Toggle
                                        FilledTonalButton(
                                            onClick = {
                                                selectedTaskIds = if (isAllSelected) emptySet() else currentTaskList.map { it.id }.toSet()
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .height(44.dp)
                                                .testTag("btn_toggle_select_all")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SelectAll,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isAllSelected) "Kaldır" else "Tümü",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    // Secondary Action Row: Selected operations (visible when items selected)
                                    AnimatedVisibility(
                                        visible = selectedTaskIds.isNotEmpty(),
                                        enter = fadeIn(),
                                        exit = fadeOut()
                                    ) {
                                        Column {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Start Selected
                                                Button(
                                                    onClick = {
                                                        viewModel.startSelectedTasks(context, selectedTaskIds)
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                        contentColor = MaterialTheme.colorScheme.primary
                                                    ),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(38.dp)
                                                        .testTag("btn_download_selected")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Seçilenleri İndir (${selectedTaskIds.size})",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        maxLines = 1
                                                    )
                                                }

                                                // Delete Selected
                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.deleteSelectedTasks(context, selectedTaskIds)
                                                        selectedTaskIds = emptySet()
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = MaterialTheme.colorScheme.error
                                                    ),
                                                    modifier = Modifier
                                                        .height(38.dp)
                                                        .testTag("btn_delete_selected")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Sil",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.5.sp,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    // Task Cards
                    items(currentTaskList, key = { it.id }) { task ->
                        val isSelected = selectedTaskIds.contains(task.id)
                        DownloadTaskQueueCard(
                            task = task,
                            isSelected = isSelected,
                            onToggleSelect = {
                                selectedTaskIds = if (isSelected) {
                                    selectedTaskIds - task.id
                                } else {
                                    selectedTaskIds + task.id
                                }
                            },
                            onStart = {
                                if (task.status == DownloadStatus.PAUSED && isPaused && task.id == activeTask?.id) {
                                    viewModel.resumeDownloads(context)
                                } else {
                                    viewModel.startSingleTask(context, task.id)
                                }
                            },
                            onPause = { viewModel.pauseDownloads(context) },
                            onOpen = { viewModel.openDownloadedVideo(context, task) },
                            onDelete = {
                                viewModel.deleteTask(context, task)
                                selectedTaskIds = selectedTaskIds - task.id
                            }
                        )
                    }
                }
            }
        }
    }

    // Confirmation dialog for clearing all tasks
    if (showClearConfirmDialog) {
        Dialog(
            onDismissRequest = { showClearConfirmDialog = false },
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
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                            .border(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Tümünü Temizle",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "İndirme kuyruğundaki ve tamamlanan tüm içerikleri silmek istediğinize emin misiniz? Bu işlem geri alınamaz.",
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
                            onClick = { showClearConfirmDialog = false },
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
                                viewModel.clearAllTasks(context)
                                selectedTaskIds = emptySet()
                                showClearConfirmDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_confirm_clear_all")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Temizle",
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

@Composable
fun DownloadTaskQueueCard(
    task: DownloadTask,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val isActiveDownloading = task.status == DownloadStatus.DOWNLOADING
    val isCompleted = task.status == DownloadStatus.COMPLETED
    val isPaused = task.status == DownloadStatus.PAUSED
    val isPending = task.status == DownloadStatus.PENDING

    val percent = if (task.totalBytes > 0) {
        ((task.downloadedBytes * 100) / task.totalBytes).toInt().coerceIn(0, 100)
    } else if (isCompleted) 100 else 0
    val speedStr = formatSpeed(task.speedBytesPerSec)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isActiveDownloading) 1.5.dp else 1.dp,
            color = when {
                isActiveDownloading -> NetflixRed
                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox for bulk actions
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = NetflixRed
                    ),
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("checkbox_${task.id}")
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Poster or placeholder thumbnail
                Box(
                    modifier = Modifier
                        .size(width = 46.dp, height = 64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!task.logoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(task.logoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = task.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and Status info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Status pill
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (task.status) {
                            DownloadStatus.DOWNLOADING -> {
                                Text(
                                    text = "İndiriliyor %$percent",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NetflixRedBright
                                )
                                if (task.speedBytesPerSec > 0) {
                                    Text(
                                        text = " • $speedStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            DownloadStatus.COMPLETED -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "İndirildi",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                            }
                            DownloadStatus.PAUSED -> {
                                Text(
                                    text = "Duraklatıldı (%$percent)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WarningAmber,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            DownloadStatus.PENDING -> {
                                Icon(
                                    imageVector = Icons.Default.HourglassEmpty,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Kuyrukta bekliyor",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DownloadStatus.FAILED -> {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Hata oluştu",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ErrorRed
                                )
                            }
                            else -> {
                                Text(
                                    text = task.status.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Action button (Play, Pause, Resume, or Delete)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when {
                        isCompleted -> {
                            IconButton(
                                onClick = onOpen,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Oynat",
                                    tint = SuccessGreen
                                )
                            }
                        }
                        isActiveDownloading -> {
                            IconButton(
                                onClick = onPause,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Duraklat",
                                    tint = NetflixRedBright
                                )
                            }
                        }
                        isPending || isPaused -> {
                            IconButton(
                                onClick = onStart,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Başlat",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Sil",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Progress bar if active or paused
            if (isActiveDownloading || (isPaused && percent > 0)) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { percent / 100f },
                    color = if (isActiveDownloading) NetflixRed else WarningAmber,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

private fun formatSpeed(bytesPerSec: Long): String {
    return when {
        bytesPerSec <= 0 -> "0 KB/s"
        bytesPerSec < 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f KB/s", bytesPerSec / 1024.0)
        else -> String.format(java.util.Locale.US, "%.2f MB/s", bytesPerSec / (1024.0 * 1024.0))
    }
}
