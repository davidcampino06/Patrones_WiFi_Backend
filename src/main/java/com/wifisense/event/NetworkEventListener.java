package com.wifisense.event;

/** Observer: reacts to network events without the publisher knowing who listens. */
public interface NetworkEventListener {

    void onEvent(NetworkEvent event);
}
