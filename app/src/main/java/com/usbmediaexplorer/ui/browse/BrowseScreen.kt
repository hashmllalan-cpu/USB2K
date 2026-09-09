package com.usbmediaexplorer.ui.browse

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.usbmediaexplorer.R
import com.usbmediaexplorer.data.doc.DocNode
import com.usbmediaexplorer.data.doc.MediaCount
import com.usbmediaexplorer.data.doc.isArchive
import com.usbmediaexplorer.ui.browse.components.CreateSheet
import com.usbmediaexplorer.ui.browse.components.DetailsSheet
import com.usbmediaexplorer.ui.browse.components.DocItemsView
import com.usbmediaexplorer.ui.browse.components.ItemActionsSheet
import com.usbmediaexplorer.ui.common.BulkRenameDialog
import com.usbmediaexplorer.ui.common.ConfirmDialog
import com.usbmediaexplorer.ui.common.TextInputDialog
import com.usbmediaexplorer.ui.common.ToolAction
import com.usbmediaexplorer.ui.common.bidiName
import com.usbmediaexplorer.ui.common.viewModelFactory
import com.usbmediaexplorer.ui.nav.LocalNavigator
import com.usbmediaexplorer.ui.theme.AppRadius
import com.usbmediaexplorer.ui.theme.AppSpacing
import com.usbmediaexplorer.util.Formatters
import com.usbmediaexplorer.util.Intents
import com.usbmediaexplorer.util.Shortcuts
import kotlinx.coroutines.launch
import com.usbmediaexplorer.ui.common.LocalAppContainer

/**
 * The browser (spec §5–§9, §11, §12): the screen the app is actually used on.
 *
 * Layout is two thin bars plus the content — a header that says where you are and how much is
 * here, and an action row that changes with the situation: filter chips while browsing, a search
 * field while searching, a contextual toolbar while selecting. Everything secondary lives in an
 * overflow menu rather than competing for space with the files.
 */
