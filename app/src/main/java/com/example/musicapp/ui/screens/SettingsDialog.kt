package com.example.musicapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.musicapp.model.VisualizerType
import com.example.musicapp.viewmodel.MusicViewModel

@Composable
fun SettingsDialog(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit,
    onSelectFolder: () -> Unit
) {
    val isAuroraEnabled by viewModel.isAuroraEnabled.collectAsState()
    val isCoverBounceEnabled by viewModel.isCoverBounceEnabled.collectAsState()
    val isScreenAlwaysOn by viewModel.isScreenAlwaysOn.collectAsState()

    val isParticlesEnabled by viewModel.isParticlesEnabled.collectAsState()
    val isPulseEnabled by viewModel.isPulseEnabled.collectAsState()

    val visualizerType by viewModel.visualizerType.collectAsState()
    val isVisualizerMirrored by viewModel.isVisualizerMirrored.collectAsState() // NOVÉ
    val visualizerSensitivity by viewModel.visualizerSensitivity.collectAsState()
    val bounceIntensity by viewModel.bounceIntensity.collectAsState()
    val auroraSpeed by viewModel.auroraSpeed.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E1E1E))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Nastavení",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF333333))
                        .clickable {
                            viewModel.loadLibrary()
                            onDismiss()
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Refresh, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Načíst znovu knihovnu", color = Color.White, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- VÝBĚR STYLU VIZUALIZÉRU ---
                Text("Vzhled vizualizéru hudby", color = Color(0xFFBB86FC), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val allTypes = VisualizerType.values().toList()
                    allTypes.chunked(3).forEach { rowTypes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowTypes.forEach { type ->
                                val isSelected = visualizerType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFBB86FC) else Color(0xFF2B2B2B))
                                        .clickable { viewModel.setVisualizerType(type) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = type.title,
                                        color = if (isSelected) Color.Black else Color.White,
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

                // NOVÉ: Přepínač zrcadlení vizualizéru
                Spacer(modifier = Modifier.height(8.dp))
                SwitchRow("Zrcadlení vizualizéru", isVisualizerMirrored) { viewModel.toggleVisualizerMirroring(it) }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))

                SwitchRow("Displej vždy zapnutý", isScreenAlwaysOn) { viewModel.toggleAlwaysOn(it) }
                SwitchRow("Efekt polární záře", isAuroraEnabled) { viewModel.toggleAurora(it) }

                SwitchRow("Efekt částic (Hvězdný prach)", isParticlesEnabled) { viewModel.toggleParticles(it) }
                SwitchRow("Efekt Sonar (Vlny)", isPulseEnabled) { viewModel.togglePulse(it) }

                SwitchRow("Pulzování obalu", isCoverBounceEnabled) { viewModel.toggleCoverBounce(it) }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))

                SliderRow("Citlivost vizualizéru", visualizerSensitivity, 0.5f..2.0f) { viewModel.setVisualizerSensitivity(it) }
                SliderRow("Intenzita pulzování", bounceIntensity, 0f..2.0f) { viewModel.setBounceIntensity(it) }
                if (isAuroraEnabled) {
                    SliderRow("Rychlost záře", auroraSpeed, 0.1f..3.0f) { viewModel.setAuroraSpeed(it) }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { viewModel.resetSettingsToDefault() }) {
                        Text("Resetovat", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC))
                    ) {
                        Text("Zavřít", color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFFBB86FC),
                checkedTrackColor = Color(0xFF3700B3),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}

@Composable
fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, color = Color.White, fontSize = 14.sp)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFBB86FC),
                activeTrackColor = Color(0xFFBB86FC),
                inactiveTrackColor = Color.DarkGray
            )
        )
    }
}