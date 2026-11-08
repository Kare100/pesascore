package ke.pesascore.analysis;

import java.util.Arrays;
import java.util.Collection;

/** Small, dependency-free statistics helpers. */
public final class Stats {

    private Stats() {
    }

    public static long median(Collection<Long> values) {
        if (values.isEmpty()) {
            return 0;
        }
        long[] v = values.stream().mapToLong(Long::longValue).sorted().toArray();
        int mid = v.length / 2;
        return v.length % 2 == 1 ? v[mid] : Math.round((v[mid - 1] + v[mid]) / 2.0);
    }

    public static double mean(long[] values) {
        return values.length == 0 ? 0 : Arrays.stream(values).average().orElse(0);
    }

    /** Standard deviation divided by the mean; 1.0 when the mean is zero (no stable income). */
    public static double coefficientOfVariation(Collection<Long> values) {
        if (values.isEmpty()) {
            return 1.0;
        }
        double mean = values.stream().mapToLong(Long::longValue).average().orElse(0);
        if (mean <= 0) {
            return 1.0;
        }
        double variance = values.stream().mapToDouble(v -> (v - mean) * (v - mean)).sum() / values.size();
        return Math.sqrt(variance) / mean;
    }
}
