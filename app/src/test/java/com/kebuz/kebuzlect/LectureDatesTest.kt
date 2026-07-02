package com.kebuz.kebuzlect

import com.kebuz.kebuzlect.data.scanner.parseDateFromFilename
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LectureDatesTest {

    @Test
    fun standardCameraName() {
        assertEquals("20260413", parseDateFromFilename("IMG_20260413_135545.jpg"))
    }

    @Test
    fun bareDateName() {
        assertEquals("20260413", parseDateFromFilename("20260413_135545.jpg"))
    }

    @Test
    fun noDateReturnsNull() {
        assertNull(parseDateFromFilename("photo.jpg"))
    }

    @Test
    fun rejectsInvalidMonthAndDay() {

        assertNull(parseDateFromFilename("IMG_20261301.jpg"))
        assertNull(parseDateFromFilename("IMG_20260400.jpg"))
    }

    @Test
    fun picksFirstValidEightDigitRun() {

        assertEquals("20260413", parseDateFromFilename("99999999_20260413.jpg"))
    }
}
