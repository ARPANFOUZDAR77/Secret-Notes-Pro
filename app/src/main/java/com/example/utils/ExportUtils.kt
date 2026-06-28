package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

object ExportUtils {
    
    fun shareText(context: Context, title: String, content: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(Intent.EXTRA_TEXT, "$title\n\n$content")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Note")
        context.startActivity(shareIntent)
    }

    fun exportToTxt(context: Context, uri: Uri, title: String, content: String) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            val text = "$title\n\n$content"
            outputStream.write(text.toByteArray())
        }
    }

    fun exportToHtml(context: Context, uri: Uri, title: String, content: String) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            val htmlContent = content.replace("\n", "<br>")
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                <meta charset="UTF-8">
                <title>$title</title>
                </head>
                <body>
                <h1>$title</h1>
                <p>$htmlContent</p>
                </body>
                </html>
            """.trimIndent()
            outputStream.write(html.toByteArray())
        }
    }

    fun exportToPdf(context: Context, uri: Uri, title: String, content: String) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            
            val titlePaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 24f
                isFakeBoldText = true
            }
            val contentPaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 14f
            }
            
            var yOffset = 50f
            canvas.drawText(title, 50f, yOffset, titlePaint)
            yOffset += 40f
            
            val textWidth = pageInfo.pageWidth - 100
            val staticLayout = StaticLayout.Builder.obtain(content, 0, content.length, contentPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
                
            canvas.save()
            canvas.translate(50f, yOffset)
            staticLayout.draw(canvas)
            canvas.restore()
            
            document.finishPage(page)
            document.writeTo(outputStream)
            document.close()
        }
    }
}
