package ke.pesascore.scoring;

import ke.pesascore.analysis.CreditFeatures;

/** How much history the score rests on. Short statements get smaller limits. */
public enum Confidence {
    LOW, MEDIUM, HIGH;

    public static Confidence of(CreditFeatures f) {
        if (f.monthsAnalysed() < 3 || f.transactionCount() < 30) {
            return LOW;
        }
        return f.monthsAnalysed() < 6 ? MEDIUM : HIGH;
    }
}
