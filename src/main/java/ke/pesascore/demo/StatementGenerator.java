package ke.pesascore.demo;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Generates realistic, entirely synthetic M-Pesa statements for demos and tests.
 * Balances, Fuliza draws and repayments and transaction charges are worked out
 * line by line, so the output behaves like a real statement. No real person's data
 * is used; names and numbers are invented.
 */
public final class StatementGenerator {

    public enum Persona {
        /** Salaried teacher: steady pay, rent and bills, monthly saving. */
        SALARIED,
        /** Boda boda rider: many small daily fares, fuel, a bike loan, occasional Fuliza. */
        RIDER,
        /** Casual worker under strain: irregular pay, heavy betting, Fuliza and several digital lenders. */
        OVERSTRETCHED
    }

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String[] NAMES = {"JOHN KAMAU", "MERCY ATIENO", "BRIAN OTIENO", "FAITH WANJIRU",
        "KEVIN MUTUA", "SHARON CHEPKOECH", "DENNIS KIPROTICH", "GRACE NJERI", "PETER OUMA", "LUCY MUTHONI",
        "SAMUEL KIBET", "JANE AKINYI", "MOSES MWANGI", "ESTHER NAFULA", "VICTOR OCHIENG"};

    private record Event(LocalDateTime time, String details, long kes, boolean in, Fee fee) {
    }

    private enum Fee { NONE, TRANSFER, WITHDRAWAL }

    private final Random random;
    private final List<Event> events = new ArrayList<>();

    public StatementGenerator(long seed) {
        this.random = new Random(seed);
    }

    public String generate(Persona persona, int months, LocalDate firstDay) {
        events.clear();
        LocalDate end = firstDay.plusMonths(months).minusDays(1);
        for (LocalDate d = firstDay; !d.isAfter(end); d = d.plusDays(1)) {
            switch (persona) {
                case SALARIED -> salariedDay(d);
                case RIDER -> riderDay(d, firstDay);
                case OVERSTRETCHED -> overstretchedDay(d, firstDay);
            }
        }
        return render(persona);
    }

    // ---------------------------------------------------------------- personas

    private void salariedDay(LocalDate d) {
        int dom = d.getDayOfMonth();
        if (dom == 25) {
            in(d, 9, "Salary Payment from 521100 - BIDII ACADEMY", 68_000);
        }
        if (dom == 26) {
            out(d, 10, "Pay Bill Online to 4002001 - JAMII HOUSING Acc. B12", 18_000, Fee.NONE);
            out(d, 10, "M-Shwari Deposit", 6_000, Fee.NONE);
            out(d, 19, "Customer Transfer to 0722483910 - MARY WANJIKU", 5_000, Fee.TRANSFER);
        }
        if (dom == 27) {
            out(d, 18, "Pay Bill Online to 888880 - KPLC PREPAID Acc. 54200178321", between(1_500, 2_500), Fee.NONE);
            out(d, 18, "Pay Bill Online to 444400 - NAIROBI WATER Acc. 77120", between(600, 900), Fee.NONE);
        }
        if (dom == 28) {
            out(d, 12, "Customer Withdrawal At Agent Till 30212 - RONGAI MPESA SHOP", 3_000, Fee.WITHDRAWAL);
        }
        if (d.getDayOfWeek() == DayOfWeek.TUESDAY || d.getDayOfWeek() == DayOfWeek.SATURDAY) {
            out(d, 17, "Merchant Payment Online to 5203457 - NAIVAS SUPERMARKET", between(1_500, 4_000), Fee.NONE);
        }
        if (weekday(d) && random.nextInt(10) < 8) {
            out(d, 7, "Merchant Payment Online to 801234 - CITY HOPPA", between(100, 250), Fee.NONE);
        }
        if (d.getDayOfWeek() == DayOfWeek.MONDAY) {
            out(d, 8, "Airtime Purchase", 200, Fee.NONE);
        }
        if (random.nextInt(30) == 0) {
            in(d, 20, "Funds received from " + phone() + " - " + name(), between(500, 2_000));
        }
    }

