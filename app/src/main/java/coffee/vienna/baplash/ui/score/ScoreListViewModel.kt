package coffee.vienna.baplash.ui.score

import androidx.lifecycle.ViewModel
import coffee.vienna.baplash.data.local.db.entity.ScoreEntity
import coffee.vienna.baplash.data.repository.ScoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class ScoreListViewModel @Inject constructor(
    scoreRepository: ScoreRepository
) : ViewModel() {
    val scores: Flow<List<ScoreEntity>> = scoreRepository.getAllScores()
}
