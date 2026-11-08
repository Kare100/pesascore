package ke.pesascore.scoring;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import ke.pesascore.analysis.CreditFeatures;
import ke.pesascore.model.Money;
import ke.pesascore.scoring.ScoreResult.Factor;
import ke.pesascore.scoring.ScoreResult.Reason;

/**
 * A transparent points-based scorecard. Every factor has a fixed maximum, the
 * maximums add up to 550, and the score is 300 plus the points earned, so the
 * range is 300 to 850. Because each point is traceable to a factor, every score
 * comes with the reasons that pulled it down, the way lenders explain a decline.
 */
public final class Scorecard {

    public static final int MIN_SCORE = 300;
    public static final int MAX_SCORE = 850;

    private static final int MAX_CONCERNS = 4;
    private static final int MAX_STRENGTHS = 3;

    public ScoreResult score(CreditFeatures f) {
        List<Scored> lines = List.of(
                incomeLevel(f),
                incomeStability(f),
                salary(f),
                surplus(f),
                balanceBuffer(f),
                overdraft(f),
                loanRepayment(f),
                betting(f),
                discipline(f));

        int points = lines.stream().mapToInt(s -> s.factor.points()).sum();
        int score = Math.max(MIN_SCORE, Math.min(MAX_SCORE, MIN_SCORE + points));

        List<Reason> concerns = lines.stream()
                .filter(s -> s.factor.points() < s.factor.maxPoints() * 0.6)
                .map(s -> new Reason(s.factor.code(), s.concern, s.factor.maxPoints() - s.factor.points()))
                .sorted(Comparator.comparingInt(Reason::impact).reversed())
                .limit(MAX_CONCERNS)
                .toList();
        List<Reason> strengths = lines.stream()
                .filter(s -> s.factor.points() >= s.factor.maxPoints() * 0.8)
                .map(s -> new Reason(s.factor.code(), s.strength, s.factor.points()))
                .sorted(Comparator.comparingInt(Reason::impact).reversed())
                .limit(MAX_STRENGTHS)
                .toList();

        return new ScoreResult(score, RiskBand.of(score), Confidence.of(f),
                lines.stream().map(s -> s.factor).toList(), concerns, strengths);
    }

    private record Scored(Factor factor, String strength, String concern) {
    }

    private static Scored incomeLevel(CreditFeatures f) {
        long kes = f.medianMonthlyIncome() / 100;
        int pts = kes >= 80_000 ? 70 : kes >= 40_000 ? 60 : kes >= 20_000 ? 45 : kes >= 10_000 ? 30 : kes >= 5_000 ? 15 : 0;
        return new Scored(
                new Factor("F1", "Income level", pts, 70, "Typical month brings in " + Money.format(f.medianMonthlyIncome())),
                "Healthy monthly income", "Monthly income is low for the amount of borrowing");
    }

    private static Scored incomeStability(CreditFeatures f) {
        double cv = f.incomeVariation();
        int pts = cv < 0.15 ? 100 : cv < 0.30 ? 75 : cv < 0.50 ? 45 : cv < 0.80 ? 20 : 0;
        return new Scored(
                new Factor("F2", "Income stability", pts, 100, "Income moves about " + pct(cv) + " from month to month"),
                "Income arrives steadily every month", "Income changes a lot from month to month");
    }

    private static Scored salary(CreditFeatures f) {
        int pts = f.regularSalary() ? 30 : f.incomeSources() >= 3 ? 20 : f.incomeSources() >= 1 ? 10 : 0;
        String evidence = f.regularSalary() ? "Salary paid in most months"
                : f.incomeSources() + " repeat sources of income";
        return new Scored(new Factor("F3", "Income source", pts, 30, evidence),
                "Regular salary or several repeat customers", "No regular salary or repeat source of income");
    }

