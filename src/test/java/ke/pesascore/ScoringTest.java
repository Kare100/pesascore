package ke.pesascore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import ke.pesascore.demo.StatementGenerator;
import ke.pesascore.demo.StatementGenerator.Persona;
import ke.pesascore.report.CreditReport;
import ke.pesascore.scoring.Confidence;
import ke.pesascore.scoring.RiskBand;
import ke.pesascore.scoring.Scorecard;

class ScoringTest {

    private static CreditReport assess(Persona persona, int months) throws IOException {
        String csv = new StatementGenerator(3).generate(persona, months, LocalDate.of(2026, 1, 1));
        return new PesaScore().assess(new StringReader(csv));
    }

    @Test
    void ranksBorrowersInTheOrderALenderWould() throws IOException {
        int salaried = assess(Persona.SALARIED, 6).score().score();
        int rider = assess(Persona.RIDER, 6).score().score();
        int strained = assess(Persona.OVERSTRETCHED, 6).score().score();

        assertTrue(salaried > rider, "salaried " + salaried + " should beat rider " + rider);
        assertTrue(rider > strained, "rider " + rider + " should beat overstretched " + strained);
    }

    @Test
    void scoresStayInRangeAndAddUp() throws IOException {
        for (Persona p : Persona.values()) {
            CreditReport r = assess(p, 6);
            int points = r.score().factors().stream().mapToInt(f -> f.points()).sum();
            assertEquals(Scorecard.MIN_SCORE + points, r.score().score());
            assertTrue(r.score().score() >= Scorecard.MIN_SCORE && r.score().score() <= Scorecard.MAX_SCORE);
            int max = r.score().factors().stream().mapToInt(f -> f.maxPoints()).sum();
            assertEquals(Scorecard.MAX_SCORE - Scorecard.MIN_SCORE, max);
        }
    }

    @Test
    void explainsWhyARiskyBorrowerScoresLow() throws IOException {
        CreditReport r = assess(Persona.OVERSTRETCHED, 6);
        assertFalse(r.offer().approved());
        assertTrue(r.offer().rationale().startsWith("Policy rule"), r.offer().rationale());
        assertEquals(4, r.score().concerns().size());
        assertTrue(r.score().factors().stream()
                .anyMatch(f -> f.code().equals("F6") && f.points() <= 10), "Fuliza reliance should cost points");
        assertTrue(r.score().factors().stream()
                .anyMatch(f -> f.code().equals("F8") && f.points() == 0), "Heavy betting should cost all betting points");
        assertTrue(r.features().bettingShare() > 0.1);
    }

    @Test
    void shortStatementsGetLowConfidence() throws IOException {
        assertEquals(Confidence.LOW, assess(Persona.SALARIED, 2).score().confidence());
        assertEquals(Confidence.HIGH, assess(Persona.SALARIED, 6).score().confidence());
    }

    @Test
    void bandsFollowTheScore() {
        assertEquals(RiskBand.A, RiskBand.of(850));
        assertEquals(RiskBand.A, RiskBand.of(740));
        assertEquals(RiskBand.B, RiskBand.of(739));
        assertEquals(RiskBand.C, RiskBand.of(580));
        assertEquals(RiskBand.D, RiskBand.of(500));
        assertEquals(RiskBand.E, RiskBand.of(499));
    }
}
