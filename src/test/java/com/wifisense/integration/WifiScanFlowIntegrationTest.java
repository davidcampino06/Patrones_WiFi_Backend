package com.wifisense.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Scans of the nearby networks through the real API and database: the schedule asks the agent for a scan with its
 * report, the user asks for another one, the agent picks it up and delivers, repeating a scan updates the same access
 * points instead of copying them, a failure keeps its reason, and neither another user nor another agent can read
 * or write the scans of a network that is not theirs.
 */
@EnabledIfEnvironmentVariable(named = PostgresIntegrationTest.DATABASE_URL, matches = ".+")
@TestPropertySource(properties = {"wifisense.scan.manual-min-interval=0s", "wifisense.scan.poll-min-interval=0s"})
class WifiScanFlowIntegrationTest extends PostgresIntegrationTest {

    private static final String VECINO = "a".repeat(24);
    private static final String CASA = "b".repeat(24);

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper json;

    private final List<String> created = new ArrayList<>();

    @AfterEach
    void deleteUsers() {
        created.forEach(name -> jdbc.update("DELETE FROM users WHERE username = ?", name));
    }

    @Test
    void theAgentScansWhenAskedAndOnlyTheOwnerSeesTheResult() throws Exception {
        Owner ana = installAgent("Ana.Scan");
        Owner beto = installAgent("Beto.Scan");

        // first report: the network never scanned, so the schedule asks for a scan right away
        long scheduled = call(post("/api/agent/reports"), null, ana.key, Map.of("agentVersion", "1.9.0",
                "deviceCount", 1, "ssid", "Casa de Ana"), 201).get("scanId").asLong();
        deliver(ana.key, Map.of("scanId", scheduled, "status", "COMPLETED", "interfaceName", "Wi-Fi",
                "scanTool", "NETSH", "fresh", true, "durationMs", 4100, "agentVersion", "1.9.0",
                "accessPoints", List.of(ap(VECINO, "Café ☕ \"Vecino\"", 41, false), ap(CASA, "Casa de Ana", 88, true))),
                200);

        // the user presses "Escanear ahora"; the agent picks the scan up between reports
        long manual = call(post("/api/my/networks/" + ana.network + "/scans"), ana.token, null, null, 202)
                .get("id").asLong();
        call(post("/api/my/networks/" + ana.network + "/scans"), ana.token, null, null, 409);  // one at a time
        assertThat(call(get("/api/agent/scans/next"), null, ana.key, null, 200).get("scanId").asLong())
                .isEqualTo(manual);
        deliver(ana.key, Map.of("scanId", manual, "status", "COMPLETED", "agentVersion", "1.9.0",
                "accessPoints", List.of(ap(CASA, "Casa de Ana", 85, true))), 200);
        call(get("/api/agent/scans/next"), null, ana.key, null, 204);

        // repeating the scan updated the same access point instead of copying it
        assertThat(jdbc.queryForObject("SELECT count(*) FROM wifi_access_points WHERE home_network_id = ?",
                Integer.class, ana.network)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT times_seen FROM wifi_access_points WHERE bssid_fingerprint = ? "
                + "AND home_network_id = ?", Integer.class, CASA, ana.network)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT ssid FROM wifi_access_points WHERE bssid_fingerprint = ? "
                + "AND home_network_id = ?", String.class, VECINO, ana.network)).isEqualTo("Café ☕ \"Vecino\"");

        // a failed scan keeps its reason and the page keeps showing the last good result
        long failed = call(post("/api/my/networks/" + ana.network + "/scans"), ana.token, null, null, 202)
                .get("id").asLong();
        deliver(ana.key, Map.of("scanId", failed, "status", "FAILED", "failureReason", "LOCATION_PERMISSION",
                "agentVersion", "1.9.0"), 200);
        JsonNode panel = call(get("/api/my/networks/" + ana.network + "/nearby"), ana.token, null, null, 200);
        assertThat(panel.get("current").get("status").asText()).isEqualTo("FAILED");
        assertThat(panel.get("current").get("failureMessage").asText()).contains("permiso de ubicación");
        assertThat(panel.get("lastCompleted").get("id").asLong()).isEqualTo(manual);
        assertThat(panel.get("accessPoints").findValuesAsText("ssid")).containsExactly("Casa de Ana");
        assertThat(panel.get("accessPoints").get(0).get("timesSeen").asInt()).isEqualTo(2);
        assertThat(panel.get("stale").asBoolean()).isFalse();
        assertThat(call(get("/api/my/networks/" + ana.network + "/scans"), ana.token, null, null, 200)
                .findValuesAsText("status")).containsExactly("FAILED", "COMPLETED", "COMPLETED");
        assertThat(call(get("/api/my/networks/" + ana.network + "/scans/" + scheduled), ana.token, null, null, 200)
                .get("accessPoints").findValuesAsText("ssid")).containsExactly("Casa de Ana", "Café ☕ \"Vecino\"");

        // a finished scan cannot be delivered again
        deliver(ana.key, Map.of("scanId", manual, "status", "COMPLETED", "agentVersion", "1.9.0",
                "accessPoints", List.of()), 409);

        // Beto cannot see nor start the scans of Ana's network; his agent cannot write into them
        call(get("/api/my/networks/" + ana.network + "/nearby"), beto.token, null, null, 404);
        call(post("/api/my/networks/" + ana.network + "/scans"), beto.token, null, null, 404);
        call(get("/api/my/networks/" + ana.network + "/scans"), beto.token, null, null, 404);
        call(get("/api/my/networks/" + ana.network + "/scans/" + manual), beto.token, null, null, 404);
        call(get("/api/my/networks/" + beto.network + "/scans/" + manual), beto.token, null, null, 404);
        long anas = call(post("/api/my/networks/" + ana.network + "/scans"), ana.token, null, null, 202)
                .get("id").asLong();
        deliver(beto.key, Map.of("scanId", anas, "status", "COMPLETED", "agentVersion", "1.9.0",
                "accessPoints", List.of(ap("c".repeat(24), "Intruso", 99, false))), 404);
        assertThat(jdbc.queryForObject("SELECT status FROM wifi_scan_sessions WHERE id = ?", String.class, anas))
                .isEqualTo("PENDING");

        // the agent never needs a session; a wrong key gets nothing
        call(get("/api/agent/scans/next"), null, "wsa_" + "x".repeat(43), null, 401);
    }

    @Test
    void aNetworkWithoutAgentCannotScan() throws Exception {
        String token = signUp("Carla.Scan");
        long network = call(post("/api/my/networks"), token, null, Map.of("name", "Sin agente"), 201)
                .get("id").asLong();

        call(post("/api/my/networks/" + network + "/scans"), token, null, null, 409);
        JsonNode panel = call(get("/api/my/networks/" + network + "/nearby"), token, null, null, 200);
        assertThat(panel.get("agentInstalled").asBoolean()).isFalse();
        assertThat(panel.get("accessPoints")).isEmpty();
    }

    private record Owner(String token, long network, String key) {
    }

    private Owner installAgent(String prefix) throws Exception {
        String token = signUp(prefix);
        long network = call(post("/api/my/networks"), token, null, Map.of("name", "Casa " + prefix), 201)
                .get("id").asLong();
        String code = call(post("/api/my/networks/" + network + "/agent/pairing"), token, null, null, 201)
                .get("code").asText();
        String key = call(post("/api/agent/pair"), null, null, Map.of("code", code), 200).get("key").asText();
        return new Owner(token, network, key);
    }

    private String signUp(String prefix) throws Exception {
        String name = prefix + unique();
        created.add(name);
        return call(post("/api/auth/register"), null, null,
                Map.of("username", name, "password", PASSWORD, "email", name + "@ejemplo.com"), 201)
                .get("token").asText();
    }

    private static Map<String, Object> ap(String bssid, String ssid, int signal, boolean connected) {
        Map<String, Object> ap = new HashMap<>(Map.of("bssid", bssid, "bssidSuffix", "F1", "ssid", ssid,
                "signalPercent", signal, "channel", 6, "band", "2.4", "authentication", "WPA2",
                "securityLabel", "WPA2-Personal", "cipher", "CCMP"));
        ap.put("connected", connected);
        return ap;
    }

    private void deliver(String key, Map<String, Object> result, int expected) throws Exception {
        call(post("/api/agent/scans"), null, key, result, expected);
    }

    private JsonNode call(MockHttpServletRequestBuilder request, String token, String agentKey, Object body,
                          int expected) throws Exception {
        request.header("X-Forwarded-For", "10.24." + ThreadLocalRandom.current().nextInt(256) + "."
                + ThreadLocalRandom.current().nextInt(256));
        if (token != null) {
            request.header(AUTHORIZATION, "Bearer " + token);
        }
        if (agentKey != null) {
            request.header("X-Agent-Key", agentKey);
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
        }
        String response = mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse()
                .getContentAsString();
        return response.isEmpty() ? json.nullNode() : json.readTree(response);
    }
}
