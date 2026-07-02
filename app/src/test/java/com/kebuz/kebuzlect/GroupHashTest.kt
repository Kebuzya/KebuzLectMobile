package com.kebuz.kebuzlect

import com.kebuz.kebuzlect.data.scanner.computeGroupHash
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupHashTest {

    @Test
    fun matchesDesktopGoldenValue() {
        assertEquals(
            "fff1ee5eaa956743724f7a16693ba643",
            computeGroupHash(listOf("b.jpg", "a.jpg")),
        )
    }

    @Test
    fun isOrderIndependent() {
        assertEquals(
            computeGroupHash(listOf("a.jpg", "b.jpg")),
            computeGroupHash(listOf("b.jpg", "a.jpg")),
        )
    }

    @Test
    fun singleFile() {
        assertEquals(
            "3d7672dbd851fe6419cf3791dda290c8",
            computeGroupHash(listOf("IMG_20260413_135545.jpg")),
        )
    }
}
