package ke.pesascore.analysis;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import ke.pesascore.model.Category;
import ke.pesascore.model.Category.Flow;
import ke.pesascore.model.Transaction;

/** Computes {@link CreditFeatures} from a time-ordered list of transactions. */
public final class FeatureExtractor {

    private static final long LOW_BALANCE_CENTS = 100_00;
    private static final double MAX_BUFFER_DAYS = 90;

    public CreditFeatures extract(List<Transaction> all) {
        if (all.isEmpty()) {
            throw new IllegalArgumentException("The statement has no transactions to analyse");
        }
        List<Transaction> done = all.stream().filter(Transaction::completed).toList();
        if (done.isEmpty()) {
            throw new IllegalArgumentException("The statement has no completed transactions");
        }
        LocalDate start = done.get(0).date();
        LocalDate end = done.get(done.size() - 1).date();
        List<YearMonth> months = fullMonths(start, end);

        Map<YearMonth, Long> income = zeroed(months);
        Map<YearMonth, Long> spend = zeroed(months);
        Set<YearMonth> salaryMonths = new HashSet<>();
        Set<YearMonth> billMonths = new HashSet<>();
        Map<String, Integer> incomePayers = new HashMap<>();
        Set<String> lenders = new HashSet<>();
        long betting = 0;
        long savings = 0;
        long loansOut = 0;
        long loansBack = 0;
        long overdraftTotal = 0;
        int overdraftDraws = 0;

        for (Transaction t : done) {
            YearMonth m = t.month();
            if (!income.containsKey(m)) {
                continue;
            }
            Category c = t.category();
            if (c.flow() == Flow.INCOME && t.isMoneyIn()) {
                income.merge(m, t.paidInCents(), Long::sum);
                if (c != Category.CASH_DEPOSIT) {
                    incomePayers.merge(t.counterparty(), 1, Integer::sum);
                }
            }
            if (c.flow() == Flow.SPEND && !t.isMoneyIn()) {
                spend.merge(m, t.withdrawnCents(), Long::sum);
            }
            switch (c) {
                case SALARY -> salaryMonths.add(m);
                case UTILITY, BILL_PAYMENT -> billMonths.add(m);
                case BETTING -> betting += t.withdrawnCents();
                case SAVINGS_DEPOSIT -> savings += t.withdrawnCents();
                case LOAN_DISBURSEMENT -> {
                    loansOut += t.paidInCents();
                    lenders.add(t.counterparty());
                }
                case LOAN_REPAYMENT -> loansBack += t.withdrawnCents();
                case OVERDRAFT_DRAW -> {
                    overdraftDraws++;
                    overdraftTotal += t.paidInCents();
                }
                default -> {
                }
            }
        }

        int n = months.size();
        long totalIncome = income.values().stream().mapToLong(Long::longValue).sum();
        long medianIncome = Stats.median(income.values());
        long medianSpend = Stats.median(spend.values());
        double surplus = medianIncome == 0 ? -1 : (double) (medianIncome - medianSpend) / medianIncome;

        long[] daily = dailyClosingBalances(done, start, end);
        long avgBalance = Math.round(Stats.mean(daily));
        double dailySpend = medianSpend / 30.0;
        double buffer = dailySpend <= 0 ? MAX_BUFFER_DAYS : Math.min(MAX_BUFFER_DAYS, avgBalance / dailySpend);
        long lowDays = java.util.Arrays.stream(daily).filter(b -> b < LOW_BALANCE_CENTS).count();

        long failed = all.stream().filter(t -> !t.completed()).count();
        int repeatPayers = (int) incomePayers.values().stream().filter(k -> k >= 2).count();

        return new CreditFeatures(
                start, end, n, done.size(),
                medianIncome,
                Stats.coefficientOfVariation(income.values()),
                n > 0 && salaryMonths.size() >= Math.max(1, Math.round(n * 0.66)),
                repeatPayers,
                medianSpend,
                surplus,
                avgBalance,
                buffer,
                daily.length == 0 ? 0 : (double) lowDays / daily.length,
                n == 0 ? 0 : (double) overdraftDraws / n,
                overdraftTotal,
                loansOut, loansBack, lenders.size(),
                share(betting, totalIncome),
                n == 0 ? 0 : (double) billMonths.size() / n,
                share(savings, totalIncome),
                (double) failed / all.size());
    }

    /**
     * Calendar months between start and end. A first or last month that the statement
     * only partly covers is dropped, so a statement starting on the 20th does not look
     * like a month with a third of the usual income. At least one month is always kept.
     */
    static List<YearMonth> fullMonths(LocalDate start, LocalDate end) {
        List<YearMonth> months = new ArrayList<>();
        for (YearMonth m = YearMonth.from(start); !m.isAfter(YearMonth.from(end)); m = m.plusMonths(1)) {
            months.add(m);
        }
        if (months.size() > 2 && start.getDayOfMonth() > 3) {
            months.remove(0);
        }
        if (months.size() > 2 && end.getDayOfMonth() < end.lengthOfMonth() - 2) {
            months.remove(months.size() - 1);
        }
        return months;
    }

    private static long[] dailyClosingBalances(List<Transaction> done, LocalDate start, LocalDate end) {
        TreeMap<LocalDate, Long> closing = new TreeMap<>();
        for (Transaction t : done) {
            closing.put(t.date(), t.balanceCents());
        }
        int days = (int) (end.toEpochDay() - start.toEpochDay()) + 1;
        long[] out = new long[days];
        long last = closing.firstEntry().getValue();
        for (int i = 0; i < days; i++) {
            Long b = closing.get(start.plusDays(i));
            if (b != null) {
                last = b;
            }
            out[i] = last;
        }
        return out;
    }

    private static Map<YearMonth, Long> zeroed(List<YearMonth> months) {
        Map<YearMonth, Long> map = new TreeMap<>();
        months.forEach(m -> map.put(m, 0L));
        return map;
    }

    private static double share(long part, long whole) {
        if (whole <= 0) {
            return part > 0 ? 1 : 0;
        }
        return (double) part / whole;
    }
}
