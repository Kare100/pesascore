package ke.pesascore.scoring;

import java.util.List;

/** The score, its band, and the evidence behind it. */
public record ScoreResult(
        int score,
        RiskBand band,
        Confidence confidence,
        List<Factor> factors,
        List<Reason> concerns,
        List<Reason> strengths) {

    /** One scorecard line: what was measured and how many of the available points it earned. */
    public record Factor(String code, String name, int points, int maxPoints, String evidence) {
    }

    /** A plain-language reason, ranked by how many points it moved the score. */
    public record Reason(String code, String message, int impact) {
    }
}
