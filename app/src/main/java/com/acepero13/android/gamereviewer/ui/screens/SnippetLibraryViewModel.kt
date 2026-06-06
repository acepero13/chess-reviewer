package com.acepero13.android.gamereviewer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.model.Snippet
import com.acepero13.android.gamereviewer.data.model.parsedTags
import com.acepero13.android.gamereviewer.data.repository.SnippetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class SnippetLibraryUiState(
    val snippets: List<Snippet> = emptyList(),
    val selectedTag: String? = null,
    val allTags: List<String> = emptyList(),
)

class SnippetLibraryViewModel(
    private val repo: SnippetRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SnippetLibraryUiState())
    val uiState: StateFlow<SnippetLibraryUiState> = _uiState.asStateFlow()

    private var allSnippets: List<Snippet> = emptyList()

    init {
        viewModelScope.launch {
            repo.observeAll().collectLatest { snippets ->
                allSnippets = snippets
                val allTags = snippets.flatMap { it.parsedTags() }.distinct().sorted()
                _uiState.value = _uiState.value.copy(
                    snippets = applyTagFilter(snippets, _uiState.value.selectedTag),
                    allTags  = allTags,
                )
            }
        }
    }

    fun selectTag(tag: String?) {
        _uiState.value = _uiState.value.copy(
            selectedTag = tag,
            snippets    = applyTagFilter(allSnippets, tag),
        )
    }

    fun deleteSnippet(snippet: Snippet) {
        viewModelScope.launch(Dispatchers.IO) { repo.delete(snippet) }
    }

    private fun applyTagFilter(snippets: List<Snippet>, tag: String?): List<Snippet> =
        if (tag == null) snippets else snippets.filter { tag in it.parsedTags() }
}
