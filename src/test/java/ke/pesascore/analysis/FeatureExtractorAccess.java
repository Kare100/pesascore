package ke.pesascore.analysis;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Exposes package-private helpers to tests in other packages. */
public final class FeatureExtractorAccess {

    private FeatureExtractorAccess() {
    }

    public static List<YearMonth> fullMonths(LocalDate start, LocalDate end) {
        return FeatureExtractor.fullMonths(start, end);
    }
}
