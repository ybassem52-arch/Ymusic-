package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.service.PlaybackManager
import com.example.ui.theme.DarkBackground
import com.example.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.launch
import java.io.File

// --- Sophisticated Dark Theme Colors ---
val PremiumRed = Color(0xFFD0BCFF)
val PremiumSlate = Color(0xFF1C1B1F)
val SurfaceCard = Color(0xFF2B2930)
val SecondaryText = Color(0xFFCAC4D0)
val AccentCyan = Color(0xFFD0BCFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(0) } // 0: Explore, 1: Library
    var searchQuery by remember { mutableStateOf("") }
    
    // States from PlaybackManager
    val currentSong by PlaybackManager.currentSong.collectAsStateWithLifecycle()
    val isPlaying by PlaybackManager.isPlaying.collectAsStateWithLifecycle()
    
    // Dialog state for playlist selection
    var showPlaylistDialogForSong by remember { mutableStateOf<Song?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFD0BCFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "y",
                                color = Color(0xFF381E72),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ymusic",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE6E1E5)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.toggleDataSaver()
                            val msg = if (viewModel.isDataSaverEnabled) "تم تفعيل موفر البيانات" else "تم إيقاف موفر البيانات"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("data_saver_toggle")
                    ) {
                        Icon(
                            imageVector = if (viewModel.isDataSaverEnabled) Icons.Default.NetworkWifi1Bar else Icons.Default.NetworkWifi,
                            contentDescription = "Data Saver Mode",
                            tint = if (viewModel.isDataSaverEnabled) AccentCyan else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        bottomBar = {
            Column {
                // Persistent Player Mini bar
                currentSong?.let { song ->
                    MiniPlayerBar(
                        song = song,
                        isPlaying = isPlaying,
                        onPlayPause = { PlaybackManager.togglePlayPause(context) },
                        onAddToPlaylist = { showPlaylistDialogForSong = song }
                    )
                }

                // Standard Tab Selection bottom navigation
                NavigationBar(
                    containerColor = PremiumSlate,
                    tonalElevation = 8.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        label = { Text("استكشاف", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PremiumRed,
                            selectedTextColor = PremiumRed,
                            unselectedIconColor = SecondaryText,
                            unselectedTextColor = SecondaryText,
                            indicatorColor = PremiumSlate
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        label = { Text("مكتبتي", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PremiumRed,
                            selectedTextColor = PremiumRed,
                            unselectedIconColor = SecondaryText,
                            unselectedTextColor = SecondaryText,
                            indicatorColor = PremiumSlate
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        val downloadedSongs by viewModel.downloadedSongs.collectAsStateWithLifecycle()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Status Badges Row (Data Saver & Offline tracker badges)
                StatusBadgesRow(
                    isDataSaverEnabled = viewModel.isDataSaverEnabled,
                    offlineCount = downloadedSongs.size,
                    onDataSaverToggle = {
                        viewModel.toggleDataSaver()
                        val msg = if (viewModel.isDataSaverEnabled) "تم تفعيل موفر البيانات" else "تم إيقاف موفر البيانات"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                )

                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    when (currentTab) {
                        0 -> ExploreTab(
                            viewModel = viewModel,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            onSongPlay = { song -> viewModel.playSong(context, song) },
                            onSongAddToPlaylist = { song -> showPlaylistDialogForSong = song }
                        )
                        1 -> LibraryTab(
                            viewModel = viewModel,
                            onCreatePlaylistClick = { showCreatePlaylistDialog = true },
                            onSongPlay = { song -> viewModel.playSong(context, song) },
                            onSongAddToPlaylist = { song -> showPlaylistDialogForSong = song }
                        )
                    }
                }
            }

            // Expanding Full Immersive Player Sheet (Overlays Content beautifully)
            currentSong?.let { activeSong ->
                var isPlayerExpanded by remember { mutableStateOf(false) }
                
                // Slim toggle handle if player is not expanded to let user expand it
                if (!isPlayerExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .align(Alignment.BottomCenter)
                            .clickable { isPlayerExpanded = true }
                    )
                }

                AnimatedVisibility(
                    visible = isPlayerExpanded,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    ImmersivePlayerView(
                        song = activeSong,
                        isPlaying = isPlaying,
                        viewModel = viewModel,
                        onCollapse = { isPlayerExpanded = false },
                        onAddToPlaylist = { showPlaylistDialogForSong = activeSong }
                    )
                }
            }
        }
    }

    // --- Create Playlist Dialog ---
    if (showCreatePlaylistDialog) {
        var playlistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("إنشاء قائمة تشغيل جديدة", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("اسم القائمة") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PremiumRed,
                        focusedLabelColor = PremiumRed,
                        unfocusedLabelColor = SecondaryText,
                        unfocusedBorderColor = SecondaryText
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("playlist_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            viewModel.createPlaylist(playlistName)
                            showCreatePlaylistDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumRed),
                    modifier = Modifier.testTag("playlist_create_confirm")
                ) {
                    Text("إنشاء", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("إلغاء", color = Color.White)
                }
            },
            containerColor = PremiumSlate
        )
    }

    // --- Add to Playlist Dialog ---
    showPlaylistDialogForSong?.let { song ->
        val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()
        Dialog(onDismissRequest = { showPlaylistDialogForSong = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = PremiumSlate),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "إضافة الأغنية إلى قائمة تشغيل",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = song.title,
                        color = PremiumRed,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (playlists.isEmpty()) {
                        Text(
                            text = "لا توجد قوائم تشغيل حالية. يرجى إنشاء قائمة أولاً.",
                            color = SecondaryText,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                showCreatePlaylistDialog = true
                                showPlaylistDialogForSong = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إنشاء قائمة تشغيل", color = Color.White)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                        ) {
                            items(playlists) { playlist ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            viewModel.addSongToPlaylist(playlist.id, song)
                                            Toast.makeText(context, "تمت الإضافة إلى ${playlist.name}", Toast.LENGTH_SHORT).show()
                                            showPlaylistDialogForSong = null
                                        },
                                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = PremiumRed)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(playlist.name, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showPlaylistDialogForSong = null }) {
                        Text("إلغاء", color = Color.White)
                    }
                }
            }
        }
    }
}

// --- EXPLORE TAB ---
@Composable
fun ExploreTab(
    viewModel: MusicViewModel,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSongPlay: (Song) -> Unit,
    onSongAddToPlaylist: (Song) -> Unit
) {
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val trendingSongs by viewModel.trendingSongs.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // High-fidelity search bar with modern accent colors
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                onSearchQueryChange(it)
                viewModel.search(it)
            },
            placeholder = { Text("ابحث عن الأغاني أو ألبومات من YouTube Music", color = SecondaryText, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = PremiumRed) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        onSearchQueryChange("")
                        viewModel.search("")
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PremiumRed,
                focusedLabelColor = PremiumRed,
                unfocusedBorderColor = PremiumSlate,
                unfocusedContainerColor = PremiumSlate,
                focusedContainerColor = PremiumSlate,
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // YouTube Music direct link importer
        var linkImportQuery by remember { mutableStateOf("") }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = linkImportQuery,
                onValueChange = { linkImportQuery = it },
                placeholder = { Text("استيراد رابط أغنية YouTube مباشرة", color = SecondaryText, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = PremiumSlate,
                    unfocusedContainerColor = PremiumSlate,
                    focusedContainerColor = PremiumSlate,
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("link_import_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (linkImportQuery.isNotBlank()) {
                        // Import track from link (simulation of beautiful extractor)
                        val songId = "yt_link_" + System.currentTimeMillis()
                        val importedSong = Song(
                            id = songId,
                            title = "أغنية مستوردة " + linkImportQuery.takeLast(6),
                            artist = "استيراد مباشر",
                            duration = "03:45",
                            thumbnailUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=500&auto=format&fit=crop&q=80",
                            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3"
                        )
                        viewModel.playSong(context, importedSong)
                        linkImportQuery = ""
                        Toast.makeText(context, "تم استيراد وتشغيل الأغنية بنجاح", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("import_button")
            ) {
                Text("جلب", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PremiumRed)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (searchQuery.isNotEmpty()) {
                    item {
                        Text(
                            text = "نتائج البحث عن: $searchQuery",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(searchResults) { song ->
                        SongItem(
                            song = song,
                            onPlay = { onSongPlay(song) },
                            onFavoriteToggle = { viewModel.toggleFavorite(song) },
                            onDownloadClick = { viewModel.downloadSong(song) },
                            onAddToPlaylist = { onSongAddToPlaylist(song) },
                            downloadProgress = viewModel.downloadProgressMap[song.id]
                        )
                    }
                } else {
                    item {
                        // --- FEATURED HERO CARD (SOPHISTICATED DARK DESIGN) ---
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.6f)
                                .padding(bottom = 20.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF4A4458), Color(0xFF2B2930))
                                        )
                                    )
                                    .padding(24.dp)
                            ) {
                                // Now Streaming tag on Top-Right
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(50))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(50))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "NOW STREAMING",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }

                                // Inner text & action buttons aligned to Bottom-Left
                                Column(
                                    modifier = Modifier.align(Alignment.BottomStart)
                                ) {
                                    Text(
                                        text = "YouTube Music Library",
                                        color = Color(0xFFD0BCFF),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        style = androidx.compose.ui.text.TextStyle(
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Late Night\nLo-fi Beats",
                                        color = Color(0xFFE6E1E5),
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 34.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Play button (lavender pill with deep purple icon)
                                        Card(
                                            shape = CircleShape,
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFD0BCFF)),
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clickable {
                                                    if (trendingSongs.isNotEmpty()) {
                                                        onSongPlay(trendingSongs.first())
                                                    }
                                                }
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Play Beats",
                                                    tint = Color(0xFF381E72),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        // Shuffle button (translucent light background)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(Color.White.copy(alpha = 0.1f))
                                                .clickable {
                                                    if (trendingSongs.isNotEmpty()) {
                                                        viewModel.playAll(context, trendingSongs.shuffled())
                                                    }
                                                }
                                                .padding(horizontal = 24.dp, vertical = 12.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Shuffle,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "Shuffle",
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("أغاني شائعة (YouTube Charts)", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                            IconButton(onClick = { viewModel.fetchTrending() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh Charts", tint = PremiumRed)
                            }
                        }
                    }
                    items(trendingSongs) { song ->
                        SongItem(
                            song = song,
                            onPlay = { onSongPlay(song) },
                            onFavoriteToggle = { viewModel.toggleFavorite(song) },
                            onDownloadClick = { viewModel.downloadSong(song) },
                            onAddToPlaylist = { onSongAddToPlaylist(song) },
                            downloadProgress = viewModel.downloadProgressMap[song.id]
                        )
                    }
                }
            }
        }
    }
}

// --- LIBRARY TAB ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryTab(
    viewModel: MusicViewModel,
    onCreatePlaylistClick: () -> Unit,
    onSongPlay: (Song) -> Unit,
    onSongAddToPlaylist: (Song) -> Unit
) {
    val favorites by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val downloads by viewModel.downloadedSongs.collectAsStateWithLifecycle()
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedSubTab by remember { mutableStateOf(0) } // 0: Favorites, 1: Downloads, 2: Playlists

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Sub-tabs row inside Library
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("المفضلة", "التحميلات", "قوائم التشغيل").forEachIndexed { index, title ->
                val isSelected = selectedSubTab == index
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedSubTab = index }
                        .border(
                            width = 1.dp,
                            color = Color(0xFF49454F),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF4A4458) else Color(0xFF2B2930)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFFCAC4D0),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Sub tab content
        when (selectedSubTab) {
            0 -> {
                if (favorites.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.FavoriteBorder,
                        message = "لا توجد أي أغاني في المفضلة حالياً.\nاضغط على علامة القلب في استكشاف لإضافتها!"
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مفضلاتك", color = Color.White, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { viewModel.playAll(context, favorites) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AccentCyan)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تشغيل الكل", color = AccentCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(favorites) { song ->
                            SongItem(
                                song = song,
                                onPlay = { onSongPlay(song) },
                                onFavoriteToggle = { viewModel.toggleFavorite(song) },
                                onDownloadClick = { viewModel.downloadSong(song) },
                                onAddToPlaylist = { onSongAddToPlaylist(song) },
                                downloadProgress = viewModel.downloadProgressMap[song.id]
                            )
                        }
                    }
                }
            }
            1 -> {
                if (downloads.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.CloudDownload,
                        message = "لم يتم تحميل أي أغنية حتى الآن.\nقم بتحميل أغانيك المفضلة للاستماع إليها دون إنترنت وتوفير باقة البيانات!"
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("التحميلات الجاهزة دون إنترنت", color = Color.White, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { viewModel.playAll(context, downloads) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AccentCyan)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تشغيل الكل", color = AccentCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(downloads) { song ->
                            SongItem(
                                song = song,
                                onPlay = { onSongPlay(song) },
                                onFavoriteToggle = { viewModel.toggleFavorite(song) },
                                onDownloadClick = { viewModel.deleteDownload(song) }, // Clicking download action deletes the offline file
                                onAddToPlaylist = { onSongAddToPlaylist(song) },
                                downloadProgress = viewModel.downloadProgressMap[song.id]
                            )
                        }
                    }
                }
            }
            2 -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Button(
                        onClick = onCreatePlaylistClick,
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("create_playlist_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إنشاء قائمة تشغيل جديدة", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    if (playlists.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.PlaylistAdd,
                            message = "لم تقم بإنشاء أي قائمة تشغيل حتى الآن."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(playlists) { playlist ->
                                var isExpanded by remember { mutableStateOf(false) }
                                val songsInPlaylist by viewModel.allPlaylists.let {
                                    viewModel.getSongsInPlaylist(playlist.id)
                                }.collectAsStateWithLifecycle(initialValue = emptyList())

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { isExpanded = !isExpanded },
                                            onLongClick = {
                                                viewModel.deletePlaylist(playlist.id)
                                                Toast.makeText(context, "تم حذف القائمة ${playlist.name}", Toast.LENGTH_SHORT).show()
                                            }
                                        ),
                                    colors = CardDefaults.cardColors(containerColor = PremiumSlate)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = PremiumRed, modifier = Modifier.size(32.dp))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(playlist.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                                    Text("${songsInPlaylist.size} أغنية • اضغط طويلًا للحذف", color = SecondaryText, fontSize = 12.sp)
                                                }
                                            }
                                            Row {
                                                if (songsInPlaylist.isNotEmpty()) {
                                                    IconButton(onClick = { viewModel.playAll(context, songsInPlaylist) }) {
                                                        Icon(Icons.Default.PlayArrow, contentDescription = "Play Playlist", tint = AccentCyan)
                                                    }
                                                }
                                                Icon(
                                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = null,
                                                    tint = Color.White
                                                )
                                            }
                                        }

                                        if (isExpanded && songsInPlaylist.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Divider(color = SurfaceCard)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            songsInPlaylist.forEach { song ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp)
                                                        .clickable { viewModel.playSong(context, song) },
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                        AsyncImage(
                                                            model = song.thumbnailUrl,
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .clip(RoundedCornerShape(4.dp))
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Column {
                                                            Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                                                            Text(song.artist, color = SecondaryText, fontSize = 12.sp)
                                                        }
                                                    }
                                                    IconButton(onClick = { viewModel.removeSongFromPlaylist(playlist.id, song) }) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SINGLE SONG LIST ITEM ---
@Composable
fun SongItem(
    song: Song,
    onPlay: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDownloadClick: () -> Unit,
    onAddToPlaylist: () -> Unit,
    downloadProgress: Float?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        colors = CardDefaults.cardColors(containerColor = PremiumSlate),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art
            Box {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                if (song.isDownloaded) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(Color.Black.copy(alpha = 0.7f), shape = CircleShape)
                            .padding(2.dp)
                    ) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Song Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = SecondaryText,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.duration,
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }

            // Quick action icons
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Add to playlist
                IconButton(onClick = onAddToPlaylist) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to playlist", tint = Color.White, modifier = Modifier.size(22.dp))
                }

                // Download Toggle
                if (downloadProgress != null) {
                    CircularProgressIndicator(
                        progress = downloadProgress,
                        color = AccentCyan,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    IconButton(onClick = onDownloadClick) {
                        Icon(
                            imageVector = if (song.isDownloaded) Icons.Default.CloudDone else Icons.Default.FileDownload,
                            contentDescription = "Download Toggle",
                            tint = if (song.isDownloaded) AccentCyan else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Favorite Toggle
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite Toggle",
                        tint = if (song.isFavorite) PremiumRed else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// --- PERSISTENT MINI PLAYER BAR ---
@Composable
fun MiniPlayerBar(
    song: Song,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .border(1.dp, Color(0xFF49454F).copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // Tiny progress track running along the top of the mini player
            val progress by PlaybackManager.progress.collectAsStateWithLifecycle()
            val duration by PlaybackManager.duration.collectAsStateWithLifecycle()
            val progressFraction = if (duration > 0) progress.toFloat() / duration.toFloat() else 0f
            LinearProgressIndicator(
                progress = progressFraction,
                color = PremiumRed,
                trackColor = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = song.title,
                            color = Color(0xFFE6E1E5),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            color = SecondaryText,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(onClick = { PlaybackManager.playPrevious(context) }) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color(0xFFE6E1E5), modifier = Modifier.size(22.dp))
                    }
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.testTag("mini_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = PremiumRed,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(onClick = { PlaybackManager.playNext(context) }) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color(0xFFE6E1E5), modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = onAddToPlaylist) {
                        Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to playlist", tint = Color(0xFFE6E1E5), modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

// --- STATUS BADGES ROW ---
@Composable
fun StatusBadgesRow(
    isDataSaverEnabled: Boolean,
    offlineCount: Int,
    onDataSaverToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Data Saver Badge (Interactive Toggle)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF2B2930))
                .border(1.dp, Color(0xFF49454F), RoundedCornerShape(50))
                .clickable { onDataSaverToggle() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = if (isDataSaverEnabled) Color(0xFFD0BCFF) else Color(0xFFCAC4D0),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isDataSaverEnabled) "Data Saver Active" else "Data Saver Off",
                    color = Color(0xFFCAC4D0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Offline Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF2B2930))
                .border(1.dp, Color(0xFF49454F), RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "$offlineCount Offline",
                    color = Color(0xFFCAC4D0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// --- IMMERSIVE FULL-SCREEN PLAYER VIEW ---
@Composable
fun ImmersivePlayerView(
    song: Song,
    isPlaying: Boolean,
    viewModel: MusicViewModel,
    onCollapse: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    val context = LocalContext.current
    val progress by PlaybackManager.progress.collectAsStateWithLifecycle()
    val duration by PlaybackManager.duration.collectAsStateWithLifecycle()
    
    // Lyrics States
    var showLyrics by remember { mutableStateOf(false) }
    val lyrics by viewModel.activeLyrics.collectAsStateWithLifecycle()
    val lyricsLoading by viewModel.lyricsLoading.collectAsStateWithLifecycle()

    // Smooth cover rotating animation when playing
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(PremiumSlate, DarkBackground)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse, modifier = Modifier.testTag("collapse_player")) {
                    Icon(Icons.Default.ExpandMore, contentDescription = "Collapse", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "تشغيل الآن من YMUSIC",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PremiumRed,
                        letterSpacing = 1.sp
                    )
                    if (viewModel.isDataSaverEnabled) {
                        Text(
                            text = "موفر البيانات مفعل • جودة اقتصادية",
                            fontSize = 10.sp,
                            color = AccentCyan
                        )
                    }
                }
                IconButton(onClick = onAddToPlaylist) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to playlist", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            // Central Panel (Rotating Vinyl/Cover art OR Lyrics Screen)
            Crossfade(targetState = showLyrics, label = "lyrics_fade", modifier = Modifier.weight(1f)) { lyricsActive ->
                if (lyricsActive) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showLyrics = false }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("العودة للألبوم", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (lyricsLoading) {
                            CircularProgressIndicator(color = PremiumRed)
                        } else {
                            Text(
                                text = lyrics ?: "لا تتوفر كلمات لهذه الأغنية حالياً.",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                lineHeight = 28.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    ) {
                        // High fidelity rotating vinyl sleeve background
                        Box(
                            modifier = Modifier
                                .size(260.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.3f))
                        )

                        AsyncImage(
                            model = song.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(240.dp)
                                .graphicsLayer {
                                    if (isPlaying) {
                                        rotationZ = rotationAngle
                                    }
                                }
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )

                        // Center spindle hole for authenticity
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkBackground)
                        )
                    }
                }
            }

            // Titles & Artist
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = song.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.artist,
                    fontSize = 16.sp,
                    color = SecondaryText,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Seek bar / Audio Progress
            Column(modifier = Modifier.fillMaxWidth()) {
                val progressFraction = if (duration > 0) progress.toFloat() / duration.toFloat() else 0f
                Slider(
                    value = progressFraction,
                    onValueChange = { fraction ->
                        val targetMs = (fraction * duration).toLong()
                        PlaybackManager.seekTo(targetMs)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = PremiumRed,
                        activeTrackColor = PremiumRed,
                        inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(progress),
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatTime(duration),
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                }
            }

            // Media control buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    showLyrics = true
                    viewModel.loadLyrics(song)
                }) {
                    Icon(
                        imageVector = Icons.Default.Lyrics,
                        contentDescription = "Lyrics Toggle",
                        tint = if (showLyrics) AccentCyan else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(onClick = { PlaybackManager.playPrevious(context) }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(36.dp))
                }

                // Centered large play pause button
                Card(
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = PremiumRed),
                    modifier = Modifier
                        .size(68.dp)
                        .clickable { PlaybackManager.togglePlayPause(context) }
                        .testTag("player_play_pause")
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                IconButton(onClick = { PlaybackManager.playNext(context) }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                }

                IconButton(onClick = {
                    PlaybackManager.stop(context)
                    onCollapse()
                }) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

// --- EMPTY PLACEHOLDER STATE ---
@Composable
fun EmptyStateView(
    icon: ImageVector,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SecondaryText,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = SecondaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

// Helper to format Millisecond positions into "MM:SS"
fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
