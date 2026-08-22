package com.readrops.db

import com.readrops.db.util.DateUtils
import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class DateUtilsTest {

    /**
     * A feed date carrying an offset denotes a precise instant, which the parser converts to
     * the reader's zone. Expectations are therefore derived from the instant rather than
     * hardcoded, so the suite does not depend on the zone the machine happens to run in.
     */
    private fun localOf(instant: String): LocalDateTime =
        Instant.parse(instant).atZone(ZoneId.systemDefault()).toLocalDateTime()

    @Test
    fun rssDateTest() {
        val dateTime = localOf("2019-01-04T22:21:46Z")
        assertEquals(0, dateTime.compareTo(DateUtils.parse("Fri, 04 Jan 2019 22:21:46 GMT")))
    }

    @Test
    fun rssDate2Test() {
        val dateTime = localOf("2019-01-04T22:21:46Z")
        assertEquals(0, dateTime.compareTo(DateUtils.parse("Fri, 04 Jan 2019 22:21:46 +0000")))
    }

    @Test
    fun rssDateWithNumericOffsetTest() {
        // 22:21:46 in +13:00 is 09:21:46 UTC
        val dateTime = localOf("2019-01-04T09:21:46Z")
        assertEquals(0, dateTime.compareTo(DateUtils.parse("Fri, 04 Jan 2019 22:21:46 +1300")))
    }

    @Test
    fun rssDate3Test() {
        // no offset at all: the wall clock is all we have, nothing to convert
        val dateTime = LocalDateTime.of(2019, 1, 4, 22, 21, 46)
        assertEquals(0, dateTime.compareTo(DateUtils.parse("Fri, 04 Jan 2019 22:21:46")))
    }

    @Test
    fun edtPatternTest() {
        // EDT is UTC-4
        val dateTime = localOf("2020-07-17T20:30:00Z")
        assertEquals(0, dateTime.compareTo(DateUtils.parse("Fri, 17 Jul 2020 16:30:00 EDT")))
    }

    @Test
    fun atomJsonDateTest() {
        val dateTime = localOf("2019-01-04T22:21:46Z")
        assertEquals(0, dateTime.compareTo(DateUtils.parse("2019-01-04T22:21:46+00:00")))
    }

    @Test
    fun atomJsonDate2Test() {
        val dateTime = localOf("2019-01-04T22:21:46Z")
        assertEquals(0, dateTime.compareTo(DateUtils.parse("2019-01-04T22:21:46-0000")))
    }

    @Test
    fun isoPatternTest() {
        // 11:39:37.206 in -07:00 is 18:39:37.206 UTC
        val dateTime = localOf("2020-06-30T18:39:37.206Z")
        assertEquals(0, dateTime.compareTo(DateUtils.parse("2020-06-30T11:39:37.206-07:00")))
    }
}
