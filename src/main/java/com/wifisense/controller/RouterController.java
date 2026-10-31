package com.wifisense.controller;

import com.wifisense.dto.NearbyAnalysisResponse;
import com.wifisense.dto.RouterInfoResponse;
import com.wifisense.service.NearbyAnalysisService;
import com.wifisense.service.RouterService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * The router of a network (what WiFiSense knows of it and what it cannot do), for the owner of a Mi Wi-Fi network
 * and for the team; and the AI reading of the last scan of the nearby networks, for the owner.
 */
@RestController
public class RouterController {

    private final RouterService routers;
    private final NearbyAnalysisService nearby;

    public RouterController(RouterService routers, NearbyAnalysisService nearby) {
        this.routers = routers;
        this.nearby = nearby;
    }

    @GetMapping("/api/my/networks/{networkId}/router")
    public RouterInfoResponse myRouter(@PathVariable Long networkId, Authentication authentication) {
        return routers.forHome(authentication.getName(), networkId);
    }

    @GetMapping("/api/networks/{networkId}/sensor/router")
    public RouterInfoResponse teamRouter(@PathVariable Long networkId) {
        return routers.forTeam(networkId);
    }

    /** Facts and inferences about the last completed scan of "Redes cercanas". */
    @GetMapping("/api/my/networks/{networkId}/nearby/analysis")
    public NearbyAnalysisResponse nearbyAnalysis(@PathVariable Long networkId, Authentication authentication) {
        return nearby.analyze(authentication.getName(), networkId);
    }
}
