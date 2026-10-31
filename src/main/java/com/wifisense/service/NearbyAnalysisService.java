package com.wifisense.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.wifisense.analysis.AiAnalysisClient;
import com.wifisense.analysis.AiNearbyRequest;
import com.wifisense.dto.NearbyAnalysisResponse;
import com.wifisense.dto.NearbyAnalysisResponse.Item;
import com.wifisense.model.ScanStatus;
import com.wifisense.model.WifiScanObservation;
import com.wifisense.model.WifiScanSession;
import com.wifisense.repository.HomeNetworkRepository;
import com.wifisense.repository.WifiScanObservationRepository;
import com.wifisense.repository.WifiScanSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Asks the AI to read the last completed scan of a Mi Wi-Fi network: crowded channels, weak security nearby and
 * networks that look like the user's own. Only the owner can ask (another account's network is "not found"). The
 * scan is read in a short transaction and the AI is called outside it; when the AI is down the answer says so.
 */
@Service
public class NearbyAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(NearbyAnalysisService.class);

    private final HomeNetworkRepository networks;
    private final WifiScanSessionRepository scans;
    private final WifiScanObservationRepository observations;
    private final AiAnalysisClient ai;
    private final TransactionTemplate transactions;

    public NearbyAnalysisService(HomeNetworkRepository networks, WifiScanSessionRepository scans,
                                 WifiScanObservationRepository observations, AiAnalysisClient ai,
                                 TransactionTemplate transactions) {
        this.networks = networks;
        this.scans = scans;
        this.observations = observations;
        this.ai = ai;
        this.transactions = transactions;
    }

    private record Loaded(WifiScanSession scan, AiNearbyRequest request) {
    }

    public NearbyAnalysisResponse analyze(String username, Long networkId) {
        Optional<Loaded> loaded = transactions.execute(status -> {
            networks.findByIdAndOwner_Username(networkId, username)
                    .orElseThrow(() -> new ResourceNotFoundException("la red", networkId));
            return scans.findFirstByNetworkIdAndStatusOrderByFinishedAtDesc(networkId, ScanStatus.COMPLETED)
                    .map(scan -> new Loaded(scan, request(observations.findByScan(scan.getId()))));
        });
        if (loaded.isEmpty()) {
            return NearbyAnalysisResponse.unavailable(null, null,
                    "Todavía no hay un escaneo completado: escanea las redes cercanas primero.");
        }
        WifiScanSession scan = loaded.get().scan();
        if (loaded.get().request().accessPoints().isEmpty()) {
            return new NearbyAnalysisResponse(scan.getId(), scan.getFinishedAt(), true, null, List.of(), List.of(),
                    "El último escaneo no vio redes: no hay nada que analizar.");
        }
        try {
            return read(scan, ai.nearbyAnalyze(loaded.get().request()));
        } catch (RuntimeException e) {
            log.warn("Nearby analysis of network {} unavailable: {}", networkId, e.getMessage());
            return NearbyAnalysisResponse.unavailable(scan.getId(), scan.getFinishedAt(),
                    "El análisis con IA no está disponible en este momento; la lista de redes sigue siendo la del "
                            + "escaneo.");
        }
    }

    static AiNearbyRequest request(List<WifiScanObservation> seen) {
        return new AiNearbyRequest(seen.stream().map(o -> new AiNearbyRequest.AccessPoint(o.getSsid(),
                o.getAccessPoint().isHidden(), o.getAuthentication(), o.getCipher(), o.getChannel(), o.getBand(),
                o.getSignalPercent(), o.getSignalDbm(), o.isConnected(), o.getAccessPoint().getVendor())).toList());
    }

    static NearbyAnalysisResponse read(WifiScanSession scan, JsonNode answer) {
        if (answer == null || !answer.isObject()) {
            throw new IllegalStateException("La IA respondió algo que no es un análisis.");
        }
        List<Item> facts = new ArrayList<>();
        for (JsonNode fact : answer.path("facts")) {
            facts.add(item(fact, null));
        }
        List<Item> inferences = new ArrayList<>();
        for (JsonNode inference : answer.path("inferences")) {
            if (inference.path("confidence").isNumber()) {
                double c = Math.max(0, Math.min(1, inference.path("confidence").asDouble()));
                inferences.add(item(inference, BigDecimal.valueOf(c).setScale(2, RoundingMode.HALF_UP)));
            }
        }
        String uncertainty = answer.hasNonNull("uncertainty") ? answer.get("uncertainty").asText() : null;
        return new NearbyAnalysisResponse(scan.getId(), scan.getFinishedAt(), true, null, facts, inferences,
                uncertainty);
    }

    private static Item item(JsonNode node, BigDecimal confidence) {
        String severity = node.path("severity").asText("INFO");
        if (confidence != null && "CRITICAL".equals(severity)) {
            severity = "WARNING";  // an inference is never CRITICAL
        }
        return new Item(node.path("code").asText(), severity, node.path("title").asText(),
                node.path("evidence").asText(), node.hasNonNull("recommendation")
                ? node.get("recommendation").asText() : null, confidence);
    }
}
