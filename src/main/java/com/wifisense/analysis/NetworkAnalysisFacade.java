package com.wifisense.analysis;

import com.wifisense.dto.AnalysisResultResponse;
import com.wifisense.dto.StrategyResponse;
import com.wifisense.event.AnalysisCompletedEvent;
import com.wifisense.event.NetworkEventPublisher;
import com.wifisense.event.NetworkStatusChangedEvent;
import com.wifisense.model.*;
import com.wifisense.repository.*;
import com.wifisense.service.MeasurementCollectionService;
import com.wifisense.service.ResourceNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

/**
 * Facade over the analysis subsystem. Controllers and the monitoring job call one method;
 * this class coordinates data retrieval, the selected strategy, persistence, state and events.
 */
@Service
public class NetworkAnalysisFacade {

    private final Map<AnalysisType, AnalysisStrategy> strategies = new EnumMap<>(AnalysisType.class);
    private final NetworkRepository networks;
    private final MeasurementRepository measurements;
    private final TrafficObservationRepository traffic;
    private final MeasurementCollectionService collection;
    private final AnalysisResultRepository results;
    private final AiPredictionRepository predictions;
    private final UserRepository users;
    private final NetworkEventPublisher events;
    private final AnalysisProperties properties;
    private final Clock clock;

    public NetworkAnalysisFacade(List<AnalysisStrategy> strategyBeans, NetworkRepository networks,
                                 MeasurementRepository measurements, TrafficObservationRepository traffic,
                                 MeasurementCollectionService collection, AnalysisResultRepository results,
                                 AiPredictionRepository predictions, UserRepository users,
                                 NetworkEventPublisher events, AnalysisProperties properties, Clock clock) {
        strategyBeans.forEach(strategy -> strategies.put(strategy.type(), strategy));
        this.networks = networks;
        this.measurements = measurements;
        this.traffic = traffic;
        this.collection = collection;
        this.results = results;
        this.predictions = predictions;
        this.users = users;
        this.events = events;
        this.properties = properties;
        this.clock = clock;
    }

    public List<StrategyResponse> availableStrategies() {
        return strategies.values().stream().map(s -> new StrategyResponse(s.type(), s.description())).toList();
    }

    /** requestedBy is null when the analysis is triggered by the monitoring job. */
    @Transactional
    public AnalysisResultResponse analyze(Long networkId, AnalysisType type, String requestedBy) {
        AnalysisStrategy strategy = Optional.ofNullable(strategies.get(type))
                .orElseThrow(() -> new IllegalArgumentException("Unsupported analysis type " + type));
        Network network = networks.findWithZoneById(networkId)
                .orElseThrow(() -> new ResourceNotFoundException("Network", networkId));

        AnalysisContext context = buildContext(network);
        AnalysisOutcome outcome = strategy.analyze(context);

        AnalysisResult result = results.save(new AnalysisResult(network, findUser(requestedBy), type,
                outcome.status(), outcome.score(), truncate(outcome.summary()), context.history().size()));
        AiPrediction prediction = outcome.ai()
                .map(response -> predictions.save(AiPrediction.from(result, response)))
                .orElse(null);

        updateState(network, outcome, result);
        events.publish(new AnalysisCompletedEvent(network.getId(), network.getSsid(), result.getId(), type,
                outcome.status(), outcome.score(), Instant.now(clock)));
        return AnalysisResultResponse.from(result, prediction);
    }

    private AnalysisContext buildContext(Network network) {
        List<Measurement> window = recentMeasurements(network.getId());
        if (window.isEmpty()) {
            collection.collect(network.getId());
            window = recentMeasurements(network.getId());
        }
        Instant from = window.getFirst().getMeasuredAt();
        Instant to = window.getLast().getMeasuredAt();
        return new AnalysisContext(network.getId(), network.getSsid(), window,
                traffic.findByNetworkIdAndObservedAtBetween(network.getId(), from, to));
    }

    /** Chronological order (oldest first), as the strategies expect. */
    private List<Measurement> recentMeasurements(Long networkId) {
        List<Measurement> newestFirst = measurements.findByNetworkIdOrderByMeasuredAtDesc(networkId,
                PageRequest.of(0, properties.windowSize()));
        return new ArrayList<>(newestFirst).reversed();
    }

    private void updateState(Network network, AnalysisOutcome outcome, AnalysisResult result) {
        NetworkStatus previous = network.getStatus();
        if (network.applyDetectedStatus(outcome.status())) {
            events.publish(new NetworkStatusChangedEvent(network.getId(), network.getSsid(), previous,
                    network.getStatus(), result.getId(), outcome.summary(), Instant.now(clock)));
        }
    }

    private User findUser(String username) {
        return username == null ? null : users.findByUsername(username).orElse(null);
    }

    private static String truncate(String text) {
        return text.length() <= 500 ? text : text.substring(0, 497) + "...";
    }
}
