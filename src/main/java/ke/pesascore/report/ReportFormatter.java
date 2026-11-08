package ke.pesascore.report;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ke.pesascore.analysis.CreditFeatures;
import ke.pesascore.model.Money;
import ke.pesascore.scoring.LoanOffer;
import ke.pesascore.scoring.ScoreResult;
import ke.pesascore.scoring.ScoreResult.Factor;
import ke.pesascore.scoring.ScoreResult.Reason;

/** Renders a {@link CreditReport} for people (text) and for machines (JSON). */
public final class ReportFormatter {

    public String text(CreditReport r) {
        CreditFeatures f = r.features();
        ScoreResult s = r.score();
        LoanOffer o = r.offer();
        StringBuilder out = new StringBuilder();
        String rule = "-".repeat(64) + "\n";

        out.append(rule);
        out.append(String.format(Locale.US, " PesaScore  %d / 850   Band %s (%s)   Confidence: %s%n",
                s.score(), s.band().name(), s.band().label(), s.confidence()));
        out.append(String.format(Locale.US, " Statement  %s to %s, %d full months, %d transactions%n",
                f.periodStart(), f.periodEnd(), f.monthsAnalysed(), f.transactionCount()));
        out.append(rule);

        if (o.approved()) {
            out.append(String.format(Locale.US, " Suggested limit  %s over %d month%s at %.1f%% a month%n",
                    Money.format(o.limitCents()), o.termMonths(), o.termMonths() == 1 ? "" : "s",
                    o.monthlyInterestRate() * 100));
            out.append(String.format(Locale.US, " Monthly payment  %s%n", Money.format(o.monthlyInstalmentCents())));
        } else {
            out.append(" No loan suggested\n");
        }
        out.append(" ").append(o.rationale()).append("\n").append(rule);

        out.append(" Scorecard\n");
        for (Factor factor : s.factors()) {
            out.append(String.format(Locale.US, "  %-26s %3d / %-3d  %s%n",
                    factor.name(), factor.points(), factor.maxPoints(), factor.evidence()));
        }
        section(out, " What is holding the score back", s.concerns());
        section(out, " What is helping", s.strengths());
        if (!r.warnings().isEmpty()) {
            out.append(" ").append(r.warnings().size()).append(" statement line(s) were skipped:\n");
            r.warnings().stream().limit(5).forEach(w -> out.append("  - ").append(w).append('\n'));
        }
        out.append(rule);
        return out.toString();
    }

    private static void section(StringBuilder out, String title, List<Reason> reasons) {
        if (reasons.isEmpty()) {
            return;
        }
        out.append(title).append('\n');
        for (Reason reason : reasons) {
            out.append("  [").append(reason.code()).append("] ").append(reason.message()).append('\n');
        }
    }

    public String json(CreditReport r) {
        CreditFeatures f = r.features();
        ScoreResult s = r.score();
        LoanOffer o = r.offer();

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("score", s.score());
        root.put("band", s.band().name());
        root.put("bandLabel", s.band().label());
        root.put("confidence", s.confidence().name());

        Map<String, Object> offer = new LinkedHashMap<>();
        offer.put("approved", o.approved());
        offer.put("limitKes", o.limitCents() / 100);
        offer.put("termMonths", o.termMonths());
        offer.put("monthlyInstalmentKes", o.monthlyInstalmentCents() / 100);
        offer.put("monthlyInterestRate", o.monthlyInterestRate());
        offer.put("rationale", o.rationale());
        root.put("offer", offer);

        root.put("concerns", s.concerns().stream().map(ReportFormatter::reason).toList());
        root.put("strengths", s.strengths().stream().map(ReportFormatter::reason).toList());
        root.put("scorecard", s.factors().stream().map(factor -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", factor.code());
            m.put("name", factor.name());
            m.put("points", factor.points());
            m.put("maxPoints", factor.maxPoints());
            m.put("evidence", factor.evidence());
            return m;
        }).toList());

        Map<String, Object> features = new LinkedHashMap<>();
        features.put("periodStart", f.periodStart().toString());
        features.put("periodEnd", f.periodEnd().toString());
        features.put("monthsAnalysed", f.monthsAnalysed());
        features.put("transactionCount", f.transactionCount());
        features.put("medianMonthlyIncomeKes", f.medianMonthlyIncome() / 100);
        features.put("incomeVariation", f.incomeVariation());
        features.put("regularSalary", f.regularSalary());
        features.put("incomeSources", f.incomeSources());
        features.put("medianMonthlySpendKes", f.medianMonthlySpend() / 100);
        features.put("surplusRatio", f.surplusRatio());
        features.put("averageDailyBalanceKes", f.averageDailyBalance() / 100);
        features.put("bufferDays", f.bufferDays());
        features.put("lowBalanceDayShare", f.lowBalanceDayShare());
        features.put("overdraftDrawsPerMonth", f.overdraftDrawsPerMonth());
        features.put("loansDisbursedKes", f.loansDisbursed() / 100);
        features.put("loansRepaidKes", f.loansRepaid() / 100);
        features.put("lendersUsed", f.lendersUsed());
        features.put("bettingShare", f.bettingShare());
        features.put("billMonthShare", f.billMonthShare());
        features.put("savingsRate", f.savingsRate());
        features.put("failedShare", f.failedShare());
        root.put("features", features);
        root.put("warnings", r.warnings());
        return Json.write(root);
    }

    private static Map<String, Object> reason(Reason r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", r.code());
        m.put("message", r.message());
        m.put("impact", r.impact());
        return m;
    }
}
