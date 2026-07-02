package com.kebuz.kebuzlect

import com.kebuz.kebuzlect.data.pdf.PdfConverter
import org.junit.Assert.assertEquals
import org.junit.Test

class PdfFilenameTest {

    @Test
    fun subjectAndDate() {
        assertEquals(
            "САПР_20260413.pdf",
            PdfConverter.buildFilename("САПР", "20260413", null, "{predmet}_{YYYYMMDD}", 3),
        )
    }

    @Test
    fun lectureNumberZeroPadded() {
        assertEquals(
            "САПР_007.pdf",
            PdfConverter.buildFilename("САПР", "20260413", 7, "{predmet}_{lection_number}", 3),
        )
    }

    @Test
    fun nullLectureNumberBecomesZero() {
        assertEquals(
            "САПР_000.pdf",
            PdfConverter.buildFilename("САПР", "20260413", null, "{predmet}_{lection_number}", 3),
        )
    }

    @Test
    fun sanitizesInvalidFilenameChars() {
        assertEquals(
            "a_b_c_.pdf",
            PdfConverter.buildFilename("a/b:c?", "20260413", null, "{predmet}", 3),
        )
    }
}
