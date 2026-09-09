package com.usbmediaexplorer

import android.net.Uri
import com.usbmediaexplorer.ui.nav.Routes
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationRoutesTest {
    @Test
    fun `browse route encodes uri and selection flag`() {
        val uri = Uri.parse("content://usb/tree/root/document/Movies A")
        val route = Routes.browse(uri, selectAll = true)

        assertTrue(route.startsWith("browse?uri="))
        assertTrue(route.contains("%2F"))
        assertTrue(route.endsWith("&select=true"))
    }

    @Test
    fun `folder route patterns keep stable argument names`() {
        assertTrue(Routes.browseRoute().contains("uri={$ARG_URI}"))
    }

    private companion object {
        const val ARG_URI = "uri"
    }
}
