package ke.pesascore;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Locale;

import ke.pesascore.api.ScoreServer;
import ke.pesascore.demo.StatementGenerator;
import ke.pesascore.demo.StatementGenerator.Persona;
import ke.pesascore.report.CreditReport;
import ke.pesascore.report.ReportFormatter;
import ke.pesascore.scoring.LoanLimitCalculator;

/** Command-line entry point. */
public final class Main {

    private static final String USAGE = """
            PesaScore: credit scores and safe loan limits from M-Pesa statements

            Usage:
              java -jar pesascore.jar score <statement.csv> [--json] [--rate 0.015]
              java -jar pesascore.jar serve [--port 8080]
              java -jar pesascore.jar demo <salaried|rider|overstretched> [--months 6] [--out file.csv]
            """;

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.print(USAGE);
            return;
        }
        try {
            switch (args[0]) {
                case "score" -> score(args);
                case "serve" -> serve(args);
                case "demo" -> demo(args);
                default -> fail("Unknown command '" + args[0] + "'\n\n" + USAGE);
            }
        } catch (IllegalArgumentException e) {
            fail(e.getMessage());
        }
    }

    private static void score(String[] args) throws IOException {
        if (args.length < 2) {
            fail("Give the path to a statement CSV\n\n" + USAGE);
        }
        double rate = Double.parseDouble(option(args, "--rate", String.valueOf(LoanLimitCalculator.DEFAULT_MONTHLY_RATE)));
        CreditReport report;
        try (Reader in = Files.newBufferedReader(Path.of(args[1]), StandardCharsets.UTF_8)) {
            report = new PesaScore(rate).assess(in);
        }
        ReportFormatter formatter = new ReportFormatter();
        System.out.println(flag(args, "--json") ? formatter.json(report) : formatter.text(report));
    }

    private static void serve(String[] args) throws IOException {
        int port = Integer.parseInt(option(args, "--port", System.getenv().getOrDefault("PORT", "8080")));
        ScoreServer server = new ScoreServer(port);
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        System.out.println("PesaScore API listening on http://localhost:" + server.port());
    }

    private static void demo(String[] args) throws IOException {
        if (args.length < 2) {
            fail("Choose a persona: salaried, rider or overstretched");
        }
        Persona persona = Persona.valueOf(args[1].toUpperCase(Locale.ROOT));
        int months = Integer.parseInt(option(args, "--months", "6"));
        String csv = new StatementGenerator(42).generate(persona, months, LocalDate.of(2026, 1, 1));
        String out = option(args, "--out", null);
        if (out == null) {
            System.out.print(csv);
        } else {
            Files.writeString(Path.of(out), csv, StandardCharsets.UTF_8);
            System.out.println("Wrote " + out);
        }
    }

    private static String option(String[] args, String name, String fallback) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(name)) {
                return args[i + 1];
            }
        }
        return fallback;
    }

    private static boolean flag(String[] args, String name) {
        for (String a : args) {
            if (a.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static void fail(String message) {
        System.err.println(message);
        System.exit(1);
    }
}
