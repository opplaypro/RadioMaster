package com.radiomaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.viewModelScope
import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.domain.repository.StationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: StationRepository
) : ViewModel() {

    private val _query = androidx.lifecycle.MutableStateFlow("")
    val query = _query.asStateFlow()

    val results: Flow<List<Station>> = _query
        .distinctUntilChanged()
        .flatMapLatest { text ->
            repository.searchStations(text, 40)
        }

    fun setQuery(text: String) = _query.value = text
    fun clear() = _query.value = ""
}
