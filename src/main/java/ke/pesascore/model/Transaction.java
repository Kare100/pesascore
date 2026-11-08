package ke.pesascore.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * One line of an M-Pesa statement. Amounts are whole cents (KES x 100) so that
 * sums never pick up floating-point error.
 */
public record Transaction(
        String receipt,
        LocalDateTime time,
        String details,
        boolean completed,
        long paidInCents,
        long withdrawnCents,
        long balanceCents,
        Category category,
        String counterparty) {

    public boolean isMoneyIn() {
        return paidInCents > 0;
    }

    public long amountCents() {
        return isMoneyIn() ? paidInCents : withdrawnCents;
    }

    public LocalDate date() {
        return time.toLocalDate();
    }

    public YearMonth month() {
        return YearMonth.from(time);
    }
}
