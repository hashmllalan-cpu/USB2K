package com.usbmediaexplorer.data.search

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchBudgetTest {
    @Test fun `budget expires only at configured deadline`() {
        assertFalse(SearchBudget.expired(1000L, 10_999L))
        assertTrue(SearchBudget.expired(1000L, 11_000L))
    }

    @Test fun `node budget remains bounded`() {
        assertTrue(SearchBudget.MAX_WALK_NODES in 1..15_000)
    }
}
