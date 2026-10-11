package com.readrops.db.queries

/**
 * SQLite's cursor window has a limited size. An RSS item may legally contain several megabytes
 * of HTML, so project a bounded preview when reading items. The original text remains in the
 * database; the article link is still available for the complete content.
 */
object ItemContentProjection {

    const val MAX_TEXT_LENGTH = 100_000

    private const val DESCRIPTION_COLUMN =
        "CASE WHEN length(description) > $MAX_TEXT_LENGTH THEN substr(description, 1, $MAX_TEXT_LENGTH) || '…' ELSE description END AS description"
    private const val CLEAN_DESCRIPTION_COLUMN =
        "CASE WHEN length(clean_description) > $MAX_TEXT_LENGTH THEN substr(clean_description, 1, $MAX_TEXT_LENGTH) || '…' ELSE clean_description END AS clean_description"
    private const val CONTENT_COLUMN =
        "CASE WHEN length(content) > $MAX_TEXT_LENGTH THEN substr(content, 1, $MAX_TEXT_LENGTH) || '…' ELSE content END AS content"

    const val ITEM_COLUMNS =
        "id, title, $DESCRIPTION_COLUMN, $CLEAN_DESCRIPTION_COLUMN, link, image_link, author, pub_date, $CONTENT_COLUMN, feed_id, read_time, read, starred, remote_id"

    fun safeTextColumn(column: String): String =
        "CASE WHEN length($column) > $MAX_TEXT_LENGTH " +
                "THEN substr($column, 1, $MAX_TEXT_LENGTH) || '…' ELSE $column END AS ${column.substringAfterLast('.')}"
}
