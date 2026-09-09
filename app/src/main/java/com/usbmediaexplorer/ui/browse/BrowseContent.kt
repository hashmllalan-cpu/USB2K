package com.usbmediaexplorer.ui.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.usbmediaexplorer.R
import com.usbmediaexplorer.ui.browse.components.BreadcrumbBar
import com.usbmediaexplorer.ui.browse.components.DocItemsView
import com.usbmediaexplorer.ui.common.SkeletonRows
import com.usbmediaexplorer.ui.common.SkeletonTiles
import com.usbmediaexplorer.ui.common.StateBlock
import com.usbmediaexplorer.ui.nav.AppNavigator
import com.usbmediaexplorer.ui.theme.AppRadius
import com.usbmediaexplorer.ui.theme.AppSpacing

@Composable
fun BrowseContent(
    state: BrowseUiState,
    viewModel: BrowseViewModel,
    navigator: AppNavigator,
    searching: Boolean,
    query: String,
    keyboard: SoftwareKeyboardController?,
    onQueryChange: (String) -> Unit,
    onSearchingChange: (Boolean) -> Unit,
    onGrantAccess: () -> Unit,
    onCreateFolder: () -> Unit,
    onOpenItem: (DocItem) -> Unit,
    onItemActions: (DocItem) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        if (state.breadcrumb.isNotEmpty() && !state.selecting) {
            BreadcrumbBar(
                trail = state.breadcrumb,
                onNavigate = { navigator.openBreadcrumb(it.uri) },
                onHome = { navigator.home() },
            )
        }
        if (!state.selecting) {
            if (searching) {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.search_in_folder)) },
                            leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }, Modifier.size(32.dp)) {
                                    Icon(Icons.Outlined.Close, stringResource(R.string.action_clear), Modifier.size(18.dp))
                                }
                            },
                            textStyle = MaterialTheme.typography.bodyMedium,
                            shape = RoundedCornerShape(AppRadius.pill),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                        )
                        Spacer(Modifier.width(AppSpacing.sm))
                        Text(stringResource(R.string.search_results_count, state.items.size), style = MaterialTheme.typography.labelSmall)
                    }
                    FilterChips(
                        current = state.kindFilter,
                        counts = state.mediaCount,
                        total = state.totalCount,
                        onSelect = viewModel::setKindFilter,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.md),
                    )
                }
            } else if (state.items.isNotEmpty() || state.isFiltered) {
                FilterChips(
                    current = state.kindFilter,
                    counts = state.mediaCount,
                    total = state.totalCount,
                    onSelect = viewModel::setKindFilter,
                    modifier = Modifier.fillMaxWidth().padding(start = AppSpacing.md, end = AppSpacing.xs, top = AppSpacing.xs),
                )
            }
        }
        Box(Modifier.fillMaxSize()) {
            when {
                state.loading -> if (state.viewMode.isList) SkeletonRows(9, Modifier.align(Alignment.TopCenter))
                else SkeletonTiles(if (state.viewMode.columnsPortrait >= 3) 3 else 2, 3, Modifier.align(Alignment.TopCenter))
                state.error != null -> StateBlock(
                    icon = Icons.Outlined.Info,
                    title = state.error ?: "",
                    body = stringResource(R.string.hint_connect_usb),
                    tint = MaterialTheme.colorScheme.error,
                    container = MaterialTheme.colorScheme.errorContainer,
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = viewModel::reload,
                    secondaryLabel = stringResource(R.string.action_grant_access),
                    onSecondaryAction = onGrantAccess,
                    modifier = Modifier.align(Alignment.Center),
                )
                state.items.isEmpty() && state.isFiltered -> StateBlock(
                    icon = Icons.Outlined.Search,
                    title = stringResource(R.string.search_no_results),
                    body = stringResource(R.string.browse_no_match_body),
                    actionLabel = stringResource(R.string.action_clear_filters),
                    onAction = { viewModel.clearFilters(); onQueryChange(""); onSearchingChange(false) },
                    modifier = Modifier.align(Alignment.Center),
                )
                state.items.isEmpty() -> StateBlock(
                    icon = Icons.Outlined.CreateNewFolder,
                    title = stringResource(R.string.browse_empty_folder),
                    body = stringResource(R.string.browse_empty_body),
                    actionLabel = if (state.canWrite) stringResource(R.string.action_new_folder) else null,
                    onAction = if (state.canWrite) onCreateFolder else null,
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> DocItemsView(
                    items = state.items,
                    viewMode = state.viewMode,
                    selection = state.selection,
                    selecting = state.selecting,
                    onOpen = onOpenItem,
                    onLongPress = { viewModel.startSelection(it.node) },
                    onToggleSelect = { viewModel.toggleSelection(it.node) },
                    onMore = onItemActions,
                    onVisible = viewModel::onItemVisible,
                    contentPadding = PaddingValues(start = AppSpacing.md, end = AppSpacing.md, top = AppSpacing.sm, bottom = AppSpacing.xxl),
                )
            }
        }
    }
}
