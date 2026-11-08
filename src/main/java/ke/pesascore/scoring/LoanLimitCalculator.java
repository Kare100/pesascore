package ke.pesascore.scoring;

import java.util.Locale;

import ke.pesascore.analysis.CreditFeatures;
import ke.pesascore.model.Money;

/**
 * Sizes a loan from what the borrower can actually repay, not from the score alone.
 * The instalment is capped at half of the money left over each month and never more
 * than 30% of income, then scaled down for riskier bands and short histories. The
 * limit is the amount that instalment repays over the band's term (a standard annuity).
 */
public final class LoanLimitCalculator {

    public static final double DEFAULT_MONTHLY_RATE = 0.015;

    private static final double SHARE_OF_SURPLUS = 0.5;
    private static final double MAX_SHARE_OF_INCOME = 0.3;
    private static final int MAX_INCOME_MULTIPLE = 3;
    private static final long ROUND_TO_CENTS = 500_00;
    private static final double MAX_BETTING_SHARE = 0.20;

    private final double monthlyRate;

    public LoanLimitCalculator(double monthlyRate) {
        if (monthlyRate < 0 || monthlyRate > 0.2) {
            throw new IllegalArgumentException("Monthly interest rate must be between 0 and 0.2");
        }
        this.monthlyRate = monthlyRate;
    }

    public LoanLimitCalculator() {
        this(DEFAULT_MONTHLY_RATE);
    }

    public LoanOffer offer(CreditFeatures f, ScoreResult score) {
        String policy = policyDecline(f);
        if (policy != null) {
            return LoanOffer.decline(monthlyRate, "Policy rule: " + policy);
        }
        RiskBand band = score.band();
        if (band.termMonths() == 0) {
            return LoanOffer.decline(monthlyRate, "Score is in band E (" + band.label() + "): lending would likely hurt this borrower");
        }
        long income = f.medianMonthlyIncome();
        long surplus = income - f.medianMonthlySpend();
        if (income <= 0 || surplus <= 0) {
            return LoanOffer.decline(monthlyRate, "Nothing is left over after normal monthly spending, so any instalment would be unaffordable");
        }

        double instalment = Math.min(surplus * SHARE_OF_SURPLUS, income * MAX_SHARE_OF_INCOME) * band.lendingShare();
        if (score.confidence() == Confidence.LOW) {
            instalment *= 0.5;
        }
        int n = band.termMonths();
        double principal = Math.min(presentValue(instalment, monthlyRate, n), (double) income * MAX_INCOME_MULTIPLE);
        long limit = (long) Math.floor(principal / ROUND_TO_CENTS) * ROUND_TO_CENTS;
        if (limit < ROUND_TO_CENTS) {
            return LoanOffer.decline(monthlyRate, "The affordable amount is under KES 500");
        }
        long repayment = (long) Math.ceil(instalment(limit, monthlyRate, n) / 100.0) * 100;

        String why = String.format(Locale.US,
                "Band %s allows %.0f%% of a safe instalment over %d month%s. Safe instalment: the lower of half of the %s left each month and 30%% of income%s.",
                band.name(), band.lendingShare() * 100, n, n == 1 ? "" : "s", Money.format(surplus),
                score.confidence() == Confidence.LOW ? ", halved because the statement is short" : "");
        return new LoanOffer(true, limit, n, repayment, monthlyRate, why);
    }

    /**
     * Hard rules that decline regardless of score, the way lenders protect borrowers
     * who are already in trouble. A good score elsewhere cannot outweigh these.
     */
    static String policyDecline(CreditFeatures f) {
        if (f.bettingShare() >= MAX_BETTING_SHARE) {
            return String.format(Locale.US, "betting takes %.0f%% of income (limit %.0f%%)",
                    f.bettingShare() * 100, MAX_BETTING_SHARE * 100);
        }
        if (f.lendersUsed() >= 3 && f.repaymentRatio() >= 0 && f.repaymentRatio() < 0.5) {
            return "owes " + f.lendersUsed() + " lenders and has repaid under half of what was borrowed";
        }
        return null;
    }

    /** Amount that a fixed payment repays over n months at rate r. */
    static double presentValue(double payment, double r, int n) {
        return r == 0 ? payment * n : payment * (1 - Math.pow(1 + r, -n)) / r;
    }

    /** Fixed monthly payment that repays principal over n months at rate r. */
    static double instalment(double principal, double r, int n) {
        return r == 0 ? principal / n : principal * r / (1 - Math.pow(1 + r, -n));
    }
}
