package com.readrops.db.util

import android.annotation.SuppressLint
import android.util.Log
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.TimeZone

object DateUtils {

    private val TAG = DateUtils::class.java.simpleName

    private val dateFormatters = listOf(
        DateTimeFormatter.RFC_1123_DATE_TIME,
        DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss XXX", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss", Locale.ENGLISH),
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
    )

    val defaultOffset: ZoneOffset
        get() = OffsetDateTime.now(TimeZone.getDefault().toZoneId())
            .offset

    /**
     * Attempts to parse a date string representation.
     * If the provided value is null or the parsing fails, [LocalDateTime.now] is returned.
     * @return parsed date or [LocalDateTime.now]
     */
    @SuppressLint("NewApi") // works with API 21+ so the lint might be buggy
    @JvmStatic
    fun parse(value: String?): LocalDateTime {
        if (value == null) {
            return LocalDateTime.now()
        }

        val formattedValues = buildList {
            add(value.trim())
            if (value.contains(",")) {
                add(value.substringAfter(",").trim())
            }
        }

        for (formattedValue in formattedValues) {
            for (formatter in dateFormatters) {
                val parsed = runCatching {
                    formatter.parseBest(
                        formattedValue,
                        ZonedDateTime::from,
                        OffsetDateTime::from,
                        LocalDateTime::from
                    )
                }.getOrNull() ?: continue

                // An offset denotes a precise instant, so it must be converted to the
                // reader's zone instead of being dropped. Keeping the author's wall clock
                // skewed sorting across feeds from different zones, and the last 24 hours
                // filter, which both work on the stored pub_date.
                return when (parsed) {
                    is ZonedDateTime -> parsed.withZoneSameInstant(ZoneId.systemDefault())
                        .toLocalDateTime()

                    is OffsetDateTime -> parsed.atZoneSameInstant(ZoneId.systemDefault())
                        .toLocalDateTime()

                    is LocalDateTime -> parsed
                    else -> continue
                }
            }
        }

        Log.e(TAG, "Unable to parse $value")
        return LocalDateTime.now()
    }

    /**
     * Be aware of giving a second epoch value and not a millisecond one!
     */
    fun fromEpochSeconds(epoch: Long): LocalDateTime {
        return LocalDateTime.ofEpochSecond(epoch, 0, defaultOffset)
    }

    fun formattedDateByLocal(dateTime: LocalDateTime): String {
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
            .format(dateTime)
    }
}
