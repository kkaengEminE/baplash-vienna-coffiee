package coffee.vienna.baplash.ui.practice

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coffee.vienna.baplash.domain.model.AccuracyRating
import coffee.vienna.baplash.ui.theme.EarlyYellow
import coffee.vienna.baplash.ui.theme.GoodBlue
import coffee.vienna.baplash.ui.theme.LatOrange
import coffee.vienna.baplash.ui.theme.MissedRed
import coffee.vienna.baplash.ui.theme.PerfectGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    onNavigateBack: () -> Unit,
    viewModel: PracticeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("연습") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, "뒤로")
                }
            }
        )

        // BPM & Info bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "BPM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${uiState.bpm}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "마디 ${uiState.currentMeasure}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    uiState.timeSignature,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "점수",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${uiState.currentScore.toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        uiState.currentScore >= 80f -> PerfectGreen
                        uiState.currentScore >= 60f -> EarlyYellow
                        else -> MissedRed
                    }
                )
            }
        }

        // BPM Slider (when not playing)
        if (!uiState.isPlaying) {
            Slider(
                value = uiState.bpm.toFloat(),
                onValueChange = { viewModel.setBpm(it.toInt()) },
                valueRange = 40f..240f,
                steps = 199,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Beat indicator circles
        BeatIndicator(
            totalBeats = uiState.totalBeatsInMeasure,
            currentBeat = uiState.currentBeat,
            beatAccuracies = uiState.recentBeatAccuracies,
            isPlaying = uiState.isPlaying,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Feedback display
        FeedbackDisplay(
            lastAccuracy = uiState.lastAccuracy,
            lastDeviationMs = uiState.lastDeviationMs,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = { viewModel.resetSession() },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Refresh, "다시 시작")
            }

            FilledIconButton(
                onClick = {
                    if (uiState.isPlaying) viewModel.stopPractice()
                    else viewModel.startPractice()
                },
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    if (uiState.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    if (uiState.isPlaying) "정지" else "시작",
                    modifier = Modifier.size(36.dp)
                )
            }

            // Placeholder for loop button
            Spacer(modifier = Modifier.size(56.dp))
        }
    }
}

@Composable
private fun BeatIndicator(
    totalBeats: Int,
    currentBeat: Int,
    beatAccuracies: List<AccuracyRating?>,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalBeats) {
            val isCurrent = isPlaying && i == currentBeat
            val accuracy = beatAccuracies.getOrNull(i)

            val targetSize = if (isCurrent) 48f else 36f
            val size by animateFloatAsState(
                targetValue = targetSize,
                animationSpec = tween(100),
                label = "beatSize"
            )

            val color by animateColorAsState(
                targetValue = when {
                    accuracy == AccuracyRating.PERFECT -> PerfectGreen
                    accuracy == AccuracyRating.GOOD -> GoodBlue
                    accuracy == AccuracyRating.EARLY -> EarlyYellow
                    accuracy == AccuracyRating.LATE -> LatOrange
                    accuracy == AccuracyRating.MISSED -> MissedRed
                    isCurrent -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                animationSpec = tween(150),
                label = "beatColor"
            )

            Box(
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                if (i == 0) {
                    Text(
                        "1",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackDisplay(
    lastAccuracy: AccuracyRating?,
    lastDeviationMs: Float?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (lastAccuracy) {
            AccuracyRating.PERFECT -> {
                Text(
                    "PERFECT!",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = PerfectGreen
                )
            }
            AccuracyRating.GOOD -> {
                Text(
                    "GOOD",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = GoodBlue
                )
            }
            AccuracyRating.EARLY -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "EARLY",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = EarlyYellow
                    )
                    lastDeviationMs?.let {
                        Text(
                            "${it.toInt()}ms 빠름",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EarlyYellow
                        )
                    }
                }
            }
            AccuracyRating.LATE -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "LATE",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = LatOrange
                    )
                    lastDeviationMs?.let {
                        Text(
                            "+${it.toInt()}ms 느림",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LatOrange
                        )
                    }
                }
            }
            AccuracyRating.MISSED -> {
                Text(
                    "MISS",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MissedRed
                )
            }
            else -> {
                Text(
                    "연주를 시작하세요",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}
