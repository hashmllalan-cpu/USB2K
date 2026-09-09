package com.usbmediaexplorer.ui.browse

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.usbmediaexplorer.R
import com.usbmediaexplorer.ui.common.bidiName
import com.usbmediaexplorer.util.Formatters

@Composable
fun BrowseTopBar(
    state: BrowseUiState,
    selecting: Boolean,
    selectedSizeBytes: Long,
    searching: Boolean,
    showOverflow: Boolean,
    reducer: BrowseActionReducer,
    canPaste: Boolean,
    onBack: () -> Unit,
    onCloseSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onToggleSearch: () -> Unit,
    onDismissOverflow: () -> Unit,
    onOpenOverflow: () -> Unit,
) {
    if (selecting) {
        SelectionTopBar(
            count = state.selection.size,
            sizeBytes = selectedSizeBytes,
            onClose = onCloseSelection,
            onSelectAll = onSelectAll,
            onInvert = onInvertSelection,
        )
        return
    }
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back))
            }
        },
        title = {
            Column {
                Text(
                    text = (state.node?.name ?: stringResource(R.string.breadcrumb_root)).bidiName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = summaryLine(state),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        actions = {
            IconButton(onClick = onToggleSearch) {
                Icon(
                    if (searching) Icons.Outlined.Close else Icons.Outlined.Search,
                    stringResource(R.string.action_search),
                    tint = if (searching) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            androidx.compose.foundation.layout.Box {
                IconButton(onClick = onOpenOverflow) {
                    Icon(Icons.Outlined.MoreVert, stringResource(R.string.action_more))
                }
                DropdownMenu(expanded = showOverflow, onDismissRequest = onDismissOverflow) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_new_folder)) },
                        leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, null) },
                        onClick = { onDismissOverflow(); reducer.newFolder() },
                        enabled = state.canWrite,
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_new_file)) },
                        leadingIcon = { Icon(Icons.Outlined.NoteAdd, null) },
                        onClick = { onDismissOverflow(); reducer.newFile() },
                        enabled = state.canWrite,
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_paste) + if (state.clipboardCount > 0) " (${state.clipboardCount})" else "") },
                        leadingIcon = { Icon(Icons.Outlined.ContentPaste, null) },
                        onClick = { onDismissOverflow(); reducer.paste() },
                        enabled = canPaste,
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_sort)) },
                        leadingIcon = { Icon(Icons.Outlined.Sort, null) },
                        onClick = { onDismissOverflow(); reducer.sort() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_select)) },
                        leadingIcon = { Icon(Icons.Outlined.SelectAll, null) },
                        onClick = { onDismissOverflow(); reducer.select() },
                        enabled = state.items.isNotEmpty(),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.view_mode)) },
                        leadingIcon = { Icon(Icons.Outlined.GridView, null) },
                        onClick = { onDismissOverflow(); reducer.viewMode() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(if (state.showHidden) R.string.action_hide_hidden else R.string.action_show_hidden)) },
                        leadingIcon = { Icon(if (state.showHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null) },
                        onClick = { onDismissOverflow(); reducer.toggleHidden() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_refresh)) },
                        leadingIcon = { Icon(Icons.Outlined.Refresh, null) },
                        onClick = { onDismissOverflow(); reducer.refresh() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_search_volume)) },
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        onClick = { onDismissOverflow(); reducer.searchVolume() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_details)) },
                        leadingIcon = { Icon(Icons.Outlined.Info, null) },
                        onClick = { onDismissOverflow(); reducer.details() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_reset_folder_view)) },
                        leadingIcon = { Icon(Icons.Outlined.RestartAlt, null) },
                        onClick = { onDismissOverflow(); reducer.resetFolderView() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_settings)) },
                        leadingIcon = { Icon(Icons.Outlined.Settings, null) },
                        onClick = { onDismissOverflow(); reducer.settings() },
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

/**
 * Selection-mode replacement for [BrowseTopBar]: selected count + total size with
 * close / select-all / invert actions. (Was referenced but never defined — added to
 * fix the `Unresolved reference 'SelectionTopBar'` compilation error.)
 */
@Composable
fun SelectionTopBar(
    count: Int,
    sizeBytes: Long,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onInvert: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, stringResource(R.string.action_close))
            }
        },
        title = {
            Column {
                Text(
                    text = stringResource(R.string.selection_count, count),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = Formatters.size(sizeBytes),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Outlined.SelectAll, stringResource(R.string.action_select_all))
            }
            IconButton(onClick = onInvert) {
                Icon(Icons.Outlined.SwapVert, stringResource(R.string.action_invert_selection))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
        ),
    )
}
