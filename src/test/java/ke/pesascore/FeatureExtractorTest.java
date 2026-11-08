package ke.pesascore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ke.pesascore.analysis.CreditFeatures;
import ke.pesascore.analysis.FeatureExtractor;
import ke.pesascore.analysis.Stats;
import ke.pesascore.model.Category;
import ke.pesascore.model.Money;
import ke.pesascore.model.Transaction;

class FeatureExtractorTest {

    private static Transaction tx(LocalDateTime t, Category c, long kes, boolean in, long balanceKes, String who) {
        long cents = Money.shillings(kes);
        return new Transaction("R" + t, t, c.name(), true, in ? cents : 0, in ? 0 : cents,
                Money.shillings(balanceKes), c, who);
    }

    /** Three full months: salary of 50,000, rent of 20,000, one Fuliza draw and one bet a month. */
    private static List<Transaction> threeMonths() {
        List<Transaction> list = new ArrayList<>();
        long balance = 1_000;
        for (int m = 1; m <= 3; m++) {
            LocalDate d = LocalDate.of(2026, m, 1);
            balance += 50_000;
            list.add(tx(d.atTime(9, 0), Category.SALARY, 50_000, true, balance, "EMPLOYER"));
            balance -= 20_000;
            list.add(tx(d.atTime(10, 0), Category.BILL_PAYMENT, 20_000, false, balance, "LANDLORD"));
            balance -= 1_000;
            list.add(tx(d.plusDays(10).atTime(12, 0), Category.BETTING, 1_000, false, balance, "SPORTPESA"));
            list.add(tx(d.plusDays(20).atTime(12, 0), Category.OVERDRAFT_DRAW, 200, true, balance + 200, "FULIZA"));
            list.add(tx(d.withDayOfMonth(d.lengthOfMonth()).atTime(20, 0), Category.MERCHANT_PAYMENT, 10_000, false,
                    balance - 10_000 + 200, "SHOP"));
            balance = balance - 10_000;
        }
        return list;
    }

    @Test
    void measuresIncomeSpendingAndHabits() {
        CreditFeatures f = new FeatureExtractor().extract(threeMonths());

        assertEquals(3, f.monthsAnalysed());
        assertEquals(Money.shillings(50_000), f.medianMonthlyIncome());
        assertEquals(0.0, f.incomeVariation(), 1e-9);
        assertTrue(f.regularSalary());
        assertEquals(Money.shillings(30_000), f.medianMonthlySpend());
        assertEquals(0.4, f.surplusRatio(), 1e-9);
        assertEquals(1.0, f.overdraftDrawsPerMonth(), 1e-9);
        assertEquals(0.02, f.bettingShare(), 1e-9);
        assertEquals(1.0, f.billMonthShare(), 1e-9);
        assertEquals(-1.0, f.repaymentRatio(), 1e-9);
    }

    @Test
    void dropsPartialFirstAndLastMonths() {
        List<YearMonth> months = ke.pesascore.analysis.FeatureExtractorAccess.fullMonths(
                LocalDate.of(2026, 1, 20), LocalDate.of(2026, 5, 10));
        assertEquals(List.of(YearMonth.of(2026, 2), YearMonth.of(2026, 3), YearMonth.of(2026, 4)), months);
    }

    @Test
    void statisticsHelpers() {
        assertEquals(3, Stats.median(List.of(5L, 1L, 3L)));
        assertEquals(3, Stats.median(List.of(4L, 2L)));
        assertEquals(0.0, Stats.coefficientOfVariation(List.of(7L, 7L, 7L)), 1e-9);
        assertEquals(1.0, Stats.coefficientOfVariation(List.of(0L, 0L)), 1e-9);
    }
}
