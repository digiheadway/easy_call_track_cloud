package com.clicktoearn.linkbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownContent(content: String) {
    val lines = content.split("\n")
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        lines.forEach { line ->
            when {
                line.startsWith("# ") -> {
                    // H1 Heading
                    Text(
                        text = line.removePrefix("# "),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                line.startsWith("## ") -> {
                    // H2 Heading
                    Text(
                        text = line.removePrefix("## "),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                line.startsWith("### ") -> {
                    // H3 Heading
                    Text(
                        text = line.removePrefix("### "),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    // Bullet point
                    Row(
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        FormattedText(
                            text = line.substring(2),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                line.startsWith("> ") -> {
                    // Blockquote
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .matchParentSize()
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(2.dp)
                                )
                        )
                        Text(
                            text = line.removePrefix("> "),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                else -> {
                    // Regular paragraph with inline formatting
                    FormattedText(
                        text = line,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 24.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun FormattedText(
    text: String,
    style: TextStyle
) {
    val annotatedString = buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                // Bold **text**
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Italic *text*
                text.startsWith("*", i) && !text.startsWith("**", i) -> {
                    val end = text.indexOf("*", i + 1)
                    if (end != -1 && !text.startsWith("**", end)) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Inline code `code`
                text.startsWith("`", i) -> {
                    val end = text.indexOf("`", i + 1)
                    if (end != -1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color(0x20808080)
                            )
                        ) {
                            append(" ${text.substring(i + 1, end)} ")
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Strikethrough ~~text~~
                text.startsWith("~~", i) -> {
                    val end = text.indexOf("~~", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Link [text](url)
                text.startsWith("[", i) -> {
                    val textEnd = text.indexOf("]", i)
                    val urlStart = text.indexOf("(", textEnd)
                    val urlEnd = text.indexOf(")", urlStart)
                    if (textEnd != -1 && urlStart == textEnd + 1 && urlEnd != -1) {
                        val linkText = text.substring(i + 1, textEnd)
                        withStyle(
                            SpanStyle(
                                color = Color(0xFF2196F3),
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append(linkText)
                        }
                        i = urlEnd + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
    
    Text(
        text = annotatedString,
        style = style
    )
}
