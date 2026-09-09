package com.usbmediaexplorer.ui.browse

import androidx.compose.runtime.Composable
import com.usbmediaexplorer.ui.browse.components.SortSheet
import com.usbmediaexplorer.ui.browse.components.ViewModeSheet

/** Owns the lightweight view/sort sheets so BrowseScreen only coordinates state. */
@Composable
fun BrowseSheetsHost(
    showViewModes: Boolean,
    showSort: Boolean,
    state: BrowseUiState,
    onViewMode: (Boolean) -> Unit,
    onSort: (Boolean) -> Unit,
    onSetViewMode: (com.usbmediaexplorer.data.settings.ViewMode) -> Unit,
    onSetSort: (com.usbmediaexplorer.data.settings.SortMode) -> Unit,
    onFoldersFirst: (Boolean) -> Unit,
) {
    if (showViewModes) {
        ViewModeSheet(
            current = state.viewMode,
            onSelect = onSetViewMode,
            onDismiss = { onViewMode(false) },
        )
    }
    if (showSort) {
        SortSheet(
            current = state.sortMode,
            foldersFirst = state.foldersFirst,
            onSelect = onSetSort,
            onToggleFoldersFirst = onFoldersFirst,
            onDismiss = { onSort(false) },
        )
    }
}