    private void riderDay(LocalDate d, LocalDate first) {
        int dom = d.getDayOfMonth();
        if (d.getDayOfWeek() != DayOfWeek.SUNDAY) {
            int fares = 5 + random.nextInt(6);
            for (int i = 0; i < fares; i++) {
                in(d, 6 + random.nextInt(14), "Funds received from " + phone() + " - " + name(), between(100, 350));
            }
            out(d, 6, "Merchant Payment Online to 5577210 - RUBIS RONGAI", between(450, 700), Fee.NONE);
            out(d, 13, "Customer Payment to Small Business to 0798114420 - MAMA OLIECH", between(150, 300), Fee.NONE);
        }
        if (d.getDayOfWeek() == DayOfWeek.MONDAY) {
            out(d, 9, "Pay Bill Online to 4093333 - WATU CREDIT Acc. KMGX412B", 2_800, Fee.NONE);
        }
        if (d.getDayOfWeek() == DayOfWeek.SATURDAY) {
            out(d, 20, "Customer Transfer to 0733902211 - ROSE ADHIAMBO", 2_000, Fee.TRANSFER);
            out(d, 21, "Customer Withdrawal At Agent Till 44120 - KITENGELA AGENCIES", 1_000, Fee.WITHDRAWAL);
        }
        if (dom % 2 == 0) {
            out(d, 7, "Safaricom Data Bundles purchase", between(50, 100), Fee.NONE);
        }
        if (dom == 5) {
            out(d, 8, "Pay Bill Online to 4004333 - KITENGELA HOMES Acc. 7C", 7_000, Fee.NONE);
        }
        if (dom == 10 && (d.getMonthValue() == 1 || d.getMonthValue() == 5)) {
            out(d, 9, "Pay Bill Online to 247247 - KITENGELA PRIMARY SCHOOL Acc. ADM2231", 9_500, Fee.NONE);
        }
        if (dom == 6 || dom == 20) {
            out(d, 19, "Pay Bill Online to 888880 - KPLC PREPAID Acc. 37190044512", 500, Fee.NONE);
        }
        if (d.equals(first.plusMonths(1).withDayOfMonth(4))) {
            in(d, 10, "Business Payment from 4012345 - HUSTLER FUND via API", 3_000);
        }
        if (d.equals(first.plusMonths(1).withDayOfMonth(20)) || d.equals(first.plusMonths(2).withDayOfMonth(3))) {
            out(d, 18, "Pay Bill Online to 4012345 - HUSTLER FUND Acc. 0711", 1_550, Fee.NONE);
        }
    }

    private void overstretchedDay(LocalDate d, LocalDate first) {
        int dom = d.getDayOfMonth();
        if (d.getDayOfWeek() == DayOfWeek.FRIDAY && random.nextInt(4) != 0) {
            double slowMonth = new Random(d.getYear() * 100L + d.getMonthValue()).nextDouble(0.35, 1.3);
            in(d, 17, "Business Payment from 600222 - JUA KALI WORKS via API", Math.round(between(2_000, 6_500) * slowMonth));
        }
        if (random.nextInt(9) == 0) {
            in(d, 14, "Funds received from " + phone() + " - " + name(), between(300, 1_500));
        }
        if (random.nextInt(7) < 5) {
            String house = random.nextBoolean() ? "290290 - SportPesa" : "290080 - Betika";
            out(d, 12 + random.nextInt(10), "Pay Bill Online to " + house + " Acc. 0790", between(100, 600), Fee.NONE);
        }
        if (random.nextInt(14) == 0) {
            in(d, 22, "Business Payment from 290290 - SportPesa via API", between(300, 2_000));
        }
        if (dom == 5) {
            out(d, 9, "Pay Bill Online to 4005222 - MWIKI RENTALS Acc. 12", 5_000, Fee.NONE);
        }
        out(d, 13, "Customer Payment to Small Business to 0745001122 - KIBANDA CHAI", between(120, 300), Fee.NONE);
        if (dom % 3 == 0) {
            out(d, 8, "Airtime Purchase", between(20, 100), Fee.NONE);
        }
        if (d.getDayOfWeek() == DayOfWeek.SUNDAY) {
            out(d, 18, "Customer Transfer to " + phone() + " - " + name(), between(300, 1_000), Fee.TRANSFER);
        }
        String[][] loans = {
            {"851900 - TALA", "4000"}, {"7766550 - ZASH LOANS", "3000"}, {"998844 - OKASH", "2500"}, {"851900 - TALA", "5000"}};
        for (int i = 0; i < loans.length; i++) {
            LocalDate taken = first.plusMonths(i).withDayOfMonth(8);
            long amount = Long.parseLong(loans[i][1]);
            if (d.equals(taken)) {
                in(d, 11, "Business Payment from " + loans[i][0] + " via API", amount);
            }
            if (d.equals(taken.plusDays(30)) && i % 2 == 0) {
                out(d, 16, "Pay Bill Online to " + loans[i][0] + " Acc. 0790", amount / 2, Fee.NONE);
            }
        }
    }

