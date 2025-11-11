package ru.vt.presentation.measurements.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import ru.vt.domain.measurement.exceptions.InvalidPeriodValuesException
import ru.vt.presentation.measurements.common.entity.Period
import ru.vt.presentation.measurements.common.entity.getFromToValues

internal class PeriodTest {

    @Test
    fun `check Day period`() {
        val p = Period.Day

        val date = LocalDateTime.of(2022, 3, 1, 11, 34)
        val localDate = date.toLocalDate()

        val (from, to) = p.getFromToValues(date = localDate)

        // Calculate expected values using the same logic as the code
        val expectedFrom = localDate.atStartOfDay()
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val expectedTo = localDate.plusDays(1).atStartOfDay()
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        assertEquals(expectedFrom, from)
        assertEquals(expectedTo, to)
    }

    @Test
    fun `check Week period`() {
        val p = Period.Week

        val date = LocalDateTime.of(2022, 3, 3, 11, 34)
        val localDate = date.toLocalDate()

        val (from, to) = p.getFromToValues(date = localDate)

        // Calculate expected values using the same logic as the code
        val expectedFrom = localDate.with(DayOfWeek.MONDAY).atStartOfDay()
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val expectedTo = localDate.plusWeeks(1).with(DayOfWeek.MONDAY).atStartOfDay()
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        assertEquals(expectedFrom, from)
        assertEquals(expectedTo, to)
    }

    @Test
    fun `check Month period`() {
        val p = Period.Month

        val date = LocalDateTime.of(2022, 4, 3, 11, 34)
        val localDate = date.toLocalDate()

        val (from, to) = p.getFromToValues(date = localDate)

        // Calculate expected values using the same logic as the code
        val expectedFrom = localDate.withDayOfMonth(1).atStartOfDay()
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val expectedTo = localDate.plusMonths(1).withDayOfMonth(1).atStartOfDay()
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        assertEquals(expectedFrom, from)
        assertEquals(expectedTo, to)
    }

    @Test
    fun `check Last6Months period`() {
        val p = Period.Last6Months

        val date = LocalDateTime.of(2022, 4, 3, 11, 34)
        val localDate = date.toLocalDate()

        val (from, to) = p.getFromToValues(date = localDate)

        // Calculate expected values using the same logic as the code
        val expectedFrom = localDate.minusMonths(5).withDayOfMonth(1).atStartOfDay()
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val expectedTo = localDate.plusMonths(1).withDayOfMonth(1).atStartOfDay()
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        assertEquals(expectedFrom, from)
        assertEquals(expectedTo, to)
    }


    @Test
    fun `check Year period`() {
        val p = Period.Year

        val date = LocalDateTime.of(2022, 4, 3, 11, 34)
        val localDate = date.toLocalDate()

        val (from, to) = p.getFromToValues(date = localDate)

        // Calculate expected values using the same logic as the code
        val expectedFrom = localDate.withMonth(1).withDayOfMonth(1).atStartOfDay()
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val expectedTo = localDate.plusYears(1).withMonth(1).withDayOfMonth(1).atStartOfDay()
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        assertEquals(expectedFrom, from)
        assertEquals(expectedTo, to)
    }

    @Test
    fun `should throw InvalidCustomPeriodValuesException when from is bigger then to for Custom period`() {
        val p = Period.Custom(System.currentTimeMillis(), System.currentTimeMillis() - 100)

        assertThrows<InvalidPeriodValuesException> {
            p.getFromToValues(LocalDate.now())
        }
    }

}