    private static Scored surplus(CreditFeatures f) {
        double s = f.surplusRatio();
        int pts = s >= 0.30 ? 70 : s >= 0.15 ? 55 : s >= 0.05 ? 35 : s >= 0 ? 15 : 0;
        return new Scored(
                new Factor("F4", "Money left after spending", pts, 70,
                        "Spends " + Money.format(f.medianMonthlySpend()) + " of a typical month's income"),
                "Spends well within income", "Spending takes up almost all income");
    }

    private static Scored balanceBuffer(CreditFeatures f) {
        double d = f.bufferDays();
        int pts = d >= 30 ? 60 : d >= 14 ? 45 : d >= 7 ? 30 : d >= 3 ? 15 : 0;
        pts = Math.max(0, pts - (int) Math.round(f.lowBalanceDayShare() * 15));
        return new Scored(
                new Factor("F5", "Savings buffer", pts, 60, String.format(Locale.US,
                        "Balance covers about %.0f day%s of spending; under KES 100 on %s of days",
                        d, Math.round(d) == 1 ? "" : "s", pct(f.lowBalanceDayShare()))),
                "Keeps a balance that covers weeks of spending", "Balance often runs close to zero");
    }

    private static Scored overdraft(CreditFeatures f) {
        double perMonth = f.overdraftDrawsPerMonth();
        int pts = perMonth == 0 ? 60 : perMonth <= 2 ? 45 : perMonth <= 5 ? 30 : perMonth <= 10 ? 10 : 0;
        return new Scored(
                new Factor("F6", "Overdraft (Fuliza) use", pts, 60,
                        String.format(Locale.US, "%.1f Fuliza draws a month", perMonth)),
                "Rarely or never needs Fuliza", "Relies on Fuliza to cover everyday spending");
    }

    private static Scored loanRepayment(CreditFeatures f) {
        double ratio = f.repaymentRatio();
        int pts;
        String evidence;
        if (ratio < 0) {
            pts = 45;
            evidence = "No digital loans in this period";
        } else {
            pts = ratio >= 0.95 ? 70 : ratio >= 0.80 ? 50 : ratio >= 0.50 ? 20 : 0;
            evidence = "Repaid " + pct(ratio) + " of " + Money.format(f.loansDisbursed()) + " borrowed from "
                    + f.lendersUsed() + (f.lendersUsed() == 1 ? " lender" : " lenders");
        }
        if (f.lendersUsed() >= 3) {
            pts = Math.max(0, pts - 20);
        }
        return new Scored(new Factor("F7", "Loan repayment", pts, 70, evidence),
                "Pays back what it borrows", f.lendersUsed() >= 3
                        ? "Borrowing from several lenders at once"
                        : "Has not paid back much of what was borrowed");
    }

    private static Scored betting(CreditFeatures f) {
        double share = f.bettingShare();
        int pts = share == 0 ? 50 : share < 0.02 ? 40 : share < 0.05 ? 25 : share < 0.10 ? 10 : 0;
        return new Scored(
                new Factor("F8", "Betting", pts, 50, "Betting equals " + pct(share) + " of income"),
                "Little or no betting", "A large share of income goes to betting");
    }

    private static Scored discipline(CreditFeatures f) {
        int bills = f.billMonthShare() >= 0.8 ? 20 : f.billMonthShare() >= 0.5 ? 10 : 0;
        int saving = f.savingsRate() >= 0.05 ? 20 : f.savingsRate() > 0 ? 10 : 0;
        return new Scored(
                new Factor("F9", "Bills and saving", bills + saving, 40,
                        "Bills paid in " + pct(f.billMonthShare()) + " of months; saves "
                                + pct(f.savingsRate()) + " of income"),
                "Pays bills on time and saves regularly",
                bills < 10 && saving == 0 ? "No regular bill payments or saving"
                        : bills < 10 ? "Bills are not paid every month" : "Does not set money aside in savings");
    }

    private static String pct(double fraction) {
        return Math.round(fraction * 100) + "%";
    }
}
