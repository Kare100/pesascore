package ke.pesascore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import ke.pesascore.analysis.CreditFeatures;
import ke.pesascore.model.Money;
import ke.pesascore.scoring.Confidence;
import ke.pesascore.scoring.LoanLimitCalculator;
import ke.pesascore.scoring.LoanOffer;
import ke.pesascore.scoring.RiskBand;
import ke.pesascore.scoring.ScoreResult;

class LoanLimitCalculatorTest {

    private static CreditFeatures features(long incomeKes, long spendKes) {
        return new CreditFeatures(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), 6, 300,
                Money.shillings(incomeKes), 0.05, true, 1, Money.shillings(spendKes), 0.3,
                Money.shillings(10_000), 20, 0, 0, 0, 0, 0, 0, 0, 1, 0.05, 0);
    }

    private static ScoreResult score(int value, Confidence confidence) {
        return new ScoreResult(value, RiskBand.of(value), confidence, List.of(), List.of(), List.of());
    }

    @Test
    void sizesTheLoanFromWhatIsLeftEachMonth() {
        // 50,000 in, 30,000 out: 20,000 left. Safe instalment = min(10,000, 15,000) = 10,000.
        LoanOffer offer = new LoanLimitCalculator(0.015).offer(features(50_000, 30_000), score(780, Confidence.HIGH));

        assertTrue(offer.approved());
        assertEquals(6, offer.termMonths());
        // Present value of 10,000 a month for 6 months at 1.5% = 56,972, rounded down to 56,500.
        assertEquals(Money.shillings(56_500), offer.limitCents());
        assertTrue(offer.monthlyInstalmentCents() <= Money.shillings(10_000));
    }

    @Test
    void riskierBandsAndShortHistoriesBorrowLess() {
        LoanLimitCalculator calc = new LoanLimitCalculator();
        long a = calc.offer(features(50_000, 30_000), score(780, Confidence.HIGH)).limitCents();
        long c = calc.offer(features(50_000, 30_000), score(600, Confidence.HIGH)).limitCents();
        long cShort = calc.offer(features(50_000, 30_000), score(600, Confidence.LOW)).limitCents();
        assertTrue(a > c && c > cShort, a + " > " + c + " > " + cShort);
    }

    @Test
    void declinesWhenNothingIsLeftOrRiskIsTooHigh() {
        LoanLimitCalculator calc = new LoanLimitCalculator();
        assertFalse(calc.offer(features(20_000, 25_000), score(780, Confidence.HIGH)).approved());
        assertFalse(calc.offer(features(50_000, 30_000), score(420, Confidence.HIGH)).approved());
    }

    @Test
    void policyRulesOverrideAGoodScore() {
        CreditFeatures gambler = new CreditFeatures(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), 6, 300,
                Money.shillings(50_000), 0.05, true, 1, Money.shillings(30_000), 0.3,
                Money.shillings(10_000), 20, 0, 0, 0, 0, 0, 0, 0.25, 1, 0.05, 0);
        LoanOffer offer = new LoanLimitCalculator().offer(gambler, score(800, Confidence.HIGH));
        assertFalse(offer.approved());
        assertTrue(offer.rationale().contains("betting"), offer.rationale());
    }

    @Test
    void rejectsSillyInterestRates() {
        assertThrows(IllegalArgumentException.class, () -> new LoanLimitCalculator(0.5));
    }
}
