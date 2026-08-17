package com.quicklogger.app

import com.quicklogger.app.presentation.theme.BrandColors
import org.junit.Assert.assertEquals
import org.junit.Test

class BrandColorsTest {
    @Test
    fun primaryIsSealingWax() {
        assertEquals(0xFF9A4A32, BrandColors.PRIMARY)
    }

    @Test
    fun onPrimaryIsWarmWhite() {
        assertEquals(0xFFFFF8F3, BrandColors.ON_PRIMARY)
    }

    @Test
    fun surfaceIsCreamPaper() {
        assertEquals(0xFFF6F1E8, BrandColors.SURFACE)
    }

    @Test
    fun surfaceContainerIsDeeperPaper() {
        assertEquals(0xFFEFE7D8, BrandColors.SURFACE_CONTAINER)
    }

    @Test
    fun onSurfaceIsInk() {
        assertEquals(0xFF2A241F, BrandColors.ON_SURFACE)
    }

    @Test
    fun onSurfaceVariantIsMutedInk() {
        assertEquals(0xFF6F675E, BrandColors.ON_SURFACE_VARIANT)
    }

    @Test
    fun outlineIsWarmStone() {
        assertEquals(0xFFC9BBA8, BrandColors.OUTLINE)
    }

    @Test
    fun errorIsBrick() {
        assertEquals(0xFF9B2F2F, BrandColors.ERROR)
    }
}
