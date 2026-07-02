package com.kebuz.kebuzlect.data.pdf

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.Locale
import kotlin.math.min

class PdfImage(val jpeg: ByteArray, val width: Int, val height: Int)

object PdfWriter {

    private const val PAGE_WIDTH = 595f
    private const val PAGE_HEIGHT = 842f
    private const val PAGE_MARGIN = 20f
    private const val PHOTO_GAP = 10f

    fun write(out: OutputStream, images: List<PdfImage>, photosPerPage: Int) {
        val perPage = photosPerPage.coerceAtLeast(1)
        val pageChunks: List<List<PdfImage>> =
            if (images.isEmpty()) listOf(emptyList()) else images.chunked(perPage)

        var nextObj = 3
        val pagePlans = pageChunks.map { chunk ->
            val imageObjNums = chunk.map { nextObj++ }
            val contentObjNum = nextObj++
            val pageObjNum = nextObj++
            PagePlan(chunk, imageObjNums, contentObjNum, pageObjNum)
        }
        val objectCount = nextObj - 1

        val baos = ByteArrayOutputStream()
        val offsets = IntArray(objectCount + 1)
        fun text(s: String) = baos.write(s.toByteArray(Charsets.ISO_8859_1))
        fun beginObj(n: Int) {
            offsets[n] = baos.size()
            text("$n 0 obj\n")
        }

        text("%PDF-1.4\n")
        text("%âãÏÓ\n")

        beginObj(1)
        text("<< /Type /Catalog /Pages 2 0 R >>\n")
        text("endobj\n")

        beginObj(2)
        val kids = pagePlans.joinToString(" ") { "${it.pageObjNum} 0 R" }
        text("<< /Type /Pages /Kids [ $kids ] /Count ${pagePlans.size} >>\n")
        text("endobj\n")

        for (plan in pagePlans) {
            plan.images.forEachIndexed { i, image ->
                beginObj(plan.imageObjNums[i])
                text(
                    "<< /Type /XObject /Subtype /Image /Width ${image.width} /Height ${image.height} " +
                        "/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${image.jpeg.size} >>\n",
                )
                text("stream\n")
                baos.write(image.jpeg)
                text("\nendstream\n")
                text("endobj\n")
            }

            val contentBytes = buildContent(plan, perPage).toByteArray(Charsets.ISO_8859_1)
            beginObj(plan.contentObjNum)
            text("<< /Length ${contentBytes.size} >>\n")
            text("stream\n")
            baos.write(contentBytes)
            text("\nendstream\n")
            text("endobj\n")

            beginObj(plan.pageObjNum)
            val xobjects = plan.imageObjNums.mapIndexed { i, num -> "/Im$i $num 0 R" }.joinToString(" ")
            text(
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${PAGE_WIDTH.toInt()} ${PAGE_HEIGHT.toInt()}] " +
                    "/Resources << /XObject << $xobjects >> >> /Contents ${plan.contentObjNum} 0 R >>\n",
            )
            text("endobj\n")
        }

        val xrefOffset = baos.size()
        text("xref\n")
        text("0 ${objectCount + 1}\n")
        text("0000000000 65535 f \n")
        for (n in 1..objectCount) {
            text(String.format(Locale.US, "%010d 00000 n \n", offsets[n]))
        }

        text("trailer\n")
        text("<< /Size ${objectCount + 1} /Root 1 0 R >>\n")
        text("startxref\n")
        text("$xrefOffset\n")
        text("%%EOF\n")

        out.write(baos.toByteArray())
    }

    private fun buildContent(plan: PagePlan, perPage: Int): String {
        if (plan.images.isEmpty()) return ""
        val usableWidth = PAGE_WIDTH - 2 * PAGE_MARGIN
        val gap = if (perPage > 1) PHOTO_GAP else 0f
        val slotHeight = (PAGE_HEIGHT - 2 * PAGE_MARGIN - gap * (perPage - 1)) / perPage
        val sb = StringBuilder()
        plan.images.forEachIndexed { slotIndex, image ->
            val slotTop = PAGE_HEIGHT - PAGE_MARGIN - slotIndex * (slotHeight + gap)
            val slotY = slotTop - slotHeight
            val scale = min(usableWidth / image.width, slotHeight / image.height)
            val drawW = image.width * scale
            val drawH = image.height * scale
            val offsetX = PAGE_MARGIN + (usableWidth - drawW) / 2
            val offsetY = slotY + (slotHeight - drawH) / 2

            sb.append(
                String.format(
                    Locale.US,
                    "q %.2f 0 0 %.2f %.2f %.2f cm /Im%d Do Q\n",
                    drawW, drawH, offsetX, offsetY, slotIndex,
                ),
            )
        }
        return sb.toString()
    }

    private class PagePlan(
        val images: List<PdfImage>,
        val imageObjNums: List<Int>,
        val contentObjNum: Int,
        val pageObjNum: Int,
    )
}
