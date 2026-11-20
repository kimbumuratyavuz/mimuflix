package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.MimuApplication
import com.example.data.CatalogRepository
import com.example.data.OnboardingManager
import com.example.data.PreferencesManager
import com.example.data.TmdbRepository
import com.example.model.TmdbUiState
import com.example.downloader.StorageHelper
import com.example.model.CatalogEpisodeItem
import com.example.model.CatalogMovieItem
import com.example.model.CatalogSeries
import com.example.model.DownloadStatus
import com.example.model.DownloadTask
import com.example.model.LanguageBadge
import com.example.model.M3uItem
import com.example.model.SeriesSortOrder
import com.example.parser.M3uParser
import com.example.model.DownloadedEpisode
import com.example.model.DownloadedMovie
import com.example.model.DownloadedSeriesFolder
import com.example.model.PlayerMediaItem
import com.example.model.PlaybackProgressEntity
import com.example.service.DownloadService
import com.example.model.FavoriteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MainTab {
    MOVIES,
    SERIES,
    FAVORITES,
    DOWNLOADED,
    DOWNLOADER
}

enum class AppScreen {
    HOME,
    MANUAL_INPUT,
    LIST,
    MOVIE_DETAIL,
    SERIES_DETAIL,
    MOVIE_SEARCH,
    SERIES_SEARCH
}

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

class MainViewModel : ViewModel() {

    private val dao = MimuApplication.database.downloadTaskDao()
    private val favoriteDao = MimuApplication.database.favoriteDao()
    private val playbackProgressDao = MimuApplication.database.playbackProgressDao()

