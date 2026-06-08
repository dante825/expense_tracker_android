package com.kangwei.expensetracker.ui.addedit

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests the isValid predicate from AddEditExpenseViewModel.
 * The combine() logic is duplicated here to run on the JVM without
 * needing an Application context.
 */
class AddEditExpenseViewModelLogicTest {

    // Mirrors the isValid StateFlow in AddEditExpenseViewModel exactly.
    private suspend fun isValid(amount: String, categoryId: Long?): Boolean {
        val amountFlow = MutableStateFlow(amount)
        val catFlow = MutableStateFlow(categoryId)
        return combine(amountFlow, catFlow) { amt, catId ->
            amt.toDoubleOrNull() != null && catId != null
        }.first()
    }

    // ── isValid ──────────────────────────────────────────────────────────────

    @Test fun isValid_validAmountAndCategory_returnsTrue() = runTest {
        assertTrue(isValid("25.0", 1L))
    }

    @Test fun isValid_integerAmount_returnsTrue() = runTest {
        assertTrue(isValid("10", 1L))
    }

    @Test fun isValid_blankAmount_returnsFalse() = runTest {
        assertFalse(isValid("", 1L))
    }

    @Test fun isValid_nonNumericAmount_returnsFalse() = runTest {
        assertFalse(isValid("abc", 1L))
    }

    @Test fun isValid_nullCategory_returnsFalse() = runTest {
        assertFalse(isValid("25.0", null))
    }

    @Test fun isValid_bothInvalid_returnsFalse() = runTest {
        assertFalse(isValid("", null))
    }

    @Test fun isValid_zeroAmount_returnsTrue() = runTest {
        // 0.0 is a valid double — the ViewModel doesn't enforce positive amounts
        assertTrue(isValid("0.0", 1L))
    }

    @Test fun isValid_negativeAmount_returnsTrue() = runTest {
        // Negative is still parseable as a Double; the ViewModel doesn't block it
        assertTrue(isValid("-5.0", 1L))
    }

    @Test fun isValid_amountWithLeadingTrailingSpaces_returnsTrue() = runTest {
        // Double.parseDouble (used by Kotlin's toDoubleOrNull) strips whitespace,
        // so the ViewModel accepts amounts with surrounding spaces.
        assertTrue(isValid("  25.0  ", 1L))
    }
}
