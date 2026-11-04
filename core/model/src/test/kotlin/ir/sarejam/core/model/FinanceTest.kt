package ir.sarejam.core.model

import ir.sarejam.core.model.TransactionKind.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FinanceTest {
    private val base = TransactionFields(
        accountId = 1, amount = 1_250_000, direction = MoneyDirection.OUT, kind = EXPENSE, occurredAt = 1_000,
        balanceAfter = 3_750_000, categoryId = 7, counterparty = null, note = "نان",
    )

    @Test fun reparseNeverOverwritesUserEditedFields() {
        val userFixed = base.copy(amount = 1_200_000, kind = TRANSFER)
        val fresh = base.copy(amount = 1_250_000, kind = EXPENSE, counterparty = "6037-xxxx", note = null)
        val (merged, changed) = Reparse.merge(userFixed, fresh, setOf(EditableField.AMOUNT, EditableField.KIND, EditableField.NOTE))
        assertEquals(1_200_000, merged.amount)
        assertEquals(TRANSFER, merged.kind)
        assertEquals("نان", merged.note)
        assertEquals("6037-xxxx", merged.counterparty)
        assertEquals(setOf(EditableField.COUNTERPARTY), changed)
    }

    @Test fun reparseUpdatesUneditedFieldsButKeepsCategory() {
        val fresh = base.copy(amount = 1_300_000, categoryId = null)
        val (merged, changed) = Reparse.merge(base, fresh, emptySet())
        assertEquals(1_300_000, merged.amount)
        assertEquals(7, merged.categoryId)
        assertEquals(setOf(EditableField.AMOUNT), changed)
    }

    @Test fun transfersAdjustmentsAndFailedNeverCountAsExpenseOrIncome() {
        val t = Reports.totals(listOf(TRANSFER to 5_000_000L, ADJUSTMENT to 900_000L, FAILED to 300_000L))
        assertEquals(PeriodTotals(0, 0), t)
    }

    @Test fun expenseIsExpensePlusFeeMinusRefunds() {
        val t = Reports.totals(listOf(EXPENSE to 1_000_000L, FEE to 11_000L, REFUND to 200_000L, REVERSAL to 50_000L, INCOME to 9_000_000L, TRANSFER to 3_000_000L))
        assertEquals(PeriodTotals(expense = 761_000, income = 9_000_000), t)
    }

    @Test fun refundsLargerThanSpendingDoNotMakeExpenseNegative() =
        assertEquals(0, Reports.totals(listOf(REFUND to 500L)).expense)

    @Test fun amountIsNeverNegative() {
        assertTrue(runCatching { base.copy(amount = -1) }.isFailure)
    }
}
