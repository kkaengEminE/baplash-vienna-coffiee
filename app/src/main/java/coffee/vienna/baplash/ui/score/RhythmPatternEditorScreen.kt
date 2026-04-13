package coffee.vienna.baplash.ui.score

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
fun RhythmPatternEditorScreen(
    onNavigateBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var bpm by remember { mutableIntStateOf(120) }
    var beatsPerMeasure by remember { mutableIntStateOf(4) }
    var numMeasures by remember { mutableIntStateOf(4) }
    var subdivisions by remember { mutableIntStateOf(1) } // 1 = quarter, 2 = eighth, 4 = sixteenth

    // Grid state: [measure][beat * subdivision] = true (note) or false (rest)
    val totalCellsPerMeasure = beatsPerMeasure * subdivisions
    val grid = remember {
        mutableStateListOf<MutableList<Boolean>>().apply {
            repeat(numMeasures) {
                add(MutableList(totalCellsPerMeasure) { cellIndex ->
                    cellIndex % subdivisions == 0 // Default: notes on main beats
                })
            }
        }
    }

    // Update grid when parameters change
    fun resizeGrid() {
        val newTotal = beatsPerMeasure * subdivisions
        while (grid.size < numMeasures) {
            grid.add(MutableList(newTotal) { it % subdivisions == 0 })
        }
        while (grid.size > numMeasures) {
            grid.removeAt(grid.lastIndex)
        }
        for (i in grid.indices) {
            val row = grid[i]
            while (row.size < newTotal) row.add(false)
            while (row.size > newTotal) row.removeAt(row.lastIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("리듬 패턴 만들기") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, "뒤로")
                }
            },
            actions = {
                IconButton(onClick = { /* Save pattern */ }) {
                    Icon(Icons.Default.Check, "저장")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("패턴 이름") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // BPM
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("BPM: $bpm", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(16.dp))
                Slider(
                    value = bpm.toFloat(),
                    onValueChange = { bpm = it.toInt() },
                    valueRange = 40f..240f,
                    modifier = Modifier.weight(1f)
                )
            }

            // Time signature
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("박자: ", style = MaterialTheme.typography.titleMedium)
                listOf(2, 3, 4, 6).forEach { beats ->
                    val isSelected = beatsPerMeasure == beats
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                beatsPerMeasure = beats
                                resizeGrid()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$beats/4",
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Subdivisions
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("세분: ", style = MaterialTheme.typography.titleMedium)
                listOf(1 to "4분", 2 to "8분", 4 to "16분").forEach { (sub, label) ->
                    val isSelected = subdivisions == sub
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                subdivisions = sub
                                resizeGrid()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Measures count
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("마디 수: $numMeasures", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(16.dp))
                Slider(
                    value = numMeasures.toFloat(),
                    onValueChange = {
                        numMeasures = it.toInt()
                        resizeGrid()
                    },
                    valueRange = 1f..16f,
                    steps = 14,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Beat grid
            Text("패턴 편집", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "셀을 터치하여 음표(채워진)/쉼표(빈) 전환",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            grid.forEachIndexed { measureIndex, measureBeats ->
                Column {
                    Text(
                        "마디 ${measureIndex + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        measureBeats.forEachIndexed { cellIndex, isNote ->
                            val isMainBeat = cellIndex % subdivisions == 0
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(
                                        width = if (isMainBeat) 2.dp else 1.dp,
                                        color = if (isMainBeat) MaterialTheme.colorScheme.outline
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .background(
                                        if (isNote) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .clickable {
                                        grid[measureIndex][cellIndex] = !isNote
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isNote) {
                                    Text(
                                        if (isMainBeat) "${cellIndex / subdivisions + 1}" else "+",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preview button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FilledIconButton(
                    onClick = { /* Play preview */ },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, "미리 듣기")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
