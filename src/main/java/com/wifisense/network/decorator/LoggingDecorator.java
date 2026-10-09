package com.wifisense.network.decorator;

import com.wifisense.network.NetworkDataSource;
import com.wifisense.network.NetworkSnapshot;
import com.wifisense.network.NetworkTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingDecorator extends DataSourceDecorator {

    private static final Logger log = LoggerFactory.getLogger(LoggingDecorator.class);

    public LoggingDecorator(NetworkDataSource delegate) {
        super(delegate);
    }

    @Override
    public NetworkSnapshot collect(NetworkTarget target) {
        try {
            NetworkSnapshot snapshot = delegate.collect(target);
            log.debug("Collected from {} for network {}: latency={}ms loss={}%", type(), target.ssid(),
                    snapshot.latencyMs(), snapshot.packetLossPct());
            return snapshot;
        } catch (RuntimeException e) {
            log.warn("Collection from {} for network {} failed: {}", type(), target.ssid(), e.getMessage());
            throw e;
        }
    }
}
