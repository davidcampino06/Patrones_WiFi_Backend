package com.wifisense.controller;

import com.wifisense.dto.AgentPairRequest;
import com.wifisense.dto.AgentPairResponse;
import com.wifisense.dto.AgentReportAck;
import com.wifisense.dto.AgentReportRequest;
import com.wifisense.dto.AgentScanResult;
import com.wifisense.dto.AgentScanTask;
import com.wifisense.dto.WifiScanResponse;
import com.wifisense.service.AgentPairingService;
import com.wifisense.service.AgentReportService;
import com.wifisense.service.AgentScanService;
import com.wifisense.service.SensorReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Entry point of the WiFiSense agent. It has no user session: it authenticates with its own key in the X-Agent-Key
 * header, and the key decides which network the report belongs to (the body never says it): a network of Mi Wi-Fi,
 * or a team network when the agent is that network's sensor. The agent of Mi Wi-Fi also asks here for scans of the
 * nearby Wi-Fi networks and delivers their results.
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    public static final String KEY_HEADER = "X-Agent-Key";
    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final AgentReportService service;
    private final SensorReportService sensors;
    private final AgentPairingService pairing;
    private final AgentScanService scans;

    public AgentController(AgentReportService service, SensorReportService sensors, AgentPairingService pairing,
                           AgentScanService scans) {
        this.service = service;
        this.sensors = sensors;
        this.pairing = pairing;
        this.scans = scans;
    }

    /** The installer exchanges the short code from Mi Wi-Fi for the agent key (the only time the key travels). */
    @PostMapping("/pair")
    public ResponseEntity<AgentPairResponse> pair(@RequestBody AgentPairRequest request, HttpServletRequest http) {
        String forwarded = http.getHeader("X-Forwarded-For");
        String address = StringUtils.hasText(forwarded) ? forwarded.split(",")[0].trim() : http.getRemoteAddr();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(pairing.pair(request.code(), address));
    }

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public AgentReportAck report(@RequestHeader(value = KEY_HEADER, required = false) String key,
                                 @Valid @RequestBody AgentReportRequest request) {
        return sensors.receive(key, request).orElseGet(() -> withDueScan(key, service.receive(key, request)));
    }

    /** The report is already saved: a problem with the scans must not turn it into an error, the agent just waits. */
    private AgentReportAck withDueScan(String key, AgentReportAck ack) {
        try {
            return scans.dueWithReport(key).map(ack::withScan).orElse(ack);
        } catch (RuntimeException e) {
            log.warn("Report answered without a scan: {}", e.getMessage());
            return ack;
        }
    }

    /** Between reports the agent asks every 30 seconds whether the user pressed "Escanear ahora" (204: nothing). */
    @GetMapping("/scans/next")
    public ResponseEntity<AgentScanTask> nextScan(@RequestHeader(value = KEY_HEADER, required = false) String key) {
        return scans.next(key).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** What a scan saw, or why it failed. */
    @PostMapping("/scans")
    public WifiScanResponse scanResult(@RequestHeader(value = KEY_HEADER, required = false) String key,
                                       @Valid @RequestBody AgentScanResult result) {
        return WifiScanResponse.from(scans.receive(key, result));
    }
}
