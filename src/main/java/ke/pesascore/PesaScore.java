package ke.pesascore;

import java.io.IOException;
import java.io.Reader;

import ke.pesascore.analysis.CreditFeatures;
import ke.pesascore.analysis.FeatureExtractor;
import ke.pesascore.parse.MpesaCsvParser;
import ke.pesascore.report.CreditReport;
import ke.pesascore.scoring.LoanLimitCalculator;
import ke.pesascore.scoring.ScoreResult;
import ke.pesascore.scoring.Scorecard;

/** The whole pipeline: statement in, explained credit decision out. */
public final class PesaScore {

    private final MpesaCsvParser parser = new MpesaCsvParser();
    private final FeatureExtractor extractor = new FeatureExtractor();
    private final Scorecard scorecard = new Scorecard();
    private final LoanLimitCalculator calculator;

    public PesaScore(double monthlyInterestRate) {
        this.calculator = new LoanLimitCalculator(monthlyInterestRate);
    }

    public PesaScore() {
        this(LoanLimitCalculator.DEFAULT_MONTHLY_RATE);
    }

    public CreditReport assess(Reader statement) throws IOException {
        MpesaCsvParser.Result parsed = parser.parse(statement);
        CreditFeatures features = extractor.extract(parsed.transactions());
        ScoreResult score = scorecard.score(features);
        return new CreditReport(features, score, calculator.offer(features, score), parsed.warnings());
    }
}