@Composable
fun BrowseScreen(
    uri: String,
    snackbarHostState: SnackbarHostState,
    selectContent: Boolean = false,
) {
    val container = LocalAppContainer.current
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val viewModel: BrowseViewModel = viewModel(
        key = "browse-$uri",
        factory = viewModelFactory { BrowseViewModel(container, uri) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val details by viewModel.details.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    // Access can be missing or revoked (USB replugged, grant removed in system settings). The
    // error state offers the SAF picker right there instead of leaving the user stuck.
    val treeGrant = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { granted -> viewModel.onTreeGranted(granted) }

    fun toast(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    var searching by remember(uri) { mutableStateOf(false) }
    var query by remember(uri) { mutableStateOf("") }
    var showViewModes by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var actionsFor by remember { mutableStateOf<List<DocItem>?>(null) }
    var renameTarget by remember { mutableStateOf<DocNode?>(null) }
    var showNewFolder by remember { mutableStateOf(false) }
    var showNewFile by remember { mutableStateOf(false) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showZipDialog by remember { mutableStateOf(false) }
    var showBulkRename by remember { mutableStateOf(false) }
    var deleteTargets by remember { mutableStateOf<List<DocNode>?>(null) }
    var showOverflow by remember { mutableStateOf(false) }
    var showSelectionOverflow by remember { mutableStateOf(false) }

    LaunchedEffect(uri) {
        viewModel.load(uri)
        // Arriving from "select folder content": everything in this folder starts selected.
        if (selectContent) viewModel.selectAllOnLoad()
    }
    LaunchedEffect(query) { viewModel.setQuery(query) }
    LaunchedEffect(snackbarHostState) {
        viewModel.messages.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    // Back means: leave selection first, then leave search, then leave the folder.
    BackHandler(enabled = state.selecting || searching) {
        when {
            state.selecting -> viewModel.clearSelection()
            searching -> {
                searching = false
                query = ""
                keyboard?.hide()
            }
        }
    }

    fun openItem(item: DocItem) {
        when (val action = viewModel.onOpen(item)) {
            is OpenAction.Folder -> navigator.openFolder(action.uri)
            is OpenAction.Video -> navigator.playVideo(action.uri, action.folderUri)
            is OpenAction.Image -> navigator.viewImage(action.uri, action.folderUri)
            is OpenAction.External -> {
                val external = container.docRepository.externalUri(action.node)
                val opened = external != null && Intents.open(context, action.node, external)
                if (!opened) toast(context.getString(R.string.msg_shared_failed))
            }

            OpenAction.None -> Unit
        }
    }

    fun openWith(item: DocItem) {
        val external: android.net.Uri =
            container.docRepository.externalUri(item.node)
                ?: run {
                    toast(context.getString(R.string.msg_no_app_to_open))
                    return
                }
        val opened = Intents.openWith(context, item.node, external)
        if (!opened) toast(context.getString(R.string.msg_no_app_to_open))
    }

    val selectedItems = remember(state.items, state.selection) {
        state.items.filter { it.node.key in state.selection }
    }
    val selectedNodes = remember(selectedItems) { selectedItems.map { it.node } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            // One obvious creation entry point: "+" opens a sheet with folder/file. It follows
            // writability — no dead button on a read-only volume, and it steps aside for the
            // selection toolbar.
            if (state.canWrite && !state.selecting) {
                FloatingActionButton(
                    onClick = { showCreateSheet = true },
                    shape = RoundedCornerShape(AppRadius.lg),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.create_new_title),
                    )
                }
            }
        },
        topBar = {
            BrowseTopBar(
                state = state,
                selecting = state.selecting,
                selectedSizeBytes = selectedNodes.sumOf { if (it.isDirectory) 0L else it.size.coerceAtLeast(0L) },
                searching = searching,
                showOverflow = showOverflow,
                reducer = BrowseActionReducer(
                    newFolder = { showNewFolder = true },
                    newFile = { showNewFile = true },
                    paste = { viewModel.paste() },
                    sort = { showSort = true },
                    select = { state.items.firstOrNull()?.let { viewModel.startSelection(it.node) } },
                    viewMode = { showViewModes = true },
                    toggleHidden = { scope.launch { container.settingsRepository.setShowHidden(!state.showHidden) } },
                    refresh = { viewModel.reload() },
                    searchVolume = { state.node?.let { navigator.search(it.uri) } },
                    details = { state.node?.let { viewModel.requestDetails(it) } },
                    resetFolderView = { viewModel.resetFolderPrefs() },
                    settings = { navigator.settings() },
                ),
                canPaste = state.canWrite && state.clipboardCount > 0,
                onBack = { if (!navigator.back()) (context as? Activity)?.finish() },
                onCloseSelection = { viewModel.clearSelection() },
                onSelectAll = { viewModel.selectAll() },
                onInvertSelection = { viewModel.invertSelection() },
                onToggleSearch = {
                    searching = !searching
                    if (!searching) {
                        query = ""
                        keyboard?.hide()
                    }
                },
                onDismissOverflow = { showOverflow = false },
                onOpenOverflow = { showOverflow = true },
            )
        },
        bottomBar = {
            if (state.selecting) {
                SelectionActionBar(
                    count = state.selection.size,
                    canWrite = state.canWrite,
                    singleSelected = selectedItems.size == 1,
                    onCopy = { viewModel.copySelection() },
                    onCut = { viewModel.cutSelection() },
                    onShare = { shareNodes(context, viewModel, selectedNodes, ::toast) },
                    onRename = {
                        if (selectedNodes.size == 1) renameTarget = selectedNodes.first() else showBulkRename = true
                    },
                    onDelete = { deleteTargets = selectedNodes },
                    moreOpen = showSelectionOverflow,
                    onMoreOpen = { showSelectionOverflow = true },
                    onMoreDismiss = { showSelectionOverflow = false },
                    onZip = { showZipDialog = true },
                    onBulkRename = { showBulkRename = true },
                    onFavorite = { selectedItems.forEach { viewModel.toggleFavorite(it.node) } },
                    onDetails = { selectedItems.firstOrNull()?.node?.let { viewModel.requestDetails(it) } },
                    onActionsSheet = { actionsFor = selectedItems },
                )
            } else if (state.clipboardCount > 0) {
                ClipboardBar(
                    count = state.clipboardCount,
                    isCut = state.clipboardIsCut,
                    onPaste = { viewModel.paste() },
                    onClear = { container.fileOpsManager.clearClipboard() },
                )
            }
        },
    ) { padding ->
        BrowseContent(
            state = state,
            viewModel = viewModel,
            navigator = navigator,
            searching = searching,
            query = query,
            keyboard = keyboard,
            onQueryChange = { query = it },
            onSearchingChange = { searching = it },
            onGrantAccess = { treeGrant.launch(null) },
            onCreateFolder = { showNewFolder = true },
            onOpenItem = ::openItem,
            contentPadding = padding,
            onItemActions = { actionsFor = listOf(it) },
        )
    }

    // ---- sheets & dialogs -------------------------------------------------

    BrowseSheetsHost(
        showViewModes = showViewModes,
        showSort = showSort,
        state = state,
        onViewMode = { showViewModes = it },
        onSort = { showSort = it },
        onSetViewMode = viewModel::setViewMode,
        onSetSort = viewModel::setSortMode,
        onFoldersFirst = viewModel::setFoldersFirst,
    )

    actionsFor?.let { targets ->
        ItemActionsSheet(
            items = targets,
            canWrite = state.canWrite,
            favorite = targets.firstOrNull()?.favorite == true,
            onOpen = { targets.firstOrNull()?.let { openItem(it) } },
            onOpenWith = { item -> openWith(item) },
            onSelectContent = { item -> navigator.openFolderSelecting(item.node.uri) },
            onShortcut = { item ->
                if (!Shortcuts.pin(context, item.node)) {
                    toast(context.getString(R.string.msg_shortcut_unsupported))
                }
            },
            onShare = { shareNodes(context, viewModel, targets.map { it.node }, ::toast) },
            onCopy = {
                container.fileOpsManager.copyToClipboard(
                    targets.map { it.node },
                    state.node,
                    cut = false,
                )
                viewModel.clearSelection()
            },
            onCut = {
                container.fileOpsManager.copyToClipboard(
                    targets.map { it.node },
                    state.node,
                    cut = true,
                )
                viewModel.clearSelection()
            },
            onRename = { renameTarget = targets.firstOrNull()?.node },
            onDelete = { deleteTargets = targets.map { it.node } },
            onDetails = { targets.firstOrNull()?.node?.let { viewModel.requestDetails(it) } },
            onFavorite = { targets.forEach { viewModel.toggleFavorite(it.node) } },
            onZip = { showZipDialog = true },
            onUnzip = { item -> if (item.node.isArchive) viewModel.unzip(item.node) },
            onBulkRename = { showBulkRename = true },
            onDismiss = {
                actionsFor = null
                viewModel.clearSelection()
            },
        )
    }

    details?.let { detailsState ->
        DetailsSheet(
            state = detailsState,
            onDismiss = { viewModel.clearDetails() },
            onOpen = {
                viewModel.clearDetails()
                detailsState.node.let { node ->
                    when {
                        node.isDirectory -> navigator.openFolder(node.uri)
                        else -> openItem(DocItem(node))
                    }
                }
            },
            onShare = { shareNodes(context, viewModel, listOf(detailsState.node), ::toast) },
            onRename = {
                renameTarget = detailsState.node
                viewModel.clearDetails()
            },
            onDelete = {
                deleteTargets = listOf(detailsState.node)
                viewModel.clearDetails()
            },
        )
    }

    renameTarget?.let { node ->
        TextInputDialog(
            title = stringResource(R.string.dialog_rename_title),
            label = stringResource(R.string.label_name),
            initial = node.name,
            confirmLabel = stringResource(R.string.action_rename),
            onConfirm = {
                viewModel.rename(node, it)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }

    if (showCreateSheet) {
        CreateSheet(
            onFolder = { showNewFolder = true },
            onFile = { showNewFile = true },
            onDismiss = { showCreateSheet = false },
        )
    }

    if (showNewFolder) {
        TextInputDialog(
            title = stringResource(R.string.dialog_new_folder_title),
            label = stringResource(R.string.label_name),
            initial = "",
            confirmLabel = stringResource(R.string.action_save),
            onConfirm = {
                viewModel.createFolder(it)
                showNewFolder = false
            },
            onDismiss = { showNewFolder = false },
        )
    }

    if (showNewFile) {
        TextInputDialog(
            title = stringResource(R.string.dialog_new_file_title),
            label = stringResource(R.string.label_name),
            initial = "",
            confirmLabel = stringResource(R.string.action_save),
            onConfirm = {
                viewModel.createFile(it)
                showNewFile = false
            },
            onDismiss = { showNewFile = false },
        )
    }

    if (showZipDialog) {
        TextInputDialog(
            title = stringResource(R.string.action_zip),
            label = stringResource(R.string.label_name),
            initial = (actionsFor?.firstOrNull()?.node?.nameWithoutExtension
                ?: selectedNodes.firstOrNull()?.nameWithoutExtension
                ?: "archive") + ".zip",
            confirmLabel = stringResource(R.string.action_zip),
            onConfirm = { name ->
                if (state.selecting) {
                    viewModel.zipSelection(name)
                } else {
                    val nodes = actionsFor?.map { item -> item.node }
                    val destination = state.node
                    if (nodes != null && destination != null) {
                        container.fileOpsManager.zip(nodes, destination, name)
                    }
                }
                showZipDialog = false
                actionsFor = null
                viewModel.clearSelection()
            },
            onDismiss = { showZipDialog = false },
        )
    }

    if (showBulkRename) {
        val nodes = selectedNodes.ifEmpty { actionsFor?.map { it.node }.orEmpty() }
        BulkRenameDialog(
            items = nodes,
            onApply = { rules ->
                if (state.selecting) {
                    viewModel.bulkRename(rules)
                } else {
                    container.fileOpsManager.bulkRename(nodes, rules)
                }
                showBulkRename = false
                actionsFor = null
            },
            onDismiss = { showBulkRename = false },
        )
    }

    deleteTargets?.let { nodes ->
        ConfirmDialog(
            title = stringResource(R.string.dialog_delete_title, nodes.size),
            body = stringResource(R.string.dialog_delete_body),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = {
                viewModel.delete(nodes)
                deleteTargets = null
                actionsFor = null
            },
            onDismiss = { deleteTargets = null },
        )
    }
}
