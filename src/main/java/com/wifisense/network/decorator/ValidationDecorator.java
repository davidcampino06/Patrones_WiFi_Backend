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
        if (s.latencyMs() < 0) errors.add("la latencia no puede ser negativa");
        if (s.jitterMs() < 0) errors.add("el jitter no puede ser negativo");
        if (s.packetLossPct() < 0 || s.packetLossPct() > 100) errors.add("la pérdida de paquetes debe estar entre 0 y 100");
        if (s.bandwidthMbps() != null && s.bandwidthMbps() < 0) errors.add("el ancho de banda no puede ser negativo");
        if (s.signalStrengthDbm() != null && (s.signalStrengthDbm() < -100 || s.signalStrengthDbm() > 0)) {
            errors.add("la señal debe estar entre -100 y 0 dBm");
        }
        if (s.connectedDevices() != null && s.connectedDevices() < 0) errors.add("los dispositivos conectados no pueden ser negativos");
        if (s.collectedAt() == null) errors.add("la fecha de la medición es obligatoria");
        return errors;
    }
}
