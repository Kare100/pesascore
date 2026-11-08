package ke.pesascore.analysis;

import java.time.LocalDate;

/**
 * Behavioural signals pulled out of a statement. Money is in cents; shares and
 * ratios are fractions between 0 and 1 unless noted.
 */
public record CreditFeatures(
        LocalDate periodStart,
        LocalDate periodEnd,
        int monthsAnalysed,
        int transactionCount,

        long medianMonthlyIncome,
        double incomeVariation,
        boolean regularSalary,
        int incomeSources,

        long medianMonthlySpend,
        double surplusRatio,

        long averageDailyBalance,
        double bufferDays,
        double lowBalanceDayShare,

        double overdraftDrawsPerMonth,
        long overdraftDrawnTotal,

        long loansDisbursed,
        long loansRepaid,
        int lendersUsed,

        double bettingShare,
        double billMonthShare,
        double savingsRate,
        double failedShare) {

    /** Share of borrowed money that has been paid back, or -1 if there were no loans. */
    public double repaymentRatio() {
        return loansDisbursed == 0 ? -1 : Math.min(1.0, (double) loansRepaid / loansDisbursed);
    }
}
