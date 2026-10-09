package com.wifisense.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Logs every event and keeps the most recent ones in a bounded FIFO queue for the activity feed. */
@Component
public class ActivityLogObserver implements NetworkEventListener {

    public record ActivityEntry(String type, long networkId, String ssid, String description, Instant occurredAt) {
    }

    static final int CAPACITY = 100;
    private static final Logger log = LoggerFactory.getLogger(ActivityLogObserver.class);

    private final Deque<ActivityEntry> recent = new ArrayDeque<>(CAPACITY);

    @Override
    public void onEvent(NetworkEvent event) {
        log.info("[{}] {} - {}", event.getClass().getSimpleName(), event.ssid(), event.describe());
        ActivityEntry entry = new ActivityEntry(event.getClass().getSimpleName(), event.networkId(), event.ssid(),
                event.describe(), event.occurredAt());
        synchronized (recent) {
            if (recent.size() == CAPACITY) {
                recent.removeFirst();
            }
            recent.addLast(entry);
        }
    }

    /** Newest first. */
    public List<ActivityEntry> recentActivity() {
        synchronized (recent) {
            List<ActivityEntry> copy = new ArrayList<>(recent);
            return copy.reversed();
        }
    }
}
