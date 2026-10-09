package com.wifisense.event;

import org.springframework.stereotype.Component;

import java.util.List;

/** Subject: notifies every registered listener synchronously, inside the caller's transaction. */
@Component
public class NetworkEventPublisher {

    private final List<NetworkEventListener> listeners;

    public NetworkEventPublisher(List<NetworkEventListener> listeners) {
        this.listeners = List.copyOf(listeners);
    }

    public void publish(NetworkEvent event) {
        listeners.forEach(listener -> listener.onEvent(event));
    }
}
