package com.wifisense.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Isolation between users through the real HTTP API and database: changing the id of a network in the browser never
 * shows or changes someone else's data (it answers 404, the same as a network that does not exist), and an end user
 * does not even learn that the routes of the team exist.
 */
@EnabledIfEnvironmentVariable(named = PostgresIntegrationTest.DATABASE_URL, matches = ".+")
class UserIsolationIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper json;

    private final List<String> created = new ArrayList<>();

    @AfterEach
    void deleteUsers() {
        created.forEach(name -> jdbc.update("DELETE FROM users WHERE username = ?", name));  // their networks go too
    }

    private String signUp(String prefix) throws Exception {
        String name = prefix + unique();
        created.add(name);
        return call(post("/api/auth/register"), null,
                Map.of("username", name, "password", PASSWORD, "email", name + "@ejemplo.com"), 201)
                .get("token").asText();
    }

    @Test
    void aUserCannotSeeOrChangeTheNetworkOfAnotherByChangingItsId() throws Exception {
        String ana = signUp("Ana.Iso");
        String beto = signUp("Beto.Iso");
        long casa = call(post("/api/my/networks"), ana, Map.of("name", "Casa de Ana", "contractedMbps", 200), 201)
                .get("id").asLong();
        String fingerprint = "a".repeat(24);

        // Ana sees her network and its agent page
        assertThat(call(get("/api/my/networks"), ana, null, 200).findValuesAsText("name")).contains("Casa de Ana");
        call(get("/api/my/networks/" + casa + "/agent"), ana, null, 200);

        // Beto does not see it in his list, and every route with its id answers 404 as if it did not exist
        assertThat(call(get("/api/my/networks"), beto, null, 200).findValuesAsText("name")).doesNotContain("Casa de Ana");
        call(put("/api/my/networks/" + casa), beto, Map.of("name", "Robada"), 404);
        call(get("/api/my/networks/" + casa + "/agent"), beto, null, 404);
        call(post("/api/my/networks/" + casa + "/agent/pairing"), beto, null, 404);
        call(post("/api/my/networks/" + casa + "/agent/identify"), beto, null, 404);
        call(put("/api/my/networks/" + casa + "/agent/devices/" + fingerprint), beto, Map.of("authorized", false), 404);
        call(get("/api/my/networks/" + casa + "/agent/devices/" + fingerprint + "/sessions"), beto, null, 404);
        call(delete("/api/my/networks/" + casa + "/agent/key"), beto, null, 404);
        call(delete("/api/my/networks/" + casa), beto, null, 404);
        call(get("/api/my/networks/" + Long.MAX_VALUE + "/agent"), beto, null, 404);  // the same as one that exists

        // nothing changed
        assertThat(jdbc.queryForObject("SELECT name FROM home_networks WHERE id = ?", String.class, casa))
                .isEqualTo("Casa de Ana");
    }

    @Test
    void anEndUserDoesNotLearnThatTheRoutesOfTheTeamExist() throws Exception {
        String carla = signUp("Carla.Iso");

        for (String route : List.of("/api/networks", "/api/home-networks", "/api/ai/model", "/api/ai/training",
                "/api/users", "/api/alerts", "/api/dashboard/summary", "/api/reports/networks/1/weekly")) {
            call(get(route), carla, null, 404);
        }
        call(post("/api/ai/training"), carla, null, 404);
        call(post("/api/networks/1/sensor/identify"), carla, null, 404);
        call(get("/api/networks"), null, null, 401);  // without a session: 401, not 404
    }

    private JsonNode call(MockHttpServletRequestBuilder request, String token, Object body, int expected)
            throws Exception {
        // a different address each time: the sign-up limit per address does not get in the way
        request.header("X-Forwarded-For", "10.13." + ThreadLocalRandom.current().nextInt(256) + "."
                + ThreadLocalRandom.current().nextInt(256));
        if (token != null) {
            request.header(AUTHORIZATION, "Bearer " + token);
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
        }
        String response = mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse()
                .getContentAsString();
        return response.isEmpty() ? json.nullNode() : json.readTree(response);
    }
}
