package com.usbmediaexplorer.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand palette — the single source of colour truth for the app.
 *
 * Design intent (a file manager, not a settings screen):
 *  - **deep navy and teal define the identity**, reserved for navigation and primary actions.
 *    Storage and media content remain the visual focus.
 *  - **surfaces are neutral**, barely tinted, so thumbnails and posters — the real content of this
 *    app — are the most saturated things on screen.
 *  - **meaning gets its own colour**: success, warning and danger are semantic and never borrow the
 *    primary, so "delete" can never be mistaken for "open".
 *  - storage kinds are distinguishable at a glance: internal = violet, USB = teal, SD = amber.
 *
 * Used when Material You dynamic colour is unavailable (Android 11 and below) or switched off.
 */
object Palette {

    // ---- light -------------------------------------------------------------
    val PrimaryLight = Color(0xFF087F8C)
    val OnPrimaryLight = Color(0xFFFFFFFF)
    val PrimaryContainerLight = Color(0xFFC4EEF0)
    val OnPrimaryContainerLight = Color(0xFF00363A)

    val SecondaryLight = Color(0xFF48636A)
    val OnSecondaryLight = Color(0xFFFFFFFF)
    val SecondaryContainerLight = Color(0xFFCCE8EC)
    val OnSecondaryContainerLight = Color(0xFF061F24)

    val TertiaryLight = Color(0xFFA15C00)
    val OnTertiaryLight = Color(0xFFFFFFFF)
    val TertiaryContainerLight = Color(0xFFFFDDB5)
    val OnTertiaryContainerLight = Color(0xFF321900)

    val BackgroundLight = Color(0xFFF7F9FA)
    val OnBackgroundLight = Color(0xFF17272B)
    val SurfaceLight = Color(0xFFF7F9FA)
    val OnSurfaceLight = Color(0xFF17272B)
    val SurfaceVariantLight = Color(0xFFDCE5E7)
    val OnSurfaceVariantLight = Color(0xFF405156)
    val OutlineLight = Color(0xFF6E7C80)
    val OutlineVariantLight = Color(0xFFBECBCD)

    val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
    val SurfaceContainerLowLight = Color(0xFFEEF3F4)
    val SurfaceContainerLight = Color(0xFFE8EFF0)
    val SurfaceContainerHighLight = Color(0xFFE1E9EA)
    val SurfaceContainerHighestLight = Color(0xFFD9E2E3)
    val SurfaceDimLight = Color(0xFFD0DADC)
    val SurfaceBrightLight = Color(0xFFF7F9FA)

    val ErrorLight = Color(0xFFB3261E)
    val OnErrorLight = Color(0xFFFFFFFF)
    val ErrorContainerLight = Color(0xFFF9DEDC)
    val OnErrorContainerLight = Color(0xFF410E0B)

    // ---- dark --------------------------------------------------------------
    val PrimaryDark = Color(0xFF62D5D7)
    val OnPrimaryDark = Color(0xFF00363A)
    val PrimaryContainerDark = Color(0xFF005F64)
    val OnPrimaryContainerDark = Color(0xFFC4EEF0)

    val SecondaryDark = Color(0xFFB0CCD1)
    val OnSecondaryDark = Color(0xFF1B3439)
    val SecondaryContainerDark = Color(0xFF334B50)
    val OnSecondaryContainerDark = Color(0xFFCCE8EC)

    val TertiaryDark = Color(0xFFFFB95E)
    val OnTertiaryDark = Color(0xFF542B00)
    val TertiaryContainerDark = Color(0xFF773F00)
    val OnTertiaryContainerDark = Color(0xFFFFDDB5)

    val BackgroundDark = Color(0xFF10191B)
    val OnBackgroundDark = Color(0xFFE0E9EA)
    val SurfaceDark = Color(0xFF10191B)
    val OnSurfaceDark = Color(0xFFE0E9EA)
    val SurfaceVariantDark = Color(0xFF3F4A4D)
    val OnSurfaceVariantDark = Color(0xFFBECBCD)
    val OutlineDark = Color(0xFF899598)
    val OutlineVariantDark = Color(0xFF3F4A4D)

    val SurfaceContainerLowestDark = Color(0xFF0C0C11)
    val SurfaceContainerLowDark = Color(0xFF182225)
    val SurfaceContainerDark = Color(0xFF1C292B)
    val SurfaceContainerHighDark = Color(0xFF273437)
    val SurfaceContainerHighestDark = Color(0xFF323F42)
    val SurfaceDimDark = Color(0xFF10191B)
    val SurfaceBrightDark = Color(0xFF39474A)

    val ErrorDark = Color(0xFFF2B8B5)
    val OnErrorDark = Color(0xFF601410)
    val ErrorContainerDark = Color(0xFF8C1D18)
    val OnErrorContainerDark = Color(0xFFF9DEDC)

    // ---- semantic (extended) ----------------------------------------------
    val SuccessLight = Color(0xFF146C2E)
    val OnSuccessLight = Color(0xFFFFFFFF)
    val SuccessContainerLight = Color(0xFFC2F0CE)
    val OnSuccessContainerLight = Color(0xFF00210B)

    val SuccessDark = Color(0xFF6DD58C)
    val OnSuccessDark = Color(0xFF00391A)
    val SuccessContainerDark = Color(0xFF11512C)
    val OnSuccessContainerDark = Color(0xFFC2F0CE)

    val WarningLight = Color(0xFF8A5300)
    val OnWarningLight = Color(0xFFFFFFFF)
    val WarningContainerLight = Color(0xFFFFE0B2)
    val OnWarningContainerLight = Color(0xFF2C1700)

    val WarningDark = Color(0xFFFFB95E)
    val OnWarningDark = Color(0xFF4A2800)
    val WarningContainerDark = Color(0xFF693A00)
    val OnWarningContainerDark = Color(0xFFFFE0B2)

    // ---- storage kinds -----------------------------------------------------
    /** USB / OTG: cool teal, reads as "plugged in hardware". */
    val UsbLight = Color(0xFF0F766E)
    val UsbDark = Color(0xFF5EEAD4)

    /** SD card: amber, distinct from both USB and internal. */
    val SdLight = Color(0xFFB45309)
    val SdDark = Color(0xFFFCD34D)

    // ---- media surfaces ----------------------------------------------------
    val MediaOverlayScrim = Color(0x66000000)
    val VideoPlaceholder = Color(0xFF1B1B24)
    val PlayerScrim = Color(0xB3000000)

    /**
     * Colour of a storage-usage ring by how full the volume is: comfortable, getting full, full.
     * Keeping this out of the primary colour is what makes "92 % used" readable at a glance.
     */
    fun usageColor(fraction: Float, dark: Boolean): Color = when {
        fraction >= 0.92f -> if (dark) ErrorDark else ErrorLight
        fraction >= 0.75f -> if (dark) WarningDark else WarningLight
        else -> if (dark) SuccessDark else SuccessLight
    }
}
