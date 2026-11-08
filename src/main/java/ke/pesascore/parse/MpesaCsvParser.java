package ke.pesascore.parse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ke.pesascore.model.Category;
import ke.pesascore.model.Money;
import ke.pesascore.model.Transaction;

/**
 * Reads an M-Pesa statement exported to CSV (columns: Receipt No., Completion Time,
 * Details, Transaction Status, Paid In, Withdrawn, Balance). Bad rows are skipped and
 * reported as warnings rather than failing the whole statement.
 */
public final class MpesaCsvParser {

    public record Result(List<Transaction> transactions, List<String> warnings) {
    }

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

    private final TransactionClassifier classifier;

    public MpesaCsvParser(TransactionClassifier classifier) {
        this.classifier = classifier;
    }

    public MpesaCsvParser() {
        this(new TransactionClassifier());
    }

    public Result parse(Reader source) throws IOException {
        BufferedReader reader = new BufferedReader(source);
        List<String> warnings = new ArrayList<>();
        List<Transaction> transactions = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        Columns columns = null;
        String line;
        int lineNo = 0;
        while ((line = reader.readLine()) != null) {
            lineNo++;
            if (lineNo == 1 && line.startsWith("﻿")) {
                line = line.substring(1);
            }
            if (line.isBlank()) {
                continue;
            }
            List<String> cells = Csv.split(line);
            if (columns == null) {
                columns = Columns.fromHeader(cells);
                continue;
            }
            if (cells.size() < columns.width()) {
                warnings.add("Line " + lineNo + ": expected " + columns.width() + " columns, found " + cells.size());
                continue;
            }
            try {
                Transaction t = toTransaction(cells, columns);
                String key = t.receipt() + '|' + t.details() + '|' + t.amountCents();
                if (seen.add(key)) {
                    transactions.add(t);
                }
            } catch (IllegalArgumentException e) {
                warnings.add("Line " + lineNo + ": " + e.getMessage());
            }
        }
        if (columns == null) {
            throw new IllegalArgumentException(
                    "This does not look like an M-Pesa statement: no 'Receipt No.' / 'Completion Time' header row found");
        }
        transactions.sort(Comparator.comparing(Transaction::time));
        return new Result(List.copyOf(transactions), List.copyOf(warnings));
    }

    private Transaction toTransaction(List<String> cells, Columns c) {
        String receipt = cells.get(c.receipt);
        String details = cells.get(c.details).replaceAll("\\s+", " ").trim();
        if (receipt.isEmpty() || details.isEmpty()) {
            throw new IllegalArgumentException("missing receipt or details");
        }
        LocalDateTime time = parseTime(cells.get(c.time));
        boolean completed = c.status < 0 || cells.get(c.status).equalsIgnoreCase("Completed");
        long paidIn = Math.abs(Money.parseCents(cells.get(c.paidIn)));
        long withdrawn = Math.abs(Money.parseCents(cells.get(c.withdrawn)));
        long balance = c.balance < 0 ? 0 : Money.parseCents(cells.get(c.balance));
        if (paidIn == 0 && withdrawn == 0) {
            throw new IllegalArgumentException("no amount in 'Paid In' or 'Withdrawn'");
        }
        Category category = classifier.classify(details, paidIn > 0);
        return new Transaction(receipt, time, details, completed, paidIn, withdrawn, balance, category,
                classifier.counterparty(details));
    }

    private static LocalDateTime parseTime(String raw) {
        for (DateTimeFormatter f : DATE_FORMATS) {
            try {
                return LocalDateTime.parse(raw.trim(), f);
            } catch (DateTimeParseException ignored) {
                // try the next format
            }
        }
        throw new IllegalArgumentException("unreadable date '" + raw + "'");
    }

    private record Columns(int receipt, int time, int details, int status, int paidIn, int withdrawn, int balance) {

        static Columns fromHeader(List<String> header) {
            int receipt = find(header, "receipt");
            int time = find(header, "completion time", "time", "date");
            int details = find(header, "details", "description");
            int paidIn = find(header, "paid in", "credit", "money in");
            int withdrawn = find(header, "withdrawn", "debit", "money out");
            if (receipt < 0 || time < 0 || details < 0 || paidIn < 0 || withdrawn < 0) {
                return null;
            }
            return new Columns(receipt, time, details, find(header, "status"), paidIn, withdrawn,
                    find(header, "balance"));
        }

        int width() {
            return Math.max(Math.max(Math.max(receipt, time), Math.max(details, status)),
                    Math.max(Math.max(paidIn, withdrawn), balance)) + 1;
        }

        private static int find(List<String> header, String... names) {
            for (String name : names) {
                for (int i = 0; i < header.size(); i++) {
                    if (header.get(i).toLowerCase(Locale.ROOT).contains(name)) {
                        return i;
                    }
                }
            }
            return -1;
        }
    }
}
