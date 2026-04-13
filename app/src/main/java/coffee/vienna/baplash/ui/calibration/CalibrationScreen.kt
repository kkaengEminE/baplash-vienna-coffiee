package coffee.vienna.baplash.ui.calibration

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coffee.vienna.baplash.ui.theme.PerfectGreen

enum class CalibrationState {
    READY,
    LISTENING,
    COMPLETE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    onNavigateBack: () -> Unit
) {
    var state by remember { mutableStateOf(CalibrationState.READY) }
    var tapCount by remember { mutableIntStateOf(0) }
    var latencyResult by remember { mutableIntStateOf(0) }
    val requiredTaps = 16

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("레이턴시 캘리브레이션") },
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
            when (state) {
                CalibrationState.READY -> {
                    Text(
                        "오디오 지연 보정",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "메트로놈 소리에 맞춰 화면을 터치하세요.\n16번 터치하면 기기의 오디오 지연을 측정합니다.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = { state = CalibrationState.LISTENING }) {
                        Text("시작")
                    }
                }

                CalibrationState.LISTENING -> {
                    Text(
                        "메트로놈에 맞춰 터치하세요",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "$tapCount / $requiredTaps",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { tapCount.toFloat() / requiredTaps },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    // Tap area
                    val isTapped = remember { mutableStateOf(false) }
                    val bgColor by animateColorAsState(
                        if (isTapped.value) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primaryContainer,
                        tween(100),
                        label = "tapColor"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(bgColor, MaterialTheme.shapes.large)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                tapCount++
                                isTapped.value = true
                                if (tapCount >= requiredTaps) {
                                    latencyResult = 15 // Placeholder - would come from native calibrator
                                    state = CalibrationState.COMPLETE
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "TAP",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                CalibrationState.COMPLETE -> {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = PerfectGreen,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        "캘리브레이션 완료!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = PerfectGreen
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "측정된 지연: ${latencyResult}ms",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "이 값은 연습 시 자동으로 적용됩니다",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = onNavigateBack) {
                        Text("확인")
                    }
                }
            }
        }
    }
}
