package ke.pesascore.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Parsing and formatting of Kenya shilling amounts held as cents. */
public final class Money {

    private Money() {
    }

    /** Parses "1,250.00", "-1,250.00", "KES 300" or "" (zero) into cents. */
    public static long parseCents(String raw) {
        if (raw == null) {
            return 0;
        }
        String s = raw.replace("KES", "").replace("Ksh", "").replace(",", "").replace(" ", "").trim();
        if (s.isEmpty() || s.equals("-")) {
            return 0;
        }
        boolean negative = s.startsWith("(") && s.endsWith(")");
        if (negative) {
            s = s.substring(1, s.length() - 1);
        }
        try {
            long cents = new BigDecimal(s).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
            return negative ? -cents : cents;
        } catch (NumberFormatException | ArithmeticException e) {
            throw new IllegalArgumentException("Not a money amount: '" + raw + "'", e);
        }
    }

    /** Formats cents as whole shillings, e.g. "KES 12,345". */
    public static String format(long cents) {
        return String.format(Locale.US, "KES %,d", Math.round(cents / 100.0));
    }

    public static long shillings(long kes) {
        return kes * 100;
    }
}
