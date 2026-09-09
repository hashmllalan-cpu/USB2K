package com.usbmediaexplorer.ui.browse

/** Actions exposed by the browser header; keeping them here prevents UI from owning mutations. */
data class BrowseActionReducer(
    val newFolder: () -> Unit,
    val newFile: () -> Unit,
    val paste: () -> Unit,
    val sort: () -> Unit,
    val select: () -> Unit,
    val viewMode: () -> Unit,
    val toggleHidden: () -> Unit,
    val refresh: () -> Unit,
    val searchVolume: () -> Unit,
    val details: () -> Unit,
    val resetFolderView: () -> Unit,
    val settings: () -> Unit,
)
