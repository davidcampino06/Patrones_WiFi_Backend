package com.wifisense.controller;

import com.wifisense.dto.NearbyNetworksResponse;
import com.wifisense.dto.WifiScanDetail;
import com.wifisense.dto.WifiScanResponse;
import com.wifisense.service.WifiScanService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * "Redes cercanas" of one of the user's networks: the Wi-Fi networks the antenna of the agent's computer can see.
 * Only the owner of the network can use it (the role is checked by SecurityConfig, the ownership by the service).
 */
@RestController
@RequestMapping("/api/my/networks/{networkId}")
public class MyWifiScanController {

    private final WifiScanService scans;

    public MyWifiScanController(WifiScanService scans) {
        this.scans = scans;
    }

    /** The panel: last result, the scan in progress and whether the result is out of date. */
    @GetMapping("/nearby")
    public NearbyNetworksResponse nearby(@PathVariable Long networkId, Authentication authentication) {
        return scans.nearby(authentication.getName(), networkId);
    }

    /** "Escanear ahora": the scan waits for the agent (202); the page follows it with GET /nearby. */
    @PostMapping("/scans")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public WifiScanResponse scan(@PathVariable Long networkId, Authentication authentication) {
        return scans.request(authentication.getName(), networkId);
    }

    /** History of scans, newest first (default 10, at most 50). */
    @GetMapping("/scans")
    public List<WifiScanResponse> history(@PathVariable Long networkId, @RequestParam(defaultValue = "10") int limit,
                                          Authentication authentication) {
        return scans.history(authentication.getName(), networkId, limit);
    }

    /** One scan of the history and what it saw. */
    @GetMapping("/scans/{scanId}")
    public WifiScanDetail detail(@PathVariable Long networkId, @PathVariable Long scanId,
                                 Authentication authentication) {
        return scans.detail(authentication.getName(), networkId, scanId);
    }
}
