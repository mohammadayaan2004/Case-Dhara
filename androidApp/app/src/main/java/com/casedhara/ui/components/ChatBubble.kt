package com.casedhara.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.casedhara.domain.model.ChatMessage
import com.casedhara.ui.components.design.GlassSurface
import com.casedhara.ui.theme.DharaRadius
import com.casedhara.ui.theme.GreenIndia
import com.casedhara.ui.theme.SaffronBright

private fun parseMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    val lines = text.split("\n")
    lines.forEachIndexed { index, rawLine ->
        val line = rawLine.trimEnd()
        val headingMatch = Regex("""^#{1,3}\s+(.*)""").matchEntire(line)
        if (headingMatch != null) {
            appendInline(headingMatch.groupValues[1], bold = true)
            if (index < lines.lastIndex) append("\n")
            return@forEachIndexed
        }
        val numberedMatch = Regex("""^(\d+\.\s+)(.*)""").matchEntire(line)
        if (numberedMatch != null) {
            append(numberedMatch.groupValues[1])
            appendInline(numberedMatch.groupValues[2])
            if (index < lines.lastIndex) append("\n")
            return@forEachIndexed
        }
        val bulletMatch = Regex("""^[-•]\s+(.*)""").matchEntire(line)
        if (bulletMatch != null) {
            append("• ")
            appendInline(bulletMatch.groupValues[1])
            if (index < lines.lastIndex) append("\n")
            return@forEachIndexed
        }
        appendInline(line)
        if (index < lines.lastIndex) append("\n")
    }
}

private fun AnnotatedString.Builder.appendInline(text: String, bold: Boolean = false) {
    val pattern = Regex("""\*\*(.+?)\*\*|\*(.+?)\*""")
    var cursor = 0
    for (match in pattern.findAll(text)) {
        if (match.range.first > cursor) {
            val plain = text.substring(cursor, match.range.first)
            if (bold) {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(plain)
                pop()
            } else {
                append(plain)
            }
        }
        when {
            match.groupValues[1].isNotEmpty() -> {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(match.groupValues[1])
                pop()
            }
            match.groupValues[2].isNotEmpty() -> {
                val style = if (bold)
                    SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
                else
                    SpanStyle(fontStyle = FontStyle.Italic)
                pushStyle(style)
                append(match.groupValues[2])
                pop()
            }
        }
        cursor = match.range.last + 1
    }
    if (cursor < text.length) {
        val remaining = text.substring(cursor)
        if (bold) {
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append(remaining)
            pop()
        } else {
            append(remaining)
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    val isUser = message.role == "user"
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        GlassSurface(
            modifier = Modifier.widthIn(max = 340.dp),
            cornerRadius = DharaRadius.lg,
            glowColor = if (isUser) SaffronBright else GreenIndia,
        ) {
            Text(
                text = if (isUser) AnnotatedString(message.content) else parseMarkdown(message.content),
                modifier = Modifier.padding(14.dp),
                color = if (isUser) Color(0xFF1A1208) else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
