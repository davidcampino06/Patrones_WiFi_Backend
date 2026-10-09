package com.wifisense.analysis;

import com.wifisense.model.Measurement;
import com.wifisense.model.TrafficObservation;

import java.util.List;

/** Input shared by every strategy: a chronological window of measurements for one network. */
public record AnalysisContext(long networkId, String ssid, List<Measurement> history,
                              List<TrafficObservation> traffic) {

    public AnalysisContext {
        if (history.isEmpty()) {
            throw new InsufficientDataException("Se necesita al menos una medición");
        }
        history = List.copyOf(history);
        traffic = List.copyOf(traffic);
    }

    public Measurement latest() {
        return history.getLast();
    }

    public List<Measurement> baseline() {
        return history.subList(0, history.size() - 1);
    }

    public boolean simulatedData() {
        return history.stream().anyMatch(Measurement::isSimulated);
    }
}