    val allPlaybackProgress: StateFlow<List<PlaybackProgressEntity>> = playbackProgressDao.getAllProgressFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val moviePlaybackProgress: StateFlow<List<PlaybackProgressEntity>> = playbackProgressDao.getMovieProgressFlow()
        .map { list -> list.filter { it.progressPercent in 0.02f..0.96f } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val seriesPlaybackProgress: StateFlow<List<PlaybackProgressEntity>> = playbackProgressDao.getSeriesProgressFlow()
        .map { list -> list.filter { it.progressPercent in 0.02f..0.96f } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteMovieIds: StateFlow<Set<String>> = favoriteDao.getFavoriteMovieIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val favoriteSeriesIds: StateFlow<Set<String>> = favoriteDao.getFavoriteSeriesIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val dbTasks: StateFlow<List<DownloadTask>> = dao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeServiceTask: StateFlow<DownloadTask?> = DownloadService.currentActiveTask
    val isDownloading: StateFlow<Boolean> = DownloadService.isServiceActive
    val isPaused: StateFlow<Boolean> = DownloadService.isServicePaused

    private val preferencesManager = PreferencesManager(MimuApplication.instance)
    private val _isOnboardingCompleted = MutableStateFlow(preferencesManager.isOnboardingCompleted)
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    fun completeOnboarding() {
        preferencesManager.completeOnboarding()
        _isOnboardingCompleted.value = true
    }

    private val _originTab = MutableStateFlow<MainTab?>(null)
    val originTab: StateFlow<MainTab?> = _originTab.asStateFlow()

    private val _currentTab = MutableStateFlow(MainTab.MOVIES)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _screenBackStack = MutableStateFlow<List<AppScreen>>(emptyList())
    val screenBackStack: StateFlow<List<AppScreen>> = _screenBackStack.asStateFlow()

    private val _selectedMovieForDetail = MutableStateFlow<CatalogMovieItem?>(null)
    val selectedMovieForDetail: StateFlow<CatalogMovieItem?> = _selectedMovieForDetail.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _activePlayerMedia = MutableStateFlow<PlayerMediaItem?>(null)
    val activePlayerMedia: StateFlow<PlayerMediaItem?> = _activePlayerMedia.asStateFlow()

    private val _downloadedMovies = MutableStateFlow<List<DownloadedMovie>>(emptyList())
    val downloadedMovies: StateFlow<List<DownloadedMovie>> = _downloadedMovies.asStateFlow()

    private val _downloadedSeries = MutableStateFlow<List<DownloadedSeriesFolder>>(emptyList())
    val downloadedSeries: StateFlow<List<DownloadedSeriesFolder>> = _downloadedSeries.asStateFlow()

    private val _themeMode = MutableStateFlow(preferencesManager.themeMode)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(preferencesManager.keepScreenOn)
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private val _m3uInputText = MutableStateFlow("")
    val m3uInputText: StateFlow<String> = _m3uInputText.asStateFlow()

    private val _parsedItems = MutableStateFlow<List<M3uItem>>(emptyList())
    val parsedItems: StateFlow<List<M3uItem>> = _parsedItems.asStateFlow()

    // ------------------------------------------------------------------------
    // Splash Screen State
    // ------------------------------------------------------------------------
    private val _isSplashVisible = MutableStateFlow(true)
    val isSplashVisible: StateFlow<Boolean> = _isSplashVisible.asStateFlow()

    private val _splashMovieCoverUrl = MutableStateFlow(CatalogRepository.getRandomMovieCover())
    val splashMovieCoverUrl: StateFlow<String> = _splashMovieCoverUrl.asStateFlow()

    private val _splashProgress = MutableStateFlow(0.15f)
    val splashProgress: StateFlow<Float> = _splashProgress.asStateFlow()

    // ------------------------------------------------------------------------
    // Catalog State: Movies
    // ------------------------------------------------------------------------
    private val _moviesList = MutableStateFlow<List<CatalogMovieItem>>(emptyList())
    val moviesList: StateFlow<List<CatalogMovieItem>> = _moviesList.asStateFlow()

    private val _isLoadingMovies = MutableStateFlow(false)
    val isLoadingMovies: StateFlow<Boolean> = _isLoadingMovies.asStateFlow()

    private val _moviesError = MutableStateFlow<String?>(null)
    val moviesError: StateFlow<String?> = _moviesError.asStateFlow()

    private val _movieSearchQuery = MutableStateFlow("")
    val movieSearchQuery: StateFlow<String> = _movieSearchQuery.asStateFlow()

    private val _selectedMovieCategory = MutableStateFlow<String?>(null)
    val selectedMovieCategory: StateFlow<String?> = _selectedMovieCategory.asStateFlow()

    private val _selectedMovieLanguage = MutableStateFlow(LanguageBadge.ALL)
    val selectedMovieLanguage: StateFlow<LanguageBadge> = _selectedMovieLanguage.asStateFlow()

    val movieCategories: StateFlow<List<String>> = _moviesList.combine(_moviesList) { list, _ ->
        val categories = mutableSetOf<String>()
        list.forEach { movie -> categories.addAll(movie.categories) }
        categories.sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredMovies: StateFlow<List<CatalogMovieItem>> = combine(
        _moviesList,
        _movieSearchQuery,
        _selectedMovieCategory,
        _selectedMovieLanguage
    ) { movies, query, category, lang ->
        var result = movies
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter { it.title.lowercase().contains(q) || it.cleanTitle.lowercase().contains(q) }
        }
        if (!category.isNullOrBlank()) {
            result = result.filter { it.categories.any { c -> c.equals(category, ignoreCase = true) } }
        }
        if (lang != LanguageBadge.ALL) {
            result = result.filter { it.language == lang || it.language == LanguageBadge.BOTH }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Deliver the complete catalog directly without 21-by-21 chunk pagination
    val pagedMovies: StateFlow<List<CatalogMovieItem>> = filteredMovies

    val featuredMovies: StateFlow<List<CatalogMovieItem>> = _moviesList.map { list ->
        if (list.size <= 5) list else list.filter { !it.posterUrl.isNullOrBlank() }.shuffled().take(5).ifEmpty { list.take(5) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ------------------------------------------------------------------------
    // Catalog State: Series
    // ------------------------------------------------------------------------
    private val _seriesList = MutableStateFlow<List<CatalogSeries>>(emptyList())
    val seriesList: StateFlow<List<CatalogSeries>> = _seriesList.asStateFlow()

    private val _isLoadingSeries = MutableStateFlow(false)
    val isLoadingSeries: StateFlow<Boolean> = _isLoadingSeries.asStateFlow()

    private val _seriesError = MutableStateFlow<String?>(null)
    val seriesError: StateFlow<String?> = _seriesError.asStateFlow()

    private val _seriesSearchQuery = MutableStateFlow("")
    val seriesSearchQuery: StateFlow<String> = _seriesSearchQuery.asStateFlow()

    private val _selectedSeriesLanguage = MutableStateFlow(LanguageBadge.ALL)
    val selectedSeriesLanguage: StateFlow<LanguageBadge> = _selectedSeriesLanguage.asStateFlow()

    private val _seriesSortOrder = MutableStateFlow(SeriesSortOrder.DEFAULT)
    val seriesSortOrder: StateFlow<SeriesSortOrder> = _seriesSortOrder.asStateFlow()

    private val _selectedSeriesForDetail = MutableStateFlow<CatalogSeries?>(null)
    val selectedSeriesForDetail: StateFlow<CatalogSeries?> = _selectedSeriesForDetail.asStateFlow()

    val filteredSeries: StateFlow<List<CatalogSeries>> = combine(
        _seriesList,
        _seriesSearchQuery,
        _selectedSeriesLanguage,
        _seriesSortOrder
    ) { series, query, lang, sortOrder ->
        var result = series
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter { it.seriesName.lowercase().contains(q) }
        }
        if (lang != LanguageBadge.ALL) {
            result = result.filter { it.languages.contains(lang) || it.languages.contains(LanguageBadge.BOTH) }
        }
        when (sortOrder) {
            SeriesSortOrder.DEFAULT -> result // Preserve natural source ordering
            SeriesSortOrder.EPISODES_ASC -> result.sortedBy { it.totalEpisodesCount }
            SeriesSortOrder.EPISODES_DESC -> result.sortedByDescending { it.totalEpisodesCount }
            SeriesSortOrder.NAME_ASC -> result.sortedBy { it.seriesName.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val featuredSeries: StateFlow<List<CatalogSeries>> = _seriesList.map { list ->
        if (list.size <= 5) list else list.filter { !it.posterUrl.isNullOrBlank() }.shuffled().take(5).ifEmpty { list.take(5) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Deliver the complete series catalog directly without 21-by-21 chunk pagination
    val pagedSeries: StateFlow<List<CatalogSeries>> = filteredSeries

    fun loadMoreMovies() {
        // No-op: full list is already presented
    }

    fun loadMoreSeries() {
        // No-op: full list is already presented
    }

    val favoriteMovies: StateFlow<List<CatalogMovieItem>> = combine(
        _moviesList,
        favoriteMovieIds
    ) { movies, favIds ->
        movies.filter { favIds.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSeries: StateFlow<List<CatalogSeries>> = combine(
        _seriesList,
        favoriteSeriesIds
    ) { series, favIds ->
        series.filter { favIds.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadCatalogs()
    }

    fun loadCatalogs(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val minDurationMs = 10000L

            _isSplashVisible.value = true
            _splashProgress.value = 0.05f

            // Steady 10-second progress ticker (100 steps x 100ms = 10,000ms)
            val progressJob = launch {
                val totalSteps = 100
                val stepDelay = minDurationMs / totalSteps
                for (step in 1..totalSteps) {
                    delay(stepDelay)
                    val target = (step.toFloat() / totalSteps) * 0.95f
                    if (_splashProgress.value < target) {
                        _splashProgress.value = target
                    }
                }
            }

            // Preload initial splash cover immediately at launch
            try {
                val imageLoader = coil.Coil.imageLoader(MimuApplication.instance)
                imageLoader.enqueue(
                    coil.request.ImageRequest.Builder(MimuApplication.instance)
                        .data(_splashMovieCoverUrl.value)
                        .build()
                )
            } catch (e: Exception) {
                // ignore
            }

            val moviesJob = launch { loadMovies(forceRefresh) }
            val seriesJob = launch { loadSeries(forceRefresh) }

            moviesJob.invokeOnCompletion {
                val moviePosters = _moviesList.value.mapNotNull { it.posterUrl }.filter { it.isNotBlank() }
                if (moviePosters.isNotEmpty()) {
                    CatalogRepository.persistMediaPosters(moviePosters)
                    if (_splashMovieCoverUrl.value.isBlank()) {
                        _splashMovieCoverUrl.value = moviePosters.random()
                    }
                }
                // Preload featured & top movie posters into Coil cache
                try {
                    val imageLoader = coil.Coil.imageLoader(MimuApplication.instance)
                    featuredMovies.value.mapNotNull { it.posterUrl }.forEach { url ->
                        imageLoader.enqueue(coil.request.ImageRequest.Builder(MimuApplication.instance).data(url).build())
                    }
                    _moviesList.value.take(40).mapNotNull { it.posterUrl }.forEach { url ->
                        imageLoader.enqueue(coil.request.ImageRequest.Builder(MimuApplication.instance).data(url).build())
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }

            seriesJob.invokeOnCompletion {
                val seriesPosters = _seriesList.value.mapNotNull { it.posterUrl }.filter { it.isNotBlank() }
                if (seriesPosters.isNotEmpty()) {
                    CatalogRepository.persistMediaPosters(seriesPosters)
                    if (_splashMovieCoverUrl.value.isBlank()) {
                        _splashMovieCoverUrl.value = seriesPosters.random()
                    }
                }
                // Preload featured & top series posters into Coil cache
                try {
                    val imageLoader = coil.Coil.imageLoader(MimuApplication.instance)
                    featuredSeries.value.mapNotNull { it.posterUrl }.forEach { url ->
                        imageLoader.enqueue(coil.request.ImageRequest.Builder(MimuApplication.instance).data(url).build())
                    }
                    _seriesList.value.take(40).mapNotNull { it.posterUrl }.forEach { url ->
                        imageLoader.enqueue(coil.request.ImageRequest.Builder(MimuApplication.instance).data(url).build())
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }

            moviesJob.join()
            seriesJob.join()

            // Ensure at least 10 seconds (10000ms) has elapsed before dismissing splash
            val elapsed = System.currentTimeMillis() - startTime
            val remaining = minDurationMs - elapsed
            if (remaining > 0) {
                delay(remaining)
            }
            progressJob.cancel()

            _splashProgress.value = 1.0f
            delay(350)
            _isSplashVisible.value = false
        }
    }

    fun loadMovies(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoadingMovies.value = true
            _moviesError.value = null
            try {
                val list = CatalogRepository.getMovies(forceRefresh)
                _moviesList.value = list
            } catch (e: Exception) {
                _moviesError.value = e.localizedMessage ?: "Filmler yüklenemedi"
            } finally {
                _isLoadingMovies.value = false
            }
        }
    }

    fun loadSeries(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoadingSeries.value = true
            _seriesError.value = null
            try {
                val list = CatalogRepository.getSeries(forceRefresh)
                _seriesList.value = list
                reconcileLegacySeriesFavorites(list)
            } catch (e: Exception) {
                _seriesError.value = e.localizedMessage ?: "Diziler yüklenemedi"
            } finally {
                _isLoadingSeries.value = false
            }
        }
    }

    private suspend fun reconcileLegacySeriesFavorites(seriesList: List<CatalogSeries>) {
        withContext(Dispatchers.IO) {
            try {
                if (seriesList.isEmpty()) return@withContext
                val existingFavs = favoriteDao.getSeriesFavoritesSync()
                if (existingFavs.isEmpty()) return@withContext
                val validIds = seriesList.map { it.id }.toSet()
                existingFavs.forEach { fav ->
                    if (!validIds.contains(fav.id)) {
                        val matched = seriesList.firstOrNull {
                            it.seriesName.equals(fav.title, ignoreCase = true)
                        }
                        if (matched != null) {
                            favoriteDao.deleteById(fav.id)
                            favoriteDao.insert(
                                FavoriteEntity(
                                    id = matched.id,
                                    isSeries = true,
                                    title = matched.seriesName,
                                    posterUrl = matched.posterUrl ?: fav.posterUrl,
                                    createdAt = fav.createdAt
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore reconciliation errors gracefully
            }
        }
    }

    // ------------------------------------------------------------------------
    // Filter & Sort Setters
    // ------------------------------------------------------------------------
    fun setMovieSearchQuery(query: String) {
        _movieSearchQuery.value = query
    }

    fun setMovieCategory(category: String?) {
        _selectedMovieCategory.value = category
    }

    fun setMovieLanguage(badge: LanguageBadge) {
        _selectedMovieLanguage.value = badge
    }

    fun resetMovieFiltersToAll() {
        _selectedMovieCategory.value = null
        _selectedMovieLanguage.value = LanguageBadge.ALL
    }

    fun setSeriesSearchQuery(query: String) {
        _seriesSearchQuery.value = query
    }

    fun setSeriesLanguage(badge: LanguageBadge) {
        _selectedSeriesLanguage.value = badge
    }

    fun setSeriesSortOrder(sortOrder: SeriesSortOrder) {
        _seriesSortOrder.value = sortOrder
    }

    private val _currentMovieTmdbState = MutableStateFlow<TmdbUiState>(TmdbUiState.Idle)
    val currentMovieTmdbState: StateFlow<TmdbUiState> = _currentMovieTmdbState.asStateFlow()

    private val _currentSeriesTmdbState = MutableStateFlow<TmdbUiState>(TmdbUiState.Idle)
    val currentSeriesTmdbState: StateFlow<TmdbUiState> = _currentSeriesTmdbState.asStateFlow()

    fun loadTmdbForMovie(rawTitle: String) {
        viewModelScope.launch {
            _currentMovieTmdbState.value = TmdbUiState.Loading
            try {
                val detail = TmdbRepository.getMovieDetail(rawTitle)
                if (detail != null) {
                    _currentMovieTmdbState.value = TmdbUiState.Success(detail)
                } else {
                    _currentMovieTmdbState.value = TmdbUiState.Error("TMDB detayları bulunamadı")
                }
            } catch (e: Exception) {
                _currentMovieTmdbState.value = TmdbUiState.Error(e.message ?: "Hata oluştu")
            }
        }
    }

    fun loadTmdbForSeries(seriesName: String) {
        viewModelScope.launch {
            _currentSeriesTmdbState.value = TmdbUiState.Loading
            try {
                val detail = TmdbRepository.getSeriesDetail(seriesName)
                if (detail != null) {
                    _currentSeriesTmdbState.value = TmdbUiState.Success(detail)
                } else {
                    _currentSeriesTmdbState.value = TmdbUiState.Error("TMDB detayları bulunamadı")
                }
            } catch (e: Exception) {
                _currentSeriesTmdbState.value = TmdbUiState.Error(e.message ?: "Hata oluştu")
            }
        }
    }

    fun openMovieDetail(movie: CatalogMovieItem) {
        _selectedMovieForDetail.value = movie
        loadTmdbForMovie(movie.title)
        navigateTo(AppScreen.MOVIE_DETAIL)
    }

    fun closeMovieDetail() {
        _selectedMovieForDetail.value = null
        _currentMovieTmdbState.value = TmdbUiState.Idle
        navigateBack()
    }

    fun openSeriesDetail(series: CatalogSeries) {
        _selectedSeriesForDetail.value = series
        loadTmdbForSeries(series.seriesName)
        navigateTo(AppScreen.SERIES_DETAIL)
    }

    fun closeSeriesDetail() {
        _selectedSeriesForDetail.value = null
        _currentSeriesTmdbState.value = TmdbUiState.Idle
        navigateBack()
    }

    fun openMovieSearch() {
        navigateTo(AppScreen.MOVIE_SEARCH)
    }

    fun openSeriesSearch() {
        navigateTo(AppScreen.SERIES_SEARCH)
    }

    // ------------------------------------------------------------------------
    // M3U Copying & Direct Download
    // ------------------------------------------------------------------------
    fun copyMovieM3u(context: Context, movie: CatalogMovieItem) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("M3U Movie", movie.rawM3uEntry)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "${movie.cleanTitle} M3U kopyalandı!", Toast.LENGTH_SHORT).show()
    }

    fun copySeriesM3u(context: Context, series: CatalogSeries) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("M3U Series", series.generatedM3uAllEpisodes)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "${series.seriesName} (${series.totalEpisodesCount} Bölüm) M3U kopyalandı!", Toast.LENGTH_SHORT).show()
    }

    fun copyEpisodeM3u(context: Context, episode: CatalogEpisodeItem) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("M3U Episode", episode.rawM3uEntry)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "${episode.episodeNumberText} M3U kopyalandı!", Toast.LENGTH_SHORT).show()
    }

    fun directDownloadMovie(context: Context, movie: CatalogMovieItem) {
        viewModelScope.launch {
            val task = DownloadTask(
                id = movie.id,
                title = movie.cleanTitle,
                streamUrl = movie.streamUrl,
                logoUrl = movie.posterUrl,
                httpReferrer = movie.httpReferrer,
                httpUserAgent = movie.httpUserAgent,
                targetFileName = M3uParser.sanitizeFileName(movie.cleanTitle),
                status = DownloadStatus.PENDING,
                isSeries = false
            )
            dao.insert(task)
            Toast.makeText(context, "${movie.cleanTitle} indirme kuyruğuna eklendi", Toast.LENGTH_SHORT).show()
        }
    }

    fun directDownloadSeries(context: Context, series: CatalogSeries) {
        viewModelScope.launch {
            val newTasks = series.episodes.map { ep ->
                val safeTitle = "${series.seriesName} - ${ep.episodeNumberText}"
                DownloadTask(
                    id = ep.id,
                    title = safeTitle,
                    streamUrl = ep.streamUrl,
                    logoUrl = series.posterUrl,
                    httpReferrer = ep.httpReferrer,
                    httpUserAgent = ep.httpUserAgent,
                    targetFileName = M3uParser.sanitizeFileName(safeTitle),
                    status = DownloadStatus.PENDING,
                    isSeries = true,
                    seriesName = series.seriesName,
                    episodeName = ep.episodeNumberText
                )
            }
            if (newTasks.isNotEmpty()) {
                dao.insertAll(newTasks)
                Toast.makeText(context, "${newTasks.size} bölüm indirme kuyruğuna eklendi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun downloadSelectedEpisodes(context: Context, series: CatalogSeries, episodes: List<CatalogEpisodeItem>) {
        if (episodes.isEmpty()) {
            Toast.makeText(context, "Lütfen en az bir bölüm seçin", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            val newTasks = episodes.map { ep ->
                val safeTitle = "${series.seriesName} - ${ep.episodeNumberText}"
                DownloadTask(
                    id = ep.id,
                    title = safeTitle,
                    streamUrl = ep.streamUrl,
                    logoUrl = series.posterUrl,
                    httpReferrer = ep.httpReferrer,
                    httpUserAgent = ep.httpUserAgent,
                    targetFileName = M3uParser.sanitizeFileName(safeTitle),
                    status = DownloadStatus.PENDING,
                    isSeries = true,
                    seriesName = series.seriesName,
                    episodeName = ep.episodeNumberText
                )
            }
            dao.insertAll(newTasks)
            Toast.makeText(context, "${newTasks.size} bölüm indirme kuyruğuna eklendi", Toast.LENGTH_SHORT).show()
        }
    }

    fun directDownloadEpisode(context: Context, episode: CatalogEpisodeItem, seriesName: String? = null) {
        viewModelScope.launch {
            val task = DownloadTask(
                id = episode.id,
                title = episode.title,
                streamUrl = episode.streamUrl,
                logoUrl = null,
                httpReferrer = episode.httpReferrer,
                httpUserAgent = episode.httpUserAgent,
                targetFileName = M3uParser.sanitizeFileName(episode.title),
                status = DownloadStatus.PENDING,
                isSeries = seriesName != null,
                seriesName = seriesName,
                episodeName = episode.episodeNumberText
            )
            dao.insert(task)
            Toast.makeText(context, "${episode.title} indirme kuyruğuna eklendi", Toast.LENGTH_SHORT).show()
        }
    }

    fun startAllQueue(context: Context) {
        viewModelScope.launch {
            val nonCompleted = dao.getTasksByStatuses(
                listOf(DownloadStatus.PAUSED, DownloadStatus.FAILED, DownloadStatus.CANCELLED)
            )
            if (nonCompleted.isNotEmpty()) {
                val updated = nonCompleted.map { it.copy(status = DownloadStatus.PENDING) }
                dao.insertAll(updated)
            }
            DownloadService.startQueue(context)
            Toast.makeText(context, "İndirme kuyruğu sırayla başlatıldı", Toast.LENGTH_SHORT).show()
        }
    }

    fun startSelectedTasks(context: Context, taskIds: Set<String>) {
        if (taskIds.isEmpty()) {
            Toast.makeText(context, "Lütfen en az bir video seçin", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            for (id in taskIds) {
                val task = dao.getTaskById(id)
                if (task != null && task.status != DownloadStatus.COMPLETED) {
                    dao.insert(task.copy(status = DownloadStatus.PENDING))
                }
            }
            DownloadService.startQueue(context)
            Toast.makeText(context, "${taskIds.size} video sırayla indirilmeye başlandı", Toast.LENGTH_SHORT).show()
        }
    }

    fun startSingleTask(context: Context, taskId: String) {
        viewModelScope.launch {
            val task = dao.getTaskById(taskId)
            if (task != null && task.status != DownloadStatus.COMPLETED) {
                dao.insert(task.copy(status = DownloadStatus.PENDING))
            }
            DownloadService.startQueue(context)
        }
    }

    fun deleteTask(context: Context, task: DownloadTask) {
        viewModelScope.launch {
            if (DownloadService.currentActiveTask.value?.id == task.id) {
                DownloadService.cancel(context)
            }
            task.filePath?.let { path ->
                val f = java.io.File(path)
                if (f.exists()) f.delete()
            }
            dao.delete(task)
            Toast.makeText(context, "${task.title} kaldırıldı", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteSelectedTasks(context: Context, taskIds: Set<String>) {
        if (taskIds.isEmpty()) return
        viewModelScope.launch {
            val activeId = DownloadService.currentActiveTask.value?.id
            if (activeId != null && taskIds.contains(activeId)) {
                DownloadService.cancel(context)
            }
            for (id in taskIds) {
                val task = dao.getTaskById(id)
                if (task != null) {
                    task.filePath?.let { path ->
                        val f = java.io.File(path)
                        if (f.exists()) f.delete()
                    }
                    dao.delete(task)
                }
            }
            Toast.makeText(context, "${taskIds.size} video kaldırıldı", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearAllTasks(context: Context) {
        viewModelScope.launch {
            DownloadService.cancel(context)
            dao.deleteAll()
            Toast.makeText(context, "Kuyruk ve indirilenler temizlendi", Toast.LENGTH_SHORT).show()
        }
    }

    // ------------------------------------------------------------------------
    // General Navigation & Settings
    // ------------------------------------------------------------------------
    fun handleBackFromList(): Boolean {
        val origin = _originTab.value
        if (origin != null) {
            _originTab.value = null
            _currentTab.value = origin
            return true
        }
        if (_currentScreen.value != AppScreen.HOME) {
            _currentScreen.value = AppScreen.HOME
            return true
        }
        return false
    }

    fun selectTab(tab: MainTab) {
        _currentTab.value = tab
    }

    fun navigateTo(screen: AppScreen) {
        val current = _currentScreen.value
        if (current != screen) {
            _screenBackStack.value = _screenBackStack.value + current
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        val stack = _screenBackStack.value
        if (stack.isNotEmpty()) {
            val previous = stack.last()
            _screenBackStack.value = stack.dropLast(1)
            _currentScreen.value = previous
            return true
        } else if (_currentScreen.value != AppScreen.HOME) {
            _currentScreen.value = AppScreen.HOME
            return true
        }
        return false
    }

    fun openSettings() {
        _showSettingsDialog.value = true
    }

    fun closeSettings() {
        _showSettingsDialog.value = false
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        preferencesManager.themeMode = mode
    }

    fun toggleKeepScreenOn(enabled: Boolean) {
        _keepScreenOn.value = enabled
        preferencesManager.keepScreenOn = enabled
    }

    fun updateM3uInputText(text: String) {
        _m3uInputText.value = text
    }

    fun parseManualInput(context: Context) {
        val text = _m3uInputText.value.trim()
        if (text.isEmpty()) {
            Toast.makeText(context, "Lütfen bir M3U içeriği veya linki girin", Toast.LENGTH_SHORT).show()
            return
        }
        val items = M3uParser.parse(text)
        if (items.isEmpty()) {
            Toast.makeText(context, "Geçerli bir akış bağlantısı bulunamadı", Toast.LENGTH_LONG).show()
            return
        }
        _parsedItems.value = items
        _currentScreen.value = AppScreen.LIST
        Toast.makeText(context, "${items.size} video/kanal çözümlendi", Toast.LENGTH_SHORT).show()
    }

    fun quickPasteFromClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = clipboard?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString()?.trim() ?: ""
            if (text.isNotEmpty()) {
                val items = M3uParser.parse(text)
                if (items.isNotEmpty()) {
                    _m3uInputText.value = text
                    _parsedItems.value = items
                    _currentScreen.value = AppScreen.LIST
                    Toast.makeText(context, "Panodan ${items.size} video çözümlendi", Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }
        Toast.makeText(context, "Panoda geçerli bir M3U içeriği bulunamadı", Toast.LENGTH_SHORT).show()
    }

    fun toggleItemSelection(id: String) {
        _parsedItems.value = _parsedItems.value.map {
            if (it.id == id) it.copy(isSelected = !it.isSelected) else it
        }
    }

    fun selectAll(select: Boolean) {
        _parsedItems.value = _parsedItems.value.map {
            it.copy(isSelected = select)
        }
    }

    fun downloadSelected(context: Context) {
        val selected = _parsedItems.value.filter { it.isSelected }
        if (selected.isEmpty()) {
            Toast.makeText(context, "Lütfen en az bir video seçin", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            val newTasks = selected.map { item ->
                DownloadTask(
                    id = item.id,
                    title = item.name,
                    streamUrl = item.streamUrl,
                    logoUrl = item.logoUrl,
                    httpReferrer = item.httpReferrer,
                    httpUserAgent = item.httpUserAgent,
                    targetFileName = M3uParser.sanitizeFileName(item.name),
                    status = DownloadStatus.PENDING
                )
            }
            dao.insertAll(newTasks)
            DownloadService.startQueue(context)
            Toast.makeText(context, "${newTasks.size} video indirme kuyruğuna eklendi", Toast.LENGTH_SHORT).show()
        }
    }

    fun downloadSingle(context: Context, item: M3uItem) {
        viewModelScope.launch {
            val task = DownloadTask(
                id = item.id,
                title = item.name,
                streamUrl = item.streamUrl,
                logoUrl = item.logoUrl,
                httpReferrer = item.httpReferrer,
                httpUserAgent = item.httpUserAgent,
                targetFileName = M3uParser.sanitizeFileName(item.name),
                status = DownloadStatus.PENDING
            )
            dao.insert(task)
            DownloadService.startQueue(context)
            Toast.makeText(context, "${item.name} kuyruğa eklendi", Toast.LENGTH_SHORT).show()
        }
    }

    fun pauseDownloads(context: Context) {
        DownloadService.pause(context)
    }

    fun resumeDownloads(context: Context) {
        DownloadService.resume(context)
    }

    fun stopDownloads(context: Context) {
        DownloadService.cancel(context)
        Toast.makeText(context, "İndirme durduruldu", Toast.LENGTH_SHORT).show()
    }

    fun openDownloadedVideo(context: Context, task: DownloadTask) {
        task.filePath?.let { path ->
            StorageHelper.openInExternalPlayer(context, java.io.File(path))
        } ?: run {
            Toast.makeText(context, "Dosya yolu bulunamadı", Toast.LENGTH_SHORT).show()
        }
    }

    fun openFolder(context: Context) {
        StorageHelper.openDownloadsFolder(context)
    }

    // ------------------------------------------------------------------------
    // Downloaded Media Library Management
    // ------------------------------------------------------------------------
    fun refreshDownloadedMedia(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val moviesDir = StorageHelper.getMoviesDirectory(context)
            val seriesDir = StorageHelper.getSeriesDirectory(context)

            // Scan Movies
            val movieFiles = moviesDir.listFiles { file ->
                file.isFile && (file.extension.equals("mp4", true) || file.extension.equals("ts", true) || file.extension.equals("mkv", true))
            }?.sortedByDescending { it.lastModified() } ?: emptyList()

            val allCompletedTasks = dao.getTasksByStatuses(listOf(DownloadStatus.COMPLETED))
            val tasksByPath = allCompletedTasks.associateBy { it.filePath }
            val tasksByName = allCompletedTasks.associateBy { it.targetFileName }

            val movies = movieFiles.map { f ->
                val match = tasksByPath[f.absolutePath] ?: tasksByName[f.nameWithoutExtension]
                val movieTitle = match?.title ?: f.nameWithoutExtension.replace("_", " ")
                val moviePoster = match?.logoUrl
                    ?: _moviesList.value.find {
                        it.title.equals(movieTitle, ignoreCase = true) ||
                        it.cleanTitle.equals(movieTitle, ignoreCase = true)
                    }?.posterUrl

                DownloadedMovie(
                    file = f,
                    title = movieTitle,
                    sizeBytes = f.length(),
                    lastModified = f.lastModified(),
                    correspondingTask = match,
                    posterUrl = moviePoster
                )
            }

            // Scan Series & auto cleanup empty folders
            StorageHelper.cleanEmptySeriesFolders(context)

            val seriesFolders = seriesDir.listFiles { file -> file.isDirectory } ?: emptyArray()
            val seriesList = mutableListOf<DownloadedSeriesFolder>()

            for (folder in seriesFolders) {
                val epFiles = folder.listFiles { file ->
                    file.isFile && (file.extension.equals("mp4", true) || file.extension.equals("ts", true) || file.extension.equals("mkv", true))
                }?.sortedBy { it.name } ?: emptyList()

                if (epFiles.isEmpty()) {
                    folder.delete()
                } else {
                    val episodes = epFiles.map { epFile ->
                        val match = tasksByPath[epFile.absolutePath] ?: tasksByName[epFile.nameWithoutExtension]
                        DownloadedEpisode(
                            file = epFile,
                            episodeName = match?.episodeName ?: epFile.nameWithoutExtension.replace("${folder.name} - ", "").replace("_", " "),
                            sizeBytes = epFile.length(),
                            lastModified = epFile.lastModified(),
                            correspondingTask = match,
                            posterUrl = match?.logoUrl
                        )
                    }

                    val seriesPoster = episodes.firstOrNull { !it.correspondingTask?.logoUrl.isNullOrEmpty() }?.correspondingTask?.logoUrl
                        ?: _seriesList.value.find { it.seriesName.equals(folder.name, ignoreCase = true) }?.posterUrl

                    seriesList.add(
                        DownloadedSeriesFolder(
                            seriesName = folder.name,
                            folder = folder,
                            episodes = episodes,
                            totalSizeBytes = episodes.sumOf { it.sizeBytes },
                            posterUrl = seriesPoster
                        )
                    )
                }
            }

            _downloadedMovies.value = movies
            _downloadedSeries.value = seriesList.sortedBy { it.seriesName }
        }
    }

    fun deleteDownloadedMovie(context: Context, movie: DownloadedMovie) {
        viewModelScope.launch(Dispatchers.IO) {
            StorageHelper.deleteFileAndCleanEmptyFolder(context, movie.file)
            movie.correspondingTask?.let { dao.delete(it) }
            refreshDownloadedMedia(context)
        }
    }

    fun deleteDownloadedEpisode(context: Context, folder: DownloadedSeriesFolder, episode: DownloadedEpisode) {
        viewModelScope.launch(Dispatchers.IO) {
            StorageHelper.deleteFileAndCleanEmptyFolder(context, episode.file)
            episode.correspondingTask?.let { dao.delete(it) }
            refreshDownloadedMedia(context)
        }
    }

    fun deleteDownloadedSeriesFolder(context: Context, folder: DownloadedSeriesFolder) {
        viewModelScope.launch(Dispatchers.IO) {
            folder.folder.listFiles()?.forEach { file ->
                val match = dao.getTasksByStatuses(listOf(DownloadStatus.COMPLETED)).find { it.filePath == file.absolutePath }
                file.delete()
                match?.let { dao.delete(it) }
            }
            folder.folder.delete()
            refreshDownloadedMedia(context)
        }
    }

    // ------------------------------------------------------------------------
    // In-App Video Player State & Actions
    // ------------------------------------------------------------------------
    fun playDownloadedMovie(movie: DownloadedMovie) {
        _activePlayerMedia.value = PlayerMediaItem(
            title = movie.title,
            mediaUri = movie.file.absolutePath,
            isStream = false,
            isSeries = false
        )
    }

    fun playDownloadedEpisode(folder: DownloadedSeriesFolder, episode: DownloadedEpisode) {
        val selectedIndex = folder.episodes.indexOf(episode).coerceAtLeast(0)
        val rawList = folder.episodes.mapIndexed { index, ep ->
            PlayerMediaItem(
                title = "${folder.seriesName} - ${ep.episodeName}",
                mediaUri = ep.file.absolutePath,
                isStream = false,
                isSeries = true,
                seriesName = folder.seriesName,
                episodeName = ep.episodeName,
                currentIndex = index
            )
        }
        val fullPlaylist = rawList.map { it.copy(playlist = rawList) }
        _activePlayerMedia.value = fullPlaylist[selectedIndex]
    }

    fun playStreamMovie(movie: CatalogMovieItem, resume: Boolean = true) {
        viewModelScope.launch {
            val savedPos = if (resume) getSavedPosition(movie.id) else 0L
            _activePlayerMedia.value = PlayerMediaItem(
                title = movie.cleanTitle,
                mediaUri = movie.streamUrl,
                isStream = true,
                isSeries = false,
                initialPositionMs = savedPos,
                mediaId = movie.id,
                posterUrl = movie.posterUrl
            )
        }
    }

    fun playStreamEpisode(
        series: CatalogSeries,
        episode: CatalogEpisodeItem,
        customPlaylist: List<CatalogEpisodeItem> = series.episodes,
        resume: Boolean = true
    ) {
        viewModelScope.launch {
            val savedPos = if (resume) getSavedPosition(episode.id) else 0L
            val selectedIndex = customPlaylist.indexOf(episode).coerceAtLeast(0)
            val rawItems = customPlaylist.mapIndexed { index, ep ->
                PlayerMediaItem(
                    title = "${series.seriesName} - ${ep.episodeNumberText}",
                    mediaUri = ep.streamUrl,
                    isStream = true,
                    isSeries = true,
                    seriesName = series.seriesName,
                    episodeName = ep.episodeNumberText,
                    currentIndex = index,
                    mediaId = ep.id,
                    posterUrl = series.posterUrl
                )
            }
            val fullPlaylist = rawItems.mapIndexed { i, itm ->
                itm.copy(
                    playlist = rawItems,
                    initialPositionMs = if (i == selectedIndex) savedPos else 0L
                )
            }
            _activePlayerMedia.value = fullPlaylist[selectedIndex]
        }
    }

    fun resumeFromProgress(progress: PlaybackProgressEntity) {
        _activePlayerMedia.value = PlayerMediaItem(
            title = progress.title,
            mediaUri = progress.streamUrl,
            isStream = true,
            isSeries = progress.isSeries,
            seriesName = progress.seriesName,
            episodeName = progress.episodeName,
            initialPositionMs = progress.positionMs,
            mediaId = progress.mediaId,
            posterUrl = progress.posterUrl
        )
    }

    fun savePlaybackProgress(
        mediaId: String,
        title: String,
        posterUrl: String?,
        streamUrl: String,
        isSeries: Boolean,
        seriesName: String? = null,
        episodeName: String? = null,
        positionMs: Long,
        durationMs: Long
    ) {
        if (positionMs <= 1000L || durationMs <= 0L) return
        viewModelScope.launch(Dispatchers.IO) {
            val percent = positionMs.toFloat() / durationMs.toFloat()
            if (percent >= 0.96f) {
                playbackProgressDao.deleteProgress(mediaId)
            } else {
                playbackProgressDao.insertOrUpdate(
                    PlaybackProgressEntity(
                        mediaId = mediaId,
                        title = title,
                        posterUrl = posterUrl,
                        streamUrl = streamUrl,
                        isSeries = isSeries,
                        seriesName = seriesName,
                        episodeName = episodeName,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun getSavedPosition(mediaId: String): Long {
        return withContext(Dispatchers.IO) {
            playbackProgressDao.getProgressById(mediaId)?.positionMs ?: 0L
        }
    }

    fun clearPlaybackProgress(mediaId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            playbackProgressDao.deleteProgress(mediaId)
        }
    }

    fun playStreamSeries(series: CatalogSeries) {
        val firstEp = series.episodes.firstOrNull() ?: return
        playStreamEpisode(series, firstEp)
    }

    fun playNextEpisode(nextItem: PlayerMediaItem) {
        val current = _activePlayerMedia.value
        val fullList = if (nextItem.playlist.isNotEmpty()) nextItem.playlist else current?.playlist ?: emptyList()
        _activePlayerMedia.value = nextItem.copy(playlist = fullList)
    }

    fun closePlayer() {
        _activePlayerMedia.value = null
    }

    fun toggleMovieFavorite(context: Context, movie: CatalogMovieItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val isFav = favoriteMovieIds.value.contains(movie.id)
            if (isFav) {
                favoriteDao.deleteById(movie.id)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "\"${movie.cleanTitle}\" favorilerden çıkarıldı", Toast.LENGTH_SHORT).show()
                }
            } else {
                favoriteDao.insert(
                    FavoriteEntity(
                        id = movie.id,
                        isSeries = false,
                        title = movie.cleanTitle,
                        posterUrl = movie.posterUrl
                    )
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "\"${movie.cleanTitle}\" favorilere eklendi", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun toggleSeriesFavorite(context: Context, series: CatalogSeries) {
        viewModelScope.launch(Dispatchers.IO) {
            val isFav = favoriteSeriesIds.value.contains(series.id)
            if (isFav) {
                favoriteDao.deleteById(series.id)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "\"${series.seriesName}\" favorilerden çıkarıldı", Toast.LENGTH_SHORT).show()
                }
            } else {
                favoriteDao.insert(
                    FavoriteEntity(
                        id = series.id,
                        isSeries = true,
                        title = series.seriesName,
                        posterUrl = series.posterUrl
                    )
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "\"${series.seriesName}\" favorilere eklendi", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
