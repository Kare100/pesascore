package ke.pesascore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ke.pesascore.api.ScoreServer;
import ke.pesascore.demo.StatementGenerator;
import ke.pesascore.demo.StatementGenerator.Persona;

class ScoreServerTest {

    private ScoreServer server;
    private final HttpClient client = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).build();

    @BeforeEach
    void start() throws IOException {
        server = new ScoreServer(0);
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop();
    }

    private HttpResponse<String> post(String path, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + server.port() + path))
                .header("Content-Type", "text/csv")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void scoresAStatement() throws IOException, InterruptedException {
        String csv = new StatementGenerator(1).generate(Persona.SALARIED, 6, LocalDate.of(2026, 1, 1));
        HttpResponse<String> res = post("/api/v1/score?rate=0.02", csv);

        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("\"score\":"));
        assertTrue(res.body().contains("\"monthlyInterestRate\":0.02"));
        assertTrue(res.body().contains("\"scorecard\":["));
    }

    @Test
    void explainsBadRequests() throws IOException, InterruptedException {
        HttpResponse<String> res = post("/api/v1/score", "hello,world\n1,2\n");
        assertEquals(400, res.statusCode());
        assertTrue(res.body().contains("\"error\""));

        assertEquals(400, post("/api/v1/score", "").statusCode());
    }

    @Test
    void healthCheck() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + server.port() + "/health")).build();
        HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode());
        assertEquals("{\"status\":\"ok\"}", res.body());
    }
}
