package com.example.musicapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.musicapp.model.SortOption
import com.example.musicapp.model.VisualizerType
import com.example.musicapp.utils.formatTime
import com.example.musicapp.viewmodel.MusicViewModel
import java.util.Locale

private enum class SettingsCategory(val title: String, val icon: ImageVector) {
    ALL("Vše", Icons.Rounded.Tune),
    AUDIO("Zvuk & Časovač", Icons.Rounded.GraphicEq),
    VISUALS("Vizuály", Icons.Rounded.AutoAwesome),
    LIBRARY("Knihovna", Icons.Rounded.LibraryMusic)
}

@Composable
fun SettingsDialog(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val isAuroraEnabled by viewModel.isAuroraEnabled.collectAsState()
    val isCoverBounceEnabled by viewModel.isCoverBounceEnabled.collectAsState()
    val isScreenAlwaysOn by viewModel.isScreenAlwaysOn.collectAsState()
    val isParticlesEnabled by viewModel.isParticlesEnabled.collectAsState()
    val isPulseEnabled by viewModel.isPulseEnabled.collectAsState()
    val isPowerSaveMode by viewModel.isPowerSaveMode.collectAsState()

    val visualizerType by viewModel.visualizerType.collectAsState()
    val isVisualizerMirrored by viewModel.isVisualizerMirrored.collectAsState()
    val visualizerSensitivity by viewModel.visualizerSensitivity.collectAsState()
    val bounceIntensity by viewModel.bounceIntensity.collectAsState()
    val auroraSpeed by viewModel.auroraSpeed.collectAsState()

    val sortOption by viewModel.sortOption.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val skipSilenceEnabled by viewModel.skipSilenceEnabled.collectAsState()
    val sleepTimerRemainingSec by viewModel.sleepTimerRemainingSec.collectAsState()
    val sleepTimerFinishTrack by viewModel.sleepTimerFinishTrack.collectAsState()

    val allSongs by viewModel.allSongs.collectAsState()
    val allAlbums by viewModel.allAlbums.collectAsState()
    val allArtists by viewModel.allArtists.collectAsState()

    var selectedCategory by remember { mutableStateOf(SettingsCategory.ALL) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val accentPurple = Color(0xFFBB86FC)
    val spotifyGreen = Color(0xFF1DB954)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1A1829), Color(0xFF111118), Color(0xFF0C0C12))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(30.dp))
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // --- HLAVIČKA NASTAVENÍ ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentPurple.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Tune, contentDescription = null, tint = accentPurple, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Nastavení",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Přizpůsobení přehrávače a výkonu",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Zavřít", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- FILTROVACÍ PILULKY KATEGORIÍ ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingsCategory.entries.forEach { category ->
                        val isSelected = selectedCategory == category
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) accentPurple else Color.White.copy(alpha = 0.06f))
                                .border(
                                    1.dp,
                                    if (isSelected) accentPurple else Color.White.copy(alpha = 0.08f),
                                    RoundedCornerShape(50)
                                )
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF120D1D) else Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = category.title,
                                color = if (isSelected) Color(0xFF120D1D) else Color.White.copy(alpha = 0.85f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Informační banner při akci (např. Obnovení knihovny / Vymazání cache)
                AnimatedVisibility(visible = statusMessage != null) {
                    statusMessage?.let { msg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(spotifyGreen.copy(alpha = 0.16f))
                                .border(1.dp, spotifyGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = spotifyGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(msg, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- HLAVNÍ OBSAH NASTAVENÍ ---
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. ČASOVAČ VYPNUTÍ (SLEEP TIMER)
                    if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.AUDIO) {
                        val isTimerActive = sleepTimerRemainingSec > 0 || sleepTimerFinishTrack
                        SettingsSectionCard(
                            title = "Časovač vypnutí (Sleep Timer)",
                            icon = Icons.Rounded.Bedtime,
                            badgeText = when {
                                sleepTimerFinishTrack -> "Konec skladby"
                                sleepTimerRemainingSec > 0 -> formatTime(sleepTimerRemainingSec * 1000L)
                                else -> null
                            },
                            badgeColor = spotifyGreen
                        ) {
                            Text(
                                text = "Automaticky zastaví přehrávání po uplynutí zvolené doby.",
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            val presets = listOf(
                                Triple("Vypnuto", 0, false),
                                Triple("15 min", 15, false),
                                Triple("30 min", 30, false),
                                Triple("45 min", 45, false),
                                Triple("60 min", 60, false),
                                Triple("Do konce skladby", 0, true)
                            )
                            presets.chunked(3).forEach { rowPresets ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowPresets.forEach { (label, mins, finishTrack) ->
                                        val selected = when {
                                            finishTrack -> sleepTimerFinishTrack
                                            mins == 0 -> !isTimerActive
                                            else -> !sleepTimerFinishTrack && sleepTimerRemainingSec in ((mins - 1) * 60 + 1)..(mins * 60)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (selected) spotifyGreen else Color.White.copy(alpha = 0.05f))
                                                .border(
                                                    1.dp,
                                                    if (selected) spotifyGreen else Color.White.copy(alpha = 0.08f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    if (mins == 0 && !finishTrack) viewModel.cancelSleepTimer()
                                                    else viewModel.setSleepTimer(mins, finishTrack)
                                                }
                                                .padding(vertical = 9.dp, horizontal = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (selected) Color.Black else Color.White.copy(alpha = 0.85f),
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. ZVUK A PŘEHRÁVÁNÍ
                        SettingsSectionCard(
                            title = "Zvuk a přehrávání",
                            icon = Icons.Rounded.Speed,
                            badgeText = "${String.format(Locale.US, "%.2fx", playbackSpeed).trimEnd('0').trimEnd('.')}x"
                        ) {
                            Text(
                                text = "Rychlost přehrávání",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                speeds.forEach { spd ->
                                    val isSelected = kotlin.math.abs(playbackSpeed - spd) < 0.03f
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) accentPurple else Color.White.copy(alpha = 0.05f))
                                            .clickable { viewModel.setPlaybackSpeed(spd) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${spd}x",
                                            color = if (isSelected) Color(0xFF120D1D) else Color.White.copy(alpha = 0.85f),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 6.dp))
                            SwitchRow(
                                label = "Přeskočit ticho ve skladbách",
                                subtitle = "Automaticky ořízne hluchá místa na začátku a konci nahrávek",
                                checked = skipSilenceEnabled,
                                onCheckedChange = { viewModel.toggleSkipSilence(it) }
                            )
                        }
                    }

                    // 3. VIZUALIZÉR
                    if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.VISUALS) {
                        SettingsSectionCard(
                            title = "Styl audio vizualizéru",
                            icon = Icons.Rounded.Equalizer,
                            badgeText = visualizerType.title
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                VisualizerType.entries.chunked(3).forEach { rowTypes ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowTypes.forEach { type ->
                                            val isSelected = visualizerType == type
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isSelected) accentPurple else Color.White.copy(alpha = 0.05f))
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) accentPurple else Color.White.copy(alpha = 0.08f),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable { viewModel.setVisualizerType(type) }
                                                    .padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = type.title,
                                                    color = if (isSelected) Color(0xFF120D1D) else Color.White.copy(alpha = 0.85f),
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                        repeat(3 - rowTypes.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            SwitchRow(
                                label = "Zrcadlení vizualizéru",
                                subtitle = "Symetrické zobrazení frekvencí od středu",
                                checked = isVisualizerMirrored,
                                onCheckedChange = { viewModel.toggleVisualizerMirroring(it) }
                            )
                            SliderRow(
                                label = "Citlivost vizualizéru",
                                value = visualizerSensitivity,
                                range = 0.5f..2.0f,
                                onValueChange = { viewModel.setVisualizerSensitivity(it) }
                            )
                        }

                        // 4. VIZUÁLNÍ EFEKTY A VÝKON
                        SettingsSectionCard(
                            title = "Vizuální efekty a úspora baterie",
                            icon = Icons.Rounded.AutoAwesome,
                            badgeText = if (isPowerSaveMode) "Úsporný režim" else null,
                            badgeColor = spotifyGreen
                        ) {
                            SwitchRow(
                                label = "Úsporný režim (Battery Saver)",
                                subtitle = "Pozastaví náročné animace na pozadí pro maximální plynulost a výdrž",
                                checked = isPowerSaveMode,
                                accentColor = spotifyGreen,
                                onCheckedChange = { viewModel.togglePowerSaveMode(it) }
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 6.dp))

                            AnimatedVisibility(visible = !isPowerSaveMode) {
                                Column {
                                    SwitchRow(
                                        label = "Efekt polární záře (Aurora)",
                                        subtitle = "Dynamické barevné mlhoviny podle obalu alba",
                                        checked = isAuroraEnabled,
                                        onCheckedChange = { viewModel.toggleAurora(it) }
                                    )
                                    SwitchRow(
                                        label = "Efekt částic (Hvězdný prach)",
                                        subtitle = "Jemné částice stoupající do rytmu hudby",
                                        checked = isParticlesEnabled,
                                        onCheckedChange = { viewModel.toggleParticles(it) }
                                    )
                                    SwitchRow(
                                        label = "Efekt Sonar (Zvukové vlny)",
                                        subtitle = "Pulzující kruhové vlny při basových úderech",
                                        checked = isPulseEnabled,
                                        onCheckedChange = { viewModel.togglePulse(it) }
                                    )
                                    SwitchRow(
                                        label = "Pulzování obalu do rytmu",
                                        subtitle = "Obal alba reaguje na basovou složku skladby",
                                        checked = isCoverBounceEnabled,
                                        onCheckedChange = { viewModel.toggleCoverBounce(it) }
                                    )
                                    if (isCoverBounceEnabled) {
                                        SliderRow("Intenzita pulzování", bounceIntensity, 0f..2.0f) { viewModel.setBounceIntensity(it) }
                                    }
                                    if (isAuroraEnabled) {
                                        SliderRow("Rychlost polární záře", auroraSpeed, 0.1f..3.0f) { viewModel.setAuroraSpeed(it) }
                                    }
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 6.dp))
                                }
                            }

                            SwitchRow(
                                label = "Displej vždy zapnutý",
                                subtitle = "Zabrání zhasnutí obrazovky během přehrávání",
                                checked = isScreenAlwaysOn,
                                onCheckedChange = { viewModel.toggleAlwaysOn(it) }
                            )
                        }
                    }

                    // 5. KNIHOVNA A MEZIPAMĚŤ
                    if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.LIBRARY) {
                        SettingsSectionCard(
                            title = "Knihovna a úložiště",
                            icon = Icons.Rounded.LibraryMusic
                        ) {
                            Text(
                                text = "Výchozí řazení skladeb",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            SortOption.entries.chunked(2).forEach { rowOptions ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowOptions.forEach { option ->
                                        val isSelected = sortOption == option
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) accentPurple else Color.White.copy(alpha = 0.05f))
                                                .border(
                                                    1.dp,
                                                    if (isSelected) accentPurple else Color.White.copy(alpha = 0.08f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable { viewModel.setSortOption(option) }
                                                .padding(vertical = 9.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = option.title,
                                                color = if (isSelected) Color(0xFF120D1D) else Color.White.copy(alpha = 0.85f),
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Statistiky knihovny
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatColumn(value = allSongs.size.toString(), label = "Skladeb")
                                StatColumn(value = allAlbums.size.toString(), label = "Alb")
                                StatColumn(value = allArtists.size.toString(), label = "Umělců")
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Akční tlačítka pro správu knihovny a cache
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(accentPurple.copy(alpha = 0.14f))
                                        .border(1.dp, accentPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.loadLibrary()
                                            statusMessage = "Knihovna byla úspěšně obnovena"
                                        }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Refresh, null, tint = accentPurple, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Obnovit knihovnu", color = accentPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.06f))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.clearAllCaches()
                                            statusMessage = "Mezipaměť textů a obrázků vymazána"
                                        }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.CleaningServices, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Vymazat cache", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- SPODNÍ LIŠTA DIALOGU ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            viewModel.resetSettingsToDefault()
                            statusMessage = "Obnoveno výchozí nastavení"
                        }
                    ) {
                        Icon(Icons.Rounded.RestartAlt, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Výchozí", color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
                    }
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentPurple, contentColor = Color(0xFF120D1D)),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Text("Hotovo", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        Text(text = label, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    badgeText: String? = null,
    badgeColor: Color = Color(0xFFBB86FC),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.038f))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(20.dp))
            .padding(15.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            if (badgeText != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(badgeColor.copy(alpha = 0.16f))
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        content()
    }
}

@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    subtitle: String? = null,
    accentColor: Color = Color(0xFFBB86FC),
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, lineHeight = 14.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF120D1D),
                checkedTrackColor = accentColor,
                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.1f),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                text = String.format(Locale.US, "%.1fx", value),
                color = Color(0xFFBB86FC),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFBB86FC),
                activeTrackColor = Color(0xFFBB86FC),
                inactiveTrackColor = Color.White.copy(alpha = 0.12f)
            )
        )
    }
}