package com.acepero13.android.gamereviewer.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide holder for the active [AnalyticsFilter]. Registered as a Koin singleton so the
 * selection survives tab switches / ViewModel recreation and every Analytics tab observes the
 * same value.
 */
class AnalyticsFilterStore {

    private val _filter = MutableStateFlow<AnalyticsFilter>(AnalyticsFilter.All)
    val filter: StateFlow<AnalyticsFilter> = _filter.asStateFlow()

    fun set(value: AnalyticsFilter) {
        _filter.value = value
    }
}