    // ---------------------------------------------------------------- rendering

    private String render(Persona persona) {
        events.sort(Comparator.comparing(Event::time));
        long fulizaLimit = persona == Persona.SALARIED ? 0 : persona == Persona.RIDER ? 2_000 : 3_000;
        long balance = persona == Persona.SALARIED ? 8_000 : persona == Persona.RIDER ? 600 : 150;
        long owed = 0;
        List<String> rows = new ArrayList<>();

        for (Event e : events) {
            LocalDateTime t = e.time();
            if (e.in()) {
                balance += e.kes();
                rows.add(row(t, e.details(), e.kes(), 0, balance));
                if (owed > 0) {
                    long repay = Math.min(owed, balance);
                    balance -= repay;
                    owed -= repay;
                    rows.add(row(t.plusSeconds(1), "OD Loan Repayment to 232323 - M-PESA Overdraw", 0, repay, balance));
                }
                continue;
            }
            long fee = fee(e.fee(), e.kes());
            long need = e.kes() + fee;
            if (need > balance) {
                long draw = need - balance;
                if (owed + draw > fulizaLimit) {
                    continue; // not enough money: the payment never happens
                }
                owed += draw;
                balance += draw;
                rows.add(row(t.minusSeconds(1), "OverDraft of Credit Party", draw, 0, balance));
            }
            balance -= e.kes();
            rows.add(row(t, e.details(), 0, e.kes(), balance));
            if (fee > 0) {
                balance -= fee;
                String label = e.fee() == Fee.WITHDRAWAL ? "Withdrawal Charge" : "Customer Transfer of Funds Charge";
                rows.add(row(t.plusSeconds(1), label, 0, fee, balance));
            }
        }

        StringBuilder csv = new StringBuilder(
                "Receipt No.,Completion Time,Details,Transaction Status,Paid In,Withdrawn,Balance\n");
        for (int i = rows.size() - 1; i >= 0; i--) {
            csv.append(rows.get(i)).append('\n'); // M-Pesa lists newest first
        }
        return csv.toString();
    }

    private String row(LocalDateTime t, String details, long in, long out, long balance) {
        return String.join(",", receipt(t), t.format(TIME), quote(details), "Completed",
                in > 0 ? quote(money(in)) : "", out > 0 ? quote("-" + money(out)) : "", quote(money(balance)));
    }

    /** Approximate Safaricom tariff bands in KES. */
    private static long fee(Fee type, long kes) {
        return switch (type) {
            case NONE -> 0;
            case TRANSFER -> kes <= 100 ? 0 : kes <= 500 ? 7 : kes <= 1_000 ? 13 : kes <= 1_500 ? 23
                    : kes <= 2_500 ? 33 : kes <= 3_500 ? 53 : kes <= 5_000 ? 57 : kes <= 7_500 ? 78 : 90;
            case WITHDRAWAL -> kes <= 2_500 ? 29 : kes <= 3_500 ? 52 : kes <= 5_000 ? 69 : 115;
        };
    }

    // ---------------------------------------------------------------- helpers

    private void in(LocalDate d, int hour, String details, long kes) {
        events.add(new Event(at(d, hour), details, kes, true, Fee.NONE));
    }

    private void out(LocalDate d, int hour, String details, long kes, Fee fee) {
        events.add(new Event(at(d, hour), details, kes, false, fee));
    }

    private LocalDateTime at(LocalDate d, int hour) {
        return LocalDateTime.of(d, LocalTime.of(Math.min(hour, 23), random.nextInt(60), random.nextInt(60)));
    }

    private long between(int lo, int hi) {
        return lo + random.nextInt(hi - lo + 1);
    }

    private static boolean weekday(LocalDate d) {
        return d.getDayOfWeek().getValue() <= 5;
    }

    private String phone() {
        return "07" + (10 + random.nextInt(90)) + String.format("%06d", random.nextInt(1_000_000));
    }

    private String name() {
        return NAMES[random.nextInt(NAMES.length)];
    }

    private String receipt(LocalDateTime t) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789";
        StringBuilder r = new StringBuilder("T").append((char) ('A' + t.getMonthValue() - 1));
        for (int i = 0; i < 8; i++) {
            r.append(chars.charAt(random.nextInt(chars.length())));
        }
        return r.toString();
    }

    private static String money(long kes) {
        return String.format(Locale.US, "%,d.00", kes);
    }

    private static String quote(String s) {
        return '"' + s.replace("\"", "\"\"") + '"';
    }
}
