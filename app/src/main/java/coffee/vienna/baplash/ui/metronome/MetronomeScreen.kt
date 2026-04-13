package coffee.vienna.baplash.ui.metronome

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetronomeScreen(
    onNavigateBack: () -> Unit
) {
    var bpm by remember { mutableIntStateOf(120) }
    var beatsPerMeasure by remember { mutableIntStateOf(4) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentBeat by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("메트로놈") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, "뒤로")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // BPM display
            Text(
                "$bpm",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "BPM",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // BPM controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FilledIconButton(
                    onClick = { bpm = (bpm - 5).coerceAtLeast(40) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Remove, "-5")
                }
                FilledIconButton(
                    onClick = { bpm = (bpm - 1).coerceAtLeast(40) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Remove, "-1")
                }
                FilledIconButton(
                    onClick = { bpm = (bpm + 1).coerceAtMost(240) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Add, "+1")
                }
                FilledIconButton(
                    onClick = { bpm = (bpm + 5).coerceAtMost(240) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Add, "+5")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Slider(
                value = bpm.toFloat(),
                onValueChange = { bpm = it.toInt() },
                valueRange = 40f..240f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Time signature selector
            Text(
                "박자",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(2, 3, 4, 6).forEach { beats ->
                    val isSelected = beatsPerMeasure == beats
                    FilledIconButton(
                        onClick = { beatsPerMeasure = beats },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Text(
                            "$beats",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Beat indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                for (i in 0 until beatsPerMeasure) {
                    val isActive = isPlaying && i == currentBeat
                    val size by animateFloatAsState(
                        if (isActive) 40f else 28f,
                        tween(100),
                        label = "size"
                    )
                    val color by animateColorAsState(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        tween(100),
                        label = "color"
                    )
                    Box(
                        modifier = Modifier
                            .size(size.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Play/Stop button
            FilledIconButton(
                onClick = { isPlaying = !isPlaying },
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    if (isPlaying) "정지" else "시작",
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}
