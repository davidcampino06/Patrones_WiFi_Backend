package com.wifisense.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WifiAccessPointTest {

    private static final Instant FIRST = Instant.parse("2026-10-26T20:00:00Z");

    @Test
    void repeatingAScanUpdatesTheSameAccessPointInsteadOfCopyingIt() {
        WifiAccessPoint ap = new WifiAccessPoint(mock(HomeNetwork.class), "a".repeat(24), FIRST);
        ap.seen("Vecino", "TP-Link", "F1", FIRST);
        ap.seen("Vecino 5G", null, null, FIRST.plusSeconds(3600));

        assertThat(ap.getTimesSeen()).isEqualTo(2);
        assertThat(ap.getFirstSeenAt()).isEqualTo(FIRST);
        assertThat(ap.getLastSeenAt()).isEqualTo(FIRST.plusSeconds(3600));
        assertThat(ap.getSsid()).isEqualTo("Vecino 5G");
        assertThat(ap.getVendor()).isEqualTo("TP-Link");  // a scan that could not read the maker does not erase it
        assertThat(ap.getBssidSuffix()).isEqualTo("F1");
    }

    @Test
    void aNetworkThatStopsAnnouncingItsNameIsHidden() {
        WifiAccessPoint ap = new WifiAccessPoint(mock(HomeNetwork.class), "b".repeat(24), FIRST);
        ap.seen("Oficina", null, null, FIRST);
        ap.seen(null, null, null, FIRST.plusSeconds(60));

        assertThat(ap.isHidden()).isTrue();
        assertThat(ap.getSsid()).isNull();
    }

    @Test
    void anOlderResultDoesNotMoveTheLastTimeBack() {
        WifiAccessPoint ap = new WifiAccessPoint(mock(HomeNetwork.class), "c".repeat(24), FIRST);
        ap.seen("Red", null, null, FIRST.plusSeconds(600));
        ap.seen("Red", null, null, FIRST.plusSeconds(300));

        assertThat(ap.getLastSeenAt()).isEqualTo(FIRST.plusSeconds(600));
    }
}
