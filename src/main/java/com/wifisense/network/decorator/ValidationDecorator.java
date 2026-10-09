package com.wifisense.network.decorator;

import com.wifisense.network.NetworkDataSource;
import com.wifisense.network.NetworkSnapshot;
import com.wifisense.network.NetworkTarget;

import java.util.ArrayList;
import java.util.List;

/** Rejects physically impossible readings before they reach the database or the analysis. */
public class ValidationDecorator extends DataSourceDecorator {

    public ValidationDecorator(NetworkDataSource delegate) {
        super(delegate);
    }

    @Override
    public NetworkSnapshot collect(NetworkTarget target) {
        NetworkSnapshot snapshot = delegate.collect(target);
        List<String> violations = violations(snapshot);
        if (!violations.isEmpty()) {
            throw new InvalidSnapshotException(violations);
        }
        return snapshot;
    }

    static List<String> violations(NetworkSnapshot s) {
        List<String> errors = new ArrayList<>();
        if (s.latencyMs() < 0) errors.add("latency must be >= 0");
        if (s.jitterMs() < 0) errors.add("jitter must be >= 0");
        if (s.packetLossPct() < 0 || s.packetLossPct() > 100) errors.add("packet loss must be within 0-100");
        if (s.bandwidthMbps() != null && s.bandwidthMbps() < 0) errors.add("bandwidth must be >= 0");
        if (s.signalStrengthDbm() != null && (s.signalStrengthDbm() < -100 || s.signalStrengthDbm() > 0)) {
            errors.add("signal strength must be within -100..0 dBm");
        }
        if (s.connectedDevices() != null && s.connectedDevices() < 0) errors.add("connected devices must be >= 0");
        if (s.collectedAt() == null) errors.add("timestamp is required");
        return errors;
    }
}
