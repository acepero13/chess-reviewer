package com.acepero13.android.gamereviewer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.model.Snippet
import com.acepero13.android.gamereviewer.data.repository.SnippetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SnippetAnalysisUiState(
    val snippet: Snippet? = null,
    val loading: Boolean = true,
)

class SnippetAnalysisViewModel(
    private val snippetId: Long,
    private val repo: SnippetRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SnippetAnalysisUiState())
    val uiState: StateFlow<SnippetAnalysisUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val snippet = repo.findById(snippetId)
            _uiState.value = SnippetAnalysisUiState(snippet = snippet, loading = false)
        }
    }
}
