package com.wifisense.controller;

import com.wifisense.dto.NearbyAccessPoint;
import com.wifisense.dto.NearbyNetworksResponse;
import com.wifisense.dto.WifiScanDetail;
import com.wifisense.dto.WifiScanResponse;
import com.wifisense.security.SecurityConfig;
import com.wifisense.security.SecurityProperties;
import com.wifisense.service.ResourceNotFoundException;
import com.wifisense.service.ScanLimitException;
import com.wifisense.service.WifiScanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MyWifiScanController.class)
@Import(SecurityConfig.class)
@EnableConfigurationProperties(SecurityProperties.class)
@TestPropertySource(properties = "wifisense.security.jwt-secret=test-secret-that-is-at-least-32-bytes-long")
class MyWifiScanControllerTest {

    private static final Instant AT = Instant.parse("2026-10-26T20:00:00Z");
    private static final WifiScanResponse PENDING = new WifiScanResponse(50L, "MANUAL", "PENDING", AT, null, null,
            null, null, null, null, null, null, null, null);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private WifiScanService scans;
    @MockitoBean
    private UserDetailsService userDetailsService;

    private static RequestPostProcessor as(String username, String role) {
        return jwt().jwt(token -> token.subject(username)).authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Test
    void theOwnerStartsAScanAndIsToldItIsWaiting() throws Exception {
        when(scans.request("ana", 7L)).thenReturn(PENDING);

        mvc.perform(post("/api/my/networks/7/scans").with(as("ana", "USER")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.id").value(50));
    }

    @Test
    void thePanelListsWhatTheLastScanSaw() throws Exception {
        NearbyAccessPoint vecino = new NearbyAccessPoint(11L, "Café ☕", false, "F1", "TP-Link", 72, null, 6, "2.4",
                "802.11n", "WPA2", "WPA2-Personal", "CCMP", false, AT, 3);
        when(scans.nearby("ana", 7L)).thenReturn(new NearbyNetworksResponse(true, true, PENDING, null, false, 150,
                AT, null, List.of(vecino)));

        mvc.perform(get("/api/my/networks/7/nearby").with(as("ana", "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessPoints[0].ssid").value("Café ☕"))
                .andExpect(jsonPath("$.accessPoints[0].signalDbm").doesNotExist())
                .andExpect(jsonPath("$.current.status").value("PENDING"));
    }

    @Test
    void theHistoryAndOneScanOfIt() throws Exception {
        when(scans.history("ana", 7L, 5)).thenReturn(List.of(PENDING));
        when(scans.detail("ana", 7L, 50L)).thenReturn(new WifiScanDetail(PENDING, List.of()));

        mvc.perform(get("/api/my/networks/7/scans?limit=5").with(as("ana", "USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(50));
        mvc.perform(get("/api/my/networks/7/scans/50").with(as("ana", "USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.scan.id").value(50));
    }

    @Test
    void theNetworkOfAnotherAccountIsNotFound() throws Exception {
        when(scans.nearby("beto", 7L)).thenThrow(new ResourceNotFoundException("la red", 7L));

        mvc.perform(get("/api/my/networks/7/nearby").with(as("beto", "USER"))).andExpect(status().isNotFound());
    }

    @Test
    void pressingTheButtonAgainRightAwaySaysWhenToRetry() throws Exception {
        when(scans.request("ana", 7L)).thenThrow(new ScanLimitException("Acabas de escanear.", 21));

        mvc.perform(post("/api/my/networks/7/scans").with(as("ana", "USER")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "21"));
    }

    @Test
    void aNetworkWithoutAgentAnswersConflictWithTheReason() throws Exception {
        when(scans.request("ana", 7L)).thenThrow(new IllegalStateException("Esta red aún no tiene el agente instalado."));

        mvc.perform(post("/api/my/networks/7/scans").with(as("ana", "USER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Esta red aún no tiene el agente instalado."));
    }

    @Test
    void theTeamAndAnonymousVisitorsCannotUseIt() throws Exception {
        mvc.perform(post("/api/my/networks/7/scans").with(as("jefe", "ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/my/networks/7/nearby")).andExpect(status().isUnauthorized());
        verifyNoInteractions(scans);
    }
}
