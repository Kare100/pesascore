package ke.pesascore.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import ke.pesascore.PesaScore;
import ke.pesascore.report.CreditReport;
import ke.pesascore.report.Json;
import ke.pesascore.report.ReportFormatter;
import ke.pesascore.scoring.LoanLimitCalculator;

/**
 * REST API on the JDK's built-in HTTP server, one virtual thread per request.
 *
 * <pre>
 * GET  /health          liveness check
 * POST /api/v1/score    body: M-Pesa statement CSV; optional ?rate=0.015 (monthly interest)
 * GET  /                small upload page for trying it in a browser
 * </pre>
 *
 * Statements are scored in memory and never written to disk or logged.
 */
public final class ScoreServer {

    static final int MAX_BODY_BYTES = 5 * 1024 * 1024;

    private final HttpServer server;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final ReportFormatter formatter = new ReportFormatter();

    public ScoreServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", ex -> send(ex, 200, "application/json", "{\"status\":\"ok\"}"));
        server.createContext("/api/v1/score", this::score);
        server.createContext("/", this::home);
        server.setExecutor(executor);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
        executor.shutdown();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    private void score(HttpExchange ex) throws IOException {
        if (ex.getRequestMethod().equals("OPTIONS")) {
            send(ex, 204, "text/plain", "");
            return;
        }
        if (!ex.getRequestMethod().equals("POST")) {
            error(ex, 405, "Use POST with the statement CSV as the request body");
            return;
        }
        byte[] body;
        try (InputStream in = ex.getRequestBody()) {
            body = in.readNBytes(MAX_BODY_BYTES + 1);
        }
        if (body.length > MAX_BODY_BYTES) {
            error(ex, 413, "Statement is larger than 5 MB");
            return;
        }
        if (body.length == 0) {
            error(ex, 400, "Request body is empty: send the M-Pesa statement CSV");
            return;
        }
        try {
            double rate = rate(ex.getRequestURI());
            CreditReport report = new PesaScore(rate).assess(new StringReader(new String(body, StandardCharsets.UTF_8)));
            send(ex, 200, "application/json", formatter.json(report));
        } catch (IllegalArgumentException e) {
            error(ex, 400, e.getMessage());
        } catch (RuntimeException e) {
            error(ex, 500, "Could not score this statement");
        }
    }

    private static double rate(URI uri) {
        String query = uri.getRawQuery();
        if (query == null) {
            return LoanLimitCalculator.DEFAULT_MONTHLY_RATE;
        }
        for (String pair : query.split("&")) {
            if (pair.startsWith("rate=")) {
                try {
                    return Double.parseDouble(pair.substring(5));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("rate must be a number such as 0.015");
                }
            }
        }
        return LoanLimitCalculator.DEFAULT_MONTHLY_RATE;
    }

    private void home(HttpExchange ex) throws IOException {
        if (!ex.getRequestURI().getPath().equals("/")) {
            error(ex, 404, "Not found");
            return;
        }
        send(ex, 200, "text/html; charset=utf-8", HomePage.HTML);
    }

    private static void error(HttpExchange ex, int status, String message) throws IOException {
        send(ex, status, "application/json", Json.write(Map.of("error", message)));
    }

    private static void send(HttpExchange ex, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        if (status == 204) {
            ex.sendResponseHeaders(status, -1);
            ex.close();
            return;
        }
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }
}
