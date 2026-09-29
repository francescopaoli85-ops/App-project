package com.francescopaoli.northstar.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Icone a tratto (stile Feather) e piene, le stesse del mockup. */
object NsIcons {
    private fun stroke(name: String, vararg d: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        d.forEach {
            addPath(
                addPathNodes(it), stroke = SolidColor(Color.White), strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

    private fun fill(name: String, d: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes(d), fill = SolidColor(Color.White)).build()

    /** Logo "G" di Google a 4 colori (come nel mockup). */
    val Google: ImageVector = ImageVector.Builder("google", 24.dp, 24.dp, 48f, 48f).apply {
        listOf(
            0xFFFFC107 to "M43.6 20.5H42V20H24v8h11.3c-1.6 4.7-6.1 8-11.3 8-6.6 0-12-5.4-12-12s5.4-12 12-12c3.1 0 5.8 1.1 8 3l5.7-5.7C34.6 6.1 29.6 4 24 4 12.9 4 4 12.9 4 24s8.9 20 20 20 20-8.9 20-20c0-1.3-.1-2.7-.4-3.5z",
            0xFFFF3D00 to "M6.3 14.7l6.6 4.8C14.6 15.6 18.9 13 24 13c3.1 0 5.8 1.1 8 3l5.7-5.7C34.6 6.1 29.6 4 24 4c-7.4 0-13.7 4.2-16.9 10.3z",
            0xFF4CAF50 to "M24 44c5.5 0 10.4-2.1 14.1-5.6l-6.5-5.5C29.5 34.9 26.9 36 24 36c-5.2 0-9.6-3.3-11.3-8l-6.6 5.1C9.2 39.6 16 44 24 44z",
            0xFF1976D2 to "M43.6 20.5H42V20H24v8h11.3c-.8 2.3-2.3 4.2-4.2 5.5l6.5 5.5C40.9 36.3 44 30.6 44 24c0-1.3-.1-2.7-.4-3.5z",
        ).forEach { (c, d) -> addPath(addPathNodes(d), fill = SolidColor(Color(c))) }
    }.build()

    val Star = fill("star", "M12 1l2.9 8.9H24l-7.5 5.5 2.9 8.9L12 19l-7.4 5.3 2.9-8.9L0 9.9h9.1z")
    val Sparkle = fill("sparkle", "M12 2l1.9 6.6L21 10l-7.1 1.4L12 22l-1.9-6.6L4 14l6.1-1.4z")
    val Trophy = fill(
        "trophy",
        "M19 5h-2V3H7v2H5c-1.1 0-2 .9-2 2v1c0 2.55 1.92 4.63 4.39 4.94.63 1.5 1.98 2.63 3.61 2.96V19H7v2h10v-2h-4v-3.1c1.63-.33 2.98-1.46 3.61-2.96C19.08 12.63 21 10.55 21 8V7c0-1.1-.9-2-2-2zM5 8V7h2v3.82C5.84 10.4 5 9.3 5 8zm14 0c0 1.3-.84 2.4-2 2.82V7h2v1z",
    )
    val Quote = fill(
        "quote",
        "M0 20V11.3C0 4.7 3.9.5 9.7 0l.8 2.8c-3.7.7-5.7 3.1-5.9 6.2H9.2v11H0zm14.1 0V11.3c0-6.6 3.9-10.8 9.7-11.3l.8 2.8c-3.7.7-5.7 3.1-5.9 6.2h4.6v11h-9.2z",
    )
    val Bell = stroke("bell", "M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9", "M13.7 21a2 2 0 01-3.4 0")
    val Mic = stroke(
        "mic", "M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z", "M19 10v2a7 7 0 0 1-14 0v-2", "M12 19L12 23",
    )
    val Calendar = stroke(
        "calendar", "M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z",
        "M16 2v4", "M8 2v4", "M3 10h18",
    )
    val Home = stroke("home", "M3 11l9-8 9 8", "M5 10v10h14V10")
    val Award = stroke("award", "M6 8a6 6 0 1 0 12 0a6 6 0 1 0 -12 0", "M8.2 13.6L6 22l6-3 6 3-2.2-8.4")
    val Settings = stroke(
        "settings", "M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
        "M19.4 15a1.7 1.7 0 00.3 1.9l.1.1a2 2 0 11-2.8 2.8l-.1-.1a1.7 1.7 0 00-1.9-.3 1.7 1.7 0 00-1 1.6V21a2 2 0 11-4 0v-.1a1.7 1.7 0 00-1-1.6 1.7 1.7 0 00-1.9.3l-.1.1a2 2 0 11-2.8-2.8l.1-.1a1.7 1.7 0 00.3-1.9 1.7 1.7 0 00-1.6-1H3a2 2 0 110-4h.1a1.7 1.7 0 001.6-1 1.7 1.7 0 00-.3-1.9l-.1-.1a2 2 0 112.8-2.8l.1.1a1.7 1.7 0 001.9.3H9a1.7 1.7 0 001-1.6V3a2 2 0 114 0v.1a1.7 1.7 0 001 1.6 1.7 1.7 0 001.9-.3l.1-.1a2 2 0 112.8 2.8l-.1.1a1.7 1.7 0 00-.3 1.9V9a1.7 1.7 0 001.6 1H21a2 2 0 110 4h-.1a1.7 1.7 0 00-1.6 1z",
    )
    val Close = stroke("close", "M18 6L6 18", "M6 6l12 12")
    val Back = stroke("back", "M15 18l-6-6 6-6")
    val Chevron = stroke("chevron", "M9 18l6-6-6-6")
    val Arrow = stroke("arrow", "M5 12h14", "M12 5l7 7-7 7")
    val Check = stroke("check", "M20 6L9 17l-5-5")
    val Plus = stroke("plus", "M12 5v14", "M5 12h14")
    val Pencil = stroke("pencil", "M12 20h9", "M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z")
    val Trash = stroke("trash", "M3 6h18", "M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6", "M10 11v6", "M14 11v6", "M9 6V4h6v2")
}
