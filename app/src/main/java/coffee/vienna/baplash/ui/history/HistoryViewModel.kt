package coffee.vienna.baplash.ui.history

import androidx.lifecycle.ViewModel
import coffee.vienna.baplash.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    sessionRepository: SessionRepository
) : ViewModel() {
    val recentSessions = sessionRepository.getRecentSessions(20)
    val totalSessionCount = sessionRepository.getTotalSessionCount()
    val totalPracticeTimeMs = sessionRepository.getTotalPracticeTimeMs()
}
