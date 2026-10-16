package com.readrops.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.annotation.ColorInt
import com.readrops.app.R
import com.readrops.db.pojo.ItemWithFeed
import java.util.Locale

object Utils {

    private const val AVERAGE_WORDS_PER_MINUTE = 250

    fun readTimeFromString(value: String): Double {
        val nbWords = value.split(Regex("\\s+")).size
        return nbWords.toDouble() / AVERAGE_WORDS_PER_MINUTE
    }

    fun getCssColor(@ColorInt color: Int): String {
        return String.format(
            Locale.US, "rgba(%d,%d,%d,%.2f)",
            Color.red(color),
            Color.green(color),
            Color.blue(color),
            Color.alpha(color) / 255.0
        )
    }

    fun normalizeUrl(url: String): String {
        return buildString {
            if (!url.contains("https://") && !url.contains("http://")) {
                append("https://$url")
            } else {
                append(url)
            }

            if (!url.endsWith("/")) {
                append("/")
            }
        }
    }

    fun shareItem(
        itemWithFeed: ItemWithFeed,
        context: Context,
        useCustomShareIntentTpl: Boolean,
        customShareIntentTpl: String
    ) {
        val intentContent =
            if(!useCustomShareIntentTpl || customShareIntentTpl.isBlank()) itemWithFeed.item.link
            else ShareIntentTextRenderer(itemWithFeed).render(customShareIntentTpl)
        Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, intentContent)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }.also {
            context.startActivity(Intent.createChooser(it, null))
        }
    }

    fun formatSelectionWithReference(
        selectedText: String,
        author: String?,
        title: String?,
        feedName: String?,
        url: String?
    ): String {
        val citation = buildList {
            author?.takeIf(String::isNotBlank)?.let(::add)
            title?.takeIf(String::isNotBlank)?.let { add("\"$it\"") }
            feedName?.takeIf(String::isNotBlank)?.let(::add)
        }.joinToString(", ")
        return listOf(selectedText.trim(), citation, url.orEmpty())
            .filter(String::isNotBlank)
            .joinToString("\n\n")
    }

    fun shareSelectionWithReference(
        selectedText: String,
        itemWithFeed: ItemWithFeed,
        context: Context
    ) {
        val item = itemWithFeed.item
        val content = formatSelectionWithReference(
            selectedText = selectedText,
            author = item.author,
            title = item.title,
            feedName = itemWithFeed.feedName,
            url = item.link
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
        }
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.share_selection_with_reference))
        )
    }

    fun truncateString(text: String, max: Int): String {
        return if (text.length > max) {
            text.substring(0, max) + "..."
        } else {
            text
        }
    }
}
