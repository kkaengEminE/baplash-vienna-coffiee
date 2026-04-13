package coffee.vienna.baplash.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToCalibration: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("설정") })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp)
        ) {
            Text(
                "오디오",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = { Text("레이턴시 캘리브레이션") },
                supportingContent = { Text("오디오 입력 지연 보정") },
                leadingContent = { Icon(Icons.Default.Speed, null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToCalibration() }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            ListItem(
                headlineContent = { Text("기본 악기") },
                supportingContent = { Text("베이스") },
                leadingContent = { Icon(Icons.Default.MusicNote, null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* Instrument picker */ }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Text(
                "연습",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = { Text("기본 난이도") },
                supportingContent = { Text("중급") },
                leadingContent = { Icon(Icons.Default.Tune, null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* Difficulty picker */ }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Text(
                "정보",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = { Text("버전") },
                supportingContent = { Text("1.0.0") }
            )
        }
    }
}
