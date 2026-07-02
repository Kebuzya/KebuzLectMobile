package com.kebuz.kebuzlect

import com.kebuz.kebuzlect.data.pdf.PdfImage
import com.kebuz.kebuzlect.data.pdf.PdfWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import javax.imageio.ImageIO
import kotlin.math.min

class PdfWriterTest {

    @Test
    fun singlePhotoOnePerPage() {
        val pdf = pdf(listOf(image(1200, 900, "1", Color(0xC0, 0x39, 0x2B))), perPage = 1)
        validate(pdf)
        writeSample("sample_1_single.pdf", pdf)
    }

    @Test
    fun twoPhotosTwoPerPage() {
        val pdf = pdf(
            listOf(
                image(1200, 900, "1", Color(0xC0, 0x39, 0x2B)),
                image(900, 1200, "2", Color(0x29, 0x80, 0xB9)),
            ),
            perPage = 2,
        )
        validate(pdf)
        writeSample("sample_2_two_per_page.pdf", pdf)
    }

    @Test
    fun oddThreePhotosTwoPerPage() {
        val pdf = pdf(
            listOf(
                image(1200, 900, "1", Color(0xC0, 0x39, 0x2B)),
                image(900, 1200, "2", Color(0x29, 0x80, 0xB9)),
                image(1000, 1000, "3", Color(0x27, 0xAE, 0x60)),
            ),
            perPage = 2,
        )
        validate(pdf)
        writeSample("sample_3_odd_three.pdf", pdf)
    }

    @Test
    fun threePhotosOnePerPage() {
        val pdf = pdf(
            listOf(
                image(1200, 900, "1", Color(0xC0, 0x39, 0x2B)),
                image(900, 1200, "2", Color(0x29, 0x80, 0xB9)),
                image(1000, 1000, "3", Color(0x27, 0xAE, 0x60)),
            ),
            perPage = 1,
        )
        validate(pdf)
        writeSample("sample_4_one_per_page_three.pdf", pdf)
    }

    @Test
    fun emptyDocumentIsValid() {
        validate(pdf(emptyList(), perPage = 2))
    }

    private fun pdf(images: List<PdfImage>, perPage: Int): ByteArray {
        val baos = ByteArrayOutputStream()
        PdfWriter.write(baos, images, perPage)
        return baos.toByteArray()
    }

    private fun validate(bytes: ByteArray) {
        val text = String(bytes, StandardCharsets.ISO_8859_1)
        assertTrue("missing header", text.startsWith("%PDF-1.4"))
        assertTrue("missing EOF", text.trimEnd().endsWith("%%EOF"))

        val startxrefIndex = text.lastIndexOf("startxref")
        assertTrue("no startxref", startxrefIndex >= 0)
        val xrefOffset = text.substring(startxrefIndex + "startxref".length)
            .trim().substringBefore('\n').trim().toInt()
        assertEquals("xref", String(bytes, xrefOffset, 4, StandardCharsets.ISO_8859_1))

        val headerStart = xrefOffset + "xref\n".length
        val headerEnd = text.indexOf('\n', headerStart)
        val size = text.substring(headerStart, headerEnd).trim().split(" ")[1].toInt()
        val entriesStart = headerEnd + 1

        for (n in 1 until size) {
            val entryOffset = entriesStart + n * 20
            val declared = String(bytes, entryOffset, 10, StandardCharsets.ISO_8859_1).toInt()
            val header = String(bytes, declared, "$n 0 obj".length, StandardCharsets.ISO_8859_1)
            assertEquals("xref offset wrong for object $n", "$n 0 obj", header)
        }
        assertTrue("trailer /Size mismatch", text.contains("/Size $size"))
    }

    private fun image(width: Int, height: Int, label: String, color: Color): PdfImage {
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.color = color
        g.fillRect(0, 0, width, height)
        g.color = Color.WHITE
        g.font = Font("SansSerif", Font.BOLD, min(width, height) / 3)
        g.drawString(label, width / 12, height * 6 / 10)
        g.color = Color.BLACK
        g.drawRect(0, 0, width - 1, height - 1)
        g.dispose()
        val baos = ByteArrayOutputStream()
        ImageIO.write(img, "jpg", baos)
        return PdfImage(baos.toByteArray(), width, height)
    }

    private fun writeSample(name: String, bytes: ByteArray) {
        val dir = File("build/sample-pdfs").apply { mkdirs() }
        File(dir, name).writeBytes(bytes)
    }
}
