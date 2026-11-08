package ke.pesascore;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ke.pesascore.report.Json;

class JsonTest {

    @Test
    void writesNestedValuesAndEscapesText() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", "Wanjiru \"Wawa\"\n");
        m.put("score", 712);
        m.put("ratio", 0.123456);
        m.put("ok", true);
        m.put("tags", List.of("a", 1));
        m.put("none", null);
        assertEquals("{\"name\":\"Wanjiru \\\"Wawa\\\"\\n\",\"score\":712,\"ratio\":0.1235,\"ok\":true,"
                + "\"tags\":[\"a\",1],\"none\":null}", Json.write(m));
    }
}
