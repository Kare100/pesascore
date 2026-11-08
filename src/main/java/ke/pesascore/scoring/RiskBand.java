package ke.pesascore.scoring;

/** Score bands, with how much of the affordable instalment to lend and over how many months. */
public enum RiskBand {
    A(740, "Low risk", 1.00, 6),
    B(670, "Moderate-low risk", 0.80, 4),
    C(580, "Moderate risk", 0.60, 3),
    D(500, "High risk", 0.35, 1),
    E(300, "Very high risk", 0.00, 0);

    private final int minScore;
    private final String label;
    private final double lendingShare;
    private final int termMonths;

    RiskBand(int minScore, String label, double lendingShare, int termMonths) {
        this.minScore = minScore;
        this.label = label;
        this.lendingShare = lendingShare;
        this.termMonths = termMonths;
    }

    public static RiskBand of(int score) {
        for (RiskBand band : values()) {
            if (score >= band.minScore) {
                return band;
            }
        }
        return E;
    }

    public String label() {
        return label;
    }

    public double lendingShare() {
        return lendingShare;
    }

    public int termMonths() {
        return termMonths;
    }
}
