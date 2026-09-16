package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.player.PlayerManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(onNavigateBack: () -> Unit) {
    val equalizer = PlayerManager.equalizer ?: return
    var isEnabled by remember { mutableStateOf(equalizer.enabled) }
    
    val numBands = equalizer.numberOfBands.toInt()
    val bandLevels = remember { mutableStateListOf<Int>() }
    var activePreset by remember { mutableStateOf("مخصص") }
    
    LaunchedEffect(Unit) {
        if (bandLevels.isEmpty()) {
            for (i in 0 until numBands) bandLevels.add(equalizer.getBandLevel(i.toShort()).toInt())
        }
    }
    
    fun applyPreset(name: String, levels: List<Int>) {
        activePreset = name
        if (levels.size == numBands) {
            for (i in 0 until numBands) {
                bandLevels[i] = levels[i]
                if (isEnabled) equalizer.setBandLevel(i.toShort(), levels[i].toShort())
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2C1E30))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("المعدّل", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
            Switch(checked = isEnabled, onCheckedChange = { isEnabled = it; equalizer.enabled = it })
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
            val minEQ = equalizer.bandLevelRange[0].toFloat()
            val maxEQ = equalizer.bandLevelRange[1].toFloat()
            
            for (i in 0 until numBands) {
                if (i < bandLevels.size) {
                    val freq = equalizer.getCenterFreq(i.toShort()) / 1000
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("+5", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Slider(
                                value = bandLevels[i].toFloat(),
                                onValueChange = { 
                                    bandLevels[i] = it.toInt()
                                    activePreset = "مخصص"
                                    if(isEnabled) equalizer.setBandLevel(i.toShort(), it.toInt().toShort())
                                },
                                valueRange = minEQ..maxEQ,
                                modifier = Modifier.rotate(-90f).width(200.dp),
                                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.DarkGray)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(if (freq >= 1000) "${freq/1000}K" else "$freq", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        val maxEqInt = equalizer.bandLevelRange[1].toInt()
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PresetCard("معزز الباس", Icons.Filled.Speaker, modifier = Modifier.weight(1f), isActive = activePreset == "معزز الباس") {
                applyPreset("معزز الباس", List(numBands) { if (it < 2) maxEqInt else 0 })
            }
            PresetCard("مخصص", Icons.Filled.GraphicEq, modifier = Modifier.weight(1f), isActive = activePreset == "مخصص") {
                applyPreset("مخصص", List(numBands) { 0 })
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PresetCard("معزز الصوت المرتفع", Icons.Filled.Speaker, modifier = Modifier.weight(1f), isActive = activePreset == "معزز الصوت المرتفع") {
                applyPreset("معزز الصوت المرتفع", List(numBands) { if (it > numBands - 3) maxEqInt else 0 })
            }
            PresetCard("معزز الصوت البشري", Icons.Filled.Mic, modifier = Modifier.weight(1f), isActive = activePreset == "معزز الصوت البشري") {
                applyPreset("معزز الصوت البشري", List(numBands) { if (it in 1..3) maxEqInt else 0 })
            }
        }
    }
}

@Composable
fun PresetCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, isActive: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .aspectRatio(1.5f)
            .background(if (isActive) Color.White else Color.Transparent, RoundedCornerShape(16.dp))
            .border(1.dp, if (isActive) Color.Transparent else Color.DarkGray, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = if (isActive) Color.Black else Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = if (isActive) Color.Black else Color.White, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
