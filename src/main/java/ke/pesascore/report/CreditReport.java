package ke.pesascore.report;

import java.util.List;

import ke.pesascore.analysis.CreditFeatures;
import ke.pesascore.scoring.LoanOffer;
import ke.pesascore.scoring.ScoreResult;

/** Everything PesaScore knows about one statement. */
public record CreditReport(CreditFeatures features, ScoreResult score, LoanOffer offer, List<String> warnings) {
}
