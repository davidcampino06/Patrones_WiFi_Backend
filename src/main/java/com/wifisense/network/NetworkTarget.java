package com.wifisense.network;

import com.wifisense.model.Network;

/** What a data source needs to know about a network, without depending on the JPA entity. */
public record NetworkTarget(long networkId, String ssid, String bssid, String frequencyBand) {

    public static NetworkTarget of(Network network) {
        return new NetworkTarget(network.getId(), network.getSsid(), network.getBssid(), network.getFrequencyBand());
    }
}
