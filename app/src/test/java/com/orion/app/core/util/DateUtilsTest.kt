package com.orion.app.core.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/**
 * Unit tests for [DateUtils]. English UI labels ("Unknown date", "Released"...) come from
 * `strings.xml`, while day/month names are always expected in English regardless of locale,
 * per the app's date-formatting policy (see [DateUtils]'s class doc).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [])
class DateUtilsTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `formatDay returns unknown date when input is null or blank`() {
        assertEquals(
            "Unknown date",
            DateUtils.formatDay(context, null)
        )
        assertEquals(
            "Unknown date",
            DateUtils.formatDay(context, "")
        )
    }

    @Test
    fun `formatDay returns the raw input when the ISO format is invalid`() {
        assertEquals(
            "not a date",
            DateUtils.formatDay(context, "not a date")
        )
    }

    @Test
    fun `formatDay returns a far past date without a day offset`() {
        val result = DateUtils.formatDay(context, "2020-01-03")

        assertEquals(
            "Friday January 3 2020",
            result
        )
    }

    @Test
    fun `formatDay returns a far future date without a day offset`() {
        val result = DateUtils.formatDay(context, "2030-08-07")

        assertEquals(
            "Wednesday August 7 2030",
            result
        )
    }

    @Test
    fun `formatDay omits the year when the date falls in the current year`() {
        val currentYear = LocalDate.now().year

        val result = DateUtils.formatDay(
            context,
            "$currentYear-12-25"
        )

        assertTrue(
            "expected no year for the current year, got: $result",
            !result.contains(currentYear.toString())
        )
    }

    @Test
    fun `daysRemainingLabel returns D-minus-n days for a far future date`() {
        val farFuture = LocalDate.now().plusDays(10).toString()

        assertEquals(
            "D-10",
            DateUtils.daysRemainingLabel(context, farFuture)
        )
    }

    @Test
    fun `daysRemainingLabel returns Released for a past date`() {
        val past = LocalDate.now().minusDays(5).toString()

        assertEquals(
            "Released",
            DateUtils.daysRemainingLabel(context, past)
        )
    }

    @Test
    fun `daysRemainingLabel returns a dash when the input is invalid`() {
        assertEquals(
            "—",
            DateUtils.daysRemainingLabel(context, null)
        )
        assertEquals(
            "—",
            DateUtils.daysRemainingLabel(context, "invalid")
        )
    }

    @Test
    fun `isReleased is true only strictly before today`() {
        val yesterday = LocalDate.now().minusDays(1).toString()
        val today = LocalDate.now().toString()
        val tomorrow = LocalDate.now().plusDays(1).toString()

        assertTrue(DateUtils.isReleased(yesterday))
        assertFalse(DateUtils.isReleased(today))
        assertFalse(DateUtils.isReleased(tomorrow))
    }

    @Test
    fun `isWatchable is true on and before today, false after`() {
        val yesterday = LocalDate.now().minusDays(1).toString()
        val today = LocalDate.now().toString()
        val tomorrow = LocalDate.now().plusDays(1).toString()

        assertTrue(DateUtils.isWatchable(yesterday))
        assertTrue(DateUtils.isWatchable(today))
        assertFalse(DateUtils.isWatchable(tomorrow))
    }
}
