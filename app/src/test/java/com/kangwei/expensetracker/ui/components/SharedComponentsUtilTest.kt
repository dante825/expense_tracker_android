package com.kangwei.expensetracker.ui.components

import org.junit.Assert.*
import org.junit.Test
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class SharedComponentsUtilTest {

    // ── formatCurrency ────────────────────────────────────────────────────────

    @Test fun formatCurrency_producesLocaleFormattedString() {
        val expected = NumberFormat.getCurrencyInstance().format(42.5)
        assertEquals(expected, formatCurrency(42.5))
    }

    @Test fun formatCurrency_zeroAmount_producesLocaleZero() {
        val expected = NumberFormat.getCurrencyInstance().format(0.0)
        assertEquals(expected, formatCurrency(0.0))
    }

    @Test fun formatCurrency_negativeAmount_matchesNumberFormat() {
        val expected = NumberFormat.getCurrencyInstance().format(-10.0)
        assertEquals(expected, formatCurrency(-10.0))
    }

    // ── formatDate ────────────────────────────────────────────────────────────

    @Test fun formatDate_epochZero_formatsViaSimpleDateFormat() {
        val expected = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(0L))
        assertEquals(expected, formatDate(0L))
    }

    @Test fun formatDate_specificMillis_returnsCorrectString() {
        // 2024-03-15 00:00:00 UTC in millis
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            set(2024, Calendar.MARCH, 15, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val millis = cal.timeInMillis
        val result = formatDate(millis)
        assertTrue("Expected 'Mar' in date string but got: $result", result.startsWith("Mar"))
        assertTrue("Expected '15' in date string but got: $result", result.contains("15"))
        assertTrue("Expected '2024' in date string but got: $result", result.contains("2024"))
    }
}
